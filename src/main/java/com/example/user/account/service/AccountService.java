package com.example.user.account.service;

import com.example.exception.AccountNotFoundException;
import com.example.user.account.entity.Account;
import com.example.user.account.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    private final AccountRepository repository;

    @Autowired
    public AccountService(final AccountRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void save(final Account account) {
        this.repository.save(account);
    }

    public List<Account> findByUserId(final Long userId) {
        return this.repository.findByUserId(userId);
    }

    public Account findActiveByUserId(final Long userId) {
        return Optional
                .of(this.repository.findActiveByUserId(userId))
                .orElseThrow(AccountNotFoundException::new);
    }

    @Transactional
    public void deleteByUserId(final Long userId) {
        this.repository.deleteAll(this.repository.findByUserId(userId));
    }
}
