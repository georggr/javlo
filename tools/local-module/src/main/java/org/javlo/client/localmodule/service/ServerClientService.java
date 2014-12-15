package org.javlo.client.localmodule.service;

import java.io.IOException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionManagerFactory;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.util.EntityUtils;
import org.javlo.client.localmodule.model.HttpException;
import org.javlo.client.localmodule.model.RemoteNotification;
import org.javlo.client.localmodule.model.ServerConfig;
import org.javlo.client.localmodule.model.ServerStatus;
import org.javlo.helper.StringHelper;
import org.javlo.helper.URLHelper;
import org.javlo.service.NotificationService.NotificationContainer;
import org.javlo.utils.JSONMap;

import com.google.gson.reflect.TypeToken;

public class ServerClientService {

	private static final Logger logger = Logger.getLogger(NotificationClientService.class.getName());

	private DefaultHttpClient httpClient;

	private final ServerConfig server;

	private Date lastNotificationsCheckDate;

	private ServerStatus status = ServerStatus.UNKNOWN;

	private String statusInfo;

	public ServerClientService(ServerConfig server, String proxyHost, Integer proxyPort, String proxyUsername, String proxyPassword) {
		this.server = server;

		ClientConnectionManager connMan = new PoolingClientConnectionManager();//TODO Is it correct?
		httpClient = new DefaultHttpClient(connMan);

		if (proxyHost != null) {
			httpClient.getCredentialsProvider().setCredentials(
					new AuthScope(proxyHost, proxyPort),
					new UsernamePasswordCredentials(proxyUsername, proxyPassword));

			HttpHost proxy = new HttpHost(proxyHost, proxyPort);

			httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
		}
	}

	@Deprecated
	public DefaultHttpClient getHttpClient() {
		return httpClient;
	}

	public ServerConfig getServer() {
		return server;
	}

	public ServerStatus getStatus() {
		return status;
	}

	public String getStatusInfo() {
		return statusInfo;
	}

	private void onSuccess() {
		status = ServerStatus.OK;
		statusInfo = null;
		ServiceFactory.getInstance().onServerStatusChange(server);
	}

	private void onWarning(String info) {
		status = ServerStatus.WARNING;
		statusInfo = info;
		ServiceFactory.getInstance().onServerStatusChange(server);
	}

	private void onError(Throwable ex) {
		status = ServerStatus.ERROR;
		statusInfo = ex.getClass().getSimpleName() + ": " + ex.getMessage();
		ServiceFactory.getInstance().onServerStatusChange(server);
	}

	private void onConnectionError(Throwable ex) {
		status = ServerStatus.UNKNOWN;
		statusInfo = "Connection problem. " + ex.getMessage();
	}

	public synchronized void dispose() {
		httpClient.getConnectionManager().shutdown();
	}

	/**
	 * Process the exception thrown by the {@link ServerClientService} and determine 
	 * if this exception can be generated by an internet connection problem.
	 * @param ex
	 * @return <code>true</code>if the exception is possibly caused by a connection problem.
	 */
	public boolean processException(Throwable ex) {
		if (ex instanceof HttpHostConnectException) {
			return processException(ex.getCause());
		} else if (ex instanceof UnknownHostException) {
			logger.log(Level.WARNING, "Connection problem.", ex);
			onConnectionError(ex);
			return true;
		} else if (ex instanceof NoRouteToHostException) {
			logger.log(Level.WARNING, "Connection problem.", ex);
			onConnectionError(ex);
			return true;
		} else if (ex instanceof SocketTimeoutException) {
			logger.log(Level.WARNING, "Connection problem.", ex);
			onConnectionError(ex);
			return true;
		} else if (ex instanceof HttpException) {
			logger.log(Level.WARNING, "Http error.", ex);
			onWarning(ex.getMessage());
			return false;
		} else {
			logger.log(Level.SEVERE, "Exception processed.", ex);
			onError(ex);
			return false;
		}
	}

