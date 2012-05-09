package org.javlo.module;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;

import org.javlo.actions.IModuleAction;
import org.javlo.context.ContentContext;
import org.javlo.context.GlobalContext;
import org.javlo.helper.AjaxHelper;
import org.javlo.helper.StringHelper;
import org.javlo.helper.URLHelper;
import org.javlo.utils.ReadOnlyPropertiesMap;

public class Module {
	
	private static final String CSS_FOLDER = "css";
	
	private static final String JS_FOLDER = "js";

	public static final class EmptyAction implements IModuleAction {

		@Override
		public String getActionGroupName() {
			return "empty";
		}

		@Override
		public String prepare(ContentContext ctx, ModuleContext moduleContext) {
			return null;
		}

	}

	private static final EmptyAction emptyAction = new EmptyAction();

	public class Link {
		public Link(String params, String label, String style) {
			this.params = params;
			this.label = label;
			this.style = style;
		}

		private String params;
		private String label;
		private String style;
		private boolean active;

		public String getParams() {
			return params;
		}

		public String getLabel() {
			return label;
		}

		public String getStyle() {
			return style;
		}

		public boolean isActive() {
			return active;
		}

		public void setActive(boolean active) {
			this.active = active;
		}

	}

	public class Box {

		private Box(String title, String renderer, boolean action) {
			this.title = StringHelper.neverNull(title);
			this.renderer = URLHelper.mergePath(path, renderer);
			this.action = action;			
			id = "box-"+StringHelper.getRandomId();
		}

		private String title;
		private String renderer;
		private String id;
		private boolean action;
		

		public String getTitle() {
			return title;
		}

		public String getRenderer() {
			return renderer;
		}

		public boolean isAction() {
			return action;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public void setRenderer(String renderer) {
			this.renderer = renderer;
		}

		public void setAction(boolean action) {
			this.action = action;
		}
		public String getId() {
			return id;
		}
		
		/**
		 * this method is called when the box must be updated.  This method is used only in ajax context.
		 * @param ctx
		 * @throws IOException 
		 * @throws ServletException 
		 */
		public void update(ContentContext ctx) throws ServletException, IOException {
			AjaxHelper.updateBox(ctx, this);
		}

		public Module getModule() {
			return Module.this;
		}
		
	}

	private String name;
	private String title;
	private String path;
	private File moduleRoot;
	private boolean sidebar = false;
	private boolean breadcrumb = false;
	Collection<Box> mainBoxes = new LinkedList<Box>();
	Collection<Box> sideBoxes = new LinkedList<Box>();
	String navigationTitle = null;
	Collection<Box> navigation = new LinkedList<Box>();
	private String helpTitle = null;
	private String helpText = null;
	private String toolsTitle = null;
	private String toolsRenderer = null;	
	private String defaultToolsRenderer = null;
	private IModuleAction action = emptyAction;
	private Collection<String> cssURI = new LinkedList<String>();
	private Collection<String> jsURI = new LinkedList<String>();
	private String renderer;
	private String defaultRenderer;
	private Map<String,Box> boxes = new HashMap<String, Box>();
	private String backUrl = null;

