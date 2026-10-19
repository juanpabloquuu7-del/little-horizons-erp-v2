-- =====================================================================
-- Little Horizons ERP - Base de datos erp_little_horizons
-- Motor: MySQL 8.0 o superior (REQUERIDO por los CHECK constraints)
--
-- SECCIÓN 1: DDL  (basado en el script oficial v2.0 del anexo del proyecto)
-- SECCIÓN 2: DML  (datos de prueba coherentes y realistas)
--
-- Cambio respecto al script oficial (justificado):
--   * Se agrega la tabla `horarios_clase` (módulo académico). El enunciado (1.2)
--     exige registrar días y horas de cada asignación para evitar conflictos
--     básicos de horario, y el script oficial no tenía dónde guardarlos.
-- =====================================================================

DROP DATABASE IF EXISTS erp_little_horizons;
CREATE DATABASE erp_little_horizons CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE erp_little_horizons;

-- =====================================================================
-- SECCIÓN 1: DDL
-- =====================================================================

-- ============================================
-- 1. MÓDULO DE ESTUDIANTES Y ACUDIENTES
-- ============================================

-- Tabla de grados académicos
CREATE TABLE grados (
    id_grado INT AUTO_INCREMENT PRIMARY KEY,
    nombre_grado VARCHAR(50) NOT NULL UNIQUE COMMENT 'Ej: Preescolar, Primero, Segundo, ... Once',
    nivel_numerico INT NOT NULL UNIQUE COMMENT 'Nivel numérico para ordenamiento (0 para preescolar, 1-11 para grados)',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Catálogo de grados académicos del colegio';

-- Tabla de estudiantes
CREATE TABLE estudiantes (
    id_estudiante INT AUTO_INCREMENT PRIMARY KEY,
    tipo_documento ENUM('TI', 'CC', 'CE', 'Pasaporte') NOT NULL DEFAULT 'TI',
    numero_documento VARCHAR(20) NOT NULL UNIQUE,
    primer_nombre VARCHAR(100) NOT NULL,
    segundo_nombre VARCHAR(100),
    primer_apellido VARCHAR(100) NOT NULL,
    segundo_apellido VARCHAR(100),
    fecha_nacimiento DATE NOT NULL,
    genero ENUM('M', 'F', 'Otro') NOT NULL,
    direccion VARCHAR(255),
    telefono VARCHAR(20),
    correo_electronico VARCHAR(100),
    tipo_sangre VARCHAR(5),
    alergias TEXT,
    condiciones_medicas TEXT,
    fecha_ingreso DATE NOT NULL,
    estado ENUM('Activo', 'Graduado', 'Retirado', 'Suspendido') DEFAULT 'Activo',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_documento (tipo_documento, numero_documento),
    INDEX idx_estado (estado),
    INDEX idx_nombre (primer_apellido, segundo_apellido, primer_nombre)
) ENGINE=InnoDB COMMENT='Información completa de estudiantes';

-- Tabla de acudientes/tutores legales
CREATE TABLE acudientes (
    id_acudiente INT AUTO_INCREMENT PRIMARY KEY,
    tipo_documento ENUM('CC', 'CE', 'Pasaporte') NOT NULL DEFAULT 'CC',
    numero_documento VARCHAR(20) NOT NULL UNIQUE,
    primer_nombre VARCHAR(100) NOT NULL,
    segundo_nombre VARCHAR(100),
    primer_apellido VARCHAR(100) NOT NULL,
    segundo_apellido VARCHAR(100),
    parentesco VARCHAR(50) NOT NULL COMMENT 'Padre, Madre, Abuelo, Tío, Tutor Legal, etc.',
    telefono_principal VARCHAR(20) NOT NULL,
    telefono_secundario VARCHAR(20),
    correo_electronico VARCHAR(100),
    direccion VARCHAR(255),
    ocupacion VARCHAR(100),
    es_responsable_financiero BOOLEAN DEFAULT TRUE COMMENT 'Indica si es responsable de pagos',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_documento (tipo_documento, numero_documento),
    INDEX idx_nombre (primer_apellido, segundo_apellido)
) ENGINE=InnoDB COMMENT='Acudientes y tutores legales';

-- Relación estudiantes-acudientes (muchos a muchos)
CREATE TABLE estudiante_acudiente (
    id_estudiante INT NOT NULL,
    id_acudiente INT NOT NULL,
    es_principal BOOLEAN DEFAULT FALSE COMMENT 'Acudiente principal para comunicaciones',
    es_contacto_emergencia BOOLEAN DEFAULT TRUE COMMENT 'Disponible para emergencias',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_estudiante, id_acudiente),
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    FOREIGN KEY (id_acudiente) REFERENCES acudientes(id_acudiente) ON DELETE CASCADE,
    INDEX idx_estudiante (id_estudiante),
    INDEX idx_acudiente (id_acudiente)
) ENGINE=InnoDB COMMENT='Relación entre estudiantes y sus acudientes';

-- Tabla de matrículas (historial de grados por año)
CREATE TABLE matriculas (
    id_matricula INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    id_grado INT NOT NULL,
    año_academico INT NOT NULL COMMENT 'Año académico (ej: 2024)',
    estado_matricula ENUM('Activa', 'Completada', 'Retirada') DEFAULT 'Activa',
    fecha_matricula DATE NOT NULL,
    fecha_retiro DATE,
    observaciones TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    FOREIGN KEY (id_grado) REFERENCES grados(id_grado),
    UNIQUE KEY matricula_unica (id_estudiante, año_academico),
    INDEX idx_año_grado (año_academico, id_grado),
    INDEX idx_estado (estado_matricula)
) ENGINE=InnoDB COMMENT='Historial de matrículas de estudiantes por año y grado';

-- ============================================
-- 2. MÓDULO ACADÉMICO
-- ============================================

