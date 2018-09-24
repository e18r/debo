DROP SCHEMA IF EXISTS debo CASCADE;

CREATE SCHEMA debo;

CREATE TABLE debo.currency_types (
       id SMALLINT PRIMARY KEY,
       name VARCHAR UNIQUE NOT NULL
);

INSERT INTO debo.currency_types (id, name)
VALUES (1, 'fiat'),
       (2, 'crypto'),
       (3, 'stablecoin');

CREATE TABLE debo.account_types (
       id SERIAL PRIMARY KEY,
       name VARCHAR UNIQUE NOT NULL
);

INSERT INTO debo.account_types (id, name)
VALUES (1, 'asset'),
       (2, 'liability'),
       (3, 'income'),
       (4, 'expense'),
       (5, 'equity');

CREATE TABLE debo.users (
       id SERIAL PRIMARY KEY,
       email VARCHAR UNIQUE NOT NULL,
       session_token VARCHAR NOT NULL
);

INSERT INTO debo.users (email, session_token)
VALUES ('satoshi@nakamotoinstitute.org', 'V014adcmHAVMq6DTAv5QCVbGWMg45kaCgCUGmU6VL8bsFpnWuF97zJ15psvWqWto');

CREATE TABLE debo.currencies (
       id SERIAL PRIMARY KEY,
       user_id INT NOT NULL REFERENCES debo.users,
       code CHAR(3) NOT NULL,
       name VARCHAR,
       type SMALLINT NOT NULL REFERENCES debo.currency_types DEFAULT 1,
       CONSTRAINT code_uniqueness UNIQUE (code, user_id)
);

INSERT INTO debo.currencies (user_id, code, name, type)
VALUES (1, 'USD', 'United States Dollar', 1),
       (1, 'EUR', 'Euro', 1),
       (1, 'CHF', 'Swiss Franc', 1),
       (1, 'BRL', 'Brazilian Real', 1),
       (1, 'COP', 'Colombian Peso', 1),
       (1, 'BTC', 'Bitcoin', 2),
       (1, 'ETH', 'Ether', 2),
       (1, 'EOS', 'EOS', 2),
       (1, 'MKR', 'Maker', 2),
       (1, 'ANT', 'Aragon', 2),
       (1, 'GNO', 'Gnosis', 2),
       (1, 'SNT', 'Status', 2),
       (1, 'DAI', 'Dai', 3);

CREATE TABLE debo.accounts (
       id SERIAL PRIMARY KEY,
       user_id INT NOT NULL REFERENCES debo.users,
       name VARCHAR NOT NULL,
       type INT NOT NULL REFERENCES debo.account_types,
       CONSTRAINT name_uniqueness UNIQUE (name, user_id)
);

INSERT INTO debo.accounts (user_id, name, type)
VALUES (1, 'Bancolombia', 1),
       (1, 'wallet', 1),
       (1, 'Samourai', 1),
       (1, 'restaurant', 4),
       (1, 'COP equity', 5),
       (1, 'BTC equity', 5);

CREATE TABLE debo.transactions (
       id SERIAL PRIMARY KEY,
       user_id INT NOT NULL REFERENCES debo.users,
       date TIMESTAMP (0) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
       amount NUMERIC NOT NULL,
       currency INT NOT NULL REFERENCES debo.currencies,
       debit INT NOT NULL REFERENCES debo.accounts,
       credit INT NOT NULL REFERENCES debo.accounts,
       comment TEXT,
       CONSTRAINT debit_not_credit CHECK (debit != credit)
);

INSERT INTO debo.transactions (user_id, amount, currency, debit, credit, comment)
VALUES (1, 35450, 5, 1, 5, 'initial balance'),
       (1, 22000, 5, 2, 5, 'initial balance'),
       (1, 0.58374957, 6, 3, 6, 'initial balance'),
       (1, 12000, 5, 4, 2, 'McDonalds with friends'),
       (1, 600000, 5, 2, 1, 'took some bucks out of the ATM'),
       (1, 35550, 5, 4, 2, NULL),
       (1, 100000, 5, 1, 2, 'I deposited some money in my bank account'),
       (1, 10000, 5, 2, 4, 'Got a refund from the restaurant'),
       (1, 123450, 5, 5, 4, 'the restaurant had refunded me in the past');
