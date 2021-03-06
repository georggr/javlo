package org.javlo.component.ecom;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.javlo.actions.IAction;
import org.javlo.component.core.IContentVisualComponent;
import org.javlo.component.properties.AbstractPropertiesComponent;
import org.javlo.context.ContentContext;
import org.javlo.ecom.Basket;
import org.javlo.ecom.Product;
import org.javlo.helper.StringHelper;
import org.javlo.helper.URLHelper;
import org.javlo.i18n.I18nAccess;
import org.javlo.message.GenericMessage;
import org.javlo.message.MessageRepository;
import org.javlo.navigation.MenuElement;
import org.javlo.service.ContentService;
import org.javlo.service.RequestService;


public class ProductComponent extends AbstractPropertiesComponent implements IAction {

	static final List<String> FIELDS = Arrays.asList(new String[] { "name", "price", "vat", "promo", "currency", "offset", "weight", "production", "basket-page" });

	@Override
	public String getHeader() {
		return "Article V 1.0";
	}
	
	@Override
	public List<String> getFields(ContentContext ctx) throws Exception {
		return FIELDS;
	}

	public String getType() {
		return "product";
	}

	public String getName() {
		return getFieldValue("name");
	}

	public double getPrice() {
		return getFieldDoubleValue("price");
	}

	public double getVAT() {
		return getFieldDoubleValue("vat");
	}

	public double getReduction() {
		return getFieldDoubleValue("promo");
	}
	
	public String getCurrency() {
		return getFieldValue("currency", "EUR");
	}
	
	public String getBasketPage() {
		return getFieldValue("basket-page", "");
	}

	public long getOffset() {
		return getFieldLongValue("offset");
	}

	public long getWeight() {
		return getFieldLongValue("weight");
	}

	public long getProduction() {
		return getFieldLongValue("production");
	}

	public long getRealStock(ContentContext ctx) {
		try {
			loadViewData(ctx);
			String value = getViewData(ctx).getProperty("stock");
			return Long.valueOf(value);
		} catch (Exception e) {
			logger.log(Level.FINE, "invalid real stock, setting to zero...");
			setRealStock(ctx, 0);
			return 0;
		}
	}

	public long getVirtualStock(ContentContext ctx) {
		try {
			loadViewData(ctx);
			String value = getViewData(ctx).getProperty("virtual");
			return Long.valueOf(value);
		} catch (Exception e) {
			logger.log(Level.FINE, "invalid virtual stock, setting to zero...");
			setVirtualStock(ctx, 0);
			return 0;
		}
	}