-- Tabla de materias/asignaturas
CREATE TABLE materias (
    id_materia INT AUTO_INCREMENT PRIMARY KEY,
    codigo_materia VARCHAR(20) NOT NULL UNIQUE COMMENT 'Código único de la materia (ej: MAT-5, ESP-3)',
    nombre_materia VARCHAR(100) NOT NULL,
    descripcion TEXT,
    id_grado INT NOT NULL COMMENT 'Grado al que pertenece la materia',
    horas_semanales INT DEFAULT 2 COMMENT 'Horas semanales asignadas',
    esta_activa BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_grado) REFERENCES grados(id_grado),
    INDEX idx_grado (id_grado),
    INDEX idx_activa (esta_activa),
    INDEX idx_codigo (codigo_materia)
) ENGINE=InnoDB COMMENT='Catálogo de materias por grado';

-- Tabla de profesores
CREATE TABLE profesores (
    id_profesor INT AUTO_INCREMENT PRIMARY KEY,
    tipo_documento ENUM('CC', 'CE', 'Pasaporte') NOT NULL DEFAULT 'CC',
    numero_documento VARCHAR(20) NOT NULL UNIQUE,
    primer_nombre VARCHAR(100) NOT NULL,
    segundo_nombre VARCHAR(100),
    primer_apellido VARCHAR(100) NOT NULL,
    segundo_apellido VARCHAR(100),
    correo_electronico VARCHAR(100) NOT NULL UNIQUE,
    telefono VARCHAR(20),
    especializacion VARCHAR(100) COMMENT 'Área de especialización (Matemáticas, Español, etc.)',
    fecha_contratacion DATE NOT NULL,
    estado ENUM('Activo', 'Inactivo', 'Licencia') DEFAULT 'Activo',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_documento (tipo_documento, numero_documento),
    INDEX idx_estado (estado),
    INDEX idx_nombre (primer_apellido, segundo_apellido)
) ENGINE=InnoDB COMMENT='Información de profesores';

-- Asignación de materias a profesores
CREATE TABLE asignaciones_profesor (
    id_asignacion INT AUTO_INCREMENT PRIMARY KEY,
    id_profesor INT NOT NULL,
    id_materia INT NOT NULL,
    año_academico INT NOT NULL,
    seccion VARCHAR(10) COMMENT 'Sección o grupo (A, B, C)',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_profesor) REFERENCES profesores(id_profesor) ON DELETE CASCADE,
    FOREIGN KEY (id_materia) REFERENCES materias(id_materia) ON DELETE CASCADE,
    UNIQUE KEY asignacion_unica (id_profesor, id_materia, año_academico, seccion),
    INDEX idx_profesor_año (id_profesor, año_academico),
    INDEX idx_materia_año (id_materia, año_academico)
) ENGINE=InnoDB COMMENT='Asignación de profesores a materias por año académico';

-- Horarios de clase de cada asignación (AGREGADA: requerida por el enunciado 1.2)
CREATE TABLE horarios_clase (
    id_horario INT AUTO_INCREMENT PRIMARY KEY,
    id_asignacion INT NOT NULL,
    dia_semana ENUM('Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes') NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_asignacion) REFERENCES asignaciones_profesor(id_asignacion) ON DELETE CASCADE,
    CHECK (hora_fin > hora_inicio),
    INDEX idx_asignacion_dia (id_asignacion, dia_semana)
) ENGINE=InnoDB COMMENT='Días y horas en que cada profesor dicta cada materia';

-- Inscripción de estudiantes en materias
CREATE TABLE inscripciones_materia (
    id_inscripcion INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    id_materia INT NOT NULL,
    año_academico INT NOT NULL,
    periodo_academico ENUM('I', 'II', 'III', 'IV') NOT NULL COMMENT 'Período académico',
    seccion VARCHAR(10) COMMENT 'Sección o grupo asignado',
    fecha_inscripcion DATE NOT NULL,
    estado ENUM('Activa', 'Retirada', 'Completada') DEFAULT 'Activa',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    FOREIGN KEY (id_materia) REFERENCES materias(id_materia) ON DELETE CASCADE,
    UNIQUE KEY inscripcion_unica (id_estudiante, id_materia, año_academico, periodo_academico),
    INDEX idx_estudiante_año (id_estudiante, año_academico),
    INDEX idx_materia_periodo (id_materia, periodo_academico)
) ENGINE=InnoDB COMMENT='Inscripción de estudiantes en materias específicas';

-- Tabla de registros académicos (calificaciones/notas)
CREATE TABLE registros_academicos (
    id_registro INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    id_materia INT NOT NULL,
    id_profesor INT NOT NULL,
    año_academico INT NOT NULL,
    periodo_academico ENUM('I', 'II', 'III', 'IV', 'Final') NOT NULL,
    valor_nota DECIMAL(2,1) NOT NULL COMMENT 'Nota de 0.0 a 5.0 según estándar colombiano',
    observaciones TEXT,
    fecha_registro DATE NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    FOREIGN KEY (id_materia) REFERENCES materias(id_materia) ON DELETE CASCADE,
    FOREIGN KEY (id_profesor) REFERENCES profesores(id_profesor),
    CHECK (valor_nota >= 0.0 AND valor_nota <= 5.0),
    UNIQUE KEY registro_unico (id_estudiante, id_materia, año_academico, periodo_academico),
    INDEX idx_estudiante_año (id_estudiante, año_academico),
    INDEX idx_materia_periodo (id_materia, periodo_academico),
    INDEX idx_valor_nota (valor_nota)
) ENGINE=InnoDB COMMENT='Registro de calificaciones de estudiantes';

