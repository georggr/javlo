<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><%@ taglib uri="/WEB-INF/javlo.tld" prefix="jv"
%><%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" 
%><%@page contentType="text/html" import="
    	    org.javlo.helper.XHTMLHelper,
    	    org.javlo.helper.URLHelper,
    	    org.javlo.helper.MacroHelper,
    	    org.javlo.component.core.IContentVisualComponent,
    	    org.javlo.helper.XHTMLNavigationHelper,
    	    org.javlo.context.ContentContext,
    	    org.javlo.module.core.ModulesContext,
    	    org.javlo.context.GlobalContext,
    	    org.javlo.module.content.Edit,
    	    org.javlo.message.MessageRepository,
    	    org.javlo.message.GenericMessage"
%><%
ContentContext ctx = ContentContext.getContentContext(request, response);
ContentContext editCtx = new ContentContext(ctx);
editCtx.setRenderMode(ContentContext.EDIT_MODE);
ContentContext returnEditCtx = new ContentContext(editCtx);
returnEditCtx.setEditPreview(false);
GlobalContext globalContext = GlobalContext.getInstance(request);
ModulesContext moduleContext = ModulesContext.getInstance(request.getSession(), globalContext);
MessageRepository msgRepo = MessageRepository.getInstance(request); // load request message

