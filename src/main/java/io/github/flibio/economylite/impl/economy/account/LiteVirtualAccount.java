/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2017 Flibio
 * Copyright (c) Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.github.flibio.economylite.impl.economy.account;

import io.github.flibio.economylite.CauseFactory;
import io.github.flibio.economylite.EconomyLite;
import io.github.flibio.economylite.api.CurrencyEconService;
import io.github.flibio.economylite.api.VirtualEconService;
import io.github.flibio.economylite.impl.economy.event.LiteEconomyTransactionEvent;
import io.github.flibio.economylite.impl.economy.result.LiteTransactionResult;
import io.github.flibio.economylite.impl.economy.result.LiteTransferResult;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.account.VirtualAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.service.economy.transaction.TransactionType;
import org.spongepowered.api.service.economy.transaction.TransactionTypes;
import org.spongepowered.api.service.economy.transaction.TransferResult;
import org.spongepowered.api.text.Text;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class LiteVirtualAccount implements VirtualAccount {

    private VirtualEconService virtualService = EconomyLite.getVirtualService();
    private CurrencyEconService currencyService = EconomyLite.getCurrencyService();

    private String name;

    public LiteVirtualAccount(String id) {
        this.name = id;
    }

    @Override
    public Text getDisplayName() {
        return Text.of(name);
    }

    @Override
    public BigDecimal getDefaultBalance(Currency currency) {
        Optional<Double> bOpt = EconomyLite.getFileManager().getValue("config.conf", "virt-default-balance", Double.class);
        if (bOpt.isPresent()) {
            return BigDecimal.valueOf(bOpt.get());
        } else {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public boolean hasBalance(Currency currency, Set<Context> contexts) {
        return virtualService.accountExists(name, currency, CauseFactory.create("Has Balance"));
    }

    @Override
    public BigDecimal getBalance(Currency currency, Set<Context> contexts) {
        return virtualService.getBalance(name, currency, CauseFactory.create("Get Balance"));
    }

    @Override
    public Map<Currency, BigDecimal> getBalances(Set<Context> contexts) {
        HashMap<Currency, BigDecimal> balances = new HashMap<Currency, BigDecimal>();
        for (Currency currency : currencyService.getCurrencies()) {
            if (virtualService.accountExists(name, currency, CauseFactory.create("Get Balances"))) {
                balances.put(currency, virtualService.getBalance(name, currency, CauseFactory.create("Get Balances Put")));
            }
        }
        return balances;
    }

    @Override
    public TransactionResult setBalance(Currency currency, BigDecimal amount, Cause cause, Set<Context> contexts) {
        // Check if the new balance is in bounds
        if (amount.compareTo(BigDecimal.ZERO) == -1 || amount.compareTo(BigDecimal.valueOf(999999999)) == 1) {
            return resultAndEvent(this, amount, currency, ResultType.ACCOUNT_NO_SPACE, TransactionTypes.DEPOSIT, cause);
        }
        if (virtualService.setBalance(name, amount, currency, cause)) {
            return resultAndEvent(this, amount, currency, ResultType.SUCCESS, TransactionTypes.DEPOSIT, cause);
        } else {
            return resultAndEvent(this, amount, currency, ResultType.FAILED, TransactionTypes.DEPOSIT, cause);
        }
    }

    @Override
    public Map<Currency, TransactionResult> resetBalances(Cause cause, Set<Context> contexts) {
        HashMap<Currency, TransactionResult> results = new HashMap<>();
        for (Currency currency : currencyService.getCurrencies()) {
            if (virtualService.accountExists(name, currency, cause)) {
                if (virtualService.setBalance(name, getDefaultBalance(currency), currency, cause)) {
                    results.put(currency, resultAndEvent(this, getBalance(currency), currency, ResultType.SUCCESS, TransactionTypes.WITHDRAW, cause));
                } else {
                    results.put(currency, resultAndEvent(this, getBalance(currency), currency, ResultType.FAILED, TransactionTypes.WITHDRAW, cause));
                }
            }
        }
        return results;
    }

    @Override
    public TransactionResult resetBalance(Currency currency, Cause cause, Set<Context> contexts) {
        if (virtualService.setBalance(name, getDefaultBalance(currency), currency, cause)) {
            return resultAndEvent(this, BigDecimal.ZERO, currency, ResultType.SUCCESS, TransactionTypes.WITHDRAW, cause);
        } else {
            return resultAndEvent(this, BigDecimal.ZERO, currency, ResultType.FAILED, TransactionTypes.WITHDRAW, cause);
        }
    }

    @Override
    public TransactionResult deposit(Currency currency, BigDecimal amount, Cause cause, Set<Context> contexts) {
        BigDecimal newBal = getBalance(currency).add(amount);
        // Check if the new balance is in bounds
        if (newBal.compareTo(BigDecimal.ZERO) == -1 || newBal.compareTo(BigDecimal.valueOf(999999999)) == 1) {
            return resultAndEvent(this, amount, currency, ResultType.ACCOUNT_NO_SPACE, TransactionTypes.DEPOSIT, cause);
        }
        if (virtualService.deposit(name, amount, currency, cause)) {
            return resultAndEvent(this, amount, currency, ResultType.SUCCESS, TransactionTypes.DEPOSIT, cause);
        } else {
            return resultAndEvent(this, amount, currency, ResultType.FAILED, TransactionTypes.DEPOSIT, cause);
        }
    }

    @Override
    public TransactionResult withdraw(Currency currency, BigDecimal amount, Cause cause, Set<Context> contexts) {
        BigDecimal newBal = getBalance(currency).subtract(amount);
        // Check if the new balance is in bounds
        if (newBal.compareTo(BigDecimal.ZERO) == -1) {
            return resultAndEvent(this, amount, currency, ResultType.ACCOUNT_NO_FUNDS, TransactionTypes.WITHDRAW, cause);
        }
        if (newBal.compareTo(BigDecimal.valueOf(999999999)) == 1) {
            return resultAndEvent(this, amount, currency, ResultType.ACCOUNT_NO_SPACE, TransactionTypes.WITHDRAW, cause);
        }
        if (virtualService.withdraw(name, amount, currency, cause)) {
            return resultAndEvent(this, amount, currency, ResultType.SUCCESS, TransactionTypes.WITHDRAW, cause);
        } else {
            return resultAndEvent(this, amount, currency, ResultType.FAILED, TransactionTypes.WITHDRAW, cause);
        }
    }

    @Override
    public TransferResult transfer(Account to, Currency currency, BigDecimal amount, Cause cause, Set<Context> contexts) {
        BigDecimal newBal = to.getBalance(currency).add(amount);
        // Check if the new balance is in bounds
        if (newBal.compareTo(BigDecimal.ZERO) == -1 || newBal.compareTo(BigDecimal.valueOf(999999999)) == 1) {
            return resultAndEvent(this, amount, currency, ResultType.ACCOUNT_NO_SPACE, to, cause);
        }
        // Check if the account has enough funds
        if (amount.compareTo(getBalance(currency)) == 1) {
            return resultAndEvent(this, amount, currency, ResultType.ACCOUNT_NO_FUNDS, to, cause);
        }
        if (withdraw(currency, amount, cause).getResult().equals(ResultType.SUCCESS)
                && to.deposit(currency, amount, cause).getResult().equals(ResultType.SUCCESS)) {
            return resultAndEvent(this, amount, currency, ResultType.SUCCESS, to, cause);
        } else {
            return resultAndEvent(this, amount, currency, ResultType.FAILED, to, cause);
        }
    }

    @Override
    public String getIdentifier() {
        return name;
    }

    @Override
    public Set<Context> getActiveContexts() {
        return new HashSet<Context>();
    }

    private TransactionResult resultAndEvent(Account account, BigDecimal amount, Currency currency, ResultType resultType,
            TransactionType transactionType, Cause cause) {
        TransactionResult result = new LiteTransactionResult(account, amount, currency, resultType, transactionType);
        Sponge.getEventManager().post(new LiteEconomyTransactionEvent(result, cause));
        return result;
    }

    private TransferResult resultAndEvent(Account account, BigDecimal amount, Currency currency, ResultType resultType, Account toWho, Cause cause) {
        TransferResult result = new LiteTransferResult(account, amount, currency, resultType, toWho);
        Sponge.getEventManager().post(new LiteEconomyTransactionEvent(result, cause));
        return result;
    }

}
