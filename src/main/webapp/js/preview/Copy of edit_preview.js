var dragging = false;

jQuery(document).ready(
		function() {
			try {
				jQuery(".floating-preview #preview_command").draggable({
					handle : ".pc_header"
				});
				jQuery("body").append(
						'<div id="preview-layer"><span>&nbsp;</span></div>');
				jQuery("body").append(
						'<div id="droppable-layer"><span>&nbsp;</span></div>');
				jQuery("._page_associate").append('<div class="_pdf_page_limit"><span>&nbsp;</span></div>');
				
				jQuery( document ).tooltip({
					position: {
					my: "center bottom-20",
					at: "center top",					
					using: function( position, feedback ) {
						 jQuery( this ).css( position );
						 jQuery( "<div>" )
						 .addClass( "arrow" )
						 .addClass( feedback.vertical )
						 .addClass( feedback.horizontal )
						 .appendTo( this );
					  }
					},
					items: "#preview-layer",
					show: {delay:500}
				});
				 
			} catch (err) {
				if (typeof console != 'undefined') {
					console.log("error on : " + err);
				}
			}

			initPreview();

			jQuery("body").mouseover(function() {				
				return true;
			});
			jQuery(window).scroll(function() {				
				layerOver(null, false);
				return true;
			});
			jQuery("#preview-layer").click(
					function() {
						layerOver(null);
						jQuery(this).data("subItem").trigger("click",
								jQuery(this).data("subItem"));
						return true;
					});
			jQuery(".preview-edit").click(function() {
				var elems = jQuery(this);
				var editURL = elems.attr("action");
				var param = "";
				jQuery.colorbox({
					href : editURL + param,
					opacity : 0.6,
					iframe : true,
					width : "95%",
					height : "95%"
				});
				return false;
			});
			
			/////////// SORTABLE /////////////
			jQuery(".sortable").sortable({
				   placeholder: "sortable-target"
				   ,stop: function(event, ui) {
					   var url = jQuery("#children_list").attr("action");
					   url=url+"?webaction=edit.movePage&page="+jQuery(ui.item).attr("id")+"&previous="+jQuery(ui.item).prev().attr("id");
					   ajaxRequest(url);
				   }
			});
		});

layerOver = function(item, deletable) {	
	var layer = jQuery("#preview-layer");	
	layer.data("deletable", deletable);
	
	var insideLayer = jQuery("#preview-layer span");
	if (item == null) {		
		layer.css("z-index", -1);
		layer.css("display", "none");
		layer.data("compType", null);
		layer.data("sharedContent", null);
		layer.attr("title", "");
	} else {		
		var comp = jQuery(item);
		if (layer.width() > 0) {
			layer.css("z-index", 10000);
			layer.css("display", "block");
		}
		
		if (comp.hasClass("editable-component")) {
			layer.attr("title", "move with drag and drop or click to edit.");
		} else {
			layer.attr("title", "move with drag and drop.");
		}
		
		layer.css("top", comp.offset().top);
		layer.css("left", comp.offset().left);
		
		layer.css("width", comp.outerWidth(false));			
		layer.css("height", comp.outerHeight(false));
		
		layer.data("subItem", comp);
	}
}

extractId = function(data) {
	console.log("data="+data);
	return data.replace("cp_", "");
}

_extractId = function(data) {
	console.log("data="+data);
	if (data.indexOf("cp_")>=0) {
		var dataSplitted = data.split("cp_");
		if (dataSplitted.length == 0) {
			return dataSplitted[0];
		} else {
			return data.split("cp_")[1];
		}
	} else if (data.indexOf("comp_")>=0) {
		var dataSplitted = data.split("comp_");
		if (dataSplitted.length == 0) {
			return dataSplitted[0];
		} else {
			return data.split("comp_")[1];
		}		
	} else {
		return data;
	}
}

searchPageId = function(node) {
	var parent = jQuery(node).parent();
	while (parent !== undefined && !parent.hasClass("_area")) {
		parent = jQuery(parent).parent();
	}
	return jQuery(parent).attr("id");
}