	public Module(File configFile, Locale locale, String modulePath) throws IOException {
		FileReader fileReader = null;
		Map<String, String> config;
		try {
			fileReader = new FileReader(configFile);
			Properties properties = new Properties();
			properties.load(fileReader);
			config = new ReadOnlyPropertiesMap(properties);
		} finally {
			if (fileReader != null) {
				fileReader.close();
			}
		}
		path = modulePath.replace('\\', '/');
		name = config.get("name");
		breadcrumb = StringHelper.isTrue(config.get("breadcrumb"));
		title = config.get("title." + locale.getLanguage());
		if (title == null) {
			title = config.get("title");
		}
		
		moduleRoot = configFile.getParentFile();
		
		/* css */
		File cssFolder = new File(URLHelper.mergePath(moduleRoot.getAbsolutePath(), CSS_FOLDER));		
		if (cssFolder.isDirectory()) {
			File[] cssFiles = cssFolder.listFiles();
			for (File file : cssFiles) {				
				if (file.isFile() && StringHelper.getFileExtension(file.getName()).equalsIgnoreCase("css")) {					
					cssURI.add(ModuleContext.MODULES_FOLDER+'/'+getName()+'/'+CSS_FOLDER+'/'+file.getName());				
				}
			}			
		}
		
		/* js */
		File jsFolder = new File(URLHelper.mergePath(moduleRoot.getAbsolutePath(), JS_FOLDER));		
		if (jsFolder.isDirectory()) {
			File[] jspFiles = jsFolder.listFiles();
			for (File file : jspFiles) {				
				if (file.isFile() && StringHelper.getFileExtension(file.getName()).equalsIgnoreCase("js")) {					
					jsURI.add(ModuleContext.MODULES_FOLDER+'/'+getName()+'/'+JS_FOLDER+'/'+file.getName());				
				}
			}			
		}		
		
		/* main renderer */
		renderer = config.get("renderer");
		if (renderer != null) {
			renderer = URLHelper.mergePath(path, renderer);
			defaultRenderer = renderer;
		}
		
		/* tools */
		toolsTitle = config.get("tools.title." + locale.getLanguage());
		if (toolsTitle == null) {
			toolsTitle = config.get("tools.title");
		}
		toolsRenderer = config.get("tools.renderer");
		if (toolsRenderer != null) {
			toolsRenderer = URLHelper.mergePath(path, toolsRenderer);
			defaultToolsRenderer = toolsRenderer;
		}

		/* help text */
		helpTitle = config.get("help.title." + locale.getLanguage());
		if (helpTitle == null) {
			helpTitle = config.get("help.title");
		}
		helpText = config.get("help.text." + locale.getLanguage());
		if (helpText == null) {
			helpText = config.get("help.text");
		}

		/* navigation */
		navigationTitle = config.get("navigation.title" + locale.getLanguage());
		if (navigationTitle == null) {
			navigationTitle = config.get("navigation.title");
		}

		/* navigation */
		for (int i = 1; i < 100; i++) {
			String navigationBaseKey = "navigation." + i;
			String renderer = config.get(navigationBaseKey + ".renderer");

			if (renderer != null) {
				String boxTitle = config.get(navigationBaseKey + ".title." + locale.getLanguage());
				if (boxTitle == null) {
					boxTitle = config.get(navigationBaseKey + ".title");
				}
				Box box = new Box(boxTitle, renderer, StringHelper.isTrue(config.get(navigationBaseKey + ".action")));
				navigation.add(box);
				if (config.get(navigationBaseKey+".name") != null) { 
					boxes.put(config.get(navigationBaseKey+".name"), box);
				}
			} else {
				break;
			}
		}

		/* box */
		for (int i = 1; i < 100; i++) {
			String boxBaseKey = "box.main." + i;
			String renderer = config.get(boxBaseKey + ".renderer");

			if (renderer != null) {
				String boxTitle = config.get(boxBaseKey + ".title." + locale.getLanguage());
				if (boxTitle == null) {
					boxTitle = config.get(boxBaseKey + ".title");
				}
				Box box = new Box(boxTitle, renderer, StringHelper.isTrue(config.get(boxBaseKey + ".action")));
				mainBoxes.add(box);
				if (config.get(boxBaseKey+".name") != null) { 
					boxes.put(config.get(boxBaseKey+".name"), box);
				}
			} else {
				break;
			}
		}
		for (int i = 1; i < 100; i++) {
			String boxBaseKey = "box.side." + i;
			String renderer = config.get(boxBaseKey + ".renderer");
			if (renderer != null) {
				sidebar = true;
				String boxTitle = config.get(boxBaseKey + ".title." + locale.getLanguage());
				if (boxTitle == null) {
					boxTitle = config.get(boxBaseKey + ".title");
				}
				Box box = new Box(boxTitle, renderer, StringHelper.isTrue(config.get(boxBaseKey + ".action")));
				sideBoxes.add(box);
				if (config.get(boxBaseKey+".name") != null) { 
					boxes.put(config.get(boxBaseKey+".name"), box);
				}
			} else {
				break;
			}
		}

		/* action */
		String actionName = config.get("class.action");
		if (actionName != null) {
			try {
				action = (IModuleAction) Class.forName(actionName).newInstance();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public Collection<Box> getMainBoxes() {
		return mainBoxes;
	}

	public Collection<Box> getSideBoxes() {
		return sideBoxes;
	}

	public String getName() {
		return name;
	}

	public String getHelpTitle() {
		return helpTitle;
	}

	public String getHelpText() {
		return helpText;
	}
	
	public String getRenderer() {
		return renderer;
	}
	
	public void setRenderer(String renderer) {
		if (renderer != null) {
			this.renderer = URLHelper.mergePath(path, renderer);
		} else {
			this.renderer = null;
		}
	}
	
	/**
	 * restore the renderer of the config file.
	 */
	public void restoreRenderer() {
		renderer = defaultRenderer;
	}
	
	/**
	 * restore all element to value defined in the config.
	 */
	public void restoreAll() {
		restoreRenderer();
		restoreToolsRenderer();
	}

	public String getTitle() {
		if (title != null) {
			return title;
		} else {
			return getName();
		}
	}

	public boolean isSidebar() {
		return sidebar;
	}

	public String getNavigationTitle() {
		return navigationTitle;
	}

	public Collection<Box> getNavigation() {
		return navigation;
	}

	public IModuleAction getAction() {
		return action;
	}

	public boolean isBreadcrumb() {
		return breadcrumb;
	}

	public void setBreadcrumb(boolean breadcrumb) {
		this.breadcrumb = breadcrumb;
	}
	
	/**
	 * return the list of URL to CSS files of the module.
	 * @return
	 */
	public Collection<String> getCSS() {
		return cssURI;
	}
	
	/**
	 * return the list of URL to JS files of the module.
	 * @return
	 */
	public Collection<String> getJS() {
		return jsURI;
	}
	
	public Properties loadEditI18n (GlobalContext globalContext) throws IOException {
		File file = new File(moduleRoot.getAbsolutePath(), "edit_"+globalContext.getEditLanguage());
		if (!file.exists()) {
			file = new File(URLHelper.mergePath(moduleRoot.getAbsolutePath(), "/i18n/edit_"+globalContext.getDefaultEditLanguage()+".properties"));
		}	
		Properties prop = null;
		if (file.exists()) {		
			prop = new Properties();			
			FileReader reader = new FileReader(file);
			prop.load(reader);
			reader.close();
		}
		return prop;
	}
	
	public String getToolsTitle() {
		return toolsTitle;
	}

	public void setToolsTitle(String toolsTitle) {
		this.toolsTitle = toolsTitle;
	}

	public String getToolsRenderer() {
		return toolsRenderer;
	}

	public void setToolsRenderer(String toolsRenderer) {
		if (toolsRenderer != null) {
			this.toolsRenderer = URLHelper.mergePath(path, toolsRenderer);
		} else {
			this.toolsRenderer = null;
		}
	}
	
	/**
	 * restore the tools renderer of the config file.
	 */
	public void restoreToolsRenderer() {
		toolsRenderer = defaultToolsRenderer;
	}
	
	public Box getBox(String name) {
		return boxes.get(name);
	}
	
	public Box createMainBox(String name, String title, String renderer, boolean action) {
		Box box = new Box(title, renderer, action);
		mainBoxes.add(box);
		if (name != null) {
			boxes.put(name, box);
		}
		return box;
	}
	
	public Box createSideBox(String name, String title, String renderer, boolean action) {
		Box box = new Box(title, renderer, action);
		sideBoxes.add(box);
		if (name != null) {
			boxes.put(name, box);
		}
		return box;
	}
	
	public void clearAllBoxes() {
		boxes.clear();
		sideBoxes.clear();
		mainBoxes.clear();
	}

	public String getBackUrl() {
		return backUrl;
	}

	public void setBackUrl(String backUrl) {
		this.backUrl = backUrl;
	}
	
	public String getPath() {
		return path;
	}
 	
}
