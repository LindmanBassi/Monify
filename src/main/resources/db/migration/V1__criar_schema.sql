CREATE TABLE usuarios (
    id            BINARY(16)   NOT NULL,
    nome          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    senha_hash    VARCHAR(100) NOT NULL,
    criado_em     DATETIME(6)  NOT NULL,
    atualizado_em DATETIME(6)  NOT NULL,
    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_email UNIQUE (email)
);

CREATE TABLE tokens_renovacao (
    id          BINARY(16)  NOT NULL,
    usuario_id  BINARY(16)  NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    expira_em   DATETIME(6) NOT NULL,
    revogado_em DATETIME(6) NULL,
    criado_em   DATETIME(6) NOT NULL,
    CONSTRAINT pk_tokens_renovacao PRIMARY KEY (id),
    CONSTRAINT uk_tokens_renovacao_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_tokens_renovacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE TABLE categorias (
    id         BINARY(16)  NOT NULL,
    usuario_id BINARY(16)  NOT NULL,
    nome       VARCHAR(50) NOT NULL,
    tipo       VARCHAR(10) NOT NULL,
    cor        VARCHAR(7)  NOT NULL,
    arquivada  BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_categorias PRIMARY KEY (id),
    CONSTRAINT uk_categorias_usuario_nome_tipo UNIQUE (usuario_id, nome, tipo),
    CONSTRAINT fk_categorias_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT ck_categorias_tipo CHECK (tipo IN ('RECEITA', 'DESPESA'))
);

CREATE TABLE transacoes (
    id              BINARY(16)     NOT NULL,
    usuario_id      BINARY(16)     NOT NULL,
    categoria_id    BINARY(16)     NOT NULL,
    tipo            VARCHAR(10)    NOT NULL,
    valor           DECIMAL(19, 2) NOT NULL,
    descricao       VARCHAR(255)   NULL,
    data_ocorrencia DATE           NOT NULL,
    criado_em       DATETIME(6)    NOT NULL,
    atualizado_em   DATETIME(6)    NOT NULL,
    CONSTRAINT pk_transacoes PRIMARY KEY (id),
    CONSTRAINT fk_transacoes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_transacoes_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id),
    CONSTRAINT ck_transacoes_tipo CHECK (tipo IN ('RECEITA', 'DESPESA')),
    CONSTRAINT ck_transacoes_valor CHECK (valor > 0)
);

CREATE INDEX idx_transacoes_usuario_data ON transacoes (usuario_id, data_ocorrencia);

CREATE TABLE orcamentos (
    id             BINARY(16)     NOT NULL,
    usuario_id     BINARY(16)     NOT NULL,
    categoria_id   BINARY(16)     NOT NULL,
    mes_referencia DATE           NOT NULL,
    valor_limite   DECIMAL(19, 2) NOT NULL,
    CONSTRAINT pk_orcamentos PRIMARY KEY (id),
    CONSTRAINT uk_orcamentos_usuario_categoria_mes UNIQUE (usuario_id, categoria_id, mes_referencia),
    CONSTRAINT fk_orcamentos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_orcamentos_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id),
    CONSTRAINT ck_orcamentos_valor_limite CHECK (valor_limite > 0),
    CONSTRAINT ck_orcamentos_mes_dia_1 CHECK (DAY(mes_referencia) = 1)
);

CREATE TABLE metas_economia (
    id               BINARY(16)     NOT NULL,
    usuario_id       BINARY(16)     NOT NULL,
    categoria_id     BINARY(16)     NOT NULL,
    orcamento_id     BINARY(16)     NULL,
    mes_referencia   DATE           NOT NULL,
    tipo_referencia  VARCHAR(20)    NOT NULL,
    valor_economia   DECIMAL(19, 2) NOT NULL,
    valor_referencia DECIMAL(19, 2) NULL,
    valor_limite     DECIMAL(19, 2) NULL,
    congelada_em     DATETIME(6)    NULL,
    cancelada_em     DATETIME(6)    NULL,
    -- 1 enquanto não cancelada; NULL depois. Índices únicos aceitam múltiplos NULL,
    -- então metas canceladas não conflitam (MySQL não suporta índice parcial).
    ativa            TINYINT GENERATED ALWAYS AS (IF(cancelada_em IS NULL, 1, NULL)) STORED,
    CONSTRAINT pk_metas_economia PRIMARY KEY (id),
    CONSTRAINT uk_metas_economia_ativa UNIQUE (usuario_id, categoria_id, mes_referencia, ativa),
    CONSTRAINT fk_metas_economia_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_metas_economia_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id),
    -- Excluir o orçamento cancela as metas pendentes (no service); as demais mantêm os valores congelados.
    CONSTRAINT fk_metas_economia_orcamento FOREIGN KEY (orcamento_id) REFERENCES orcamentos (id)
        ON DELETE SET NULL,
    CONSTRAINT ck_metas_economia_tipo_referencia
        CHECK (tipo_referencia IN ('ORCAMENTO', 'MES_ANTERIOR', 'MEDIA_TRES_MESES')),
    CONSTRAINT ck_metas_economia_valor_economia CHECK (valor_economia > 0),
    CONSTRAINT ck_metas_economia_mes_dia_1 CHECK (DAY(mes_referencia) = 1)
);

CREATE TABLE metas_poupanca (
    id         BINARY(16)     NOT NULL,
    usuario_id BINARY(16)     NOT NULL,
    nome       VARCHAR(100)   NOT NULL,
    valor_alvo DECIMAL(19, 2) NOT NULL,
    prazo      DATE           NULL,
    status     VARCHAR(10)    NOT NULL,
    criado_em  DATETIME(6)    NOT NULL,
    CONSTRAINT pk_metas_poupanca PRIMARY KEY (id),
    CONSTRAINT fk_metas_poupanca_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT ck_metas_poupanca_valor_alvo CHECK (valor_alvo > 0),
    CONSTRAINT ck_metas_poupanca_status CHECK (status IN ('ATIVA', 'CONCLUIDA', 'CANCELADA'))
);

CREATE TABLE aportes (
    id               BINARY(16)     NOT NULL,
    meta_poupanca_id BINARY(16)     NOT NULL,
    valor            DECIMAL(19, 2) NOT NULL,
    data             DATE           NOT NULL,
    observacao       VARCHAR(255)   NULL,
    CONSTRAINT pk_aportes PRIMARY KEY (id),
    CONSTRAINT fk_aportes_meta_poupanca FOREIGN KEY (meta_poupanca_id) REFERENCES metas_poupanca (id),
    CONSTRAINT ck_aportes_valor CHECK (valor <> 0)
);

CREATE INDEX idx_aportes_meta_poupanca ON aportes (meta_poupanca_id);