GenericMessage msg = msgRepo.getGlobalMessage();
boolean rightOnPage = Edit.checkPageSecurity(ctx);
msgRepo.setGlobalMessageForced(msg);
String readOnlyPageHTML = "";
String readOnlyClass = "access";
String accessType = "submit";
if (!rightOnPage) {
	readOnlyPageHTML = " disabled";
	readOnlyClass = "no-access";
	accessType = "button";
}
request.setAttribute("editUser", ctx.getCurrentEditUser());
%><c:set var="pdf" value="${info.device.code == 'pdf'}" /><div id="preview_command" lang="${info.editLanguage}" class="edit-${not empty editUser} ${editPreview == 'true'?'edit':'preview'}">
	<script type="text/javascript">	
		var i18n_preview_edit = "${i18n.edit['component.preview-edit']}";	
		var i18n_first_component = "${i18n.edit['component.insert.first']}";
	</script>
	<div id="modal-container" style="display: none;">[modal]</div>
	<div class="header">	
		<nav class="navbar navbar-default navbar-left">
  			<div class="container-fluid">    
    			<div class="navbar-header">      				
      				<a class="navbar-brand" href="#">Javlo</a>
      				<img class="ajax-loading" src="${info.ajaxLoaderURL}" alt="loading..." lang="en" />
    			</div>
			</div>			
			<div class="collapse navbar-collapse">
      			<ul class="nav navbar-nav menu">      				
      				<li${info.page.root?' class="active"':''}><a class="home" title="home" href="<%=URLHelper.createURL(ctx,"/")%>"><span aria-hidden="true" class="glyphicon glyphicon-home" aria-hidden="true"></span></a></li>
      				<li${!info.page.root?' class="active action-title"':'class="action-title"'}><span class="inwrapper"><span class="glyphicon glyphicon-edit" aria-hidden="true"></span>${info.template.mailing?'edit mailing':'edit content'}</span></li>
      			</ul><ul class="nav navbar-nav actions">
      				<li class="page-title"><span class="inwrapper"><h1>${info.page.rootOfChildrenAssociation.title}</h1></span></li>
      				<c:if test="${not empty editUser}">
      				<c:if test="${fn:length(contentContext.deviceNames)>1}">
					<li class="renderers"><form id="renderers_form" action="${info.currentURL}" method="post">
						<div class="input-wrapper">
							<c:url var="url" value="${info.currentURL}" context="/">
								<c:param name="${info.staticData.forceDeviceParameterName}" value=""></c:param>
							</c:url>							
							<select class="form-control" id="renderers_button" onchange="window.location='${url}'+pjq('#renderers_button option:selected').val();" data-toggle="tooltip" data-placement="left" title="${i18n.edit['command.renderers']}">
								<c:forEach var="renderer" items="${contentContext.deviceNames}">
									<c:url var="url" value="${info.currentURL}" context="/">
										<c:param name="${info.staticData.forceDeviceParameterName}" value="${renderer}"></c:param>
									</c:url>									 
									<option${info.device.code eq renderer?' selected="selected"':''}>${renderer}</option>
								</c:forEach>
							</select>
						</div>
					</form></li></c:if>
					<li><form class="${info.page.pageEmpty?'no-access':''}" id="copy_page" action="${info.currentURL}?webaction=edit.copyPage" method="post">
						<button id="pc_copy_page" type="submit" class="btn btn-default btn-xs"><span class="glyphicon glyphicon-copy" aria-hidden="true"></span><span class="text">${i18n.edit['action.copy-page']}</span></button>
					</form></li>
					<li><form class="${empty info.contextForCopy || !info.page.pageEmpty?'no-access':''}" id="paste_page" action="${info.currentURL}" method="post">
						<input type="hidden" name="webaction" value="edit.pastePage" />
						<button class="btn btn-default btn-xs" id="pc_paste_page" type="submit" ${empty info.contextForCopy || !info.page.pageEmpty?'disabled="disabled"':''}>
						<span class="glyphicon glyphicon-paste" aria-hidden="true"></span><span class="text">${i18n.edit['action.paste-page-preview']}</span>
						</button>							
					</form></li>
					<li><form class="${empty info.contextForCopy || !info.page.pageEmpty?'no-access':''}" action="${info.currentURL}" method="get">						
						<button class="btn btn-default btn-xs btn-refresh" id="pc_paste_page" type="submit"><span class="glyphicon glyphicon-refresh" aria-hidden="true"></span><span class="text">${i18n.edit['global.refresh']}</span></button>							
					</form></li>
					<li><form id="pc_del_page_form" class="<%=readOnlyClass%>" action="${info.currentURL}" method="post">
						<div>
							<input type="hidden" value="${info.pageID}" name="page"/>
							<input type="hidden" value="edit.deletePage" name="webaction"/>
							<c:if test="${!info.page.root}">
							<button class="btn btn-default btn-xs btn-delete" type="submit" onclick="if (!confirm('${i18n.edit['menu.confirm-page']}')) return false;">
								<span class="glyphicon glyphicon-trash" aria-hidden="true"></span><span class="text">${i18n.edit['menu.delete']}</span>
							</button>							
							</c:if><c:if test="${info.page.root}">
							<button class="btn btn-default btn-delete" type="button" onclick="if (!confirm('${i18n.edit['menu.confirm-page']}')) return false;" disabled="disabled"><span class="text">${i18n.edit['menu.delete']}</span></button>
							</c:if>
						</div>
					</form></li>	
					</c:if>
        		</ul>
        	</div>
		</nav>
		<nav class="navbar navbar-default navbar-right">
			<div class="collapse navbar-collapse">
      			<ul class="nav navbar-nav">
      				<li><c:if test="${globalContext.previewMode}"><form id="pc_form" action="${info.currentURL}" method="post">						
						<div class="pc_line">
							<input type="hidden" name="webaction" value="edit.previewedit" />
							<c:if test='${!editPreview}'>								 
								<button class="btn btn-default btn-xs" type="submit"><span class="glyphicon glyphicon-eye-open" aria-hidden="true"></span>${i18n.edit['preview.label.edit-page']}"</button>								
							</c:if> 
							<c:if test='${editPreview}'>								
								<button class="btn btn-default btn-xs" type="submit"><span class="glyphicon glyphicon-eye-open" aria-hidden="true"></span>${i18n.edit['preview.label.not-edit-page']}</button>								
							</c:if>
						</div>				
					</form></c:if>
					<c:if test="${!globalContext.previewMode}">
						<div class="link-wrapper">
						<a class="btn btn-default btn-xs" href="${info.currentViewURL}" target="_blank"><span class="glyphicon glyphicon-eye-open" aria-hidden="true"></span>${i18n.edit['preview.label.not-edit-page']}</a>
						</div>
					</c:if>					
					</li>
					<c:if test="${!pdf}"><li>
					<c:url var="url" value="<%=URLHelper.createURL(editCtx)%>" context="/">	
						<c:param name="module" value="mailing"></c:param>
						<c:param name="previewEdit" value="true"></c:param>
					</c:url>					
					<form>					
					<button class="btn btn-default btn-xs btn-send btn-color" type="<%=accessType%>" value="${i18n.edit['preview.label.mailing']}" onclick="editPreview.openModal('${i18n.edit['preview.label.mailing']}','${url}'); return false;">
						<span class="glyphicon glyphicon-send" aria-hidden="true"></span>${i18n.edit['preview.label.mailing']}
					</button>
					</form>							
					</li></c:if>
					<c:if test="${pdf}"><li>
					<li><form id="export_pdf_page_form" action="${info.currentPDFURL}" method="post" target="_blanck">						
						<button class="btn btn-default btn-xs btn-pdf btn-color" id="export_pdf_button" type="submit" value="${i18n.edit['preview.label.pdf']}">${i18n.edit['preview.label.pdf']}</button>
					</form></li>
					</c:if>					  				
      				<c:if test="${globalContext.previewMode}"><li class="publish"><form id="pc_publish_form" action="${info.currentURL}" method="post">						
						<input type="hidden" name="webaction" value="edit.publish" />						
						<button type="submit" class="btn btn-default btn-xs">
							<span class="glyphicon glyphicon-arrow-up" aria-hidden="true"></span>
							${i18n.edit['command.publish']}
						</button>						
					</form></li></c:if>      				      				
      				<c:if test="${not empty editUser}">        								
	        			<li class="user"><c:if test="${!userInterface.contributor}"><a id="pc_edit_mode_button" title="${i18n.edit['global.exit']}" href="<%=URLHelper.createURL(returnEditCtx)%>">X</a></c:if>
							<c:url var="url" value="<%=URLHelper.createURL(returnEditCtx)%>" context="/">
								<c:param name="edit-logout" value="true" />
							</c:url>
						<c:if test="${userInterface.contributor}">
						<a href="${info.currentEditURL}?module=users&webaction=user.changeMode&mode=myself&previewEdit=true" class="as-modal"><span class="glyphicon glyphicon-user" aria-hidden="true"></span>${info.userName}</a>
						<a id="pc_edit_mode_button" class="logout" title="${i18n.edit['global.logout']}" href="${url}">${i18n.edit["global.logout"]}</a>
						</c:if></li>
					</c:if>
        		</ul>
        	</div>
		</nav>
	</div>
	<div class="sidebar panel panel-default">
		<div class="panel-body">
			<c:if test="${empty editUser}">
			<div class="panel panel-default">
			<div class="panel-heading">${i18n.edit['login.authentification']}</div>
			<div class="panel-body">
				<form method="post" action="${info.currentURL}" id="_ep_login">
			    	<div class="form-group">
		    			<input type="hidden" name="login-type" value="adminlogin">
		    			<input type="hidden" name="edit-login" value="edit-login">    		    		
		            	<input type="text" name="j_username" id="j_username" class="form-control" placeholder="${i18n.edit['login.user']}">
		            </div><div class="form-group">
		            	<input type="password" name="j_password" id="j_password" class="form-control" placeholder="${i18n.edit['login.password']}">
		            </div>
		            <button class="btn btn-default pull-right">Login</button>	        	        
				</form>
			</div>
			</div>
			</c:if><c:if test="${not empty editUser}">
			<div role="tabpanel">  
				<ul class="nav nav-tabs" role="tablist">
				  <li role="presentation" class="active"><a href="#_ep_navigation" aria-controls="_ep_navigation" role="tab" data-toggle="tab">Navigation</a></li>
				  <li role="presentation"><a href="#_ep_settings" aria-controls="_ep_settings" role="tab" data-toggle="tab">Settings</a></li>
				  <li role="presentation"><a href="#_ep_content" aria-controls="_ep_content" role="tab" data-toggle="tab">Content</a></li>
				  <li role="presentation"><a href="#_ep_files" aria-controls="_ep_files" role="tab" data-toggle="tab">Files</a></li>
				</ul>
				<div class="tab-content">
				  <div role="tabpanel" class="tab-pane fade in active navigation_panel" id="_ep_navigation">
				  <c:if test="${contentContext.currentTemplate.mailing}">							
					<jsp:include page="bootstrap/navigation_mailing.jsp"></jsp:include>
				  </c:if><c:if test="${!contentContext.currentTemplate.mailing}">							
					<jsp:include page="bootstrap/navigation.jsp"></jsp:include>
				   </c:if>
				  </div>
				  <div role="tabpanel" class="tab-pane fade" id="_ep_settings"><jsp:include page="bootstrap/settings.jsp" /></div>
				  <div role="tabpanel" class="tab-pane fade" id="_ep_content"><jsp:include page="bootstrap/component.jsp" /></div>
				  <div role="tabpanel" class="tab-pane fade" id="_ep_files"><jsp:include page="bootstrap/shared_content.jsp" /></div>
				</div>
			</div>
			</c:if>
		</div>
	</div>
	
	<div class="modal fade fancybox-wrapper" id="preview-modal" tabindex="-1" role="dialog" aria-labelledby="previewModalTitle" aria-hidden="true">
	  <div class="modal-dialog modal-full fancybox-skin">
      <div class="fancybox-outer">
  	    <div class="modal-content fancybox-inner">
          <div class="for-fancy">
    	      <div class="modal-header page-title">
    	        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
    	        <h4 class="modal-title" id="previewModalTitle">[title]</h4>
    	      </div>
            <div class="box">
      	      <div class="modal-body tabs-edit-fancy">
      	        <iframe id="preview-modal-frame" data-wait="/wait.html" src="/wait.html" ></iframe>
      	      </div>
      	      <div class="modal-footer box-foot">
      	        <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>        
      	      </div>
            </div>
          </div>
  	    </div>
      </div>
	  </div>
	</div>
	
</div>	 