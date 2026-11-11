<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="_cabecera.jspf" %>
<form method="post" action="planificador">
  <input type="hidden" name="accion" value="generar">
  <div class="grid">
    <div class="card">
      <h2>Materias que quieres cursar</h2>
      <p class="mu"></p>
      <c:forEach var="m" items="${lista}">
        <c:set var="falta" value="${estado.faltantes(m)}"/>
        <c:choose>
          <c:when test="${not empty falta}">
            <div class="row dis">
              <input type="checkbox" disabled>
              <div><c:out value="${m.nombre}"/>
                <small>No disponible: requiere <c:out value="${fn:join(falta.toArray(), ' y ')}"/> (seriación).</small></div>
            </div>
          </c:when>
          <c:otherwise>
            <label class="row">
              <input type="checkbox" name="sel" value="${fn:escapeXml(m.clave)}" class="sel"
                     ${estado.deseadas.contains(m.clave) ? 'checked' : ''}>
              <span style="flex:1"><c:out value="${m.nombre}"/></span>
            </label>
          </c:otherwise>
        </c:choose>
      </c:forEach>
      <p><b><span id="cuenta">${estado.deseadas.size()}</span> materias seleccionadas</b></p>
    </div>
    <div class="card">
      <h2>Preferencias de horario</h2>
      <p class="mu" style="margin:0">Turno</p>
      <label class="row"><input type="radio" name="turno" value="mat" ${estado.turno == 'MANANA' ? 'checked' : ''}> Matutino</label>
      <label class="row"><input type="radio" name="turno" value="ves" ${estado.turno == 'TARDE' ? 'checked' : ''}> Vespertino</label>
      <label class="row"><input type="radio" name="turno" value="sin" ${estado.turno == 'SIN_PREFERENCIA' ? 'checked' : ''}> Sin preferencia</label>
      <p class="mu" style="margin:8px 0 0">Día libre</p>
      <label class="row"><input type="checkbox" name="viernes" ${estado.diaLibre == 'VIE' ? 'checked' : ''}> Viernes</label>
    </div>
  </div>
  <div class="bar">
    <a class="btn o" href="planificador?paso=1">Atrás</a>
    <button class="btn" type="submit">Generar horarios</button>
  </div>
  <c:if test="${not empty error}"><p class="err">${fn:escapeXml(error)}</p></c:if>
</form>
<script>
  const cuenta = document.getElementById('cuenta');
  document.querySelectorAll('input.sel').forEach(c => c.addEventListener('change',
    () => cuenta.textContent = document.querySelectorAll('input.sel:checked').length));
</script>
<%@ include file="_pie.jspf" %>
