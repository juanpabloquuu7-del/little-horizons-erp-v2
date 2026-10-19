# 1. Título del Proyecto

**ERP Colegio Internacional "Little Horizons"** — Sistema de gestión educativa de consola (Java 17 + MySQL 8).

# 2. Descripción

**Propósito.** Centralizar y automatizar los procesos académicos, extracurriculares, financieros y de recursos de un colegio de 500+ estudiantes, reemplazando cuadernos y hojas de cálculo dispersas por una base de datos única.

**Problemas que resuelve**

| Problema | Solución en el sistema |
|---|---|
| Notas en cuadernos que se pierden | Registros académicos en MySQL con historial desde el ingreso |
| Actividades extracurriculares sin control | Clubes con cupo estricto, eventos, participantes y posiciones |
| Facturación manual y sin DIAN | Factura electrónica simulada en PDF con CUFE y QR |
| Recursos que "desaparecen" | Inventario con préstamos/devoluciones y reporte de vencidos |
| Profesores sin visibilidad | Promedios por estudiante, materia, curso y período; alertas de inasistencia |

**Funcionalidades principales:** ver la sección 10.

# 3. Autores

| Nombre completo | Correo electrónico | Contacto |
|---|---|---|
| Juan Pablo Quintanilla Rodríguez | juanpabloquuu7@gmail.com | [@juanpabloquuu7-del](https://github.com/juanpabloquuu7-del) |
| Miguel Alejandro Acevedo Acevedo | *[correo por completar]* | [@alejandro-xoxo](https://github.com/alejandro-xoxo) |

# 4. Tecnologías Utilizadas

- **Lenguaje:** Java 17 (records, text blocks, `switch` con flechas, Stream API, lambdas)
- **IDE:** Apache NetBeans (proyecto Maven estándar; también abre en IntelliJ/VS Code)
- **Construcción:** Maven 3.9+
- **Base de datos:** MySQL 8.0 o superior (`erp_little_horizons`)
- **Librerías:** `mysql-connector-j 8.4.0` (JDBC) · `Apache PDFBox 2.0.31` (PDF) · `ZXing core 3.5.3` (código QR)

# 5. Requerimientos Previos

- **JDK 17** o superior
- **MySQL Server 8.0 o superior** *(obligatorio: el esquema usa `CHECK` constraints, que MySQL aplica desde 8.0.16)*
- Maven 3.9+ (o el Maven incluido en NetBeans)
- Terminal con UTF-8 para ver los marcos Unicode (Windows: `chcp 65001`)

# 6. Configuración de la Base de Datos

## 6.1. Creación de la base de datos

`docs/database.sql` crea la base `erp_little_horizons`, todas las tablas y los datos de prueba. **Ojo:** empieza con `DROP DATABASE IF EXISTS erp_little_horizons`.

- **Con DBeaver / Workbench:** abra `docs/database.sql` y ejecute el script completo (en DBeaver: `Alt+X`).
- **Con la consola de MySQL:**

  ```bash
  mysql -u root -p --default-character-set=utf8mb4 -e "source docs/database.sql"
  ```

El script tiene dos secciones: **DDL** (basado en el script oficial v2.0 del anexo) y **DML** (≥ 30 registros por tabla principal: estudiantes, acudientes, profesores, materias [60], registros académicos [300], pagos [150], clubes y recursos).

> **Cambio justificado respecto al script oficial:** se agregó la tabla `horarios_clase`, porque el enunciado (1.2) exige registrar días y horas de cada asignación para evitar conflictos de horario y el script oficial no tenía dónde guardarlos. También se agregó la tabla `becas` (porcentaje de descuento por estudiante y año), para el ajuste por beca que pide el módulo financiero.

## 6.2. Configuración de credenciales

Ninguna credencial está en el código. Edite `src/main/resources/config.properties`:

| Clave | Uso |
|---|---|
| `db.url`, `db.user`, `db.password` | Conexión JDBC |
| `paths.logFile`, `paths.invoicesDir` | Ruta del log y de las facturas |
| `business.academicYear` | Año académico (0 = año actual) |
| `business.delinquencyConcept` / `delinquencyDays` | Concepto y días para el reporte de morosos |
| `business.siblingDiscountPercent` | Descuento por hermanos en conceptos recurrentes |
| `business.loanDays`, `business.weeklyAbsenceThreshold` | Plazo de préstamos y umbral de inasistencias |
| `school.*` | Razón social, NIT, dirección y resolución de la factura |

Para no guardar la contraseña en el repositorio use la variable de entorno `LH_DB_PASSWORD`, o un `config.properties` en la carpeta de ejecución (sobrescribe al del classpath).

## 6.3. Instalación del driver JDBC

No hay que instalar nada a mano: Maven descarga `mysql-connector-j` declarado en el `pom.xml`.

# 7. Instalación y Ejecución

## 7.1. Clonar el repositorio

```bash
git clone <url-del-repositorio>
cd little-horizons-erp
```

## 7.2. Abrir el proyecto en el IDE

*File → Open Project* en Apache NetBeans y seleccione la carpeta (detecta el `pom.xml`). Verifique que use JDK 17 en *Project Properties → Build → Compile*.

## 7.3. Compilar

```bash
mvn clean compile
```

En NetBeans: clic derecho → *Clean and Build*.

## 7.4. Ejecutar

```bash
mvn exec:java
```

o, tras `mvn clean package`:

```bash
java -jar target/little-horizons-erp-1.0.0.jar
```

En NetBeans: *Run Project* (F6). Las facturas quedan en `./facturas/factura_XXXXXX.pdf`.

# 8. Estructura del Proyecto

Arquitectura **MVC** con capas **service** y **dao**:

```text
little-horizons-erp/
├── pom.xml
├── docs/database.sql              # DDL + DML
├── logs/error_log.txt             # errores con timestamp y stacktrace
├── facturas/                      # PDFs generados
└── src/main/
    ├── resources/config.properties
    └── java/com/littlehorizons/erp/
        ├── Main.java              # punto de entrada (cablea todo)
        ├── config/                # AppConfig, DatabaseConnection (Singleton)
        ├── model/                 # Entidades del dominio (records)
        ├── dao/                   # Interfaces + implementaciones JDBC
        ├── service/               # Reglas de negocio
        ├── controller/            # Orquestan vista ↔ servicio
        ├── view/                  # Menús y tablas de consola
        └── util/                  # LoggerUtility, CustomExceptions, PdfGenerator, InputValidator
```

- **model:** records inmutables, sin lógica de persistencia.
- **view:** sólo entrada/salida (marcos Unicode, validación de entradas, tablas).
- **controller:** lee datos de la vista, llama al servicio y muestra el resultado; `BaseController.safely` captura todas las excepciones.
- **service:** reglas de negocio (nota 0.0–5.0, cupo de clubes, conflictos de horario, CUFE...).
- **dao:** único lugar con SQL.

# 9. Principios y Patrones de Diseño Aplicados

## 9.1. Principios SOLID

- **S — Responsabilidad única:** `PdfGenerator` sólo dibuja el PDF, `FinanceService` decide cuánto y a quién se factura, `PaymentDaoImpl` sólo persiste.
- **O — Abierto/cerrado:** `BaseJdbcDao` centraliza `PreparedStatement`/`try-with-resources`; un DAO nuevo lo extiende sin modificar a los existentes.
- **L — Sustitución de Liskov:** cualquier `XxxDaoImpl` reemplaza a su interfaz `XxxDao` (los servicios aceptan DAOs por constructor, útil para pruebas).
- **I — Segregación de interfaces:** interfaces pequeñas por agregado (`ClubDao`, `LoanDao`, `AssignmentDao`...), sin una interfaz gigante.
- **D — Inversión de dependencias:** los servicios dependen de las interfaces DAO, no de las implementaciones.

## 9.2. Patrones de diseño

- **Singleton:** `DatabaseConnection` y `AppConfig` — una única fuente de configuración y de fábrica de conexiones.
- **DAO:** interfaz + implementación JDBC por entidad; aísla el SQL del resto de la aplicación.
- **MVC:** separación estricta modelo / vista / controlador.
- **Inyección por constructor:** cableada en `Main`.

# 10. Funcionalidades Principales

**1. Gestión de Estudiantes:** registro completo (documento, contacto, fecha de nacimiento...), acudientes con relación N:M (hermanos), matrícula por grado y año con estado, cambio de grado, cambio de estado (Activo/Graduado/Retirado/Suspendido), inscripción en materias por período.

**2. Gestión Académica:** catálogo de materias; notas 0.0–5.0 por período (I–IV y Final) con validación de que el profesor esté asignado y el estudiante inscrito; **promedios automáticos** por materia, por período, por curso y por materia del curso (Streams); historial completo; asistencia por materia con 4 estados; **alerta de más de 3 ausencias en una semana** e inasistencias acumuladas.

**3. Gestión de Profesores:** directorio, asignación de materias (carga académica) y **horarios con detección de conflictos**.

**4. Actividades Extracurriculares:** clubes con **cupo máximo estricto** (inscripción atómica), retiro con motivo; eventos (Académico, Cultural, Deportivo, Social, Científico), participantes y posiciones en eventos con competencia.

**5. Gestión Financiera:** catálogo de conceptos, **descuento automático por hermanos**, **becas por estudiante** (se aplica el mayor de los dos descuentos, no se suman) y monto editable por estudiante, registro de pagos (método y número de recibo consecutivo), historial por estudiante y por acudiente, **factura electrónica PDF** con CUFE (SHA-256) y QR generada al confirmar el pago, reporte de morosos.

**6. Gestión de Recursos:** inventario por categoría y ubicación, estados (Disponible, Prestado, En Mantenimiento, Dado de Baja), préstamos y devoluciones transaccionales a estudiantes y profesores (tablas separadas), reporte de préstamos vencidos.

**7. Reportes y Estadísticas:** estudiantes por grado, mejores promedios, participación en clubes y eventos, ingresos por concepto y por mes, estado de los recursos.

**Factura electrónica (simulada).** Incluye razón social, NIT, dirección, resolución, cliente (acudiente con documento), ítems, descuento, totales, **CUFE = SHA-256(número de factura + NIT + fecha)** y QR. Documento académico, sin validez fiscal.

# 11. Manejo de Errores y Logs

- Las capas **DAO** y **servicio** traducen `SQLException` a `DataAccessException` y reglas violadas a `BusinessException` (paquete `util/CustomExceptions`).
- `LoggerUtility.logError` agrega a **`logs/error_log.txt`** una entrada con fecha-hora, contexto, mensaje y **stacktrace completo**.
- El usuario sólo ve un mensaje claro en español; ningún error cierra el programa (`BaseController.safely`).
- Todas las consultas usan `PreparedStatement` y `try-with-resources`; no hay SQL concatenado con datos del usuario.

# 12. Capturas de Pantalla

*(Agregue capturas del menú principal, de un reporte y de la factura PDF.)*

# 13. Problemas Conocidos y Limitaciones

- Interfaz sólo de consola; no hay autenticación de usuarios.
- La factura es una simulación: no se comunica con la DIAN.
- La inscripción de clubes es atómica por sentencia, pero no hay bloqueo entre varias instancias simultáneas de la aplicación.

# 14. Futuras Mejoras

- Roles y autenticación, pruebas automatizadas con JUnit y una base de datos de prueba.
- Interfaz gráfica o API REST, y envío de la factura por correo.
- Generación del boletín de notas en PDF.

# 15. Licencia

Proyecto académico distribuido bajo licencia **MIT** (ver el archivo [`LICENSE`](LICENSE)).

# 16. Contacto

Juan Pablo Quintanilla Rodríguez — juanpabloquuu7@gmail.com — https://github.com/juanpabloquuu7-del