-- Tabla de asistencia
CREATE TABLE asistencias (
    id_asistencia INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    id_materia INT NOT NULL,
    fecha_asistencia DATE NOT NULL,
    estado_asistencia ENUM('Presente', 'Ausente', 'Tardanza', 'Excusa Médica') NOT NULL,
    observaciones TEXT,
    registrado_por INT COMMENT 'ID del profesor que registró la asistencia',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    FOREIGN KEY (id_materia) REFERENCES materias(id_materia) ON DELETE CASCADE,
    FOREIGN KEY (registrado_por) REFERENCES profesores(id_profesor),
    UNIQUE KEY asistencia_unica (id_estudiante, id_materia, fecha_asistencia),
    INDEX idx_estudiante_fecha (id_estudiante, fecha_asistencia),
    INDEX idx_materia_fecha (id_materia, fecha_asistencia),
    INDEX idx_estado (estado_asistencia)
) ENGINE=InnoDB COMMENT='Registro de asistencia diaria de estudiantes';

-- ============================================
-- 3. MÓDULO DE ACTIVIDADES EXTRACURRICULARES
-- ============================================

-- Tabla de clubes
CREATE TABLE clubes (
    id_club INT AUTO_INCREMENT PRIMARY KEY,
    nombre_club VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    id_profesor_supervisor INT COMMENT 'Profesor encargado del club',
    capacidad_maxima INT DEFAULT 30 COMMENT 'Cupo máximo de estudiantes',
    horario_reunion VARCHAR(100) COMMENT 'Días y horario de reunión (ej: Martes 3-5 PM)',
    esta_activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_profesor_supervisor) REFERENCES profesores(id_profesor),
    INDEX idx_activo (esta_activo),
    INDEX idx_nombre (nombre_club)
) ENGINE=InnoDB COMMENT='Catálogo de clubes extracurriculares';

-- Membresías en clubes
CREATE TABLE membresias_club (
    id_membresia INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    id_club INT NOT NULL,
    fecha_ingreso DATE NOT NULL,
    estado_membresia ENUM('Activa', 'Retirada') DEFAULT 'Activa',
    fecha_retiro DATE,
    motivo_retiro TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    FOREIGN KEY (id_club) REFERENCES clubes(id_club) ON DELETE CASCADE,
    INDEX idx_estudiante (id_estudiante),
    INDEX idx_club_estado (id_club, estado_membresia)
) ENGINE=InnoDB COMMENT='Membresías de estudiantes en clubes';

-- Tabla de eventos
CREATE TABLE eventos (
    id_evento INT AUTO_INCREMENT PRIMARY KEY,
    nombre_evento VARCHAR(150) NOT NULL,
    descripcion TEXT,
    tipo_evento ENUM('Académico', 'Cultural', 'Deportivo', 'Social', 'Científico') NOT NULL,
    fecha_evento DATE NOT NULL,
    lugar VARCHAR(150),
    tiene_competencia BOOLEAN DEFAULT FALSE COMMENT 'Indica si el evento tiene competencia con posiciones',
    id_organizador INT COMMENT 'Profesor organizador del evento',
    esta_activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_organizador) REFERENCES profesores(id_profesor),
    INDEX idx_fecha (fecha_evento),
    INDEX idx_tipo (tipo_evento),
    INDEX idx_activo (esta_activo)
) ENGINE=InnoDB COMMENT='Catálogo de eventos del colegio';

-- Participantes en eventos
CREATE TABLE participantes_evento (
    id_participacion INT AUTO_INCREMENT PRIMARY KEY,
    id_evento INT NOT NULL,
    id_estudiante INT NOT NULL,
    rol_participacion ENUM('Participante', 'Competidor', 'Organizador', 'Asistente') DEFAULT 'Participante',
    posicion_obtenida VARCHAR(20) COMMENT 'Posición obtenida si hay competencia (1er Lugar, 2do Lugar, 3er Lugar, Participante)',
    observaciones TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_evento) REFERENCES eventos(id_evento) ON DELETE CASCADE,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    UNIQUE KEY participacion_unica (id_evento, id_estudiante),
    INDEX idx_evento (id_evento),
    INDEX idx_estudiante (id_estudiante)
) ENGINE=InnoDB COMMENT='Registro de participación de estudiantes en eventos';

-- ============================================
-- 4. MÓDULO FINANCIERO
-- ============================================

-- Conceptos de pago (catálogo)
CREATE TABLE conceptos_pago (
    id_concepto INT AUTO_INCREMENT PRIMARY KEY,
    codigo_concepto VARCHAR(20) NOT NULL UNIQUE,
    nombre_concepto VARCHAR(100) NOT NULL,
    descripcion TEXT,
    monto_base DECIMAL(10,2) NOT NULL COMMENT 'Valor base del concepto en pesos colombianos',
    es_recurrente BOOLEAN DEFAULT FALSE COMMENT 'Indica si es un pago recurrente (ej: pensión mensual)',
    esta_activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_activo (esta_activo),
    INDEX idx_codigo (codigo_concepto)
) ENGINE=InnoDB COMMENT='Catálogo de conceptos de pago del colegio';

-- Tabla de pagos
CREATE TABLE pagos (
    id_pago INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    id_acudiente INT COMMENT 'Acudiente que realizó el pago',
    id_concepto INT NOT NULL,
    monto_pagado DECIMAL(10,2) NOT NULL,
    fecha_pago DATE NOT NULL,
    metodo_pago ENUM('Efectivo', 'Transferencia', 'Tarjeta Débito', 'Tarjeta Crédito', 'Cheque') NOT NULL,
    numero_recibo VARCHAR(50) UNIQUE,
    año_academico INT COMMENT 'Año al que aplica el pago',
    periodo_academico ENUM('I', 'II', 'III', 'IV', 'Anual') COMMENT 'Período al que aplica el pago',
    notas TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    FOREIGN KEY (id_acudiente) REFERENCES acudientes(id_acudiente),
    FOREIGN KEY (id_concepto) REFERENCES conceptos_pago(id_concepto),
    INDEX idx_estudiante_fecha (id_estudiante, fecha_pago),
    INDEX idx_concepto (id_concepto),
    INDEX idx_fecha (fecha_pago),
    INDEX idx_recibo (numero_recibo)
) ENGINE=InnoDB COMMENT='Registro de todos los pagos realizados';

