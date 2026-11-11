<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="_cabecera.jspf" %>
<c:choose>
<c:when test="${not resultado.hayViables}">
  <div class="card warn" style="max-width:600px">
    <h2>No hay horarios viables con tu selección</h2>
    <p><c:out value="${resultado.mensajeSinResultados}"/></p>
    <p><b>Prueba esto:</b></p>
    <ul>
      <c:forEach var="sugerencia" items="${resultado.sugerencias}"><li><c:out value="${sugerencia}"/></li></c:forEach>
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
          <a class="tab ${s.index == estado.opcionSeleccionada ? 'on' : ''}" href="planificador?paso=3&opcion=${s.index}"><c:out value="${o.etiqueta}"/></a>
        </c:forEach>
        <span class="mu">Sin cruces · Seriaciones válidas ·
          ${estado.turno == 'MANANA' ? 'Turno matutino' : estado.turno == 'TARDE' ? 'Turno vespertino' : 'Sin preferencia de turno'}</span>
      </div>
      <c:if test="${not empty resultado.mensajeOpcionUnica}">
        <p class="mu"><b><c:out value="${resultado.mensajeOpcionUnica}"/></b></p>
      </c:if>
      <c:forEach var="aviso" items="${resultado.avisos}">
        <p class="mu"><c:out value="${aviso}"/></p>
      </c:forEach>
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
                    <div class="blk ${b.bloque.color}" style="top:${b.top}px;height:${b.alto}px">
                      <c:out value="${b.bloque.etiqueta}"/> · Gpo <c:out value="${b.bloque.grupo}"/>
                      <small><c:out value="${b.bloque.docente}"/></small>
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
      <p class="mu"><c:out value="${resultado.opciones[estado.opcionSeleccionada].resumen}"/></p>

      <div class="lista-carga">
        <c:forEach var="g" items="${carga}">
          <div class="item-carga">
            <div class="materia-titulo">
              <span class="dot ${g.color}"></span>
              <c:out value="${g.materia}"/>
            </div>

            <div class="mu materia-detalle">
              Grupo <c:out value="${g.grupo}"/> · <c:out value="${g.docente}"/>
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
