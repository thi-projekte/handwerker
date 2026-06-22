CREATE INDEX IF NOT EXISTS idx_material_name_trgm
    ON material USING gin (name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_material_description_trgm
    ON material USING gin (description gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_material_category_trgm
    ON material USING gin (category gin_trgm_ops);