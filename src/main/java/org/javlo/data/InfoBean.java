package org.javlo.data;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.javlo.config.StaticConfig;
import org.javlo.context.ContentContext;
import org.javlo.context.EditContext;
import org.javlo.context.GlobalContext;
import org.javlo.helper.StringHelper;
import org.javlo.helper.URLHelper;
import org.javlo.message.GenericMessage;
import org.javlo.message.MessageRepository;
import org.javlo.navigation.MenuElement;
import org.javlo.navigation.MenuElement.PageBean;
import org.javlo.rendering.Device;
import org.javlo.service.PersistenceService;
import org.javlo.service.exception.ServiceException;
import org.javlo.servlet.AccessServlet;
import org.javlo.template.Template;
import org.javlo.user.AdminUserFactory;
import org.javlo.user.AdminUserSecurity;
import org.javlo.user.IUserFactory;
import org.javlo.user.User;
import org.javlo.user.UserFactory;

public class InfoBean {
	
	private static final String ts = ""+System.currentTimeMillis();

	public static final String REQUEST_KEY = "info";

	public static final String NEW_SESSION_PARAM = "__new_session";

	public static InfoBean getCurrentInfoBean(HttpServletRequest request) {
		return (InfoBean) request.getAttribute(REQUEST_KEY);
	}

	public static InfoBean getCurrentInfoBean(ContentContext ctx) throws Exception {
		InfoBean ib = getCurrentInfoBean(ctx.getRequest());
		if (ib == null) {
			ib = updateInfoBean(ctx);
		}
		return ib;
	}

	/**
	 * create info bean in request (key=info) for jstp call in template.
	 * 
	 * @param ctx
	 * @throws Exception
	 */
	public static InfoBean updateInfoBean(ContentContext ctx) throws Exception {
		InfoBean info = new InfoBean();

		info.currentPage = ctx.getCurrentPage();
		info.ctx = ctx;
		info.globalContext = GlobalContext.getInstance(ctx.getRequest());

		ctx.getRequest().setAttribute(REQUEST_KEY, info);

		return info;
	}

	private MenuElement currentPage;
	private ContentContext ctx;
	private GlobalContext globalContext;
	private boolean tools = true;

	public String getCmsName() {
		StaticConfig staticConfig = StaticConfig.getInstance(ctx.getRequest().getSession());
		return staticConfig.getCmsName();
	}

	public String getCurrentAbsoluteURL() {
		return URLHelper.createURL(ctx.getContextForAbsoluteURL());
	}

	public String getCurrentURL() {
		return URLHelper.createURL(ctx);
	}

