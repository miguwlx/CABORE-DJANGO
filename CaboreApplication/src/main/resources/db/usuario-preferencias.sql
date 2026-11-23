-- ============================================================
-- CABORE — Sistema Global de Temas Visuales (Claro / Oscuro)
-- Tabla: usuario_preferencias (preferencia individual por usuario)
--
-- Script idempotente (CREATE TABLE IF NOT EXISTS) que se ejecuta
-- automáticamente en perfil dev sin alterar datos existentes.
-- ============================================================

CREATE TABLE IF NOT EXISTS usuario_preferencias (
    id             INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id     INT NOT NULL,
    tema           VARCHAR(16) NOT NULL DEFAULT 'claro',
    actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_usuario_preferencias_usuario UNIQUE (usuario_id)
);

CREATE INDEX IF NOT EXISTS idx_usuario_pref_usuario ON usuario_preferencias (usuario_id);
