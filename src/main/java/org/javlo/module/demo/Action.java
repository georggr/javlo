package org.javlo.module.demo;

import javax.servlet.http.HttpSession;

import org.javlo.actions.AbstractModuleAction;
import org.javlo.context.ContentContext;
import org.javlo.module.core.ModuleException;
import org.javlo.module.core.ModulesContext;
import org.javlo.user.User;

public class Action extends AbstractModuleAction {

	@Override
	public String getActionGroupName() {
		return "demo";
	}

	@Override
	public String prepare(ContentContext ctx, ModulesContext moduleContext) {
		if (ctx.getRequest().getAttribute("demoMessage") == null) {
			ctx.getRequest().setAttribute("demoMessage", "message from action prepare");
		}
		return null;
	}

	public static final String performTest(ContentContext ctx) {
		ctx.getRequest().setAttribute("demoMessage", "test performed");
		return null;
	}

	@Override
	public Boolean haveRight(HttpSession session, User user) throws ModuleException {
		return true;
	}

}
