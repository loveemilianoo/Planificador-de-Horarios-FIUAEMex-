<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ include file="_cabecera.jspf" %>
<div class="card warn" style="max-width:600px">
  <h2>Algo salió mal</h2>
  <p>La página que buscas no existe o ocurrió un error en el sistema.</p>
  <a class="btn" href="${pageContext.request.contextPath}/planificador?paso=0">Volver al inicio</a>
</div>
<%@ include file="_pie.jspf" %>
