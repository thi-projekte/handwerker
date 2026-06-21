CREATE INDEX IF NOT EXISTS idx_material_name_trgm
    ON material USING gin (name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_material_description_trgm
    ON material USING gin (description gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_material_search
    ON material
    USING gin (
    (
    setweight(to_tsvector('german', unaccent(coalesce(name, ''))), 'A') ||
    setweight(to_tsvector('german', unaccent(coalesce(description, ''))), 'B') ||
    setweight(to_tsvector('german', unaccent(coalesce(category, ''))), 'C')
    )
    );