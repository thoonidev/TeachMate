<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="container tm-messages">
    <c:forTokens var="messageKey" items="successMsg,errorMsg,successMsgForD,errorMsgForD" delims=",">
        <c:if test="${not empty sessionScope[messageKey]}">
            <c:set var="messageError" value="${messageKey eq 'errorMsg' or messageKey eq 'errorMsgForD'}" />
            <div class="alert ${messageError ? 'alert-danger' : 'alert-success'}" role="${messageError ? 'alert' : 'status'}">
                <i class="fa-solid ${messageError ? 'fa-circle-exclamation' : 'fa-circle-check'} me-2" aria-hidden="true"></i>
                <c:out value="${sessionScope[messageKey]}" />
            </div>
        </c:if>
    </c:forTokens>
    <c:remove var="successMsg" scope="session" />
    <c:remove var="errorMsg" scope="session" />
    <c:remove var="successMsgForD" scope="session" />
    <c:remove var="errorMsgForD" scope="session" />
</div>
