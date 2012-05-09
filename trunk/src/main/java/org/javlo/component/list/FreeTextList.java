/*
 * Created on 19-sept.-2003 
 */
package org.javlo.component.list;

import java.util.List;

import org.javlo.component.text.Paragraph;
import org.javlo.context.ContentContext;
import org.javlo.context.GlobalContext;
import org.javlo.helper.StringHelper;
import org.javlo.i18n.I18nAccess;
import org.javlo.service.RequestService;
import org.javlo.service.ReverseLinkService;
import org.javlo.utils.SufixPreffix;

/**
 * @author pvandermaesen
 */
public class FreeTextList extends Paragraph {

	public static final String NUMBER_LIST = "ol-ol";

	public static final String TYPE = "text-list";

	@Override
	public String getStyleTitle(ContentContext ctx) {
		try {
			I18nAccess i18nAccess = I18nAccess.getInstance(ctx.getRequest());
			return i18nAccess.getText("content.text-list.style-title");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "style";
	}

	@Override
	public String[] getStyleList(ContentContext ctx) {
		return new String[] { "ul-ul", NUMBER_LIST, "ul-ol", "ol-ul" };
	}

	@Override
	public String[] getStyleLabelList(ContentContext ctx) {

		String ulul = "unsorted list";
		String olol = "cols title";
		String lilo = "rows title";
		String olli = "no title";

		try {
			I18nAccess i18nAccess = I18nAccess.getInstance(ctx.getRequest());
			ulul = i18nAccess.getText("content.text-list.ulul");
			olol = i18nAccess.getText("content.text-list.olol");
			lilo = i18nAccess.getText("content.text-list.lilo");
			olli = i18nAccess.getText("content.text-list.olli");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new String[] { ulul, olol, lilo, olli };
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public String getPrefixViewXHTMLCode(ContentContext ctx) {
		return "";
	}

	@Override
	public String getSufixViewXHTMLCode(ContentContext ctx) {
		return "";
	}

	@Override
	public String getViewXHTMLCode(ContentContext ctx) throws Exception {
		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
		ReverseLinkService reverserLinkService = ReverseLinkService.getInstance(globalContext);
		String value = reverserLinkService.replaceLink(ctx, getValue());
		String sep = "";
		if (value.length() > 3) {
			if (value.startsWith("{")) {
				sep = "" + value.charAt(1);
				value = value.substring(3);
			}
		}
		value = applyReplacement(value);
		value = StringHelper.textToList(value, sep, getStyle(ctx), true, globalContext);

		return "<div " + getSpecialPreviewCssClass(ctx, getStyle(ctx)+' '+getType()) + getSpecialPreviewCssId(ctx) + ">" + value + "</div>";
	}

	@Override
	public boolean isInline() {
		return false;
	}

	public String getSeparatorInputName() {
		return "separator-" + getId();
	}

	public String getSeparatorValue() {
		String value = getValue();
		if (value != null) {
			if (value.length() >= 3) {
				if (value.startsWith("{")) {
					return "" + value.charAt(1);
				}
			}
		}
		return "";
	}

	@Override
	protected String getEditXHTMLCode(ContentContext ctx) throws Exception {
		StringBuffer finalCode = new StringBuffer();
		I18nAccess i18nAccess = I18nAccess.getInstance(ctx.getRequest());
		finalCode.append(getSpecialInputTag());
		finalCode.append("<div class=\"line\">");
		finalCode.append("<label for=\"" + getSeparatorInputName() + "\">" + i18nAccess.getText("content.text-list.separator") + " :</label>");
		finalCode.append("<input id=\"" + getSeparatorInputName() + "\" name=\"" + getSeparatorInputName() + "\" value=\"" + getSeparatorValue() + "\" />");
		finalCode.append("</div>");
		finalCode.append("<textarea id=\"" + getContentName() + "\" name=\"" + getContentName() + "\"");
		finalCode.append(" rows=\"" + (countLine() + 1) + "\" onkeyup=\"javascript:resizeTextArea($('" + getContentName() + "'));\">");
		String value = getValue();
		if (getSeparatorValue().length() > 0) {
			value = value.substring(3);
		}
		finalCode.append(value);
		finalCode.append("</textarea>");

		return finalCode.toString();
	}

	@Override
	public void refresh(ContentContext ctx) throws Exception {
		RequestService requestService = RequestService.getInstance(ctx.getRequest());
		String newContent = requestService.getParameter(getContentName(), null);
		String sepValue = requestService.getParameter(getSeparatorInputName(), "");
		if (sepValue.length() > 0) {
			sepValue = "" + sepValue.charAt(0);
		}

		if (newContent != null) {
			if (sepValue.length() > 0) {
				newContent = '{' + sepValue + '}' + newContent;
			}
			if (!componentBean.getValue().equals(newContent)) {
				componentBean.setValue(newContent);
				setModify();
			}
		}
	}
	
	@Override
	public List<SufixPreffix> getMarkerList(ContentContext ctx) {
		return getItalicAndStrongLanguageMarkerList(ctx);
	}
	

}