-- Tabla de facturas electrónicas
CREATE TABLE facturas (
    id_factura INT AUTO_INCREMENT PRIMARY KEY,
    numero_factura VARCHAR(50) NOT NULL UNIQUE COMMENT 'Número consecutivo de factura',
    id_pago INT NOT NULL COMMENT 'Pago asociado a esta factura',
    fecha_emision DATE NOT NULL,
    cufe VARCHAR(100) COMMENT 'Código Único de Factura Electrónica (simulado)',
    nombre_archivo_pdf VARCHAR(255) COMMENT 'Nombre del archivo PDF generado',
    ruta_archivo VARCHAR(500) COMMENT 'Ruta completa donde se guardó el PDF',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_pago) REFERENCES pagos(id_pago) ON DELETE CASCADE,
    INDEX idx_numero_factura (numero_factura),
    INDEX idx_fecha_emision (fecha_emision),
    INDEX idx_pago (id_pago)
) ENGINE=InnoDB COMMENT='Registro de facturas electrónicas generadas';

-- ============================================
-- 5. MÓDULO DE GESTIÓN DE RECURSOS
-- ============================================

-- Categorías de recursos
CREATE TABLE categorias_recurso (
    id_categoria INT AUTO_INCREMENT PRIMARY KEY,
    nombre_categoria VARCHAR(50) NOT NULL UNIQUE,
    descripcion TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Categorías de recursos (Libro, Equipo de Laboratorio, etc.)';

-- Tabla de recursos del colegio
CREATE TABLE recursos (
    id_recurso INT AUTO_INCREMENT PRIMARY KEY,
    codigo_recurso VARCHAR(50) NOT NULL UNIQUE COMMENT 'Código de inventario único',
    nombre_recurso VARCHAR(150) NOT NULL,
    id_categoria INT NOT NULL,
    descripcion TEXT,
    fecha_adquisicion DATE,
    ubicacion_fisica VARCHAR(100) COMMENT 'Ubicación física (Biblioteca, Laboratorio 1, etc.)',
    estado_recurso ENUM('Disponible', 'Prestado', 'En Mantenimiento', 'Dado de Baja') DEFAULT 'Disponible',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_categoria) REFERENCES categorias_recurso(id_categoria),
    INDEX idx_codigo (codigo_recurso),
    INDEX idx_estado (estado_recurso),
    INDEX idx_categoria (id_categoria),
    INDEX idx_nombre (nombre_recurso)
) ENGINE=InnoDB COMMENT='Inventario de recursos prestables del colegio';

-- Tabla de préstamos de recursos a ESTUDIANTES
CREATE TABLE prestamos_estudiante (
    id_prestamo INT AUTO_INCREMENT PRIMARY KEY,
    id_recurso INT NOT NULL,
    id_estudiante INT NOT NULL COMMENT 'Estudiante que toma prestado el recurso',
    fecha_prestamo DATE NOT NULL,
    fecha_devolucion_esperada DATE NOT NULL,
    fecha_devolucion_real DATE,
    estado_prestamo ENUM('Activo', 'Devuelto', 'Vencido') DEFAULT 'Activo',
    observaciones TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_recurso) REFERENCES recursos(id_recurso) ON DELETE CASCADE,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    INDEX idx_recurso_estado (id_recurso, estado_prestamo),
    INDEX idx_estudiante (id_estudiante),
    INDEX idx_estado (estado_prestamo),
    INDEX idx_fecha_devolucion (fecha_devolucion_esperada)
) ENGINE=InnoDB COMMENT='Registro de préstamos de recursos a estudiantes';

-- Tabla de préstamos de recursos a PROFESORES
CREATE TABLE prestamos_profesor (
    id_prestamo INT AUTO_INCREMENT PRIMARY KEY,
    id_recurso INT NOT NULL,
    id_profesor INT NOT NULL COMMENT 'Profesor que toma prestado el recurso',
    fecha_prestamo DATE NOT NULL,
    fecha_devolucion_esperada DATE NOT NULL,
    fecha_devolucion_real DATE,
    estado_prestamo ENUM('Activo', 'Devuelto', 'Vencido') DEFAULT 'Activo',
    observaciones TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_recurso) REFERENCES recursos(id_recurso) ON DELETE CASCADE,
    FOREIGN KEY (id_profesor) REFERENCES profesores(id_profesor) ON DELETE CASCADE,
    INDEX idx_recurso_estado (id_recurso, estado_prestamo),
    INDEX idx_profesor (id_profesor),
    INDEX idx_estado (estado_prestamo),
    INDEX idx_fecha_devolucion (fecha_devolucion_esperada)
) ENGINE=InnoDB COMMENT='Registro de préstamos de recursos a profesores';

-- =====================================================================
-- SECCIÓN 2: DML (datos de prueba)
-- =====================================================================

SET NAMES utf8mb4;

-- Insertar grados académicos
INSERT INTO grados (nombre_grado, nivel_numerico) VALUES
('Preescolar', 0), ('Primero', 1), ('Segundo', 2), ('Tercero', 3), ('Cuarto', 4), ('Quinto', 5),
('Sexto', 6), ('Séptimo', 7), ('Octavo', 8), ('Noveno', 9), ('Décimo', 10), ('Once', 11);

-- Insertar categorías de recursos
INSERT INTO categorias_recurso (nombre_categoria, descripcion) VALUES
('Libro', 'Libros de la biblioteca escolar'),
('Equipo de Laboratorio', 'Equipos científicos y de laboratorio (microscopios, balanzas, etc.)'),
('Equipo Deportivo', 'Equipos y materiales deportivos (balones, conos, etc.)'),
('Dispositivo Tecnológico', 'Tablets, calculadoras científicas, computadores portátiles'),
('Material Didáctico', 'Materiales educativos diversos (mapas, modelos, etc.)');

