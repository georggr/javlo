package org.javlo.service.social;

import java.util.HashMap;
import java.util.Map;

import org.javlo.helper.StringHelper;

public abstract class AbstractSocialNetwork implements ISocialNetwork {

	protected final Map<String, String> data = new HashMap<String, String>();

	private static final String TOKEN = "token";
	private static final String URL = "url";
	private static final String LOGIN = "login";

	@Override
	public String getToken() {
		return StringHelper.neverNull(data.get(TOKEN));
	}

	@Override
	public String getLogin() {
		return StringHelper.neverNull(data.get(LOGIN));
	}

	@Override
	public String getURL() {
		return StringHelper.neverNull(data.get(URL));
	}

	@Override
	public Map<String, String> getData() {
		return data;
	}

	@Override
	public void setToken(String token) {
		data.put(TOKEN, token);
	}

	@Override
	public void setURL(String url) {
		data.put(URL, url);
	}

	@Override
	public void setLogin(String login) {
		data.put(LOGIN, login);
	}

	@Override
	public void set(String key, String value) {
		data.put(key, value);
	}

	@Override
	public void update(Map map) {
		setURL(StringHelper.neverNull(map.get("url")));
		setToken(StringHelper.neverNull(map.get("token")));
		setLogin(StringHelper.neverNull(map.get("login")));
	}

}