	public List<RemoteNotification> getNewDataNotifications() throws IOException {
		String url = server.getServerURL();
		url = URLHelper.changeMode(url, "ajax");
		if (lastNotificationsCheckDate != null) {
			url = URLHelper.addParam(url, "webaction", "data.notifications");
			url = URLHelper.addParam(url, "lastdate", StringHelper.renderFileTime(lastNotificationsCheckDate));
		}
		url = URLHelper.addParam(url, "webaction", "data.date");
		JSONMap ajaxMap = httpGetAsJSONMap(url);
		if (ajaxMap != null) {
			String serverDateStr = null;
			List<NotificationContainer> rawNotifications = null;
			JSONMap dataMap = ajaxMap.getMap("data");
			if (dataMap != null) {
				serverDateStr = dataMap.getValue("date", String.class);
				rawNotifications = dataMap.getValue("notifications",
						new TypeToken<List<NotificationContainer>>() {
						}.getType());
			}

			if (serverDateStr != null) {
				try {
					lastNotificationsCheckDate = StringHelper.parseFileTime(serverDateStr);
				} catch (ParseException ex) {
					logger.log(Level.WARNING, "Cannot parse data.date result: " + serverDateStr, ex);
				}
			}

			List<RemoteNotification> notifications = new LinkedList<RemoteNotification>();
			if (rawNotifications != null) {
				for (NotificationContainer rawNotification : rawNotifications) {
					notifications.add(new RemoteNotification(server, rawNotification));
				}
			}
			onSuccess();
			logger.info("New notification found: " + notifications.size() + " for server: " + server.getLabel());
			return notifications;
		}
		return null;
	}

	public String tokenifyUrl(String simpleUrl) throws IOException {
		String url = server.getServerURL();
		url = URLHelper.addParam(url, "webaction", "data.oneTimeToken");
		url = URLHelper.changeMode(url, "ajax");
		JSONMap ajaxMap = httpGetAsJSONMap(url);
		if (ajaxMap != null) {
			String token = ajaxMap.getMap("data").getValue("token", String.class);
			onSuccess();
			return URLHelper.addParam(simpleUrl, "j_token", token);
		}
		return simpleUrl;
	}

	public void checkThePhrase() throws IOException {
		String url = server.getServerURL();
		I18nService i18n = ServiceFactory.getInstance().getI18n();
		String content = httpGetAsString(url);
		if (content != null) {
			if (server.getCheckPhrase() == null || server.getCheckPhrase().isEmpty()) {
				onWarning(i18n.get("alert.no-checkphrase-configured"));
			} else if (!content.contains(server.getCheckPhrase())) {
				onWarning(i18n.get("alert.checkphrase-not-found"));
			} else {
				onSuccess();
			}
		}
	}

	private JSONMap httpGetAsJSONMap(String url) throws IOException {
		String content = httpGetAsString(url);
		if (content == null) {
			return null;
		} else {
			return JSONMap.parseMap(content);
		}
	}

	private String httpGetAsString(String url) throws IOException {
		HttpResponse response = null;
		try {
			logger.info("Start request to server: " + url);
			HttpGet httpget = new HttpGet(url);
			response = httpClient.execute(httpget);

			if (response.getStatusLine().getStatusCode() == 200) {
				HttpEntity entity = response.getEntity();
				return EntityUtils.toString(entity);
			} else {
				throw new HttpException("HTTP status: " + response.getStatusLine());
			}
		} finally {
			safeConsume(response);
		}
	}

	private void safeConsume(HttpResponse response) {
		if (response != null) {
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				try {
					entity.consumeContent();
				} catch (IOException ignored) {
					//Ignore exception
				}
			}
		}
	}

	public static class HttpClientServiceClientConnectionManagerFactory implements ClientConnectionManagerFactory {
		@Override
		public ClientConnectionManager newInstance(org.apache.http.params.HttpParams params, SchemeRegistry schemeRegistry) {
			return new ThreadSafeClientConnManager(params, schemeRegistry);
		}

	}

}