-- Insertar conceptos de pago comunes
INSERT INTO conceptos_pago (codigo_concepto, nombre_concepto, descripcion, monto_base, es_recurrente) VALUES
('MAT', 'Matrícula', 'Pago anual de matrícula', 500000.00, FALSE),
('PENSION', 'Pensión Mensual', 'Pago mensual de pensión', 350000.00, TRUE),
('UNIFORME', 'Uniforme', 'Uniforme escolar completo', 250000.00, FALSE),
('TRANSPORTE', 'Transporte Escolar', 'Servicio de transporte mensual', 150000.00, TRUE),
('ALIMENTACION', 'Servicio de Alimentación', 'Almuerzo escolar mensual', 120000.00, TRUE),
('ACTIVIDAD', 'Actividad Especial', 'Salida pedagógica o actividad especial', 50000.00, FALSE),
('CERTIFICADO', 'Certificado', 'Certificado de estudio o constancia', 15000.00, FALSE);

INSERT INTO acudientes (tipo_documento, numero_documento, primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, parentesco, telefono_principal, telefono_secundario, correo_electronico, direccion, ocupacion, es_responsable_financiero) VALUES
  ('CC', '52000000', 'Carlos', 'Alberto', 'Rodríguez', 'Pérez', 'Padre', '3001200000', '6014000000', 'acudiente01@correo.com', 'Calle 72 # 10-20, Bogotá', 'Ingeniero civil', 1),
  ('CC', '52173911', 'Marta', 'Lucía', 'Gómez', 'Díaz', 'Madre', '3001237311', NULL, 'acudiente02@correo.com', 'Carrera 15 # 11-22, Bogotá', 'Docente', 1),
  ('CC', '52347822', 'Jorge', 'Enrique', 'Martínez', 'Sánchez', 'Padre', '3001274622', NULL, 'acudiente03@correo.com', 'Avenida 19 # 12-24, Bogotá', 'Contadora', 1),
  ('CC', '52521733', 'Luz', 'Marina', 'López', 'Ramos', 'Madre', '3001311933', '6014005133', 'acudiente04@correo.com', 'Calle 127 # 13-26, Bogotá', 'Médico', 1),
  ('CC', '52695644', 'Fernando', NULL, 'García', 'Silva', 'Padre', '3001349244', NULL, 'acudiente05@correo.com', 'Carrera 7 # 14-28, Bogotá', 'Comerciante', 1),
  ('CC', '52869555', 'Patricia', 'Elena', 'Hernández', 'Mora', 'Madre', '3001386555', NULL, 'acudiente06@correo.com', 'Calle 85 # 15-30, Bogotá', 'Abogada', 1),
  ('CC', '53043466', 'Ricardo', 'Antonio', 'Ramírez', 'Cruz', 'Padre', '3001423866', '6014010266', 'acudiente07@correo.com', 'Avenida 68 # 16-32, Bogotá', 'Arquitecto', 1),
  ('CC', '53217377', 'Claudia', 'Patricia', 'Torres', 'Franco', 'Madre', '3001461177', NULL, 'acudiente08@correo.com', 'Carrera 30 # 17-34, Bogotá', 'Enfermera', 1),
  ('CC', '53391288', 'Héctor', 'Fabio', 'Vargas', 'Gil', 'Padre', '3001498488', NULL, 'acudiente09@correo.com', 'Calle 53 # 18-36, Bogotá', 'Administrador de empresas', 1),
  ('CC', '53565199', 'Diana', 'Marcela', 'Castro', 'Parra', 'Madre', '3001535799', '6014015399', 'acudiente10@correo.com', 'Calle 140 # 19-38, Bogotá', 'Diseñadora gráfica', 1),
  ('CC', '53739110', 'Alberto', NULL, 'Moreno', 'Reyes', 'Padre', '3001573110', NULL, 'acudiente11@correo.com', 'Calle 72 # 20-40, Bogotá', 'Ingeniero civil', 1),
  ('CC', '53913021', 'Sandra', 'Milena', 'Ortiz', 'Soto', 'Madre', '3001610421', NULL, 'acudiente12@correo.com', 'Carrera 15 # 21-42, Bogotá', 'Docente', 1),
  ('CC', '54086932', 'Mauricio', 'Andrés', 'Rojas', 'Vega', 'Padre', '3001647732', '6014020532', 'acudiente13@correo.com', 'Avenida 19 # 22-44, Bogotá', 'Contadora', 1),
  ('CC', '54260843', 'Liliana', NULL, 'Jiménez', 'Ibáñez', 'Madre', '3001685043', NULL, 'acudiente14@correo.com', 'Calle 127 # 23-46, Bogotá', 'Médico', 1),
  ('CC', '54434754', 'Óscar', 'Iván', 'Cárdenas', 'Medina', 'Padre', '3001722354', NULL, 'acudiente15@correo.com', 'Carrera 7 # 24-48, Bogotá', 'Comerciante', 1),
  ('CC', '54608665', 'Adriana', 'María', 'Pardo', 'Cano', 'Madre', '3001759665', '6014025665', 'acudiente16@correo.com', 'Calle 85 # 25-50, Bogotá', 'Abogada', 1),
  ('CC', '54782576', 'Germán', NULL, 'Mejía', 'Lara', 'Padre', '3001796976', NULL, 'acudiente17@correo.com', 'Avenida 68 # 26-52, Bogotá', 'Arquitecto', 1),
  ('CC', '54956487', 'Beatriz', 'Eugenia', 'Suárez', 'Osorio', 'Madre', '3001834287', NULL, 'acudiente18@correo.com', 'Carrera 30 # 27-54, Bogotá', 'Enfermera', 1),
  ('CC', '55130398', 'Rafael', 'Ángel', 'Restrepo', 'Pineda', 'Padre', '3001871598', '6014030798', 'acudiente19@correo.com', 'Calle 53 # 28-56, Bogotá', 'Administrador de empresas', 1),
  ('CC', '55304309', 'Ángela', 'Rocío', 'Quintero', 'Arias', 'Madre', '3001908909', NULL, 'acudiente20@correo.com', 'Calle 140 # 29-58, Bogotá', 'Diseñadora gráfica', 1),
  ('CC', '55478220', 'Camilo', 'Ernesto', 'Salazar', 'Beltrán', 'Padre', '3001946220', NULL, 'acudiente21@correo.com', 'Calle 72 # 30-60, Bogotá', 'Ingeniero civil', 1),
  ('CC', '55652131', 'Gloria', 'Inés', 'Herrera', 'Cuervo', 'Madre', '3001983531', '6014035931', 'acudiente22@correo.com', 'Carrera 15 # 31-62, Bogotá', 'Docente', 1),
  ('CC', '55826042', 'Iván', 'Darío', 'Cortés', 'Dávila', 'Padre', '3002020842', NULL, 'acudiente23@correo.com', 'Avenida 19 # 32-64, Bogotá', 'Contadora', 1),
  ('CC', '55999953', 'Mónica', 'Alexandra', 'Navarro', 'Fuentes', 'Madre', '3002058153', NULL, 'acudiente24@correo.com', 'Calle 127 # 33-66, Bogotá', 'Médico', 1),
  ('CC', '56173864', 'Alfonso', NULL, 'Duarte', 'Giraldo', 'Abuelo', '3002095464', '6014041064', 'acudiente25@correo.com', 'Carrera 7 # 34-68, Bogotá', 'Comerciante', 1),
  ('CC', '56347775', 'Teresa', 'de Jesús', 'Bernal', 'Henao', 'Abuela', '3002132775', NULL, 'acudiente26@correo.com', 'Calle 85 # 35-70, Bogotá', 'Abogada', 1),
  ('CC', '56521686', 'Hugo', 'Armando', 'Acosta', 'Lozano', 'Tío', '3002170086', NULL, 'acudiente27@correo.com', 'Avenida 68 # 36-72, Bogotá', 'Arquitecto', 1),
  ('CC', '56695597', 'Rocío', 'del Pilar', 'Peña', 'Marín', 'Tía', '3002207397', '6014046197', 'acudiente28@correo.com', 'Carrera 30 # 37-74, Bogotá', 'Enfermera', 1),
  ('CC', '56869508', 'Gustavo', 'Adolfo', 'Ríos', 'Neira', 'Tutor Legal', '3002244708', NULL, 'acudiente29@correo.com', 'Calle 53 # 38-76, Bogotá', 'Administrador de empresas', 0),
  ('CC', '57043419', 'Elena', 'Cristina', 'Londoño', 'Ospina', 'Tutor Legal', '3002282019', NULL, 'acudiente30@correo.com', 'Calle 140 # 39-78, Bogotá', 'Diseñadora gráfica', 0);