	public void setRealStock(ContentContext ctx, long realStock) {
		try {
			getViewData(ctx).setProperty("stock", String.valueOf(realStock));
			storeViewData(ctx);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setVirtualStock(ContentContext ctx, long virtualStock) {
		try {
			getViewData(ctx).put("virtual", String.valueOf(virtualStock));
			storeViewData(ctx);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected String getEditXHTMLCode(ContentContext ctx) throws Exception {

		StringWriter writer = new StringWriter();
		PrintWriter out = new PrintWriter(writer);

		List<String> fields = getFields(ctx);

		out.println("<div class=\"edit\" style=\"padding: 3px;\">");
		
		for (String field : fields) {
			renderField(ctx, out, field, getRowSize(field), getFieldValue(field));
		}
		renderField(ctx, out, "stock", 1, getRealStock(ctx));
		renderField(ctx, out, "virtual", 1, getVirtualStock(ctx));

		out.println("</div>");

		out.flush();
		out.close();
		return writer.toString();
	}
	private void renderField(ContentContext ctx, PrintWriter out, String field, int rowSize, Object value) throws Exception {
		I18nAccess i18nAccess = I18nAccess.getInstance(ctx.getRequest());

		String fieldId = createKeyWithField(field);
		
		out.println("<div class=\"field-label\">");
		out.println("<label for=\"" + fieldId + "\">" + i18nAccess.getText("field." + field) + "</label>");
		out.println("</div>");
		out.println("<div class=\"field-input\">");
		out.print("<textarea rows=\"" + rowSize + "\" id=\"" + fieldId + "\" name=\"" + fieldId + "\">");
		out.print(String.valueOf(value));
		out.println("</textarea>");
		out.println("</div>");
	}

	@Override
	public void performEdit(ContentContext ctx) throws Exception {
		super.performEdit(ctx);
		
		RequestService requestService = RequestService.getInstance(ctx.getRequest());

		String stockValue = requestService.getParameter(createKeyWithField("stock"), null);
		try {
			long stock = Long.valueOf(stockValue);
			if (stock != getRealStock(ctx)) {
				setRealStock(ctx, stock);
			}
		} catch (Exception e) {
		}
		String virtualValue = requestService.getParameter(createKeyWithField("virtual"), null);
		try {
			long virtual = Long.valueOf(virtualValue);
			if (virtual != getVirtualStock(ctx)) {
				setVirtualStock(ctx, virtual);
			}
		} catch (Exception e) {
		}
	}

	@Override
	public String getViewXHTMLCode(ContentContext ctx) throws Exception {
		if (getOffset() > 0) {
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			PrintStream out = new PrintStream(outStream);
			
			String action;
			if (getBasketPage().trim().length() > 0) {
				ContentService content = ContentService.getInstance(ctx.getRequest());
				MenuElement page = content.getNavigation(ctx).searchChildFromName(getBasketPage());
				if (page != null) {
					action = URLHelper.createURL(ctx,page);
				} else {
					action = URLHelper.createURL(ctx);
				}
			} else {
				action = URLHelper.createURL(ctx);
			}

			out.println("<form role=\"form\" class=\"form-inline add-basket\" id=\"product-"+getName()+"_"+getId()+"\" method=\"post\" action=\""+action+"\">");
			out.println("<input type=\"hidden\" name=\"webaction\" value=\"products.buy\" />");
			out.println("<input type=\"hidden\" name=\"cid\" value=\""+getId()+"\" />");
			I18nAccess i18nAccess = I18nAccess.getInstance(ctx);
			
			out.println("<div class=\"list-group\"><div class=\"line list-group-item name\">");		
			out.println("<span>"+getName()+"</span>");
			out.println("</div>");
			
			out.println("<div class=\"line list-group-item price\">");
			out.println("<span class=\"label\">" + i18nAccess.getViewText("ecom.price") + "</span> <span class=\"badge\">"+getPrice() + "&nbsp;" + getCurrency() + "</span>");
			out.println("</div>");
			
			out.println("<div class=\"line list-group-item stock\">");
			out.println("<span class=\"label\">" + i18nAccess.getViewText("ecom.stock") + "</span> <span class=\"badge\">"+getRealStock(ctx)+"</span>");
			out.println("</div>");

			
			out.println("</div>");

			if (getVirtualStock(ctx) > getOffset()) {
				out.println("<div class=\"line form-group quantity\">");
				String Qid = "product-"+StringHelper.getRandomId();
				out.println("<label for=\""+Qid+"\"><span>"+i18nAccess.getViewText("ecom.quantity")+"</span></label>");
				out.println("<input class=\"form-control digit\" id=\""+Qid+"\" type=\"text\" name=\"quantity\" value=\"" + getOffset() + "\" maxlength=\"3\"/>");

				out.println("<span class=\"buy\"><input class=\"btn btn-default buy\" type=\"submit\" name=\"buy\" value=\""+i18nAccess.getViewText("ecom.buy")+"\" /></span>");
				out.println("</div>");
			} else {
				out.println("<span class=\"soldout\">"+i18nAccess.getViewText("ecom.soldout")+"</span>");
			}
			out.println("</form>");
			
			out.close();
			return new String(outStream.toByteArray());
		} else {
			return "";
		}
	}
	
	@Override
	public String getHexColor() {
		return ECOM_COLOR;
	}
	
	@Override
	public String getActionGroupName() {
		return "products";
	}
	
	public static String performBuy(RequestService rs, ContentContext ctx, MessageRepository messageRepository, I18nAccess i18nAccess) throws Exception {		
		
		ContentService content = ContentService.getInstance(ctx.getRequest());
		MenuElement currentPage = ctx.getCurrentPage();

		/* information from product */
		String cid = rs.getParameter("cid", null);
		if (cid != null) {
			IContentVisualComponent comp = content.getComponent(ctx, cid);
			if ((comp != null) && (comp instanceof ProductComponent)) {
				ProductComponent pComp = (ProductComponent) comp;
				Product product = new Product(pComp);

				/* information from page */
				product.setUrl(URLHelper.createURL(ctx, currentPage.getPath()));
				product.setShortDescription(currentPage.getTitle(ctx));
				product.setLongDescription(currentPage.getDescription(ctx));
				if (currentPage.getImage(ctx) != null) {
					product.setImage(ctx, currentPage.getImage(ctx));
				}

				String quantity = rs.getParameter("quantity", null);
				if (quantity != null) {
					int quantityValue = Integer.parseInt(quantity);

					quantityValue = quantityValue - (quantityValue % (int) pComp.getOffset());
					product.setQuantity(quantityValue);

					Basket basket = Basket.getInstance(ctx);
					basket.addProduct(product);
					
					String msg = i18nAccess.getViewText("ecom.product.add", new String[][] {{"product", pComp.getName()}});					
					messageRepository.setGlobalMessage(new GenericMessage(msg, GenericMessage.INFO));
				}
			}
		}

		return null;
	}
	
}
