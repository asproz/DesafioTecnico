INSERT INTO USER (ID, USER_NAME) VALUES(1, 'accounttest');
INSERT INTO ACCOUNT (ID, ACCOUNT_NUMBER, AGENCY, ACCOUNT_DIGIT, AGENCY_DIGIT, BALANCE, ACTIVE, USER_ID) VALUES (1, '99999', '1231', '1', '4', 9000, TRUE, 1);
INSERT INTO HISTORY (ID, OPERATION, MESSAGE, ACCOUNT_ID, CURRENT_BALANCE) VALUES(1, 1, 'DEPÓSITO REALIZADO', 1, 10000);
COMMIT;