package org.javlo.component.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDataFormatter;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.javlo.component.core.IContentVisualComponent;
import org.javlo.config.StaticConfig;
import org.javlo.context.ContentContext;
import org.javlo.context.GlobalContext;
import org.javlo.helper.ArrayHelper;
import org.javlo.helper.ResourceHelper;
import org.javlo.helper.StringHelper;
import org.javlo.helper.URLHelper;
import org.javlo.helper.XHTMLHelper;
import org.javlo.i18n.I18nAccess;
import org.javlo.message.GenericMessage;
import org.javlo.service.ReverseLinkService;


public class ArrayFileComponent extends GenericFile {

	public static final String REQUEST_ATTRIBUTE_KEY = "array";

	public static final String TYPE = "array-file";

	public static class Cell {
		private String value = "";
		private int rowSpan = 1;
		private int colSpan = 1;
		private Cell[][] array;
		private int x;
		private int y;

		public Cell(String value, Cell[][] arrays, int x, int y) {
			this.value = value;
			this.array = arrays;
			this.x = x;
			this.y = y;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public int getRowSpan() {
			return rowSpan;
		}

		public void setRowSpan(int rowSpan) {
			this.rowSpan = rowSpan;
		}

		public int getColSpan() {
			return colSpan;
		}

		public void setColSpan(int colSpan) {
			this.colSpan = colSpan;
		}

		public String getSpanAttributes() {
			String span = "";
			if (colSpan > 1) {
				span = " colspan=\"" + colSpan + "\"";
			}
			if (rowSpan > 1) {
				span = span + " rowspan=\"" + rowSpan + "\"";
			}
			return span;
		}

		@Override
		public String toString() {
			return value;
		}

		public Cell[][] getArray() {
			return array;
		}

		public int getRowTitleWidth() {
			int rowTitleHeight = 1;
			for (int r = 0; r < array.length; r++) {
				if (array[r][0] != null && array[r][0].getColSpan() > rowTitleHeight) {
					rowTitleHeight = array[r][0].getColSpan();
				}
			}
			return rowTitleHeight;
		}

		public int getColTitleHeight() {
			int colTitleHeight = 1;
			for (int c = 0; c < array[0].length; c++) {
				if (array[0][c] != null && array[0][c].getRowSpan() > colTitleHeight) {
					colTitleHeight = array[0][c].getRowSpan();
				}
			}
			return colTitleHeight;
		}

		public boolean isFirstCol() {
			if (x <= (getRowTitleWidth() - 1)) {
				return true;
			} else {
				return false;
			}
		}

		public boolean isFirstRow() {
			if (y <= (getColTitleHeight() - 1)) {
				return true;
			} else {
				return false;
			}
		}
	}

	/**
	 * get the size of empty cell after the current cell (for colspan)
	 * 
	 * @param cell
	 * @param startCell
	 * @return
	 */
	private static int getEmptyLength(String[] cell, int startCell) {
		int out = 0;
		int i = startCell + 1;
		while (i < cell.length && cell[i].length() == 0) {
			out++;
			i++;
		}
		return out;
	}

	@Override
	public String createFileURL(ContentContext ctx, String url) {
		StaticConfig staticConfig = StaticConfig.getInstance(ctx.getRequest().getSession());
		String outURL = URLHelper.createStaticURL(ctx, staticConfig.getCSVFolder() + '/' + url).replace('\\', '/');
		return outURL;
	}

	@Override
	public int getComplexityLevel(ContentContext ctx) {
		return COMPLEXITY_EASY;
	}

	@Override
	public String getHexColor() {
		return TEXT_COLOR;
	}

