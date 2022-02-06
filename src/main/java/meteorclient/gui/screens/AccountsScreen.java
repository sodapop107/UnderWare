package meteorclient.gui.screens;

import meteorclient.gui.GuiTheme;
import meteorclient.gui.WindowScreen;
import meteorclient.gui.widgets.WAccount;
import meteorclient.gui.widgets.containers.WContainer;
import meteorclient.gui.widgets.containers.WHorizontalList;
import meteorclient.gui.widgets.pressable.WButton;
import meteorclient.systems.accounts.Account;
import meteorclient.systems.accounts.Accounts;
import meteorclient.systems.accounts.MicrosoftLogin;
import meteorclient.systems.accounts.types.MicrosoftAccount;
import meteorclient.utils.misc.NbtUtils;
import meteorclient.utils.network.MeteorExecutor;
import org.jetbrains.annotations.Nullable;

import static meteorclient.UnderWare.mc;

public class AccountsScreen extends WindowScreen {
    public AccountsScreen(GuiTheme theme) {
        super(theme, "Accounts");
    }

    @Override
    public void initWidgets() {
        // Accounts
        for (Account<?> account : Accounts.get()) {
            WAccount wAccount = add(theme.account(this, account)).expandX().widget();
            wAccount.refreshScreenAction = this::reload;
        }

        // Add account
        WHorizontalList l = add(theme.horizontalList()).expandX().widget();

        addButton(l, "Cracked", () -> mc.setScreen(new AddCrackedAccountScreen(theme, this)));
        addButton(l, "Premium", () -> mc.setScreen(new AddPremiumAccountScreen(theme, this)));
        addButton(l, "Altening", () -> mc.setScreen(new AddAlteningAccountScreen(theme, this)));
        addButton(l, "Microsoft", () -> {
            locked = true;

            MicrosoftLogin.getRefreshToken(refreshToken -> {
                locked = false;

                if (refreshToken != null) {
                    MicrosoftAccount account = new MicrosoftAccount(refreshToken);
                    addAccount(null, this, account);
                }
            });
        });
    }

    private void addButton(WContainer c, String text, Runnable action) {
        WButton button = c.add(theme.button(text)).expandX().widget();
        button.action = action;
    }

    public static void addAccount(@Nullable AddAccountScreen screen, AccountsScreen parent, Account<?> account) {
        if (screen != null) screen.locked = true;

        MeteorExecutor.execute(() -> {
            if (account.fetchInfo() && account.fetchHead()) {
                Accounts.get().add(account);
                if (account.login()) Accounts.get().save();

                if (screen != null) {
                    screen.locked = false;
                    screen.onClose();
                }

                parent.reload();

                return;
            }

            if (screen != null) screen.locked = false;
        });
    }

    @Override
    public boolean toClipboard() {
        return NbtUtils.toClipboard(Accounts.get());
    }

    @Override
    public boolean fromClipboard() {
        return NbtUtils.fromClipboard(Accounts.get());
    }
}