	public String getUploadURL() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("webaction", "data.upload");
		return URLHelper.createAjaxURL(ctx, params);
	}

	public String getCurrentViewURL() {
		return URLHelper.createURL(ctx.getContextWithOtherRenderMode(ContentContext.VIEW_MODE).getFreeContentContext());
	}

	public String getCurrentEditURL() {
		return URLHelper.createURL(ctx.getContextWithOtherRenderMode(ContentContext.EDIT_MODE).getFreeContentContext());
	}

	public String getCurrentPreviewURL() {
		return URLHelper.createURL(ctx.getContextWithOtherRenderMode(ContentContext.PREVIEW_MODE).getFreeContentContext());
	}

	public String getCurrentAjaxURL() {
		return URLHelper.createAjaxURL(ctx, ctx.getPath());
	}

	public String getPDFURL() {
		ContentContext pdfCtx = new ContentContext(ctx);
		pdfCtx.setFormat("pdf");
		return URLHelper.createURL(pdfCtx);
	}

	public String getDate() {
		try {
			return StringHelper.renderDate(currentPage.getContentDateNeverNull(ctx), globalContext.getShortDateFormat());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public Device getDevice() {
		return ctx.getDevice();
	}

	public String getEditLanguage() {
		return globalContext.getEditLanguage(ctx.getRequest().getSession());
	}

	public String getEncoding() {
		return ContentContext.CHARACTER_ENCODING;
	}

	public GenericMessage getGlobalMessage() {
		return MessageRepository.getInstance(ctx).getGlobalMessage();
	}

	public String getGlobalTitle() {
		try {
			return currentPage.getGlobalTitle(ctx);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public String getHomeAbsoluteURL() {
		return URLHelper.createURL(ctx.getContextForAbsoluteURL(), "/");
	}

	public String getContentLanguage() {
		return ctx.getContentLanguage();
	}

	public String getRequestContentLanguage() {
		return ctx.getRequestContentLanguage();
	}

	public String getLanguage() {
		return ctx.getLanguage();
	}

	public String getPageDescription() {
		try {
			return currentPage.getDescription(ctx);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public String getPageID() {
		return currentPage.getId();
	}

	public String getPageMetaDescription() {
		try {
			return currentPage.getMetaDescription(ctx);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public String getPageName() {
		return currentPage.getName();
	}

	public String getPageTitle() {
		try {
			return currentPage.getTitle(ctx);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public String getTime() {
		try {
			return StringHelper.renderTime(ctx, currentPage.getContentDateNeverNull(ctx));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public String getUserName() {
		return ctx.getCurrentUserId();
	}

	public MenuElement.PageBean getPage() {
		try {
			return currentPage.getPageBean(ctx);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public MenuElement.PageBean getRoot() {
		try {
			return currentPage.getRoot().getPageBean(ctx);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public List<MenuElement.PageBean> getPagePath() {

		MenuElement page = currentPage;

		List<MenuElement.PageBean> pagePath = new LinkedList<MenuElement.PageBean>();

		while (page.getParent() != null) {
			page = page.getParent();
			try {
				pagePath.add(0, page.getPageBean(ctx));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return pagePath;
	}

	public String getVersion() {
		return AccessServlet.VERSION;
	}

	public Collection<String> getContentLanguages() {
		return globalContext.getContentLanguages();
	}

	public Collection<String> getLanguages() {
		return globalContext.getLanguages();
	}

	public String getEditTemplateURL() {
		EditContext editContext = EditContext.getInstance(globalContext, ctx.getRequest().getSession());
		return URLHelper.createStaticURL(ctx, editContext.getEditTemplateFolder());
	}

	public String getStaticRootURL() {
		return URLHelper.createStaticURL(ctx, "/");
	}
	
	public String getContextDownloadURL() {
		return URLHelper.createStaticURL(ctx, "/context");
	}

	public String getCaptchaURL() {
		return URLHelper.createStaticURL(ctx, "/captcha.jpg");
	}

	public int getPreviewVersion() {
		try {
			return PersistenceService.getInstance(globalContext).getVersion();
		} catch (ServiceException e) {
			e.printStackTrace();
			return -1;
		}
	}

	public Collection<String> getRoles() {
		IUserFactory userFactory = UserFactory.createUserFactory(globalContext, ctx.getRequest().getSession());
		return userFactory.getAllRoles(globalContext, ctx.getRequest().getSession());
	}
	
	public MenuElement.PageBean getParent() {
		if (currentPage.getParent() != null) {
			try {
				return currentPage.getParent().getPageBean(ctx);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public String getCopiedPath() {
		ContentContext copyCtx = EditContext.getInstance(globalContext, ctx.getRequest().getSession()).getContextForCopy(ctx);
		if (copyCtx != null) {
			if (!ctx.getPath().startsWith(copyCtx.getPath())) {
				return copyCtx.getPath();
			}
		}
		return null;
	}

	public String getPrivateHelpURL() {
		return globalContext.getPrivateHelpURL();
	}

	public Collection<String> getAdminRoles() {
		return globalContext.getAdminUserRoles();
	}

	public boolean isOpenExternalLinkAsPopup() {
		return globalContext.isOpenExternalLinkAsPopup();
	}

	public String getTemplateFolder() {
		try {
			if (ctx.getCurrentTemplate() != null) {
				return ctx.getCurrentTemplate().getFolder(globalContext);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getAbsoluteTemplateFolder() {
		try {
			if (ctx.getCurrentTemplate() != null) {
				return URLHelper.createStaticTemplateURL(ctx, "/");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getShortURL() {
		try {
			return URLHelper.createStaticURL(ctx.getContextForAbsoluteURL(), ctx.getCurrentPage().getShortURL(ctx));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public Collection<String> getTags() {
		return globalContext.getTags();
	}

	/**
	 * return the name of the first level page active. "root" if current page in root.
	 * 
	 * @return
	 * @throws Exception
	 */
	public String getSection() {
		MenuElement page;
		try {
			page = ctx.getCurrentPage();
		} catch (Exception e) {
			return "page-not-found";
		}
		if (page == null) {
			return "page-not-found";
		}
		if (page.getParent() == null) {
			return "root";
		} else {
			while (page.getParent().getParent() != null) {
				page = page.getParent();
			}
		}
		return page.getName();
	}

	public Template getTemplate() {
		try {
			return ctx.getCurrentTemplate();
		} catch (Exception e) {
			return null;
		}
	}

	public String getPathPrefix() {
		return URLHelper.getPathPrefix(ctx);
	}

	public boolean isGod() {
		return AdminUserSecurity.getInstance().isGod(ctx.getCurrentUser());
	}

	public boolean isAdmin() {
		return AdminUserSecurity.getInstance().isAdmin(ctx.getCurrentUser());
	}

	public String getPath() {
		return ctx.getPath();
	}

	public boolean isPreview() {
		return ctx.getRenderMode() == ContentContext.PREVIEW_MODE;
	}

	public String getPublishDate() {
		try {
			return StringHelper.renderShortDate(ctx, globalContext.getPublishDate());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public boolean isAccountSettings() {
		User user = AdminUserFactory.createUserFactory(ctx.getRequest()).getCurrentUser(ctx.getRequest().getSession());
		if ((!globalContext.isMaster() && AdminUserSecurity.getInstance().isMaster(user))) {
			return false;
		} else if (AdminUserSecurity.getInstance().isGod(user)) {
			return false;
		} else {
			return true;
		}

	}

	public boolean isNewSession() {
		if (StringHelper.isTrue(ctx.getRequest().getParameter(NEW_SESSION_PARAM))) {
			return true;
		} else {
			return ctx.getRequest().getSession().isNew();
		}
	}

	/**
	 * this method return true at the first call for current session and false afer.
	 * 
	 * @return
	 */
	public boolean isFirstCallForSession() {
		String KEY = "firscall_" + InfoBean.class.getCanonicalName();
		if (ctx.getRequest().getSession().getAttribute(KEY) != null) {
			return false;
		} else {
			ctx.getRequest().getSession().setAttribute(KEY, true);
			return true;
		}
	}

	public String getArea() {
		return ctx.getArea();
	}

	public boolean isEditPreview() {
		return ctx.isEditPreview();
	}

	public PageBean getFirstLevelPage() {
		MenuElement page;
		try {
			page = ctx.getCurrentPage();
			if (page.getParent() == null) {
				return page.getPageBean(ctx);
			} else {
				while (page.getParent().getParent() != null) {
					page = page.getParent();
				}
			}
			return page.getPageBean(ctx);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	public User getEditUser() {
		return ctx.getCurrentEditUser();
	}

	public String getAbsoluteURLPrefix() {
		return URLHelper.createStaticURL(ctx.getContextForAbsoluteURL(), "/");
	}

	public String getServerTime() {
		return StringHelper.renderTime(new Date());
	}

	public boolean isTools() {
		return tools;
	}

	public void setTools(boolean actionBar) {
		this.tools = actionBar;
	}
	
	public boolean isLocalModule() {
		String localModulePath = ctx.getRequest().getSession().getServletContext().getRealPath("/webstart/localmodule.jnlp.jsp");
		return (new File(localModulePath)).isFile();
	}
	
	/**
	 * timestamp initialised when java VM is staded.
	 * @return
	 */
	public String getTs() {
		return ts;
	}
}
