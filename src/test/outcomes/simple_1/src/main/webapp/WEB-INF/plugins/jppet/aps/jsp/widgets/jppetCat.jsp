<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="wp" uri="/aps-core"%>
<%@ taglib prefix="jppet" uri="/jppet-core"%>

<jppet:cat var="cat" />
<article>
<c:choose>
	<c:when test="${not empty cat}">
	<h1><wp:i18n key="jppet_CAT_ID" />: <c:out value="${cat.id}" /></h1>
	<ul>
		<li>
			<wp:i18n key="jppet_CAT_NAME" />: <c:out value="${cat.name}" /><br />
			<wp:i18n key="jppet_CAT_AGE" />: <c:out value="${cat.age}" /><br />
			<wp:i18n key="jppet_CAT_WEIGHT" />: <c:out value="${cat.weight}" /><br />
			<wp:i18n key="jppet_CAT_CREATEDAT" />: <c:out value="${cat.createdat}" /><br />
		</li>
	</ul>
	</c:when>
	<c:otherwise>
	<div class="alert alert-error">
		<p><wp:i18n key="jppet_CAT_NOT_FOUND" /></p>
	</div>
	</c:otherwise>
</c:choose>
</article>