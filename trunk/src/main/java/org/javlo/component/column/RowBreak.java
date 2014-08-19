package org.javlo.component.column;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.javlo.context.ContentContext;

public class RowBreak extends TableComponent {
	
	private static final String TYPE = "row-break";

	@Override
	public String getType() {
		return TYPE;
	}
	
	@Override
	public String getSpecificClass(ContentContext ctx) {
		try {
			if (!getContext(ctx).isTableOpen()) {
				return " error";
			} else {
				return super.getSpecificClass(ctx);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return super.getSpecificClass(ctx);
		}
	}
	
	@Override
	public String getViewXHTMLCode(ContentContext ctx) throws Exception {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(outStream);
		out.println("</td></tr><tr><td"+getColSpanHTML(ctx)+" style=\""+getTDStyle(ctx)+"\">");
		out.close();
		return new String(outStream.toByteArray());
	}
	
	@Override
	protected String getPadding(ContentContext ctx) {
		String padding = getFieldValue("padding");
		if (padding == null || padding.trim().length() == 0) {
			try {
				padding = getContext(ctx).getTableBreak().getPadding(ctx);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return padding;
	}
	
	@Override
	protected String getWidth(ContentContext ctx) {
		String width = getFieldValue("width");
		if (width == null || width.trim().length() == 0) {
			try {
				width = getContext(ctx).getTableBreak().getWidth(ctx);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return width;
	}
	
	@Override
	protected String getVAlign(ContentContext ctx) {
		String valign = getFieldValue("valign");
		if (valign == null || valign.trim().length() == 0) {
			try {
				valign = getContext(ctx).getTableBreak().getVAlign(ctx);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return valign;
	}
	
	@Override
	public boolean isRowBreak() {	
		return true;
	}

}