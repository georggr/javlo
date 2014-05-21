package org.javlo.ecom;

import java.beans.Transient;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.javlo.context.ContentContext;
import org.javlo.ecom.Product.ProductBean;
import org.javlo.helper.ResourceHelper;
import org.javlo.helper.StringHelper;
import org.javlo.helper.URLHelper;
import org.javlo.utils.CSVFactory;

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
	private List<Product.ProductBean> productsBean = null;
	private String token;
	private String payerID;
	private String validationInfo;
	private transient Object transactionManager;
	private boolean deleted;
	private String firstName = "";
	private String lastName = "";
	private String organization = "";
	private String vatNumber = "";
	private boolean pickup = false;
	private String address = "";
	private String country;
	private String zip;
	private String city;
	private String info;
	private String user;
	private String transfertAddressLogin = "";
	private String description;
	private boolean presumptiveFraud = false;

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
		productsBean = null;
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

	public double getTotalIncludingVAT() {
		double result = 0;
		for (Product.ProductBean product : getProductsBean()) {
			result = result + (product.getPrice() * (1 - product.getReduction()) * product.getQuantity());
		}
		return result + getDeliveryIncludingVAT();
	}

	public String getTotalIncludingVATString() {
		return StringHelper.renderPrice(getTotalIncludingVAT(), getCurrencyCode());
	}

	public double getTotalExcludingVAT() {
		double result = 0;
		for (Product.ProductBean product : getProductsBean()) {
			result = result + (((product.getPrice()) * (1 - product.getReduction()) * product.getQuantity()) / (1 + product.getVAT()));
		}
		return result + getDeliveryExcludingVAT();
	}

	public String getTotalExcludingVATString() {
		return StringHelper.renderPrice(getTotalExcludingVAT(), getCurrencyCode());
	}

	public double getDeliveryIncludingVAT() {
		double result = 0;
		if (!pickup) {
			if (getDeliveryZone() == null || getDeliveryZone().getPrices() != null && getDeliveryZone().getPrices().size() > 0) {

				// pick url should exist here, assert ?
				if (getDeliveryZone() != null) {
					int number = 0;
					for (Product product : products) {
						number = number + (product.getQuantity() * (int) product.getWeight());
						// result = result + ((product.getPrice() / (1 +
						// product.getVAT())) * (1 -
						// product.getReduction())*product.getQuantity());
					}

					// TODO: ensure increasing order in zones file

					Set<Integer> offsets = new TreeSet<Integer>(getDeliveryZone().getPrices().keySet());
					int up = 0;
					for (int offset : offsets) {
						up = offset;
						if (up >= number) {
							break;
						}
					}
					if (up > 0) {
						int units = number / up;
						if (number > up) {
							units = units + 1;
						}
						result = units * getDeliveryZone().getPrices().get(up);
					}
				} else {
					return defaultDeleviry;
				}
			}
		}
		return result;
	}

	public double getDeliveryExcludingVAT() {
		return getDeliveryIncludingVAT() / 1.21;
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

	public boolean isPickup() {
		return pickup;
	}

	public void setPickup(boolean pickup) {
		this.pickup = pickup;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	private DeliveryZone zone;

	public DeliveryZone getDeliveryZone() {
		return zone;
	}

	public void setDeliveryZone(DeliveryZone zone) {
		this.zone = zone;
	}

	private List<DeliveryZone> zones;
	
	private double defaultDeleviry = 0;

	public List<DeliveryZone> getDeliveryZones(ContentContext ctx) {
		
		if (zones == null) {
			try {
				File zoneFile = new File(URLHelper.mergePath(ctx.getCurrentTemplate().getTemplateRealPath(),"ecom_zones.csv"));;
				if (!zoneFile.exists()) {
					return null;
				}
				InputStream in = new FileInputStream(zoneFile);

				CSVFactory fact = new CSVFactory(in);
				String[][] csv = fact.getArray();

				zones = new ArrayList<DeliveryZone>();
				int i = 1;
				while (i < csv.length) {
					String zone = csv[i][0];
					String pickupURL = csv[i][4];								
					if (!csv[i][1].trim().equals("")) {
						zones.add(new DeliveryZone(zone, csv[i][1], ctx));
						i++;
					} else {
						Map<Integer, Float> prices = new HashMap<Integer, Float>();
						do {
							int offset = Integer.valueOf(csv[i][2]);
							float price = Float.valueOf(csv[i][3]);
							prices.put(offset, price);
							i++;	
							
							if (zone.trim().toLowerCase().equals("default")) {
								defaultDeleviry = price;
							}							
						} while (i < csv.length && csv[i][0].equals(""));
						DeliveryZone newZone = new DeliveryZone(zone, prices, ctx);
						if (pickupURL != null && pickupURL.length() > 0) {
							newZone.setPickupURL(pickupURL);
						}
						zones.add(newZone);
					}
				}
				zones.add(new DeliveryZone("other", "", ctx));
			} catch (Exception e) {
				e.printStackTrace();
				zones = Collections.emptyList();
			}
		}
		return zones;
	}

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

	public DeliveryZone getZone() {
		return zone;
	}

	public void setZone(DeliveryZone zone) {
		this.zone = zone;
	}

	public List<DeliveryZone> getZones() {
		return zones;
	}

	public void setZones(List<DeliveryZone> zones) {
		this.zones = zones;
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
		Product product = new Product(null);
		product.setFakeName("article-1");
		basket.addProduct(product);
		product = new Product(null);
		product.setFakeName("article-2");
		basket.addProduct(product);
		System.out.println("size:" + basket.getProductsBean().size());
		System.out.println(ResourceHelper.storeBeanFromXML(basket));
		System.out.println();
		// System.out.println(ResourceHelper.storeBeanFromXML(basket.getProductsBean().iterator().next()));
	}

	public List<Product.ProductBean> getProductsBean() {
		if (productsBean == null) {
			productsBean = new LinkedList<Product.ProductBean>();
			for (Product product : getProducts()) {
				productsBean.add(product.getBean());
			}
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

	public void setProductsBean(List<Product.ProductBean> productsBean) {
		this.productsBean = productsBean;
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
		out.println("total Ex. VAT : " + getTotalExcludingVATString());
		out.println("total In. VAT : " + getTotalIncludingVATString());
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

}