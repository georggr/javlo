<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"
%><c:set var="styleWidth" value="" /><c:set var="styleWidthWidthoutStyle" value="" /><c:if test="${not empty componentWidth && !param['clean-html']}"><c:set var="styleWidthWidthoutStyle" value='width: ${componentWidth};' /><c:set var="styleWidth" value=' style="width: ${componentWidth};"' /></c:if>
<div id="comp-${compid}"><table class="${empty componentWidth?'float-image':'float-image-width'} ${not empty param.right?'right':'left'}">
<c:if test="${info.device.code != 'pdf'}">
<c:if test="${empty param.right}">
<tr><td class="image-wrapper"${styleWidth}>
<c:if test="${url != '#'}">
<c:set var="rel" value="${fn:startsWith(url,'http://')?'external':'shadowbox'}" />
<c:set var="rel" value="${fn:endsWith(url,'.pdf')?'pdf':rel}" />
<a rel="${rel}" class="${type}" href="${url}" title="${not empty label?cleanLabel:cleanDescription}"></c:if>
<img src="${fn:replace(previewURL,'/full/', '/float/')}" />
<c:if test="${url != '#'}"></a></c:if>
</td><td class="sep" style="width: 10px; font-size: 0;">&nbsp;</td><td class="zone1" style="text-align: left;"><span class="container">${comp.firstText}</span></td></tr>
</c:if><c:if test="${not empty param.right}">
<tr><td class="zone1" style="text-align: left;"><span class="container">${comp.firstText}</span></td>
<td class="sep" style="width: 10px; font-size: 0;">&nbsp;</td>
<td class="image-wrapper"${styleWidth}>
<c:if test="${url != '#'}">
<c:set var="rel" value="${fn:startsWith(url,'http://')?'external':'shadowbox'}" />
<c:set var="rel" value="${fn:endsWith(url,'.pdf')?'pdf':rel}" />
<a rel="${rel}" class="${type}" href="${url}" title="${not empty label?cleanLabel:cleanDescription}"></c:if>
<img ${not empty imageWidth?imageWidthTag:''}src="${fn:replace(previewURL,'/full/', '/float/')}" />
<c:if test="${url != '#'}"></a></c:if>
</td></tr>
</c:if>
<tr><td class="zone2" colspan="3" style="text-align: left; ${areaStyle.textStyle}${styleWidth}"><span class="container">${comp.secondText}</span></td></tr>
</c:if>
<c:if test="${info.device.code == 'pdf'}">
<tr><td>
<c:set var="imageWidthTag" value='width="${imageWidth}" ' />
<c:set var="imageWidthStyle" value='width:${imageWidth}px' />
<div class="image-wrapper" style="float: ${not empty param.right?'right':'left'};${styleWidthWidthoutStyle}${param['clean-html']?imageWidthStyle:''}">
<c:if test="${url != '#'}">
<c:set var="rel" value="${fn:startsWith(url,'http://')?'external':'shadowbox'}" />
<c:set var="rel" value="${fn:endsWith(url,'.pdf')?'pdf':rel}" />
<a rel="${rel}" class="${type}" href="${url}" title="${not empty label?cleanLabel:cleanDescription}"></c:if>
<img ${not empty imageWidth?imageWidthTag:''}src="${fn:replace(previewURL,'/full/', '/float/')}" />
<c:if test="${url != '#'}"></a></c:if>
</div><div class="text">${label}</div>
</td></tr>
</c:if>
</table>
<c:if test="${contentContext.asPreviewMode && comp.textAuto}">
<div class="source" style="display: none;"><span class="container">${label}</span></div>
<script type="text/javascript">
var localJQ = jQuery;
if (typeof(pjq) !== 'undefined') {
	localJQ = pjq;
}
+function(jQuery) {
	jQuery("#comp-${compid} img").load(function() {
	try {
		if (jQuery(this).attr("src") != "${info.ajaxLoaderURL}" && !jQuery(this).hasClass("refreshed") && jQuery(this).attr("src").indexOf("/transform/")>=0) {
			editPreview.floatZone("#comp-${compid} .source .container", "#comp-${compid} .zone1 .container", "#comp-${compid} .zone2 .container", "#comp-${compid} img");
			var firstText=jQuery("#comp-${compid} .zone1 .container").html();
			var secondText = jQuery("#comp-${compid} .zone2 .container").html();
			jQuery.post( "${info.currentAjaxURL}", { webaction: "global-image.dataFeedBack", compid: "${compid}", firsttext: firstText, secondtext: secondText, height: jQuery("#comp-${compid} img").height(), width: jQuery("#comp-${compid} img").width()}, {dataType: "json"}).done(function(data) {
				jQuery("#comp-${compid} img").addClass("refreshed");		
				jQuery("#comp-${compid} img").attr("src", data.data.previewURL);
			});		
		}
		} catch(err) {
			console.log("err.message = ", err.message);
		}
	});
}(localJQ);
</script></c:if></div>