INSERT INTO estudiantes (tipo_documento, numero_documento, primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, fecha_nacimiento, genero, direccion, telefono, correo_electronico, tipo_sangre, alergias, fecha_ingreso, estado) VALUES
  ('TI', '1000000011', 'Camila', 'Andrea', 'Rodríguez', 'Mora', '2021-01-01', 'F', 'Calle 72 # 10-20, Bogotá', '3001200000', 'estudiante01@littlehorizons.edu.co', 'O+', 'Alergia al polen', '2026-01-15', 'Activo'),
  ('TI', '1002713342', 'Santiago', NULL, 'Gómez', 'Cruz', '2019-06-08', 'M', 'Carrera 15 # 11-22, Bogotá', '3001237311', 'estudiante02@littlehorizons.edu.co', 'A+', NULL, '2025-01-16', 'Activo'),
  ('TI', '1005426673', 'Valentina', 'María', 'Martínez', 'Franco', '2019-11-15', 'F', 'Avenida 19 # 12-24, Bogotá', '3001274622', 'estudiante03@littlehorizons.edu.co', 'B+', NULL, '2024-01-17', 'Activo'),
  ('TI', '1008140004', 'Mateo', 'José', 'López', 'Gil', '2017-04-22', 'M', 'Calle 127 # 13-26, Bogotá', '3001311933', 'estudiante04@littlehorizons.edu.co', 'O-', NULL, '2023-01-18', 'Activo'),
  ('TI', '1010853335', 'Isabella', NULL, 'García', 'Parra', '2017-09-01', 'F', 'Carrera 7 # 14-28, Bogotá', '3001349244', 'estudiante05@littlehorizons.edu.co', 'AB+', NULL, '2023-01-19', 'Activo'),
  ('TI', '1013566666', 'Sebastián', 'David', 'Hernández', 'Reyes', '2015-02-08', 'M', 'Calle 85 # 15-30, Bogotá', '3001386555', 'estudiante06@littlehorizons.edu.co', 'O+', NULL, '2023-01-20', 'Activo'),
  ('TI', '1016279997', 'Mariana', 'Lucía', 'Ramírez', 'Soto', '2015-07-15', 'F', 'Avenida 68 # 16-32, Bogotá', '3001423866', 'estudiante07@littlehorizons.edu.co', 'A+', NULL, '2023-01-21', 'Activo'),
  ('TI', '1018993328', 'Samuel', NULL, 'Torres', 'Vega', '2013-12-22', 'M', 'Carrera 30 # 17-34, Bogotá', '3001461177', 'estudiante08@littlehorizons.edu.co', 'B+', 'Alergia al polen', '2023-01-22', 'Activo'),
  ('TI', '1021706659', 'Daniela', 'Alejandra', 'Vargas', 'Ibáñez', '2013-05-01', 'F', 'Calle 53 # 18-36, Bogotá', '3001498488', 'estudiante09@littlehorizons.edu.co', 'O-', NULL, '2023-01-23', 'Activo'),
  ('TI', '1024419990', 'Nicolás', 'Felipe', 'Castro', 'Medina', '2011-10-08', 'M', 'Calle 140 # 19-38, Bogotá', '3001535799', 'estudiante10@littlehorizons.edu.co', 'AB+', NULL, '2023-01-24', 'Activo'),
  ('TI', '1027133321', 'Sofía', NULL, 'Moreno', 'Cano', '2011-03-15', 'F', 'Calle 72 # 20-40, Bogotá', '3001573110', 'estudiante11@littlehorizons.edu.co', 'O+', NULL, '2023-01-15', 'Activo'),
  ('TI', '1029846652', 'Emilio', 'Andrés', 'Ortiz', 'Lara', '2009-08-22', 'M', 'Carrera 15 # 21-42, Bogotá', '3001610421', 'estudiante12@littlehorizons.edu.co', 'A+', NULL, '2023-01-16', 'Activo'),
  ('TI', '1032559983', 'Luciana', 'Paola', 'Rojas', 'Osorio', '2021-01-01', 'F', 'Avenida 19 # 22-44, Bogotá', '3001647732', 'estudiante13@littlehorizons.edu.co', 'B+', NULL, '2026-01-17', 'Activo'),
  ('TI', '1035273314', 'Tomás', NULL, 'Jiménez', 'Pineda', '2019-06-08', 'M', 'Calle 127 # 23-46, Bogotá', '3001685043', 'estudiante14@littlehorizons.edu.co', 'O-', NULL, '2025-01-18', 'Activo'),
  ('TI', '1037986645', 'Gabriela', 'Sofía', 'Cárdenas', 'Arias', '2019-11-15', 'F', 'Carrera 7 # 24-48, Bogotá', '3001722354', 'estudiante15@littlehorizons.edu.co', 'AB+', 'Alergia al polen', '2024-01-19', 'Activo'),
  ('TI', '1040699976', 'Andrés', 'Camilo', 'Pardo', 'Beltrán', '2017-04-22', 'M', 'Calle 85 # 25-50, Bogotá', '3001759665', 'estudiante16@littlehorizons.edu.co', 'O+', NULL, '2023-01-20', 'Activo'),
  ('TI', '1043413307', 'Juliana', NULL, 'Mejía', 'Cuervo', '2017-09-01', 'F', 'Avenida 68 # 26-52, Bogotá', '3001796976', 'estudiante17@littlehorizons.edu.co', 'A+', NULL, '2023-01-21', 'Activo'),
  ('TI', '1046126638', 'Felipe', 'Esteban', 'Suárez', 'Dávila', '2015-02-08', 'M', 'Carrera 30 # 27-54, Bogotá', '3001834287', 'estudiante18@littlehorizons.edu.co', 'B+', NULL, '2023-01-22', 'Activo'),
  ('TI', '1048839969', 'Paula', 'Andrea', 'Restrepo', 'Fuentes', '2015-07-15', 'F', 'Calle 53 # 28-56, Bogotá', '3001871598', 'estudiante19@littlehorizons.edu.co', 'O-', NULL, '2023-01-23', 'Activo'),
  ('TI', '1051553300', 'David', 'Santiago', 'Quintero', 'Giraldo', '2013-12-22', 'M', 'Calle 140 # 29-58, Bogotá', '3001908909', 'estudiante20@littlehorizons.edu.co', 'AB+', NULL, '2023-01-24', 'Activo'),
  ('TI', '1054266631', 'Natalia', 'Carolina', 'Salazar', 'Henao', '2013-05-01', 'F', 'Calle 72 # 30-60, Bogotá', '3001946220', 'estudiante21@littlehorizons.edu.co', 'O+', NULL, '2023-01-15', 'Activo'),
  ('TI', '1056979962', 'Julián', NULL, 'Herrera', 'Lozano', '2011-10-08', 'M', 'Carrera 15 # 31-62, Bogotá', '3001983531', 'estudiante22@littlehorizons.edu.co', 'A+', 'Alergia al polen', '2023-01-16', 'Activo'),
  ('TI', '1059693293', 'Laura', 'Valentina', 'Cortés', 'Marín', '2011-03-15', 'F', 'Avenida 19 # 32-64, Bogotá', '3002020842', 'estudiante23@littlehorizons.edu.co', 'B+', NULL, '2023-01-17', 'Activo'),
  ('TI', '1062406624', 'Alejandro', 'José', 'Navarro', 'Neira', '2009-08-22', 'M', 'Calle 127 # 33-66, Bogotá', '3002058153', 'estudiante24@littlehorizons.edu.co', 'O-', NULL, '2023-01-18', 'Activo'),
  ('TI', '1065119955', 'Carolina', NULL, 'Duarte', 'Ospina', '2021-01-01', 'F', 'Carrera 7 # 34-68, Bogotá', '3002095464', 'estudiante25@littlehorizons.edu.co', 'AB+', NULL, '2026-01-19', 'Activo'),
  ('TI', '1067833286', 'Esteban', 'Mauricio', 'Bernal', 'Pérez', '2019-06-08', 'M', 'Calle 85 # 35-70, Bogotá', '3002132775', 'estudiante26@littlehorizons.edu.co', 'O+', NULL, '2025-01-20', 'Activo'),
  ('TI', '1070546617', 'Manuela', 'Isabel', 'Acosta', 'Díaz', '2019-11-15', 'F', 'Avenida 68 # 36-72, Bogotá', '3002170086', 'estudiante27@littlehorizons.edu.co', 'A+', NULL, '2024-01-21', 'Activo'),
  ('TI', '1073259948', 'Simón', NULL, 'Peña', 'Sánchez', '2017-04-22', 'M', 'Carrera 30 # 37-74, Bogotá', '3002207397', 'estudiante28@littlehorizons.edu.co', 'B+', NULL, '2023-01-22', 'Activo'),
  ('TI', '1075973279', 'Valeria', 'Fernanda', 'Rodríguez', 'Mora', '2017-09-01', 'F', 'Calle 72 # 10-20, Bogotá', '3001200000', 'estudiante29@littlehorizons.edu.co', 'O-', 'Alergia al polen', '2023-01-23', 'Activo'),
  ('TI', '1078686610', 'Martín', 'Alonso', 'Gómez', 'Cruz', '2015-02-08', 'M', 'Carrera 15 # 11-22, Bogotá', '3001237311', 'estudiante30@littlehorizons.edu.co', 'AB+', NULL, '2023-01-24', 'Activo');

