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
package io.github.flibio.economylite.modules.loan.command;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.service.economy.Currency;
import io.github.flibio.economylite.EconomyLite;
import io.github.flibio.utils.message.MessageStorage;
import io.github.flibio.economylite.modules.loan.LoanModule;
import io.github.flibio.utils.commands.AsyncCommand;
import io.github.flibio.utils.commands.BaseCommandExecutor;
import io.github.flibio.utils.commands.Command;
import io.github.flibio.utils.commands.ParentCommand;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.command.spec.CommandSpec.Builder;
import org.spongepowered.api.entity.living.player.Player;

import java.util.Locale;
import java.util.Optional;

@AsyncCommand
@ParentCommand(parentCommand = LoanCommand.class)
@Command(aliases = {"balance", "bal"}, permission = "economylite.loan.balance")
public class LoanBalanceCommand extends BaseCommandExecutor<Player> {

    private MessageStorage messages = EconomyLite.getMessageStorage();
    private LoanModule module;

    public LoanBalanceCommand(LoanModule module) {
        this.module = module;
    }

    @Override
    public Builder getCommandSpecBuilder() {
        return CommandSpec.builder()
                .executor(this);
    }

    @Override
    public void run(Player src, CommandContext args) {
        Optional<Double> bOpt = module.getLoanManager().getLoanBalance(src.getUniqueId());
        if (bOpt.isPresent()) {
            Currency cur = EconomyLite.getEconomyService().getDefaultCurrency();
            src.sendMessage(messages.getMessage("module.loan.balance", "balance", Text.of(String.format(Locale.ENGLISH, "%,.2f", bOpt.get())),
                    "label", getPrefix(bOpt.get(), cur)));
        } else {
            src.sendMessage(messages.getMessage("command.error"));
        }
    }

    private Text getPrefix(double amnt, Currency cur) {
        Text label = cur.getPluralDisplayName();
        if (amnt == 1.0) {
            label = cur.getDisplayName();
        }
        return label;
    }
}