	private Charset getCurrentEncoding(ContentContext ctx) {
		Charset charset = Charset.forName("utf-16"); // default encoding if
														// encoding not found
		try {
			if (getEncoding().equals(DEFAULT_ENCODING)) {
				charset = Charset.forName(GlobalContext.getInstance(ctx.getRequest()).getDefaultEncoding());
			} else {
				charset = Charset.forName(getEncoding());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return charset;
	}

	@Override
	protected String getDeleteTitle(ContentContext ctx) throws FileNotFoundException, IOException {
		I18nAccess i18nAccess = I18nAccess.getInstance(ctx.getRequest());
		return i18nAccess.getText("action.add-file.delete-file");
	}

	@Override
	protected String getImageChangeTitle(ContentContext ctx) throws FileNotFoundException, IOException {
		I18nAccess i18nAccess = I18nAccess.getInstance(ctx.getRequest());
		return i18nAccess.getText("action.add-file.change");
	}

	@Override
	protected String getImageUploadTitle(ContentContext ctx) throws FileNotFoundException, IOException {
		I18nAccess i18nAccess = I18nAccess.getInstance(ctx.getRequest());
		return i18nAccess.getText("action.add-file.add");
	}

	@Override
	public String getPreviewCode(ContentContext ctx) throws Exception {
		StringBuffer res = new StringBuffer();
		if ((getValue() != null) && (getValue().trim().length() > 0)) {
			StaticConfig staticConfig = StaticConfig.getInstance(ctx.getRequest().getSession());

			String url = URLHelper.mergePath(getDirSelected(), getFileName());
			url = URLHelper.createResourceURL(ctx, getPage(), staticConfig.getFileFolder() + '/' + url);
			res.append("<a href=\"");
			res.append(url);
			res.append("\">");
			if (getLabel().trim().length() == 0) {
				res.append(getFileName());
			} else {
				res.append(XHTMLHelper.textToXHTML(getLabel()));
			}
			String fullName = URLHelper.mergePath(getDirSelected(), getFileName());
			fullName = URLHelper.mergePath(staticConfig.getCSVFolder(), fullName);
			GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
			fullName = URLHelper.mergePath(globalContext.getDataFolder(), fullName);
			res.append("&nbsp;" + XHTMLHelper.getFileIcone(ctx, getFileName()) + " (" + StringHelper.getFileSize(fullName) + ")</a>");
		} else {
			res.append("&nbsp; <!--FILE NOT DEFINED--> ");
		}
		return res.toString();
	}

	@Override
	public String[] getStyleLabelList(ContentContext ctx) {

		String thth = "double title";
		String thtd = "cols title";
		String tdth = "rows title";
		String tdtd = "no title";

		try {
			I18nAccess i18nAccess = I18nAccess.getInstance(ctx.getRequest());
			thth = i18nAccess.getText("content.csv-array.thth");
			thtd = i18nAccess.getText("content.csv-array.thtd");
			tdth = i18nAccess.getText("content.csv-array.tdth");
			tdtd = i18nAccess.getText("content.csv-array.tdtd");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new String[] { thth, thtd, tdth, tdtd };
	}

	@Override
	public String[] getStyleList(ContentContext ctx) {
		return new String[] { "th-th", "th-td", "td-th", "td-td" };
	}

	@Override
	public String getStyleTitle(ContentContext ctx) {
		try {
			I18nAccess i18nAccess = I18nAccess.getInstance(ctx.getRequest());
			return i18nAccess.getText("content.csv-array.style-title");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "style";
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public void prepareView(ContentContext ctx) throws Exception {
		super.prepareView(ctx);
		ctx.getRequest().setAttribute(REQUEST_ATTRIBUTE_KEY, null);
		ctx.getRequest().setAttribute("summary", getLabel());
		
		getArray(ctx);
		ctx.getRequest().setAttribute("colHead", "th");
		ctx.getRequest().setAttribute("rowHead", "th");
		ctx.getRequest().setAttribute("tableHead", true);
		String style = getStyle();
		if (style != null) {
			if (style.equals("th-td")) {
				ctx.getRequest().setAttribute("rowHead", "td");
			} else if (style.equals("td-th")) {
				ctx.getRequest().setAttribute("colHead", "td");
				ctx.getRequest().setAttribute("tableHead", false);
			} else if (style.equals("td-td")) {
				ctx.getRequest().setAttribute("rowHead", "td");
				ctx.getRequest().setAttribute("colHead", "td");
				ctx.getRequest().setAttribute("tableHead", false);
			}
		}
	}

	protected Cell[][] getArray(ContentContext ctx) throws Exception {
		Cell[][] outArray = (Cell[][]) ctx.getRequest().getAttribute(REQUEST_ATTRIBUTE_KEY);
		if (outArray == null) {
			String basePath = URLHelper.mergePath(getFileDirectory(ctx), getDirSelected());
			basePath = URLHelper.mergePath(basePath, getFileName());
			File file = new File(basePath);

			/*
			 * if
			 * (StringHelper.getFileExtension(file.getName()).equalsIgnoreCase
			 * ("csv")) { outArray = getCSVArray(ctx, file); } else
			 */
			/*if (StringHelper.getFileExtension(file.getName()).equalsIgnoreCase("ods")) {
				outArray = getODSArray(ctx, file);
			} else*/ if (StringHelper.getFileExtension(file.getName()).equalsIgnoreCase("xlsx")) {
				outArray = getXLSXArray(ctx, file);
			} else if (StringHelper.getFileExtension(file.getName()).equalsIgnoreCase("xls")) {
				outArray = getXLSArray(ctx, file);
			}
			optimizeRowSpan(outArray);
			ctx.getRequest().setAttribute(REQUEST_ATTRIBUTE_KEY, outArray);
		}
		return outArray;
	}

	private static void optimizeRowSpan(Cell[][] outArray) {
		if (outArray == null) {
			return;
		}
		for (int x = 0; x < outArray.length; x++) {
			int minRawSpan = Integer.MAX_VALUE;
			for (int y = 0; y < outArray[x].length; y++) {
				if (outArray[x][y] != null && outArray[x][y].getRowSpan() < minRawSpan) {
					minRawSpan = outArray[x][y].getRowSpan();
				}
			}
			if (minRawSpan > 1) {
				minRawSpan--;
				for (int y = 0; y < outArray[x].length; y++) {
					if (outArray[x][y] != null) {
						outArray[x][y].setRowSpan(outArray[x][y].getRowSpan() - minRawSpan);
					}
				}
			}
		}
	}

	private String renderCell(ContentContext ctx, String cell) throws Exception {
		ReverseLinkService rlService = ReverseLinkService.getInstance(ctx.getGlobalContext());
		return XHTMLHelper.textToXHTML(rlService.replaceLink(ctx, this, cell), ctx.getGlobalContext());
	}

	private static String readExcelCell(ContentContext ctx, XSSFCell cell) {
		HSSFDataFormatter formatter = new HSSFDataFormatter(new Locale(ctx.getRequestContentLanguage()));
		String outCell;
		if (cell.getCellType() == HSSFCell.CELL_TYPE_FORMULA) {
			switch (cell.getCachedFormulaResultType()) {
			case HSSFCell.CELL_TYPE_STRING:
				outCell = cell.getStringCellValue();
				break;
			case HSSFCell.CELL_TYPE_NUMERIC:
				outCell = StringHelper.renderDouble(cell.getNumericCellValue(),new Locale(ctx.getRequestContentLanguage()));				
				break;
			case HSSFCell.CELL_TYPE_BOOLEAN:
				outCell = ""+cell.getBooleanCellValue();
				break;
			default:
				outCell = "?";
				break;
			}			
		} else {
			outCell = formatter.formatCellValue(cell);			
		}
		if (cell.getHyperlink() != null) {
			String target = "";
			String url = cell.getHyperlink().getAddress();
			if (ctx.getGlobalContext().isOpenExternalLinkAsPopup(url)) {
				target = " target=\"_blank\"";
			}
			outCell = "<a class=\"cell-link\" href=\"" + url + "\"" + target + ">" + outCell + "</a>";
		}
		return outCell;
	}

	private static String readExcelCell(ContentContext ctx, HSSFCell cell) {
		HSSFDataFormatter formatter = new HSSFDataFormatter(new Locale(ctx.getRequestContentLanguage()));
		String outCell;
		if (cell.getCellType() == HSSFCell.CELL_TYPE_FORMULA) {
			switch (cell.getCachedFormulaResultType()) {
			case HSSFCell.CELL_TYPE_STRING:
				outCell = cell.getStringCellValue();
				break;
			case HSSFCell.CELL_TYPE_NUMERIC:
				outCell = StringHelper.renderDouble(cell.getNumericCellValue(),new Locale(ctx.getRequestContentLanguage()));				
				break;
			case HSSFCell.CELL_TYPE_BOOLEAN:
				outCell = ""+cell.getBooleanCellValue();
				break;
			default:
				outCell = "?";
				break;
			}			
		} else {
			outCell = formatter.formatCellValue(cell);			
		}
		if (cell.getHyperlink() != null) {
			String target = "";
			String url = cell.getHyperlink().getAddress();
			if (ctx.getGlobalContext().isOpenExternalLinkAsPopup(url)) {
				target = " target=\"_blank\"";
			}
			outCell = "<a class=\"cell-link\" href=\"" + url + "\"" + target + ">" + outCell + "</a>";
		}
		return outCell;
	}

	/*private static String readOpenDocCell(MutableCell<SpreadSheet> mutableCell) {
		return mutableCell.getValue().toString();
	}

	protected Cell[][] getODSArray(ContentContext ctx, File odsFile) throws Exception {
		final Sheet sheet = SpreadSheet.createFromFile(odsFile).getSheet(0);
		int w = 0;
		int h = 0;
		for (int x = 0; x < Math.min(sheet.getColumnCount(), 32); x++) {
			for (int y = 0; y < Math.min(sheet.getRowCount(), 512); y++) {
				Object value = sheet.getCellAt(x, y).getValue();
				if (value != null && value.toString().trim().length() > 0) {
					if (x > h) {
						h = x;
					}
					if (y > w) {
						w = y;
					}
				}
			}
		}
		w++;
		h++;
		Cell[][] outArray = new Cell[w][];
		for (int x = 0; x < w; x++) {
			outArray[x] = new Cell[h];
			for (int y = 0; y < h; y++) {
				if (sheet.getCellAt(y, x).getValue() != null) {
					outArray[x][y] = new Cell(renderCell(readOpenDocCell(sheet.getCellAt(y, x))), outArray, x, y);
				}
			}
		}

		return outArray;
	}*/

	protected Cell[][] getXLSXArray(ContentContext ctx, File xslxFile) throws Exception {
		InputStream in = new FileInputStream(xslxFile);
		try {
			XSSFWorkbook workbook = new XSSFWorkbook(in);
			XSSFSheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = sheet.iterator();
			int w = 0;
			int h = 0;
			while (rowIterator.hasNext()) {
				h++;
				Row row = rowIterator.next();
				if (row.getLastCellNum() > w) {
					w = row.getLastCellNum();
				}
			}

			Cell[][] outArray = new Cell[h][];
			for (int y = 0; y < h; y++) {
				outArray[y] = new Cell[w];
				for (int x = 0; x < w; x++) {
					outArray[y][x] = new Cell(null, outArray, x, y);
					if (sheet.getRow(y) != null && sheet.getRow(y).getCell(x) != null) {
						outArray[y][x].setValue(renderCell(ctx, readExcelCell(ctx, sheet.getRow(y).getCell(x))));
					}
				}
			}

			for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
				CellRangeAddress cellRange = sheet.getMergedRegion(i);
				for (int x = cellRange.getFirstColumn(); x <= cellRange.getLastColumn(); x++) {
					for (int y = cellRange.getFirstRow(); y <= cellRange.getLastRow(); y++) {
						if (x > cellRange.getFirstColumn()) {
							outArray[cellRange.getFirstRow()][cellRange.getFirstColumn()].setColSpan(outArray[cellRange.getFirstRow()][cellRange.getFirstColumn()].getColSpan() + 1);
							outArray[y][x] = null;
						}
						if (y > cellRange.getFirstRow()) {
							outArray[cellRange.getFirstRow()][cellRange.getFirstColumn()].setRowSpan(outArray[cellRange.getFirstRow()][cellRange.getFirstColumn()].getRowSpan() + 1);
							outArray[y][x] = null;
						}
					}
				}
			}

			return outArray;
		} finally {
			ResourceHelper.closeResource(in);
		}
	}

	protected Cell[][] getXLSArray(ContentContext ctx, File xslxFile) throws Exception {
		InputStream in = new FileInputStream(xslxFile);
		try {
			HSSFWorkbook workbook = new HSSFWorkbook(in);
			HSSFSheet sheet = workbook.getSheetAt(0);
			
			
			
			Iterator<Row> rowIterator = sheet.iterator();
			int w = 0;
			int h = sheet.getLastRowNum()+1;
			while (rowIterator.hasNext()) {
				//h++;
				Row row = rowIterator.next();
				if (row.getLastCellNum() > w) {
					w = row.getLastCellNum();
				}
			}

			Cell[][] outArray = new Cell[h][];
			for (int y = 0; y < h; y++) {
				outArray[y] = new Cell[w];
				for (int x = 0; x < w; x++) {
					outArray[y][x] = new Cell(null, outArray, x, y);
					if (sheet.getRow(y) != null && sheet.getRow(y).getCell(x) != null) {
						outArray[y][x].setValue(renderCell(ctx, readExcelCell(ctx, sheet.getRow(y).getCell(x))));
					}
				}
			}

			for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
				CellRangeAddress cellRange = sheet.getMergedRegion(i);
				for (int x = cellRange.getFirstColumn(); x <= cellRange.getLastColumn(); x++) {
					for (int y = cellRange.getFirstRow(); y <= cellRange.getLastRow(); y++) {
						if (x > cellRange.getFirstColumn()) {
							outArray[cellRange.getFirstRow()][cellRange.getFirstColumn()].setColSpan(outArray[cellRange.getFirstRow()][cellRange.getFirstColumn()].getColSpan() + 1);
							outArray[y][x] = null;
						}
						if (y > cellRange.getFirstRow()) {
							outArray[cellRange.getFirstRow()][cellRange.getFirstColumn()].setRowSpan(outArray[cellRange.getFirstRow()][cellRange.getFirstColumn()].getRowSpan() + 1);
							outArray[y][x] = null;
						}
					}
				}
			}

			return outArray;
		} finally {
			ResourceHelper.closeResource(in);
		}

	}

	protected static Cell[][] TESTgetXLSArray(File xslxFile) throws Exception {
		InputStream in = new FileInputStream(xslxFile);
		try {
			HSSFWorkbook workbook = new HSSFWorkbook(in);
			HSSFSheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = sheet.iterator();
			int w = 0;
			int h = 0;
			while (rowIterator.hasNext()) {
				h++;
				Row row = rowIterator.next();
				if (row.getLastCellNum() > w) {
					w = row.getLastCellNum();
				}
			}

			System.out.println("***** ArrayFileComponent.TESTgetXLSArray : w = " + w); // TODO:
																						// remove
																						// debug
																						// trace

			Cell[][] outArray = new Cell[h][];
			for (int y = 0; y < h; y++) {
				outArray[y] = new Cell[w];
				for (int x = 0; x < w; x++) {
					outArray[y][x] = new Cell(null, outArray, x, y);
					if (sheet.getRow(x) != null && sheet.getRow(y).getCell(x) != null) {
						outArray[y][x].setValue(sheet.getRow(y).getCell(x).toString());
					} else {
						System.out.println("***** ArrayFileComponent.TESTgetXLSArray : null on x:" + x + " y:" + y); // TODO:
																														// remove
																														// debug
																														// trace
					}
				}
			}

			for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
				CellRangeAddress cellRange = sheet.getMergedRegion(i);
				for (int x = cellRange.getFirstColumn(); x <= cellRange.getLastColumn(); x++) {
					for (int y = cellRange.getFirstRow(); y <= cellRange.getLastRow(); y++) {
						if (x > cellRange.getFirstColumn()) {
							outArray[cellRange.getFirstRow()][cellRange.getFirstColumn()].setColSpan(outArray[cellRange.getFirstRow()][cellRange.getFirstColumn()].getColSpan() + 1);
							outArray[y][x] = null;
						}
						if (y > cellRange.getFirstRow()) {
							outArray[cellRange.getFirstRow()][cellRange.getFirstColumn()].setRowSpan(outArray[cellRange.getFirstRow()][cellRange.getFirstColumn()].getRowSpan() + 1);
							outArray[y][x] = null;
						}
					}
				}
			}

			return outArray;
		} finally {
			ResourceHelper.closeResource(in);
		}

	}

	@Override
	public String getViewXHTMLCode(ContentContext ctx) throws Exception {
		String colTH = "th";
		String rowTH = "th";
		String style = getStyle();
		if (style != null) {
			if (style.equals("th-td")) {
				rowTH = "td";
			} else if (style.equals("td-th")) {
				colTH = "td";
			} else if (style.equals("td-td")) {
				rowTH = "td";
				colTH = "td";
			}
		}

		StringWriter stringWriter = new StringWriter();
		stringWriter.append("<div " + getSpecialPreviewCssClass(ctx, getStyle(ctx) + " " + getType()) + getSpecialPreviewCssId(ctx) + ">");
		if (getLabel().trim().length() > 0) {
			stringWriter.append("<table summary=\"" + getLabel() + "\" class=\"" + getStyle(ctx) + "\">");
		} else {
			stringWriter.append("<table class=\"" + getStyle(ctx) + "\">");
		}

		Cell[][] array = getArray(ctx);

		if (array == null || array.length == 0) {
			return "<b>WARNING: no data found in file. (col)</b>";
		} else if (array[0].length == 0) {
			return "<b>WARNING: no cell found in file. (row)</b>";
		}

		for (int i = 0; i < array.length; i++) {
			if (i % 2 == 1) {
				stringWriter.append("<tr class=\"row-" + i + " odd\">");
			} else {
				stringWriter.append("<tr class=\"row-" + i + "\" >");
			}
			for (int j = 0; j < array[i].length; j++) {
				String tag = "td";

				Cell cell = array[i][j];

				if (cell != null && (j == 0 || cell.getValue().length() > 0)) {
					if (i == 0) {
						tag = colTH;
					} else if (j == 0) {
						tag = rowTH;
					}
					String cssClass = "";
					String content = cell.getValue();
					if (content == null || content.trim().length() == 0) {
						cssClass = " empty";
						content = "";
					}

					String spanHTML = "";
					if (cell.getColSpan() > 1) {
						spanHTML = " colspan=\"" + cell.getColSpan() + "\"";
					}
					if (cell.getRowSpan() > 1) {
						spanHTML = spanHTML + " rowspan=\"" + cell.getRowSpan() + "\"";
					}

					if (j % 2 == 1) {
						stringWriter.append('<' + tag + " class=\"odd" + cssClass + "\"" + spanHTML + ">");
					} else {
						stringWriter.append('<' + tag + " class=\"even" + cssClass + "\"" + spanHTML + '>');
					}

					content = renderCell(content);

					stringWriter.append(content);
					stringWriter.append("</" + tag + '>');
				}
			}
			stringWriter.append("</tr>");
		}

		stringWriter.append("</table></div>");

		return stringWriter.toString();
	}

	public static String renderArray(File file) throws Exception {
		String colTH = "th";
		String rowTH = "th";
		String style = "";
		if (style != null) {
			if (style.equals("th-td")) {
				rowTH = "td";
			} else if (style.equals("td-th")) {
				colTH = "td";
			} else if (style.equals("td-td")) {
				rowTH = "td";
				colTH = "td";
			}
		}

		StringWriter stringWriter = new StringWriter();
		stringWriter.append("<div>");
		stringWriter.append("<table border=\"1\">");

		Cell[][] array = TESTgetXLSArray(file);
		System.out.println("***** ArrayFileComponent.renderArray : size = " + array[0].length); // TODO:
																								// remove
																								// debug
																								// trace

		optimizeRowSpan(array);

		if (array == null || array.length == 0) {
			return "<b>WARNING: no data found in file. (col)</b>";
		} else if (array[0].length == 0) {
			return "<b>WARNING: no cell found in file. (row)</b>";
		}

		for (int i = 0; i < array.length; i++) {
			if (i % 2 == 1) {
				stringWriter.append("<tr class=\"row-" + i + " odd\">");
			} else {
				stringWriter.append("<tr class=\"row-" + i + "\" >");
			}
			for (int j = 0; j < array[i].length; j++) {
				String tag = "td";

				Cell cell = array[i][j];

				System.out.println(i + ',' + j + " = " + cell);

				if (cell != null && cell.getValue() != null && (j == 0 || cell.getValue().length() > 0)) {
					if (i == 0) {
						tag = colTH;
					} else if (j == 0) {
						tag = rowTH;
					}
					String cssClass = "";
					String content = cell.getValue();
					if (content == null || content.trim().length() == 0) {
						cssClass = " empty";
						content = "";
					}

					String spanHTML = "";
					if (cell.getColSpan() > 1) {
						spanHTML = " colspan=\"" + cell.getColSpan() + "\"";
					}
					if (cell.getRowSpan() > 1) {
						spanHTML = spanHTML + " rowspan=\"" + cell.getRowSpan() + "\"";
					}

					if (j % 2 == 1) {
						stringWriter.append('<' + tag + " class=\"odd" + cssClass + "\"" + spanHTML + ">");
					} else {
						stringWriter.append('<' + tag + " class=\"even" + cssClass + "\"" + spanHTML + '>');
					}

					content = content;

					stringWriter.append(content);
					stringWriter.append("</" + tag + '>');
				}
			}
			stringWriter.append("</tr>");
		}

		stringWriter.append("</table></div>");

		return stringWriter.toString();
	}

	@Override
	protected boolean isFileNameValid(String fileName) {
		return true;
	}

	@Override
	protected boolean needEncoding() {
		return true;
	}

	protected String renderCell(String content) {
		if (content.trim().length() == 0) {
			content = "&nbsp;";
		}
		return XHTMLHelper.autoLink(content);
	}

	@Override
	public int getPopularity(ContentContext ctx) {
		return 0;
	}

	@Override
	public boolean isContentCachable(ContentContext ctx) {
		return true;
	}

	@Override
	protected String getEditXHTMLCode(ContentContext ctx) throws Exception {

		I18nAccess i18nAccess = I18nAccess.getInstance(ctx.getRequest());

		StringBuffer finalCode = new StringBuffer();
		finalCode.append(getSpecialInputTag());

		finalCode.append("<div class=\"line\"><label for=\"" + getLabelXHTMLInputName() + "\">" + i18nAccess.getText("global.summary") + " : </label>");
		String[][] params = { { "rows", "1" } };
		finalCode.append(XHTMLHelper.getTextArea(getLabelXHTMLInputName(), getLabel(), params));
		finalCode.append("</div>");

		if (canUpload(ctx)) {
			finalCode.append("<div class=\"line\"><label for=\"new_dir_" + getId() + "\">");
			finalCode.append(getNewDirLabelTitle(ctx));
			finalCode.append(" : </label><input id=\"new_dir_" + getId() + "\" name=\"" + getNewDirInputName() + "\" type=\"text\"/></div>");
		}

		if ((getDirList(getFileDirectory(ctx)) != null) && (getDirList(getFileDirectory(ctx)).length > 0)) {
			finalCode.append("<div class=\"line\"><label for=\"" + getDirInputName() + "\">");
			finalCode.append(getDirLabelTitle(ctx));
			finalCode.append(" : </label>");
			finalCode.append(XHTMLHelper.getInputOneSelect(getDirInputName(), ArrayHelper.addFirstElem(getDirList(getFileDirectory(ctx)), ""), getDirSelected(), getJSOnChange(ctx), true));
			finalCode.append("</div>");
		}

		if (canUpload(ctx)) {
			finalCode.append("<div class=\"line\">");
			String uploadId = "update-" + getId();
			finalCode.append("<label for=\"" + uploadId + "\">" + getImageUploadTitle(ctx) + " :</label>");
			finalCode.append("<input id=\"" + uploadId + "\" name=\"" + getFileXHTMLInputName() + "\" type=\"file\"/>");
			finalCode.append("</div");
		}

		String[] fileList = getFileList(getFileDirectory(ctx), getFileFilter());
		if (fileList.length > 0) {

			finalCode.append(getImageChangeTitle(ctx));

			finalCode.append("<div class=\"line\">");
			String[] fileListBlanck = new String[fileList.length + 1];
			fileListBlanck[0] = "";
			System.arraycopy(fileList, 0, fileListBlanck, 1, fileList.length);

			finalCode.append("</div><div class=\"line\"><label>&nbsp</label>");
			finalCode.append(XHTMLHelper.getInputOneSelect(getSelectXHTMLInputName(), fileListBlanck, getFileName(), getJSOnChange(ctx), true));
			finalCode.append("</div>");

			if (ctx.getRenderMode() == ContentContext.EDIT_MODE && !ctx.isEditPreview()) {
				if (isLinkToStatic()) {

					Map<String, String> filesParams = new HashMap<String, String>();
					filesParams.put("path", URLHelper.mergePath("/", getRelativeFileDirectory(ctx), getDirSelected()));
					String staticURL = URLHelper.createModuleURL(ctx, ctx.getPath(), "file", filesParams);

					finalCode.append("&nbsp;<a class=\"" + IContentVisualComponent.EDIT_ACTION_CSS_CLASS + "\" href=\"" + staticURL + "\" >");
					finalCode.append(i18nAccess.getText("content.goto-static"));
					finalCode.append("</a>");
				}
			}
		}

		// validation
		if (!isFileNameValid(getFileName())) {
			setMessage(new GenericMessage(i18nAccess.getText("component.error.file"), GenericMessage.ERROR));
		}

		return finalCode.toString();
	}
	
	@Override
	public boolean isRealContent(ContentContext ctx) {	
		return getValue().trim().length() > 0;
	}

	public static void main(String[] args) {
		File test = new File("c:/trans/test.xlsx");
		File outFile = new File("c:/trans/test.html");
		try {
			ResourceHelper.writeStringToFile(outFile, renderArray(test));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