initPreview = function() {
	
	jQuery(".editable-component").click(function() {

		layerOver(null);

		var elems = jQuery(this);
		var editURL = editPreviewURL + "&comp_id=" + elems.attr("id");
		var param = "";
		if (jQuery("#search-result").length > 0) {
			param = "&noinsert=true";
		}
		jQuery.colorbox({
			href : editURL + param,
			opacity : 0.6,
			iframe : true,
			width : "95%",
			height : "95%"
		});
		return false;
	});

	jQuery(".editable-component").mouseover(function() {
		if (!dragging) {			
			layerOver(this, true);
		}
		return false;
	});
	
	jQuery(".component-list .component, .shared-content .content").mouseover(function() {
		if (!dragging) {			
			layerOver(this, false);
			var layer = jQuery("#preview-layer");
			layer.data("compType", jQuery(this).data("type"));
			layer.data("sharedContent", jQuery(this).data("shared"));
		}
		return false;
	});
	
	jQuery(".component-list .component,  .shared-content .content").mouseout(function() {
		if (!dragging) {			
			layerOver(this, false);
		}
		
		return false;
	});

	jQuery("#preview-layer").on('dragover', function(e) {
		e.preventDefault();
		e.stopPropagation();
		layer = jQuery(this);
		var height = layer.height();
		if (height < 40) {
			height = 40;
		}					
		layer.css("height", height);
	
	});
	jQuery("#preview-layer").on('mouseout', function(e) {		
		if (!dragging) {
			layerOver(null);
		}
	});	
	jQuery("#preview-layer").on('dragenter', function(e) {		
		e.preventDefault();
		e.stopPropagation();
	});
	jQuery("#preview-layer").on('drop', function(e) {
		layerOver(null);					
		jQuery(this).data("compType", null);
		e.preventDefault();
		e.stopPropagation();
	});
	
	jQuery(".editable-component, .not-editable-component, #preview-delete-zone")
			.droppable(
					{
						cursor : 'move',
						greedy : true,
						tolerance : 'pointer',
						drop : function(event, ui) {
							var layer = jQuery("#preview-layer");
							var comp = layer.data("subItem");							
							var compType = layer.data("compType");
							var sharedContent = layer.data("sharedContent");
							var area = null;
							console.log("id="+extractId(jQuery(this).attr("id")));
							if (compType !== undefined && compType != null) {								
								var previewId = extractId(jQuery(this).attr("id"));
								var parent = jQuery(this).parent();
								while (jQuery(parent).get(0).tagName.toLowerCase() != "body" && !parent.hasClass("_area")) {									
									parent = jQuery(parent).parent();									
								}
								area = jQuery(parent).attr("id");
								var ajaxURL = currentURL
								+ "?webaction_1=edit.insert&webaction_2=data.updateArea&type="
								+ compType + "&previous=" + previewId
								+ "&area=" + area+ "&render-mode=3&init=true";
								ajaxRequest(ajaxURL);
							} else if (sharedContent != null && sharedContent !== undefined) {
								var previewId = extractId(jQuery(this).attr("id"));;
								var parent = jQuery(this).parent();
								while (jQuery(parent).get(0).tagName.toLowerCase() != "body" && !parent.hasClass("_area")) {									
									parent = jQuery(parent).parent();									
								}
								area = jQuery(parent).attr("id");
								var ajaxURL = currentURL
								+ "?webaction_1=edit.insertShared&webaction_2=data.updateArea&sharedContent="
								+ sharedContent + "&previous=" + previewId
								+ "&area=" + area+ "&render-mode=3&init=true";
								ajaxRequest(ajaxURL);
							} else if (comp !== undefined && jQuery(comp).attr("id") != null && jQuery(comp).attr('id') != jQuery(this).attr("id")) {
								var compId = extractId(jQuery(this).attr("id"));
								if (jQuery(this).attr("id") == "preview-delete-zone") {									
									var ajaxURL = currentURL
											+ "?webaction=edit.delete&id="
											+ compId+ "&render-mode=3";
									jQuery(comp).remove();
								} else if (comp !== undefined) {
									jQuery(comp).insertAfter(jQuery(this));
									var previewId = extractId(jQuery(this).attr("id"));									
									var parent = jQuery(comp).parent();
									while (parent !== undefined && !parent.hasClass("_area")) {
										parent = jQuery(parent).parent();
									}
									area = jQuery(parent).attr("id");
									var ajaxURL = currentURL
											+ "?webaction_1=edit.moveComponent&webaction_2=data.updateAllArea&comp-id="
											+ compId + "&previous=" + previewId
											+ "&area=" + area+ "&render-mode=3";
								}
								ajaxRequest(ajaxURL);
							}
							layerOver(null);
							jQuery(this).find(".drop-zone").remove();
							jQuery(this).removeClass("drop-selected");
						},
						over : function(event, ui) {
							dragging = true;
							var dropLayer = jQuery("#droppable-layer");
							var layer = jQuery("#preview-layer");
							var target = jQuery(this);
							
							if (!target.hasClass("free-edit-zone")) {
								if (target.find(".drop-zone").length == 0) {
									target.append('<div class="drop-zone"><span>&nbsp;</span></div>');
								}
							} else {
								target.addClass("drop-selected");
							}
							
							if (dropLayer.offset.top < 0) {
								dropLayer.css("top", "" + target.offset().top
										+ "px");
								dropLayer.css("left", "" + target.offset().left
										+ "px");
							}
							jQuery("#preview-layer").addClass("drop-up");
						},
						out : function(event,ui) {
							jQuery(this).find(".drop-zone").remove();
							jQuery(this).removeClass("drop-selected");
							jQuery("#preview-layer").removeClass("drop-up");
						}
					});

	dragging = false;

	jQuery("#preview-layer").draggable({
		cursor : "move",
		start : function(event, ui) {
			dragging = true;
			var layer = jQuery("#preview-layer");
			jQuery(".free-edit-zone").addClass("droppable");			
			if (jQuery(this).data("deletable") !== undefined && jQuery(this).data("deletable")) {
				jQuery("#preview-delete-zone").removeClass("hidden");
				//jQuery("#preview_command .pc_body").addClass("hidden");
			}
			if (jQuery(this).height() < 40) {			
				jQuery(this).height(40);
			};
		},
		stop : function(event, ui) {	
			var dropLayer = jQuery("#droppable-layer");
			dropLayer.css("top", "-9999px");
			dragging = false;
			jQuery(".free-edit-zone").removeClass("droppable");
			if (jQuery(this).data("deletable") !== undefined && jQuery(this).data("deletable")) {
				//jQuery("#preview_command .pc_body").removeClass("hidden");
				jQuery("#preview-delete-zone").addClass("hidden");
			}
			layerOver(null);
		}
	});
}

function doNothing(evt) {
	evt.stopPropagation();
	evt.preventDefault();
}

function hidePreviewMessage() {
	jQuery("#pc_message").remove();
}