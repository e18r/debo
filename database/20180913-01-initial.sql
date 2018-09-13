DROP SCHEMA IF EXISTS debo CASCADE;

CREATE SCHEMA debo;

CREATE TABLE debo.parameters (
       id SMALLINT PRIMARY KEY,
       name VARCHAR UNIQUE NOT NULL,
       value NUMERIC NOT NULL
);

INSERT INTO debo.parameters (id, name, value)
VALUES (1, 'max debt ratio', 0.333);

CREATE TABLE debo.currency_types (
       id SMALLINT PRIMARY KEY,
       name VARCHAR UNIQUE NOT NULL
);

INSERT INTO debo.currency_types (id, name)
VALUES (1, 'fiat'),
       (2, 'crypto'),
       (3, 'stablecoin');

CREATE TABLE debo.currencies (
       id SERIAL PRIMARY KEY,
       code CHAR(3) UNIQUE NOT NULL,
       name VARCHAR,
       type SMALLINT NOT NULL REFERENCES debo.currency_types DEFAULT 1
);

INSERT INTO debo.currencies (code, name, type)
VALUES ('USD', 'United States Dollar', 1),
       ('EUR', 'Euro', 1),
       ('CHF', 'Swiss Franc', 1),
       ('BRL', 'Brazilian Real', 1),
       ('COP', 'Colombian Peso', 1),
       ('BTC', 'Bitcoin', 2),
       ('ETH', 'Ether', 2),
       ('EOS', 'EOS', 2),
       ('MKR', 'Maker', 2),
       ('ANT', 'Aragon', 2),
       ('GNO', 'Gnosis', 2),
       ('SNT', 'Status', 2),
       ('DAI', 'Dai', 3);

CREATE TABLE debo.portfolio (
       id SERIAL PRIMARY KEY,
       currency INT UNIQUE NOT NULL REFERENCES debo.currencies,
       weight DOUBLE PRECISION NOT NULL CHECK (weight >= 0 AND weight <= 1)
);

INSERT INTO debo.portfolio (currency, weight)
VALUES (1, 0.3),
       (6, 0.5),
       (10, 0.2);
       
CREATE TABLE debo.account_types (
       id SERIAL PRIMARY KEY,
       name VARCHAR UNIQUE NOT NULL
);

INSERT INTO debo.account_types (id, name)
VALUES (1, 'asset'),
       (2, 'liability'),
       (3, 'income'),
       (4, 'expense');

CREATE TABLE debo.accounts (
       id SERIAL PRIMARY KEY,
       name VARCHAR UNIQUE NOT NULL,
       currency INT NOT NULL REFERENCES debo.currencies,
       type INT NOT NULL REFERENCES debo.account_types
);

INSERT INTO debo.accounts (name, currency, type)
VALUES ('Bancolombia', 5, 1),
       ('wallet', 5, 1),
       ('Samourai', 6, 1),
       ('restaurant', 5, 4);

CREATE TABLE debo.transactions (
       id SERIAL PRIMARY KEY,
       date TIMESTAMP (0) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
       amount NUMERIC NOT NULL,
       debit INT REFERENCES debo.accounts,
       credit INT REFERENCES debo.accounts,
       comment TEXT,
       CONSTRAINT not_both_null CHECK (debit IS NOT NULL OR credit IS NOT NULL)
);

INSERT INTO debo.transactions (amount, debit, credit, comment)
VALUES (35450, 1, NULL, 'initial balance'),
       (22000, 2, NULL, 'initial balance'),
       (0.58374957, 3, NULL, 'initial balance'),
       (12000, 4, 2, 'McDonalds with friends'),
       (600000, 2, 1, 'took some bucks out of the ATM'),
       (35550, 4, 2, NULL),
       (100000, 1, 2, 'I deposited some money in my bank account'),
       (10000, 2, 4, 'Got a refund from the restaurant'),
       (123450, NULL, 4, 'the restaurant had refunded me in the past');
