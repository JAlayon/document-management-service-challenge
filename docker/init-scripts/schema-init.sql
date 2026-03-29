--The script to initialize the schema was sourced from the Spring Batch Core dependency: org.springframework.batch.core.

CREATE SCHEMA IF NOT EXISTS document_schema;
SET SCHEMA 'document_schema';

CREATE TABLE document_schema.documents (
       id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
       username     VARCHAR(255) NOT NULL,
       file_name    VARCHAR(255) NOT NULL,
       storage_path VARCHAR(500) NOT NULL,
       file_size    BIGINT       NOT NULL,
       file_type    VARCHAR(100) NOT NULL,
       created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
       CONSTRAINT uk_documents_username_file_name UNIQUE (username, file_name)
);

CREATE TABLE document_tags (
       document_id UUID         NOT NULL REFERENCES document_schema.documents(id) ON DELETE CASCADE,
       tag         VARCHAR(255) NOT NULL,
       PRIMARY KEY (document_id, tag)
);

-- Indexes to improve search performance
CREATE INDEX idx_documents_username   ON document_schema.documents(username);
CREATE INDEX idx_documents_file_name  ON document_schema.documents(file_name);
CREATE INDEX idx_documents_created_at ON document_schema.documents(created_at DESC);
CREATE INDEX idx_document_tags_tag    ON document_schema.document_tags(tag);

