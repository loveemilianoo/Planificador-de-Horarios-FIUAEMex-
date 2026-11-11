<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="_cabecera.jspf" %>
<c:choose>
<c:when test="${resultado.vacio}">
  <div class="card warn" style="max-width:600px">
    <h2>No hay horarios viables con tu selección</h2>
    <p><c:out value="${resultado.motivo}"/></p>
    <p><b>Prueba esto:</b></p>
    <ul>
      <c:if test="${estado.turno != 'sin'}"><li>Cambiar el turno a «Sin preferencia».</li></c:if>
      <c:if test="${estado.viernesLibre}"><li>Quitar el viernes como día libre.</li></c:if>
      <li>Quitar una de las materias seleccionadas.</li>
    </ul>
    <a class="btn" href="planificador?paso=2">Ajustar selección</a>
  </div>
</c:when>
<c:otherwise>
  <c:set var="dias" value="${['Lun','Mar','Mié','Jue','Vie','Sáb']}"/>
  <div class="grid res">
    <div class="card">
      <div class="tabs">
        <c:forEach var="o" items="${resultado.opciones}" varStatus="s">
          <a class="tab ${s.index == estado.opcion ? 'on' : ''}" href="planificador?paso=3&opcion=${s.index}">Opción ${s.count}</a>
        </c:forEach>
        <span class="mu">Sin cruces · Seriaciones válidas ·
          ${estado.turno == 'mat' ? 'Turno matutino' : estado.turno == 'ves' ? 'Turno vespertino' : 'Sin preferencia de turno'}</span>
      </div>
      <div class="cal">
        <table>
          <tr><th></th><c:forEach var="d" items="${dias}"><th>${d}</th></c:forEach></tr>
          <c:forEach var="h" begin="${horaIni}" end="${horaFin}">
            <tr>
              <td class="h">${h}:00</td>
              <c:forEach var="d" begin="0" end="5">
                <c:set var="k" value="${d}-${h}"/>
                <td>
                  <c:forEach var="b" items="${celdas[k]}">
                    <div class="blk c${colores[b.grupo.materia.clave] % 6}" style="top:${b.top}px;height:${b.alto}px">
                      <c:out value="${b.grupo.materia.etiqueta}"/> · Gpo <c:out value="${b.grupo.numero}"/>
                      <small><c:out value="${b.grupo.profesor}"/></small>
                    </div>
                  </c:forEach>
                </td>
              </c:forEach>
            </tr>
          </c:forEach>
        </table>
      </div>
    </div>
    <div class="card">
      <h2>Tu carga</h2>
      <c:set var="gruposActuales" value="${resultado.opciones[estado.opcion]}"/>
      
      <div class="lista-carga">
        <c:forEach var="g" items="${gruposActuales}">
          <div class="item-carga">
            <div class="materia-titulo">
              <span class="dot c${colores[g.materia.clave] % 6}"></span>
              <c:out value="${g.materia.nombre}"/>
            </div>
            
            <div class="mu materia-detalle">
              Grupo <c:out value="${g.numero}"/> · <c:out value="${g.profesor}"/>
            </div>
          </div>
        </c:forEach>
      </div>
    </div>
  </div>
  <div class="bar">
    <a class="btn o" href="planificador?paso=2">Ajustar preferencias</a>
    <button class="btn" type="button" onclick="window.print()">Imprimir o guardar como PDF</button>
  </div>
</c:otherwise>
</c:choose>
<%@ include file="_pie.jspf" %>
