CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_material_name_trgm
    ON material USING gin (name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_material_description_trgm
    ON material USING gin (description gin_trgm_ops);