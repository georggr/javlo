package org.javlo.service.shared;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.javlo.component.core.ComponentBean;
import org.javlo.context.ContentContext;
import org.javlo.helper.StringHelper;

public abstract class AbstractSharedContentProvider implements ISharedContentProvider {

	protected String name;
	protected URL url;
	protected Map<String, String> categories = new HashMap<String, String>();

	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public String getLabel(Locale locale) {
		return getName();
	}

	@Override
	public URL getURL() {
		return url;
	}

	public void setURL(URL url) {
		this.url = url;
	}

	@Override
	public abstract Collection<SharedContent> getContent(ContentContext ctx);

	@Override
	public Collection<SharedContent> searchContent(ContentContext ctx, String query) {
		Collection<SharedContent> outList = new HashSet<SharedContent>();
		query = StringHelper.createFileName(query);
		for (SharedContent content : getContent(ctx)) {
			if (content.getContent() != null) {
				for (ComponentBean bean : content.getContent()) {
					if (bean != null && !outList.contains(content) && StringHelper.createFileName(bean.getValue()).contains(query)) {
						outList.add(content);
					}
				}
			}
			if (!outList.contains(content) && StringHelper.createFileName(content.getTitle() + ' ' + content.getDescription()).contains(query)) {
				outList.add(content);
			}
		}
		return outList;
	}

	@Override 
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Map<String, String> getCategories(ContentContext ctx) {
		return categories;
	}

	public void setCategories(Map<String, String> categories) {
		this.categories = categories;
	}

	@Override
	public Collection<SharedContent> getContent(ContentContext ctx, Collection<String> categories) {
		if (getCategories(ctx).size() <= 0 || categories == null || categories.size() == 0) {			
			return getContent(ctx);
		}
		List<SharedContent> outList = new LinkedList<SharedContent>();
		for (SharedContent content : getContent(ctx)) {
			if (!Collections.disjoint(content.getCategories(), categories)) {							
				outList.add(content);
			}
		}		
		Collections.sort(outList, new SharedContent.SortOnComparator(false));	
		return outList;
	}

	@Override
	public boolean isEmpty(ContentContext ctx) {
		return getContent(ctx).size() == 0;
	}

	@Override
	public void refresh(ContentContext ctx) {
	}

	@Override
	public String getType() {
		return TYPE_DEFAULT;
	}

	@Override
	public boolean isSearch() {
		return true;
	}

	@Override
	public int getContentSize(ContentContext ctx) {
		return getContent(ctx).size();
	};

	@Override
	public int getCategoriesSize(ContentContext ctx) {
		return getCategories(ctx).size();
	}

}
