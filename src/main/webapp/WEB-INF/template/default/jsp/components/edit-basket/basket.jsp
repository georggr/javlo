<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"
%><%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>

<div class="basket">
<c:if test="${not empty messages.globalMessage && messages.globalMessage.type > 0}">
<div class="message ${messages.globalMessage.typeLabel}">
	<span>${messages.globalMessage.message}</span>
</div>
</c:if>
<jsp:include page="step-${basket.step}.jsp" />
</div>