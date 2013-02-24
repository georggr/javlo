package org.javlo.module.ticket;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.javlo.actions.AbstractModuleAction;
import org.javlo.context.ContentContext;
import org.javlo.context.GlobalContext;
import org.javlo.context.GlobalContextFactory;
import org.javlo.helper.StringHelper;
import org.javlo.i18n.I18nAccess;
import org.javlo.message.GenericMessage;
import org.javlo.message.MessageRepository;
import org.javlo.module.core.Module;
import org.javlo.module.core.ModulesContext;
import org.javlo.service.RequestService;
import org.javlo.user.AdminUserSecurity;
import org.javlo.user.User;

public class TicketAction extends AbstractModuleAction {

	@Override
	public String getActionGroupName() {
		return "ticket";
	}

	private static Map<String, TicketBean> getMyTicket(ContentContext ctx) throws IOException, ConfigurationException {
		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
		Map<String, TicketBean> myTickets = new HashMap<String, TicketBean>();

		String status = ctx.getRequest().getParameter("filter_status");

		if (globalContext.isMaster()) {
			Collection<GlobalContext> allContext = GlobalContextFactory.getAllGlobalContext(ctx.getRequest().getSession());
			for (GlobalContext gc : allContext) {
				TicketService ticketService = TicketService.getInstance(gc);
				for (TicketBean ticket : ticketService.getTickets()) {
					if (status == null || status.trim().length() == 0 || ticket.getStatus().equals(status)) {
						myTickets.put(ticket.getId(), ticket);
					}
				}
			}
		} else {

			TicketService ticketService = TicketService.getInstance(globalContext);
			for (TicketBean ticket : ticketService.getTickets()) {
				if (AdminUserSecurity.getInstance().isAdmin(ctx.getCurrentEditUser()) || ticket.getAuthors().equals(ctx.getCurrentEditUser().getLogin())) {
					if (status == null || status.trim().length() == 0 || ticket.getStatus().equals(status)) {
						myTickets.put(ticket.getId(), ticket);
					}
				}
			}
		}
		return myTickets;
	}

	@Override
	public String prepare(ContentContext ctx, ModulesContext modulesContext) throws Exception {
		String msg = super.prepare(ctx, modulesContext);

		RequestService rs = RequestService.getInstance(ctx.getRequest());

		Module ticketModule = modulesContext.getCurrentModule();
		Map<String, TicketBean> myTickets = getMyTicket(ctx);
		ctx.getRequest().setAttribute("tickets", myTickets.values());

		TicketBean ticket = myTickets.get(rs.getParameter("id", null));
		if (ticket != null && rs.getParameter("back", null) == null && !StringHelper.isTrue(ctx.getRequest().getAttribute("back-list"))) {
			if (ticket != null && ticket.getAuthors().equals(ctx.getCurrentEditUser().getLogin())) {
				ticket.setRead(true);
			}
			ctx.getRequest().setAttribute("ticket", ticket);
			if (ticketModule.getBox("main") == null) {
				ticketModule.addMainBox("main", "update ticket : " + rs.getParameter("id", ""), "/jsp/update_ticket.jsp", true);
				ticketModule.setRenderer(null);
			}
		} else {
			ticketModule.setRenderer("/jsp/list.jsp");
			ticketModule.restoreBoxes();
		}

		return msg;
	}

	public static String performList(ContentContext ctx) throws ConfigurationException, IOException {
		ctx.getAjaxData().put("tickets", getMyTicket(ctx));
		return null;
	}

	public static String performUpdate(RequestService rs, ContentContext ctx, GlobalContext globalContext, User user, MessageRepository messageRepository, I18nAccess i18nAccess) throws Exception {
		TicketService ticketService = TicketService.getInstance(globalContext);
		String id = rs.getParameter("id", "");
		TicketBean ticket;
		if (id.trim().length() > 0) { // update
			ticket = getMyTicket(ctx).get(id);
			if (ticket == null) {
				return "ticket not found : " + id;
			} else {
				ticket.setLastUpdateDate(new Date());
				ticket.setLatestEditor(user.getLogin());
			}
		} else { // create
			if (rs.getParameter("title", "").trim().length() == 0) {
				return "please enter a title.";
			}
			ticket = new TicketBean();
			ticket.setAuthors(user.getLogin());
			ticket.setContext(globalContext.getContextKey());
		}
		ticket.setTitle(rs.getParameter("title", ticket.getTitle()));
		ticket.setPriority(Integer.parseInt(rs.getParameter("priority", "" + ticket.getPriority())));
		ticket.setCategory(rs.getParameter("category", ticket.getCategory()));
		ticket.setMessage(rs.getParameter("message", ticket.getMessage()));
		ticket.setStatus(rs.getParameter("status", ticket.getStatus()));
		ticket.setUrl(rs.getParameter("url", ticket.getUrl()));
		ticket.setDeleted(rs.getParameter("delete", null) != null);
		if (rs.getParameter("comment", "").trim().length() > 0) {
			ticket.addComments(new Comment(user.getLogin(), rs.getParameter("comment", "")));
			if (!ticket.getAuthors().equals(ctx.getCurrentEditUser().getLogin())) {
				ticket.setRead(false);
			}
		} else {
			ctx.getRequest().setAttribute("back-list", true);
		}
		ticketService.updateTicket(ticket);

		messageRepository.addMessage(new GenericMessage("ticket updated.", GenericMessage.INFO));
		return null;
	}
}