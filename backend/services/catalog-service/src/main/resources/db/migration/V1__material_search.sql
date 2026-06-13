CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE INDEX IF NOT EXISTS idx_material_search
    ON material
    USING gin (
    (
    setweight(to_tsvector('german', unaccent(coalesce(name, ''))), 'A') ||
    setweight(to_tsvector('german', unaccent(coalesce(description, ''))), 'B') ||
    setweight(to_tsvector('german', unaccent(coalesce(category, ''))), 'C')
    )
    );