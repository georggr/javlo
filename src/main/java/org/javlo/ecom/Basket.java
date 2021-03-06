package org.javlo.ecom;

import java.beans.Transient;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.javlo.context.ContentContext;
import org.javlo.ecom.Product.ProductBean;
import org.javlo.helper.StringHelper;
import org.javlo.helper.URLHelper;
import org.javlo.module.ecom.DeliveryPrice;

public class Basket implements Serializable {

	public static final int START_STEP = 1;
	public static final int REGISTRATION_STEP = 2;
	public static final int ORDER_STEP = 3;
	public static final int CONFIRMATION_STEP = 4;
	public static final int FINAL_STEP = 5;
	public static final int ERROR_STEP = 99;

	public static final String STATUS_UNVALIDED = "unvalided";
	public static final String STATUS_VALIDED = "valided";
	public static final String STATUS_TO_BE_VERIFIED = "to_be_verified";
	public static final String STATUS_MANUAL_PAYED = "manual_payed";
	public static final String STATUS_NEW = "new";
	public static final String STATUS_WAIT_PAY = "wait_pay";
	public static final String STATUS_SENDED = "sended";

	private List<Product> products = new LinkedList<Product>();

	private boolean valid = false;
	private boolean confirm = false;

	private String id = null;
	private String contactEmail = "";
	private String contactPhone = "";
	private Date date = new Date();
	private String status = STATUS_NEW;
	private String token;
	private String payerID;
	private String validationInfo;
	private transient Object transactionManager;
	private boolean deleted;
	private String firstName = "";
	private String lastName = "";
	private String organization = "";
	private String vatNumber = "";
	private String address = "";
	private String country = null;
	private String zip;
	private String city;
	private String info;
	private String user;
	private String transfertAddressLogin = "";
	private String description;
	private boolean presumptiveFraud = false;
	private double userReduction = 0;
	private boolean noShipping = false;

	private int step = START_STEP;

	public static final String KEY = "basket";

	public static class PayementServiceBean {
		private PayementExternalService service;
		private String url;

		private PayementServiceBean(PayementExternalService inService, String inURL) {
			this.service = inService;
			this.url = inURL;
		}

		public String getName() {
			return service.getName();
		}

		public String getURL() {
			if (url == null) {
				return service.getURL();
			} else {
				return url;
			}
		}
	}

	private final List<PayementServiceBean> payementServices = new LinkedList<Basket.PayementServiceBean>();

	public static void setInstance(ContentContext ctx, Basket basket) {
		ctx.getRequest().getSession().setAttribute(KEY, basket);
	}

	public static String renderPrice(ContentContext ctx, double price, String currency) {
		if (currency == null || currency.equalsIgnoreCase("EUR")) {
			currency = "EUR";
		} else if (currency.equalsIgnoreCase("USD")) {
			currency = "$";
		}
		return StringHelper.renderDouble(price, ctx.getLocale()) + ' ' + currency;
	}

