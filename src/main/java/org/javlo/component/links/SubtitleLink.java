package org.javlo.component.links;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.LinkedList;

import org.javlo.component.core.AbstractVisualComponent;
import org.javlo.component.core.ContentElementList;
import org.javlo.component.core.IContentVisualComponent;
import org.javlo.component.core.ISubTitle;
import org.javlo.context.ContentContext;
import org.javlo.navigation.MenuElement;

public class SubtitleLink extends AbstractVisualComponent {

	public static class Link {
		private String url;
		private String label;
		private String level;

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public String getLevel() {
			return level;
		}

		public void setLevel(String level) {
			this.level = level;
		}
	}

	public static final String TYPE = "subtitle-link";

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public String getEditText(ContentContext ctx, String key) {
		return "";
	}

	protected String getMainArea(ContentContext ctx) {
		String mainArea = getConfig(ctx).getProperty("area.main", null);
		if (mainArea == null) {
			mainArea = ctx.getArea();
		}
		return mainArea;
	}

	@Override
	public void prepareView(ContentContext ctx) throws Exception {
		super.prepareView(ctx);
		ctx = ctx.getContextWithArea(getMainArea(ctx));
		MenuElement myPage = getPage();
		ContentElementList content = myPage.getContent(ctx);
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(outStream);
		Collection<Link> links = new LinkedList<Link>();
		while (content.hasNext(ctx)) {
			IContentVisualComponent comp = content.next(ctx);
			if (comp instanceof ISubTitle) {
				ISubTitle subTitle = (ISubTitle) comp;
				if (subTitle.getSubTitleLevel(ctx) > 1) {
					Link link = new Link();
					link.setLabel(subTitle.getSubTitle(ctx));
					link.setUrl("#" + subTitle.getXHTMLId(ctx));
					link.setLevel(""+subTitle.getSubTitleLevel(ctx));
					links.add(link);
				}
			}
		}
		out.close();
		ctx.getRequest().setAttribute("links", links);
	}

	@Override
	public String getHexColor() {
		return IContentVisualComponent.LINK_COLOR;
	}

}
