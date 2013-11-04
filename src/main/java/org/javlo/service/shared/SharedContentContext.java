package org.javlo.service.shared;

import java.util.Collection;
import java.util.HashSet;

import javax.servlet.http.HttpSession;

public class SharedContentContext {
	
	private static final String KEY = "sharedContentContext";
	
	private String provider = null;
	private Collection<String> categories = new HashSet<String>();
	
	public static final SharedContentContext getInstance(HttpSession session) {
		SharedContentContext outContext = (SharedContentContext)session.getAttribute(KEY);
		if (outContext == null) {
			outContext = new SharedContentContext();
			session.setAttribute(KEY, outContext);
		}
		return outContext;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public Collection<String> getCategories() {
		return categories;
	}

	public void setCategories(Collection<String> categories) {
		this.categories = categories;
	}
	
	public String getCategory() {
		if (categories.size() > 0) {
			return categories.iterator().next();
		} else {
			return null;
		}
	}
	

}