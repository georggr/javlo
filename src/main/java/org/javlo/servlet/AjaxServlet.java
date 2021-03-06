package org.javlo.servlet;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.javlo.context.ContentContext;
import org.javlo.context.EditContext;
import org.javlo.context.GlobalContext;
import org.javlo.data.InfoBean;
import org.javlo.helper.AjaxHelper;
import org.javlo.helper.ServletHelper;
import org.javlo.helper.StringHelper;
import org.javlo.module.core.ModulesContext;
import org.javlo.service.NotificationService;
import org.javlo.service.PersistenceService;
import org.javlo.service.RequestService;
import org.javlo.tracking.Tracker;
import org.javlo.utils.DebugListening;
import org.javlo.utils.JSONMap;

public class AjaxServlet extends HttpServlet {

	private static final long serialVersionUID = -2086338711912267925L;

	/**
	 * create a static logger.
	 */
	protected static Logger logger = Logger.getLogger(AjaxServlet.class.getName());

	@Override
	public void init() throws ServletException {
		super.init();
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		process(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		process(request, response);
	}

	private void process(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		try {
			ContentContext ctx = ContentContext.getContentContext(request, response);
			GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
			try {

				Tracker.trace(request, response);

				RequestService rs = RequestService.getInstance(request);
				if (rs.getParameter(ContentContext.FORCE_MODE_PARAMETER_NAME, null) != null) {
					ctx.setRenderMode(Integer.parseInt(rs.getParameter(ContentContext.FORCE_MODE_PARAMETER_NAME, null)));
				} else {
					ctx.setRenderMode(ContentContext.PREVIEW_MODE);
				}

				ctx.setAjax(true);
				EditContext editCtx = EditContext.getInstance(globalContext, request.getSession());
				request.setAttribute("editUser", editCtx.getEditUser());
				request.setAttribute("editPreview", editCtx.isEditPreview());

				/*** update module status before action ***/
				ModulesContext moduleContext = ModulesContext.getInstance(request.getSession(), globalContext);
				moduleContext.initContext(request, response);

				InfoBean.updateInfoBean(ctx);

				String action = ServletHelper.execAction(ctx);
				if (action != null) {
					logger.info("exec action : " + action);
				}

				ServletHelper.prepareModule(ctx);

				boolean onlyData = StringHelper.isTrue(rs.getParameter("data", null));

				Map<String, Object> outMap = new HashMap<String, Object>();
				StringWriter strWriter = new StringWriter();

				if (ctx.getSpecificJson() == null) {
					if (ctx.getAjaxMap() == null) {
						String msgXhtml = ServletHelper.executeJSP(ctx, editCtx.getMessageTemplate());
						ctx.addAjaxInsideZone("message-container", msgXhtml);
						if (editCtx.getUserPrincipal() != null) {
							int unreadNotification = NotificationService.getInstance(globalContext).getUnreadNotificationSize(editCtx.getUserPrincipal().getName(), 99);
							ctx.addAjaxInsideZone("notification-count", "" + unreadNotification);
						}
						AjaxHelper.render(ctx, ctx.getAjaxInsideZone(), ctx.getScheduledAjaxInsideZone());
						if (!onlyData) {
							outMap.put("insideZone", ctx.getAjaxInsideZone());
							outMap.put("zone", ctx.getAjaxZone());
						}
						if (ctx.getAjaxData().size() > 0) {
							outMap.put("data", ctx.getAjaxData());
						}
						JSONMap.JSON.toJson(outMap, strWriter);
					} else {
						for (Object key : ctx.getAjaxMap().keySet()) {
							outMap.put("" + key, ctx.getAjaxMap().get(key));
						}
						JSONMap.JSON.toJson(outMap, strWriter);
					}
					strWriter.flush();
				}

				response.setContentType("application/json");
				String jsonResult = strWriter.toString();
				if (ctx.getSpecificJson() != null) {
					response.getWriter().write(ctx.getSpecificJson());
				} else {
					response.getWriter().write(jsonResult);
				}
				response.flushBuffer();

			} catch (Throwable t) {
				t.printStackTrace();
				response.setStatus(503);
				DebugListening.getInstance().sendError(request, t, "path=" + request.getRequestURI());
			} finally {
				PersistenceService persistenceService = PersistenceService.getInstance(globalContext);
				if (persistenceService.isAskStore()) {
					persistenceService.store(ctx);
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