	public static Basket getInstance(ContentContext ctx) {
		Basket basket = (Basket) ctx.getRequest().getSession().getAttribute(KEY);
		if (basket == null) {
			basket = new Basket();
			for (PayementExternalService service : EcomService.getInstance(ctx.getGlobalContext(), ctx.getRequest().getSession()).getExternalService()) {
				String url = service.getURL();
				if ((url == null || url.trim().length() == 0) && service.getReturnPage() != null && service.getReturnPage().trim().length() > 0) {
					try {
						url = URLHelper.createURLFromPageName(ctx, service.getReturnPage());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				basket.payementServices.add(new PayementServiceBean(service, url));
				basket.setDescription(ctx.getGlobalContext().getGlobalTitle());
			}
			ctx.getRequest().getSession().setAttribute(KEY, basket);
		}
		basket.setUser(ctx.getCurrentUserId());
		return basket;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public void reset(ContentContext ctx) {
		ctx.getRequest().setAttribute("reset", "true");
		ctx.getRequest().getSession().removeAttribute(KEY);
	}

	public static boolean isInstance(ContentContext ctx) {
		return ctx.getRequest().getSession().getAttribute(KEY) != null;
	}

	public void payAll(ContentContext ctx) {
		for (Product product : products) {
			product.pay(ctx);
		}
	}

	public void reserve(ContentContext ctx) {
		for (Product product : products) {
			product.reserve(ctx);
		}
	}

	@Transient
	public List<Product> getProducts() {
		return products;
	}

	public void addProduct(Product product) {
		setValid(false);
		for (Product item : products) {
			if (item.getName().equals(product.getName()) && item.getPrice() == product.getPrice()) {
				item.setQuantity(item.getQuantity() + product.getQuantity());
				return;
			}
		}
		products.add(product);
		
	}

	public void removeProduct(String id) {
		Iterator<Product> iter = products.iterator();
		while (iter.hasNext()) {
			if (iter.next().getId().equals(id)) {
				setValid(false);
				iter.remove();
			}
		}
	}

	public int getProductCount() {
		int count = 0;
		for (Product product : products) {
			count = count + product.getQuantity();
		}
		return count;
	}

	public double getTotal(ContentContext ctx, boolean vat) {
		double result = 0;
		for (Product product : getProducts()) {
			double vatFactor = 1;
			if (!vat) {
				vatFactor = 1 + product.getVAT();
			}
			result = result + (product.getPrice() * (1 - product.getReduction()) * product.getQuantity()) / vatFactor;
		}
		result = result * (1 - getUserReduction()) + getDelivery(ctx, vat);
		return result;
	}

	public String getTotalString(ContentContext ctx, boolean vat) {
		return renderPrice(ctx, getTotal(ctx, vat), getCurrencyCode());
	}

	public String getVAT(ContentContext ctx) {
		return renderPrice(ctx, getTotal(ctx, true) - getTotal(ctx, false), getCurrencyCode());
	}

	public String getDeliveryZone() {
		return getCountry();
	}

	public double getDelivery(ContentContext ctx, boolean vat) {
		if (getDeliveryZone() == null || ctx == null || isNoShipping()) {
			return 0;
		}
		DeliveryPrice priceList = null;
		try {
			priceList = DeliveryPrice.getInstance(ctx);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (priceList != null) {
			double delivery = 0;
			try {
				double totalWeight = 0;
				double vatFactor = 1;
				for (Product product : products) {
					if (!vat) {
						vatFactor = 1 + product.getVAT();
					}
					totalWeight = totalWeight + product.getQuantity() * product.getWeight();
				}
				delivery += priceList.getPrice(totalWeight, getDeliveryZone()) / vatFactor;
			} catch (Exception e) {
				e.printStackTrace();
				return getEcomService(ctx).getDefaultDelivery();
			}
			return delivery;
		} else {
			return getEcomService(ctx).getDefaultDelivery();
		}
	}

	protected EcomService getEcomService(ContentContext ctx) {
		return EcomService.getInstance(ctx.getGlobalContext(), ctx.getRequest().getSession());
	}

	public int getSize() {
		return products.size();
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public void init(ContentContext ctx) {
		ctx.getRequest().getSession().removeAttribute(KEY);
	}

	public String getId() {
		if (id == null) {
			id = StringHelper.getShortRandomId();
		}
		return id;
	}

	public String getStructutedCommunication() {
		return StringHelper.encodeAsStructuredCommunicationMod97(getId());
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCurrencyCode() {
		String currencyCode = null;
		for (Product.ProductBean product : getProductsBean()) {
			if ((currencyCode != null) && (!currencyCode.equals(product.getCurrencyCode()))) {
				return null;
			}
			currencyCode = product.getCurrencyCode();
		}
		return currencyCode;
	}

	public boolean isConfirm() {
		return confirm;
	}

	public void setConfirm(boolean confirm) {
		this.confirm = confirm;
	}

	public String getContactEmail() {
		return contactEmail;
	}

	public void setContactEmail(String contactEmail) {
		this.contactEmail = contactEmail;
	}

	public String getContactPhone() {
		return contactPhone;
	}

	public void setContactPhone(String contactPhone) {
		this.contactPhone = contactPhone;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public String getVATNumber() {
		return vatNumber;
	}

	public void setVATNumber(String vatNumber) {
		this.vatNumber = vatNumber;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	private double defaultDelivery = 0;

	public String getVatNumber() {
		return vatNumber;
	}

	public void setVatNumber(String vatNumber) {
		this.vatNumber = vatNumber;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public void setProducts(List<Product> products) {		
		this.products = products;
	}

	public int getStep() {
		return step;
	}

	public void setStep(int step) {
		this.step = step;
	}

	public List<PayementServiceBean> getServices() {
		return payementServices;
	}

	public Date getDate() {
		return date;
	}

	public String getDateString() {
		return StringHelper.renderSortableTime(date);
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public static void main(String[] args) {
		Basket basket = new Basket();
		basket.setInfo("Basket info");
		basket.setOrganization("Terrieur SA");
		basket.setFirstName("Alain");
		basket.setLastName("Terrieur");
		basket.setAddress("13, Rue de la Folie");
		basket.setZip("1000");
		basket.setCity("Bruxelles");
		basket.setCountry("be");
		basket.setContactEmail("alain@terrieur.com");
		basket.setContactPhone("0123456789");
		List<ProductBean> products = new LinkedList<ProductBean>();
		ProductBean product = new ProductBean();
		product.setId("ID-ART-001");
		product.setName("Article 1");
		product.setDescription("Short Desc article 1");
		product.setPrice(12);
		product.setCurrencyCode("EUR");
		product.setQuantity(2);
		product.setVAT(0.21);
		product.setReduction(0);
		products.add(product);
		product = new ProductBean();
		product.setId("ID-ART-002");
		product.setName("Article 2");
		product.setDescription("Short Desc article 2");
		product.setPrice(25.1);
		product.setCurrencyCode("EUR");
		product.setQuantity(3);
		product.setVAT(0.21);
		product.setReduction(0);
		products.add(product);

		System.out.println("TOTAL TVAC  = " + basket.getTotal(null, true));
		System.out.println("TOTAL HTVAC = " + basket.getTotal(null, false));

	}

	public List<Product.ProductBean> getProductsBean() {
		List<Product.ProductBean> productsBean = new LinkedList<Product.ProductBean>();
		for (Product product : getProducts()) {
			productsBean.add(product.getBean());
		}
		return productsBean;
	}

	public String getProductsBeanToString() {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(outStream);
		for (Product.ProductBean product : getProductsBean()) {
			out.print("[" + product.getQuantity() + "-" + product.getName() + "] ");
		}
		out.close();
		return new String(outStream.toByteArray());
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getPayerID() {
		return payerID;
	}

	public void setPayerID(String payerID) {
		this.payerID = payerID;
	}

	public String getValidationInfo() {
		return validationInfo;
	}

	public void setValidationInfo(String validationInfo) {
		this.validationInfo = validationInfo;
	}

	/**
	 * reference to the ecom transaction manager (transiant)
	 * 
	 * @return
	 */
	public Object getTransactionManager() {
		return transactionManager;
	}

	public void setTransactionManager(Object transactionManager) {
		this.transactionManager = transactionManager;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	@Override
	public String toString() {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(outStream);
		out.println("basket");
		out.println("======");
		out.println("");
		out.println("id : " + getId());
		out.println("user : " + getUser());
		out.println("Currency : " + getCurrencyCode());
		out.println("Date : " + StringHelper.renderSortableTime(getDate()));
		out.println("Step : " + getStep());
		out.println("Size : " + getSize());
		out.println("Status : " + getStatus());
		out.println("");
		out.println("User:");
		out.println("  firstName : " + getFirstName());
		out.println("  lastName : " + getLastName());
		out.println("  email : " + getContactEmail());
		out.println("  phone : " + getContactPhone());
		out.println("  adress : " + getAddress());
		out.println("  zip : " + getZip());
		out.println("  city : " + getCity());
		out.println("  country : " + getCountry());
		if (getOrganization() != null && getOrganization().trim().length() > 0) {
			out.println("  Organization : " + getOrganization());
		}
		if (getVATNumber() != null && getVATNumber().trim().length() > 0) {
			out.println("  VAT Number : " + getVATNumber());
		}
		out.println("");
		out.println("Product :");
		for (ProductBean product : getProductsBean()) {
			out.println("   " + product);
		}
		out.println("");
		out.println("TOTAL :");
		out.println("Shiping VAT : " + getDelivery(null, valid));
		out.println("");
		out.println("Current Time : " + StringHelper.renderSortableTime(new Date()));
		out.println("");

		out.close();
		return new String(outStream.toByteArray());
	}

	public String getAdministratorEmail(ContentContext ctx) {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(outStream);
		out.println("basket");
		out.println("======");
		out.println("");
		out.println("id : " + getId());
		out.println("user : " + getUser());
		out.println("Currency : " + getCurrencyCode());
		out.println("Date : " + StringHelper.renderSortableTime(getDate()));
		out.println("Step : " + getStep());
		out.println("Size : " + getSize());
		out.println("Status : " + getStatus());
		out.println("");
		out.println("User:");
		out.println("  firstName : " + getFirstName());
		out.println("  lastName : " + getLastName());
		out.println("  email : " + getContactEmail());
		out.println("  phone : " + getContactPhone());
		out.println("  adress : " + getAddress());
		out.println("  zip : " + getZip());
		out.println("  city : " + getCity());
		out.println("  country : " + getCountry());
		if (getOrganization() != null && getOrganization().trim().length() > 0) {
			out.println("  Organization : " + getOrganization());
		}
		if (getVATNumber() != null && getVATNumber().trim().length() > 0) {
			out.println("  VAT Number : " + getVATNumber());
		}
		out.println("");
		out.println("Product :");
		for (ProductBean product : getProductsBean()) {
			out.println("   " + product);
		}
		out.println("");
		out.println("Shiping VAT : " + StringHelper.renderPrice(getDelivery(ctx, true), getCurrencyCode()));
		out.println("Shiping HVAT : " + StringHelper.renderPrice(getDelivery(ctx, false), getCurrencyCode()));
		out.println("");
		out.println("Total VAT : " + StringHelper.renderPrice(getTotal(ctx, true), getCurrencyCode()));
		out.println("Total HVAT : " + StringHelper.renderPrice(getTotal(ctx, false), getCurrencyCode()));
		out.println("");
		out.println("Current Time : " + StringHelper.renderSortableTime(new Date()));
		out.println("");

		out.close();
		return new String(outStream.toByteArray());
	}

	public String getTransfertAddressLogin() {
		return transfertAddressLogin;
	}

	/**
	 * set the username of user that we have transfert address info.
	 * 
	 * @param login
	 */
	public void setTransfertAddressLogin(String login) {
		this.transfertAddressLogin = login;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isDisplayInfo() {
		return getStep() < FINAL_STEP;
	}

	public boolean isReadyToSend() {
		return getStatus().equals(STATUS_VALIDED) || getStatus().equals(STATUS_MANUAL_PAYED);
	}

	public boolean isPresumptiveFraud() {
		return presumptiveFraud;
	}

	public void setPresumptiveFraud(boolean presumptiveFraud) {
		this.presumptiveFraud = presumptiveFraud;
	}

	public double getUserReduction() {
		return userReduction;
	}

	public void setUserReduction(double userReduction) {
		this.userReduction = userReduction;
	}

	public boolean isNoShipping() {
		return noShipping;
	}

	public void setNoShipping(boolean noShipping) {
		this.noShipping = noShipping;
	}

}
