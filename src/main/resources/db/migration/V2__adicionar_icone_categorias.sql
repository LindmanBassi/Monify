ALTER TABLE categorias ADD COLUMN icone VARCHAR(20) NOT NULL DEFAULT 'ETIQUETA';

UPDATE categorias
SET icone = CASE
    WHEN nome = 'Alimentação' THEN 'TALHERES'
    WHEN nome = 'Moradia' THEN 'CASA'
    WHEN nome = 'Transporte' THEN 'CARRO'
    WHEN nome = 'Saúde' THEN 'CORACAO'
    WHEN nome = 'Educação' THEN 'CAPELO'
    WHEN nome = 'Lazer' THEN 'CLAQUETE'
    WHEN nome = 'Compras' THEN 'SACOLA'
    WHEN nome = 'Salário' THEN 'MALETA'
    WHEN nome = 'Investimentos' THEN 'BANCO'
    WHEN nome = 'Outros' AND tipo = 'RECEITA' THEN 'MOEDAS'
    WHEN nome = 'Outros' THEN 'FORMAS'
    ELSE 'ETIQUETA'
END;

ALTER TABLE categorias ALTER COLUMN icone DROP DEFAULT;
