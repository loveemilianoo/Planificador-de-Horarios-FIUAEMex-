<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="_cabecera.jspf" %>
<form method="post" action="planificador">
  <input type="hidden" name="accion" value="avance">
  <div class="grid">
    <div class="card">
      <h2>¿A qué semestre te reinscribes?</h2>
      <c:forEach var="n" items="${[2,3,4]}">
        <label class="opt">
          <input type="radio" name="semestre" value="${n}" ${estado.semestre == n ? 'checked' : ''}
                 onchange="this.form.accion.value='semestre'; this.form.submit()">
          ${n}.${n == 3 ? 'er' : 'º'} semestre
        </label>
      </c:forEach>
      <p class="mu">Con esto se muestran las materias que puedes elegir.</p>
    </div>
    <div class="card">
      <h2>Materias que ya aprobaste</h2>
      <c:forEach var="m" items="${anteriores}">
        <label class="row">
          <input type="checkbox" name="apr" value="${fn:escapeXml(m.clave)}" ${estado.aprobadas.contains(m.clave) ? 'checked' : ''}>
          <c:out value="${m.nombre}"/>
        </label>
      </c:forEach>
      <p class="mu">Tú declaras lo que cursaste; el sistema no lo valida contra Control Escolar.</p>
    </div>
  </div>
  <div class="bar">
    <a class="btn o" href="planificador?paso=0">Atrás</a>
    <button class="btn" type="submit">Siguiente: materias y preferencias</button>
  </div>
</form>
<%@ include file="_pie.jspf" %>
