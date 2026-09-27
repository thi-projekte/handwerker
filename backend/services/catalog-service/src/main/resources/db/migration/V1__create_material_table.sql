CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS material (
    id UUID PRIMARY KEY,

    ownerId VARCHAR(255) NOT NULL,

    articleNumber VARCHAR(255) NOT NULL,

    name VARCHAR(255) NOT NULL,

    manufacturer VARCHAR(255),

    description VARCHAR(5000),

    category VARCHAR(255),

    unit VARCHAR(255) NOT NULL,

    price NUMERIC(19,2) NOT NULL,

    currency VARCHAR(10) NOT NULL,

    source VARCHAR(50) NOT NULL,

    active BOOLEAN NOT NULL,

    createdAt TIMESTAMP,

    updatedAt TIMESTAMP
    );