package com.example.operator.service;

import com.example.exception.APIException;
import com.example.exception.FieldCannotBeNullException;
import com.example.exception.FromUserIdCannotBeTheSameOfToUserIdException;
import com.example.account.entity.Account;
import com.example.exception.ValueCannotBeNegativeOrZeroException;
import com.example.operator.request.BillRequest;
import com.example.operator.request.TransferRequest;
import com.example.account.service.AccountService;
import com.example.history.entity.History;
import com.example.history.request.HistoryRequest;
import com.example.history.producer.HistoryProducer;
import com.example.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public class UserAccountOperatorService {

    @Value("${kafka.enable}")
    private boolean isKafkaEnabled;
    private final AccountService accountService;
    private final HistoryProducer historyProducer;

    @Autowired
    public UserAccountOperatorService(final AccountService accountService,
                                      final HistoryProducer historyProducer) {
        this.accountService = accountService;
        this.historyProducer = historyProducer;
    }

    @Transactional
    public void withdraw(final Long userId, final BigDecimal value) {
        validateIfObjIsNull(value);
        validateIfValueIsNegativeOrZero(value);

        final Account account = this.accountService.findByUserId(userId);
        account.withdraw(value);

        try {
            this.accountService.save(account);

            if (isKafkaEnabled) {
                this.historyProducer.send(getHistoryRequest(History.Operation.WITHDRAW, account,
                        String.format("Saque de %s realizado por %s", value, account.getUser().getUserName())));
            }
        } catch (final Exception e) {
            throw new APIException(e.getMessage());
        }
    }

    @Transactional
    public void deposit(final Long userId, final BigDecimal value) {
        validateIfObjIsNull(value);
        validateIfValueIsNegativeOrZero(value);

        final Account account = this.accountService.findByUserId(userId);

        try {
            account.deposit(value);
            this.accountService.save(account);
            if (isKafkaEnabled) {
                this.historyProducer.send(getHistoryRequest(History.Operation.DEPOSIT, account,
                        String.format("Dep??sito de %s realizado por %s", value.toString(), account.getUser().getUserName())));
            }
        } catch (final Exception e) {
            throw new APIException(e.getMessage());
        }
    }

    @Transactional
    public void transfer(final Long fromUserId,
                         final Long toUserId,
                         final TransferRequest transferRequest) {
        validateIfTransferRequestIsFilled(transferRequest);
        validateIfValueIsNegativeOrZero(transferRequest.getTransferedValue());
        validateIfBothUserIdsAreEqual(fromUserId, toUserId);

        final Account fromAccount = this.accountService.findByUserId(fromUserId);
        final Account toAccount = this.accountService.findByUserId(toUserId);
        try {
            fromAccount.withdraw(transferRequest.getTransferedValue());
            this.accountService.save(fromAccount);

            toAccount.deposit(transferRequest.getTransferedValue());
            this.accountService.save(toAccount);

            if (isKafkaEnabled) {
                this.historyProducer.send(getHistoryRequest(History.Operation.TRANSFERENCE, fromAccount,
                        String.format("Enviado transfer??ncia de %s realizado por %s para %s",
                                transferRequest.getTransferedValue().toString(),
                                fromAccount.getUser().getUserName(), toAccount.getUser().getUserName())));

                this.historyProducer.send(getHistoryRequest(History.Operation.TRANSFERENCE, toAccount,
                        String.format("Recebido Transfer??ncia de %s realizado por %s para %s",
                                transferRequest.getTransferedValue().toString(),
                                fromAccount.getUser().getUserName(), toAccount.getUser().getUserName())));
            }
        } catch (final Exception e) {
            throw new APIException(e.getMessage());
        }
    }

    @Transactional
    public void payBill(final Long userId, final BillRequest billRequest) {
        this.validateBillRequest(billRequest);
        this.validateIfValueIsNegativeOrZero(billRequest.getValue());

        final Account account = this.accountService.findByUserId(userId);
        account.withdraw(billRequest.getValue());
        this.accountService.save(account);

        try {
            if (isKafkaEnabled) {
                this.historyProducer.send(getHistoryRequest(History.Operation.BILL_PAYMENT, account,
                        String.format("Pagamento de Conta de %s realizado por %s",
                                Utils.getValueInBRLFormattedCurrency(billRequest.getValue()),
                                account.getUser().getUserName())));
            }
        } catch (final Exception e) {
            throw new APIException(e.getMessage());
        }
    }

    private HistoryRequest getHistoryRequest(final History.Operation historyOperation, final Account account, final String message) {
        final HistoryRequest history = new HistoryRequest();
        history.setOperation(historyOperation);
        history.setAccountId(account.getId());
        history.setCurrentBalance(account.getBalance());
        history.setMessage(message);
        return history;
    }

    private void validateIfObjIsNull(final Object obj) {
        if (Objects.isNull(obj)) {
            throw new FieldCannotBeNullException("H?? campos que necessitam de preenchimento para informa????es dep??sito/saque!");
        }
    }

    private void validateIfBothUserIdsAreEqual(Long fromUserId, Long toUserId) {
        if (fromUserId.equals(toUserId)) {
            throw new FromUserIdCannotBeTheSameOfToUserIdException();
        }
    }

    private void validateIfTransferRequestIsFilled(final TransferRequest transferRequest) {
        if (Objects.isNull(transferRequest) || Objects.isNull(transferRequest.getTransferedValue()) || Objects.isNull(transferRequest.getMessage())) {
            throw new FieldCannotBeNullException("H?? campos que necessitam de preenchimento para informa????es de transfer??ncia!");
        }
    }

    private void validateBillRequest(final BillRequest billRequest) {
        if (Objects.isNull(billRequest) || Objects.isNull(billRequest.getBarCode()) || Objects.isNull(billRequest.getValue())
                || Objects.isNull(billRequest.getDescription())) {
            throw new FieldCannotBeNullException("H?? campos que necessitam de preenchimento para informa????es de pagamento do boleto!");
        }
    }

    private void validateIfValueIsNegativeOrZero(final BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValueCannotBeNegativeOrZeroException();
        }
    }
}
