package org.javlo.service.shared.stockvault;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;

import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.CssSelectorNodeFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.util.NodeList;
import org.javlo.helper.StringHelper;
import org.javlo.helper.URLHelper;
import org.javlo.service.shared.AbstractSharedContentProvider;
import org.javlo.service.shared.SharedContent;

public class StockvaultSharedContentProvider extends AbstractSharedContentProvider {

	private Collection<SharedContent> content = null;

	private static Logger logger = Logger.getLogger(StockvaultSharedContentProvider.class.getName());

	public StockvaultSharedContentProvider() throws Exception {
		try {
			setURL(new URL("http://www.stockvault.net/"));
			setName("stockvault.net");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		Parser htmlParser = new Parser(getURL().openConnection());
		NodeFilter cssFilter = new CssSelectorNodeFilter(".sidebar li a");
		NodeList nodeList = htmlParser.extractAllNodesThatMatch(cssFilter);
		Map<String, String> categories = new HashMap<String, String>();
		for (int i = 0; i < nodeList.size(); i++) {
			if (nodeList.elementAt(i) instanceof TagNode) {
				TagNode tag = (TagNode) nodeList.elementAt(i);
				String label = tag.toPlainTextString().trim();
				String href = tag.getAttribute("href").trim();
				if (label.length() > 0 && href.length() > 0) {
					categories.put(href, label);
				}
			} else {
				logger.severe("bad tag format found, check html on : " + getURL());
			}
		}
		setCategories(categories);
	}
	
	@Override
	public void refresh() {
		content = null;
	}
	
	@Override
	public Collection<SharedContent> getContent() {	
		return getContent(getURL());
	}
	
	@Override
	public Collection<SharedContent> searchContent(String query) {
		try {
			Collection<SharedContent> outContent = new LinkedList<SharedContent>();
			for (int page=1; page<4; page++) { // read 3 pages
				URL url = new URL(URLHelper.addAllParams(URLHelper.mergePath(getURL().toString(),"/search/"), "query="+StringHelper.toHTMLAttribute(query), "page="+page));				
				try {
					outContent.addAll(readURL(url));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			return outContent;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}		
	}
	
	private Collection<SharedContent> getContent(URL url) {		
		if (content == null) {
			content = new LinkedList<SharedContent>();
			for (Map.Entry<String, String> category : getCategories().entrySet()) {
				logger.info("Stockvault load category : "+category.getValue());
				try {
					URL catURL = new URL(URLHelper.mergePath(url.toString(), category.getKey()));
					Collection<SharedContent> result = readURL(catURL);
					for (SharedContent sharedContent : result) {
						sharedContent.addCategory(category.getKey());
					}
					content.addAll(result);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}
		return content;
	}
	
	private Collection<SharedContent> readURL(URL url) throws Exception {
		Collection<SharedContent> outSharedContent = new LinkedList<SharedContent>();
		
		Parser htmlParser = new Parser(url.openConnection());
		NodeFilter cssFilter = new CssSelectorNodeFilter(".row_images2 li img");
		NodeList nodeList = htmlParser.extractAllNodesThatMatch(cssFilter);
		if (nodeList.size() == 0) {
			logger.severe("bad structure no images found.");
		}
		for (int i = 0; i < nodeList.size(); i++) {
			if (nodeList.elementAt(i) instanceof TagNode) {
				TagNode tag = (TagNode) nodeList.elementAt(i);
				String[] splitedSrc = tag.getAttribute("src").split("/");
				if (splitedSrc.length > 0) {
					String id = StringHelper.getFileNameWithoutExtension(splitedSrc[splitedSrc.length - 1]);
					StockvaultSharedContent sharedContent = new StockvaultSharedContent(id, null);					
					String title = tag.getAttribute("alt");
					String[] splitedTitle = title.split("-");
					if (splitedTitle.length > 0) {
						title = splitedTitle[1];
					}
					sharedContent.setTitle(title.trim());
					sharedContent.setImageUrl(URLHelper.mergePath(getURL().toString(), "data/s/", id + ".jpg"));
					sharedContent.setRemoteImageUrl(URLHelper.mergePath(getURL().toString(), "/photo/download/", id));
					outSharedContent.add(sharedContent);
				}
			}
		}		
		return outSharedContent;
	}

	public static void main(String[] args) {
		try {
			StockvaultSharedContentProvider provider = new StockvaultSharedContentProvider();
			for (SharedContent content : provider.getContent()) {
				System.out.println(content.getTitle());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public String getType() {	
		return TYPE_IMAGE;
	}

}
