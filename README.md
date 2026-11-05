# Planificador de Horarios FI UAEMéx

Plataforma web que genera automáticamente **de 2 a 4 opciones de horario sin traslapes** para alumnos de Ingeniería en Computación (ICO) de la Facultad de Ingeniería de la UAEMéx, respetando las seriaciones reglamentarias.

Proyecto de la materia **Administración de Proyectos Informáticos** (Grupo 01, profesora Cintia González Mireles) · Equipo **Informaniacos**.

---

## Problema que resuelve

En cada reinscripción, los alumnos arman su horario a mano a partir de archivos estáticos (PDF/Excel) mientras los cupos se saturan. Esto provoca traslapes, inscripciones inválidas por omitir seriaciones y decisiones apresuradas. Este sistema automatiza el cálculo combinatorio y entrega alternativas listas antes de la cita de inscripción.

## Características

- **Lectura de la oferta académica** desde Excel (`.xlsx`, plantilla de horarios de la UAEMéx) o JSON.
- **Filtro de materias**: descarta las ya aprobadas, inexistentes, sin grupos o con seriación no cumplida, y explica el motivo.
- **Preferencias del alumno**: turno (mañana/tarde) y día libre.
- **Motor combinatorio** con backtracking y poda, que elige un grupo por materia sin traslapes.
- **Ordenamiento de opciones** por comodidad (menos huecos entre clases y menos días de asistencia).
- **Avisos al usuario** cuando el escenario es infactible, hay pocas opciones o el cupo puede cambiar en tiempo real.

## Estado del proyecto

| Componente | Estado |
|---|---|
| Modelo de datos (`model`) | Terminado |
| Parser Excel / JSON (`parser`) | Terminado |
| Motor combinatorio (`motor`) | Terminado |
| Capa web (Servlets + JSP, calendario) | Pendiente |
| Empaquetado `.war` y despliegue en Tomcat | Pendiente |

## Tecnologías

- Java 17+
- Apache Maven
- Jackson (`jackson-databind` 2.17.0) para JSON
- Apache POI (`poi-ooxml` 5.2.5) para Excel
- Servlets / JSP sobre Apache Tomcat (capa web, en desarrollo)

## Estructura del repositorio

```
src/
├── model/    Records del dominio: Materia, Grupo, Horario, Alumno,
│             OfertaAcademica, OpcionHorario, Turno, DiaSemana
├── parser/   OfertaReader (interfaz), ExcelOfertaReader,
│             JsonOfertaReader, ParserException
└── motor/    MotorHorarios (punto de entrada), FiltroMaterias,
              FiltroPreferencias, GeneradorHorarios, DetectorTraslapes,
              EvaluadorOpcion, TablaSeriaciones, ConfiguracionMotor,
              ResultadoGeneracion
```

## Cómo funciona el motor

`MotorHorarios.generar(oferta, alumno)` ejecuta este flujo:

1. **Filtra materias** deseadas (aprobadas, existencia, grupos abiertos y seriación).
2. **Filtra grupos** según día libre y turno del alumno.
3. **Busca combinaciones** sin traslapes con backtracking (las materias con menos grupos se evalúan primero para podar antes).
4. **Ordena y entrega** las mejores opciones, junto con avisos y los motivos de las materias descartadas.

Dos clases se consideran traslapadas si coinciden el día y sus intervalos se cruzan; si una termina exactamente cuando empieza la otra, **no** hay traslape.

### Configuración

`ConfiguracionMotor.porDefecto()` usa los valores del Acta Constitutiva:

| Parámetro | Valor | Descripción |
|---|---|---|
| `minOpciones` | 2 | Mínimo esperado; si hay menos, se avisa |
| `maxOpciones` | 4 | Máximo de opciones entregadas |
| `maxCandidatas` | 5000 | Tope de combinaciones exploradas (evita la explosión combinatoria) |
| `horaCorteTurno` | 13:00 | Separa mañana de tarde |

## Requisitos previos

- JDK 17 o superior
- Maven 3.8+

## Compilación

```bash
git clone https://github.com/loveemilianoo/Planificador-de-Horarios-FIUAEMex-.git
cd Planificador-de-Horarios-FIUAEMex-
mvn clean compile
```

## Formatos de entrada

### Excel (`.xlsx`)

Se usa la plantilla de horarios de la UAEMéx. El lector detecta las columnas **por nombre de encabezado** (`SEM`, `CLAVE`, `UNIDAD DE APRENDIZAJE`, `GPO`, `DOCENTE` y los bloques `LUNES`…`SABADO` con `INI` / `FIN` / `AULA`), por lo que tolera plantillas con columnas distintas (por ejemplo 2025B y 2026A). Los problemas en filas individuales no detienen la lectura: se reportan con `getAdvertencias()`.

### JSON

```json
{
  "materias": [
    {
      "clave": "LINC11",
      "nombre": "FISICA",
      "semestre": 1,
      "grupos": [
        {
          "clave": "CO01",
          "docente": "DRA. QUINTANA GUERRA MARIA ROSA",
          "horarios": [
            { "dia": "LUN", "inicio": "09:00", "fin": "11:00", "aula": "C-303" }
          ]
        }
      ]
    }
  ]
}
```

- Obligatorios: `clave` y `nombre` de la materia, `clave` del grupo, y `dia`, `inicio`, `fin` del horario.
- Opcionales: `semestre` (0 si falta), `docente`, `aula` (se usa `N/D`).
- Días válidos: `LUN`, `MAR`, `MIE`, `JUE`, `VIE`, `SAB` (también se acepta `MIER`).
- Horas en formato `HH:mm`.

## Alcance

**Incluido:** lectura de oferta (Excel/JSON), validación de seriaciones, generación de 2 a 4 horarios sin traslapes, visualización en calendario, prueba piloto, despliegue en Tomcat y documentación.

**Fuera de alcance:** integración con Control Escolar, autenticación con cuenta institucional, inscripción automática, predicción de cupo en tiempo real, app móvil nativa, y carreras distintas a ICO o semestres fuera de 2.º a 4.º.

## Equipo

**Informaniacos** · Facultad de Ingeniería, UAEMéx · Grupo 01

- Bibiano Paredes Daniel Oswaldo
- Escutia Barron Erick
- Hernández Díaz Richard
- Hidalgo Gasca Emiliano
- Martínez García José Silviano
- Reyes Peña Elliot
- Sánchez Agustín Yahir
- Sánchez Callejas Eduardo Josafat (Líder de proyecto)