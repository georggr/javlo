package org.javlo.component.column;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.javlo.context.ContentContext;
import org.javlo.context.EditContext;

public class OpenCell extends TableComponent {
	
	public static final String TYPE = "open-cell";

	@Override
	public String getType() {
		return TYPE;
	}
	
	@Override
	public String getViewXHTMLCode(ContentContext ctx) throws Exception {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(outStream);
		TableContext tableContext = getContext(ctx);
		
		String positionCSS = "";		
		if (tableContext.isFirst(this)) {
			positionCSS = "first ";
		}
		if (tableContext.isLast(this)) {
			positionCSS = positionCSS + "last ";
		}
		if (isCellEmpty(ctx)) {
			positionCSS = positionCSS + "empty";
		}
		if (positionCSS.length() > 0) {
			positionCSS = "class=\""+positionCSS.trim()+"\" ";
		}		
		if (tableContext.isTableOpen()) {
			out.println("</div></td>");
			if (isRowBreak()) {
				out.println("</tr><tr>");
			}
		} else {
			tableContext.openTable();
			String tableStyle = "width:100%;";
			String border = "border=\"0\" ";			
			
			TableBreak tableBreak = tableContext.getTableBreak();
			tableStyle = (tableStyle + ' ' + tableBreak.getOpenTableStyle(ctx)).trim();
			if (tableBreak.isGrid(ctx)) {
				border = "border=\"1\" ";
			}
			
			out.println("<table "+border+"style=\""+tableStyle+"\" class=\"component-table\"><tr>");
		}
		out.println("<td"+getColSpanHTML(ctx)+' '+positionCSS+"style=\""+getTDStyle(ctx)+"\"><div class=\"cell-wrapper\">");
		if (isCellEmpty(ctx) && ctx.isAsPreviewMode() && EditContext.getInstance(ctx.getGlobalContext(), ctx.getRequest().getSession()).isEditPreview()) {
			out.print("<span class=\"cell-name\">"+tableContext.getName(this)+"</span>");
		}
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
	public String getCellBackgroundColor(ContentContext ctx) {
		String bg = getFieldValue("backgroundcolor");
		if (bg == null || bg.trim().length() == 0) {
			try {
				bg = getContext(ctx).getTableBreak().getCellBackgroundColor(ctx);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return bg;
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
	protected String getAlign(ContentContext ctx) {
		String align = getFieldValue("align");
		if (align == null || align.trim().length() == 0) {
			try {
				align = getContext(ctx).getTableBreak().getAlign(ctx);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return align;
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

}

