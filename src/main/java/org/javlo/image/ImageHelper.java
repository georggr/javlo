/*
 * Created on 8 oct. 2003
 */
package org.javlo.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.javlo.component.core.IImageFilter;
import org.javlo.context.ContentContext;
import org.javlo.helper.LangHelper;
import org.javlo.helper.ResourceHelper;
import org.javlo.helper.StringHelper;
import org.javlo.template.Template;

/**
 * @author pvanderm
 */
public class ImageHelper {

	/**
	 * create a static logger.
	 */
	protected static Logger logger = Logger.getLogger(ImageHelper.class.getName());

	public static final int NO_FILTER = 0;

	public static final int BACK_AND_WHITE_FILTER = 1;

	static final int MAX_STRING_SIZE = 18;

	static final String DB_IMAGE_NAME = "_dir_cache.jpg";

	static final Color BACK_GROUND_DIR_COLOR = Color.BLACK;

	static final int BORDER = 4;

	// static final Color BG_TITLE_COLOR = Color.decode("#99ccff");
	static final Color BG_TITLE_COLOR = Color.GRAY;

	static final Color TEXT_TITLE_COLOR = Color.BLACK;

	static final String DIR_DIR = "dir_images";

	static final float DIR_IMAGE_QUALITY = (float) 0.95;

	public static String createSpecialDirectory(int width) {
		return createSpecialDirectory(width, 0);
	}

	public static String createSpecialDirectory(ContentContext ctx, String context, String filter, String area, String deviceCode, Template template, IImageFilter comp) {
		context = StringHelper.createFileName(context);
		String out = context + '/' + filter + '/' + deviceCode + '/' + area + '/';
		if (template == null) {
			out += Template.EDIT_TEMPLATE_CODE;
		} else {
			out += template.getId();
		}
		String compFilterKey = null;
		if (comp != null) {
			compFilterKey = StringHelper.trimAndNullify(comp.getImageFilterKey(ctx));
		}
		if (compFilterKey == null) {
			out += "/none";
		} else {
			out += "/" + StringHelper.createFileName(compFilterKey);
		}	
		return out;
	}

	public static String createSpecialDirectory(int width, int filter) {
		return "width_" + width + "_filter_" + filter;
	}

	/**
	 * transform a path in a string to a key. this key can be a directory name ( sp. replace / and \ with _ ).
	 * 
	 * @param path
	 *            a path to a file
	 * @return a key can be a directory name.
	 */
	public static String pathToKey(String path) {
		// String res = path.replaceAll("\\\\|/|:| ", "_");
		String res = path.replaceAll("\\\\ ", "/");
		res = res.replaceAll(":", "_");
		return res;
	}

	public static BufferedImage createAbsoluteLittleImage(ServletContext servletContext, String name, int width) throws IOException {
		BufferedImage image = null;
		InputStream in = new FileInputStream(name);
		try {
			image = ImageIO.read(in);
			if (image != null) {
				image = resize(image, width);
			} else {
				logger.severe("error with file '" + name + "' when resizing.");
			}
		} catch (Throwable t) {
			logger.severe(t.getMessage());
		} finally {
			ResourceHelper.closeResource(in);
		}
		return image;
	}

	public static BufferedImage loadImage(ServletContext servletContext, String name) throws IOException {
		BufferedImage image = null;
		InputStream in = servletContext.getResourceAsStream(name);
		if (in != null) {
			try {
				image = ImageIO.read(in);
			} finally {
				ResourceHelper.closeResource(in);
			}
		} else {
			logger.warning("file not found : " + name);
		}
		return image;
	}

	public static BufferedImage createLittleImage(ServletContext servletContext, String name, int width) throws IOException {
		BufferedImage image = loadImage(servletContext, name);
		if (image != null) {
			image = resize(image, width);
		} else {
			logger.severe("error with file '" + name + "' when resizing.");
		}

		return image;
	}

	public static BufferedImage resize(BufferedImage aOrgImage, int width) {
		double scale;

		if (aOrgImage.getWidth() > aOrgImage.getHeight()) {
			if (aOrgImage.getWidth() < width) {
				scale = (double) width / (double) aOrgImage.getWidth(); // removed
																		// : 1
			} else {
				scale = (double) width / (double) aOrgImage.getWidth();
			}
		} else {
			if (aOrgImage.getHeight() < width) {
				scale = (double) width / (double) aOrgImage.getWidth(); // removed
																		// : 1
			} else {
				scale = (double) width / (double) aOrgImage.getHeight(null);
			}
		}

		// Determine size of new image.
		// One of them should equal aMaxDim.
		int scaledW = (int) Math.round(scale * aOrgImage.getWidth());
		int scaledH = (int) Math.round(scale * aOrgImage.getHeight());

		java.awt.Image img = aOrgImage.getScaledInstance(scaledW, scaledH, java.awt.Image.SCALE_SMOOTH);

		ColorModel cm = aOrgImage.getColorModel();
		WritableRaster wr = cm.createCompatibleWritableRaster(scaledW, scaledH);

		// Create BufferedImage
		BufferedImage buffImage = new BufferedImage(cm, wr, false, null);
		Graphics2D g2 = buffImage.createGraphics();
		g2.drawImage(img, 0, 0, null);
		g2.dispose();

		return buffImage;
	}

