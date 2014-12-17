package org.javlo.macro;

import java.util.Map;

import org.javlo.context.ContentContext;
import org.javlo.context.EditContext;
import org.javlo.context.GlobalContext;
import org.javlo.helper.MacroHelper;
import org.javlo.navigation.MenuElement;
import org.javlo.service.ContentService;

public class DuplicateChildren extends AbstractMacro {

	@Override
	public String getName() {
		return "duplicate-children";
	}

	@Override
	public String perform(ContentContext ctx, Map<String, Object> params) throws Exception {
		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
		EditContext editContext = EditContext.getInstance(ctx.getGlobalContext(), ctx.getRequest().getSession());
		if (editContext.getContextForCopy(ctx) == null) {
			return "no source page in ClipBoard.";
		} else {
			ContentService content = ContentService.getInstance(ctx.getRequest());
			MenuElement sourcePage = content.getNavigation(ctx).searchChild(ctx, editContext.getContextForCopy(ctx).getPath());
			if (sourcePage == null) {
				return "source page not found.";				
			} else {
				MacroHelper.copyChildren(ctx, sourcePage, ctx.getCurrentPage(), sourcePage.getName(), ctx.getCurrentPage().getName());
			}
		}
		return null;
	}

	@Override
	public boolean isPreview() {
		return true;
	}

};