INSERT INTO estudiante_acudiente (id_estudiante, id_acudiente, es_principal, es_contacto_emergencia) VALUES
  (1, 1, 1, 1),
  (2, 2, 1, 1),
  (3, 3, 1, 1),
  (4, 4, 1, 1),
  (5, 5, 1, 1),
  (6, 6, 1, 1),
  (7, 7, 1, 1),
  (8, 8, 1, 1),
  (9, 9, 1, 1),
  (10, 10, 1, 1),
  (11, 11, 1, 1),
  (12, 12, 1, 1),
  (13, 13, 1, 1),
  (14, 14, 1, 1),
  (15, 15, 1, 1),
  (16, 16, 1, 1),
  (17, 17, 1, 1),
  (18, 18, 1, 1),
  (19, 19, 1, 1),
  (20, 20, 1, 1),
  (21, 21, 1, 1),
  (22, 22, 1, 1),
  (23, 23, 1, 1),
  (24, 24, 1, 1),
  (25, 25, 1, 1),
  (26, 26, 1, 1),
  (27, 27, 1, 1),
  (28, 28, 1, 1),
  (29, 1, 1, 1),
  (30, 2, 1, 1),
  (3, 29, 0, 1),
  (4, 30, 0, 1);

INSERT INTO matriculas (id_estudiante, id_grado, año_academico, estado_matricula, fecha_matricula, fecha_retiro) VALUES
  (1, 1, 2026, 'Activa', '2026-01-15', NULL),
  (2, 2, 2026, 'Activa', '2026-01-16', NULL),
  (2, 1, 2025, 'Completada', '2025-01-16', NULL),
  (3, 3, 2026, 'Activa', '2026-01-17', NULL),
  (3, 2, 2025, 'Completada', '2025-01-17', NULL),
  (4, 4, 2026, 'Activa', '2026-01-18', NULL),
  (4, 3, 2025, 'Completada', '2025-01-18', NULL),
  (5, 5, 2026, 'Activa', '2026-01-19', NULL),
  (5, 4, 2025, 'Completada', '2025-01-19', NULL),
  (6, 6, 2026, 'Activa', '2026-01-20', NULL),
  (6, 5, 2025, 'Completada', '2025-01-20', NULL),
  (7, 7, 2026, 'Activa', '2026-01-21', NULL),
  (7, 6, 2025, 'Completada', '2025-01-21', NULL),
  (8, 8, 2026, 'Activa', '2026-01-22', NULL),
  (8, 7, 2025, 'Completada', '2025-01-22', NULL),
  (9, 9, 2026, 'Activa', '2026-01-23', NULL),
  (9, 8, 2025, 'Completada', '2025-01-23', NULL),
  (10, 10, 2026, 'Activa', '2026-01-24', NULL),
  (10, 9, 2025, 'Completada', '2025-01-24', NULL),
  (11, 11, 2026, 'Activa', '2026-01-15', NULL),
  (11, 10, 2025, 'Completada', '2025-01-15', NULL),
  (12, 12, 2026, 'Activa', '2026-01-16', NULL),
  (12, 11, 2025, 'Completada', '2025-01-16', NULL),
  (13, 1, 2026, 'Activa', '2026-01-17', NULL),
  (14, 2, 2026, 'Activa', '2026-01-18', NULL),
  (14, 1, 2025, 'Completada', '2025-01-18', NULL),
  (15, 3, 2026, 'Activa', '2026-01-19', NULL),
  (15, 2, 2025, 'Completada', '2025-01-19', NULL),
  (16, 4, 2026, 'Activa', '2026-01-20', NULL),
  (16, 3, 2025, 'Completada', '2025-01-20', NULL),
  (17, 5, 2026, 'Activa', '2026-01-21', NULL),
  (17, 4, 2025, 'Completada', '2025-01-21', NULL),
  (18, 6, 2026, 'Activa', '2026-01-22', NULL),
  (18, 5, 2025, 'Completada', '2025-01-22', NULL),
  (19, 7, 2026, 'Activa', '2026-01-23', NULL),
  (19, 6, 2025, 'Completada', '2025-01-23', NULL),
  (20, 8, 2026, 'Activa', '2026-01-24', NULL),
  (20, 7, 2025, 'Completada', '2025-01-24', NULL),
  (21, 9, 2026, 'Activa', '2026-01-15', NULL),
  (21, 8, 2025, 'Completada', '2025-01-15', NULL),
  (22, 10, 2026, 'Activa', '2026-01-16', NULL),
  (22, 9, 2025, 'Completada', '2025-01-16', NULL),
  (23, 11, 2026, 'Activa', '2026-01-17', NULL),
  (23, 10, 2025, 'Completada', '2025-01-17', NULL),
  (24, 12, 2026, 'Activa', '2026-01-18', NULL),
  (24, 11, 2025, 'Completada', '2025-01-18', NULL),
  (25, 1, 2026, 'Activa', '2026-01-19', NULL),
  (26, 2, 2026, 'Activa', '2026-01-20', NULL),
  (26, 1, 2025, 'Completada', '2025-01-20', NULL),
  (27, 3, 2026, 'Activa', '2026-01-21', NULL),
  (27, 2, 2025, 'Completada', '2025-01-21', NULL),
  (28, 4, 2026, 'Activa', '2026-01-22', NULL),
  (28, 3, 2025, 'Completada', '2025-01-22', NULL),
  (29, 5, 2026, 'Activa', '2026-01-23', NULL),
  (29, 4, 2025, 'Completada', '2025-01-23', NULL),
  (30, 6, 2026, 'Activa', '2026-01-24', NULL),
  (30, 5, 2025, 'Completada', '2025-01-24', NULL);

