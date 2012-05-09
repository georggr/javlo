package org.javlo.fields;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.javlo.context.ContentContext;
import org.javlo.context.GlobalContext;
import org.javlo.helper.PatternHelper;
import org.javlo.helper.StringHelper;
import org.javlo.service.RequestService;

public class FieldExternalLink extends Field {

	protected String getLabelLabel() {
		return getI18nAccess().getText("global.label");
	}

	protected String getLinkLabel() {
		return getI18nAccess().getText("global.link");
	}

	public String getInputLinkName() {
		return getName() + "-link-" + getId();
	}

	public String getInputLabelName() {
		return getName() + "-label-" + getId();
	}

	public String getViewXHTMLCode(ContentContext ctx) throws Exception {
		StringWriter writer = new StringWriter();
		PrintWriter out = new PrintWriter(writer);
		
		String displayStr = StringHelper.neverNull(getCurrentLink());
		if (displayStr.trim().length() == 0 || !isViewDisplayed()) {
			return "";
		}

		String label = getCurrentLabel();
		if (label.trim().length() == 0) {
			label = getCurrentLink();
		}

		if (label.trim().length() > 0) {
			out.println("<span class=\"" + getType() + "\">");
			String target = "";
			if (GlobalContext.getInstance(ctx.getRequest()).isOpenExernalLinkAsPopup(getCurrentLink())) {
				target = " target=\"_blank\"";
			}
			out.println("<a href=\"" + getCurrentLink() + "\""+target+">" + label + "</a>");
			out.println("</span>");
		}

		out.close();
		return writer.toString();
	}

	public String getEditXHTMLCode(ContentContext ctx) throws Exception {
		StringWriter writer = new StringWriter();
		PrintWriter out = new PrintWriter(writer);

		out.println("<fieldset>");
		out.println("<legend>" + getUserLabel(new Locale(ctx.getRequestContentLanguage())) + "</legend>");

		out.println("<div class=\"line\">");
		out.println("<label for=\"" + getInputLinkName() + "\">" + getLinkLabel() + " : </label>");
		out.println("<input id=\"" + getInputLinkName() + "\" name=\"" + getInputLinkName() + "\" value=\"" + StringHelper.neverNull(getCurrentLink()) + "\"/>");
		if (getCurrentLinkErrorMessage().trim().length() > 0) {
			out.println("<div class=\"error-message\">");
			out.println(getCurrentLinkErrorMessage());
			out.println("</div>");
		}
		out.println("</div>");

		out.println("<div class=\"line\">");
		out.println("<label for=\"" + getInputLabelName() + "\">" + getLabelLabel() + " : </label>");
		out.println("<input id=\"" + getInputLabelName() + "\" name=\"" + getInputLabelName() + "\" value=\"" + StringHelper.neverNull(getCurrentLabel()) + "\"/>");
		out.println("</div>");

		out.println("</fieldset>");

		out.close();
		return writer.toString();
	}

	@Override
	public boolean process(HttpServletRequest request) {
		RequestService requestService = RequestService.getInstance(request);

		boolean modify = false;

		String newLabel = requestService.getParameter(getInputLabelName(), "");
		if (!newLabel.equals(getCurrentLabel())) {
			setCurrentLabel(newLabel);
			modify = true;
		}

		String newLink = requestService.getParameter(getInputLinkName(), "");
		if (!newLink.equals(getCurrentLink())) {

			if (!PatternHelper.EXTERNAL_LINK_PATTERN.matcher(newLink).matches()) {
				if (getCurrentLinkErrorMessage().trim().length() == 0) {
					setNeedRefresh(true);
				}
				setCurrentLinkErrorMessage(getI18nAccess().getText("component.error.external-link"));
			} else {
				if (getCurrentLinkErrorMessage().trim().length() > 0) {
					setNeedRefresh(true);
				}
				setCurrentLinkErrorMessage("");
			}

			setCurrentLink(newLink);
			modify = true;
		}

		return modify;
	}

	public String getType() {
		return "external-link";
	}

	/* values */

	protected String getCurrentLink() {
		return properties.getProperty("field." + getUnicName() + ".value.link", "");
	}

	protected void setCurrentLink(String link) {
		properties.setProperty("field." + getUnicName() + ".value.link", link);
	}

	protected String getCurrentLinkErrorMessage() {
		return properties.getProperty("field." + getUnicName() + ".message.link", "");
	}

	protected void setCurrentLinkErrorMessage(String message) {
		properties.setProperty("field." + getUnicName() + ".message.link", message);
	}

	protected String getCurrentLabel() {
		return properties.getProperty("field." + getUnicName() + ".value.label", "");
	}

	protected void setCurrentLabel(String label) {
		properties.setProperty("field." + getUnicName() + ".value.label", label);
	}

}