	/**
	 * research image in subdirectories
	 * 
	 * @param dir
	 *            current directory
	 * @return a list of image in a subdirectory of current directory
	 */
	static Image[] getSubImage(Directory dir) {
		Image[] res = new Image[0];
		if (dir.getImages().length > 0) {
			res = dir.getImages();
		} else {
			for (int i = 0; i < dir.getChild().length; i++) {
				res = getSubImage(dir.getChild()[i]);
				if (res.length > 0) {
					i = dir.getChild().length;
				}
			}
		}
		return res;
	}

	static String cutString(String str) {
		String res = str;
		if (res.length() > MAX_STRING_SIZE) {
			res = str.substring(0, MAX_STRING_SIZE - 1).trim() + "...";
		}
		return res;
	}

	public static void main(String[] args) {
		String path = "c:\\p\\photos";
		System.out.println("path=" + path);
		System.out.println("key=" + pathToKey(path));
	}

	public static final String getImageFormat(String fileName) {
		String ext = StringHelper.getFileExtension(fileName);
		if (ext.equalsIgnoreCase("jpg")) {
			return "JPEG";
		} else if (ext.equalsIgnoreCase("png")) {
			return "PNG";
		} else if (ext.equalsIgnoreCase("gif")) {
			return "GIF";
		} else {
			return null;
		}
	}

	public static final String getImageExtensionToManType(String ext) {
		if (ext != null) {
			ext = ext.trim().toLowerCase();
			if (ext.equals("gif")) {
				return "image/GIF";
			} else if (ext.equals("png")) {
				return "image/GIF";
			} else if ((ext.equals("jpg")) || (ext.equals("jpeg"))) {
				return "image/JPEG";
			}
		}
		return null;
	}

	/**
	 * return dimension of picture in exif data, null if not found.
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static ImageSize getExifSize(InputStream in) throws IOException {
		Map<String, String> exifData = getExifData(in);
		ImageSize imageSize = null;
		try {
			imageSize = new ImageSize(Integer.parseInt(exifData.get("PixelXDimension")), Integer.parseInt(exifData.get("PixelYDimension")));
		} catch (Throwable t) {
		}
		return imageSize;
	}

	public static ImageSize getJpegSize(InputStream in) {
		ImageSize imageSize = null;
		try {
			byte[] buffer = new byte[1024 * 8];
			BufferedInputStream bufIn = new BufferedInputStream(in);
			int read = bufIn.read(buffer);

			for (int i = 0; i < read; i++) {
				if (buffer[i] == (byte) 0xff && buffer.length > i + 10) {
					if (buffer[i + 1] == (byte) 0xc0) {
						int j = 1;
						while (buffer[i + j] != 8 && j < 5) {
							j++;
						}
						if (j < 5) {
							int height = LangHelper.unsigned(buffer[i + j + 1]) * 255 + LangHelper.unsigned(buffer[i + j + 2]);
							int width = LangHelper.unsigned(buffer[i + j + 3]) * 255 + LangHelper.unsigned(buffer[i + j + 4]);
							imageSize = new ImageSize(width, height);
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return imageSize;

	}

	public static Map<String, String> getExifData(InputStream in) throws IOException {
		String rawData = ResourceHelper.loadStringFromStream(in, Charset.defaultCharset());
		Map<String, String> outData = new HashMap<String, String>();
		int index = 0;
		int exifIndex = rawData.indexOf("exif:", index);
		while (exifIndex >= 0) {
			String exifStr = rawData.substring(exifIndex, rawData.indexOf(' ', exifIndex + "exif:".length()));
			index = index + exifStr.length();
			String[] exifArray = StringUtils.split(exifStr, '=');
			if (exifArray.length == 2) {
				outData.put(exifArray[0].replace("exif:", ""), exifArray[1].replace("\"", ""));
			}
			exifIndex = rawData.indexOf("exif:", index);
		}
		return outData;
	}
}
