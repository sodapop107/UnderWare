package meteorclient.gui.themes.meteor.widgets;

import meteorclient.gui.WidgetScreen;
import meteorclient.gui.themes.meteor.MeteorWidget;
import meteorclient.gui.widgets.WAccount;
import meteorclient.systems.accounts.Account;
import meteorclient.utils.render.color.Color;

public class WMeteorAccount extends WAccount implements MeteorWidget {
    public WMeteorAccount(WidgetScreen screen, Account<?> account) {
        super(screen, account);
    }

    @Override
    protected Color loggedInColor() {
        return theme().loggedInColor.get();
    }

    @Override
    protected Color accountTypeColor() {
        return theme().textSecondaryColor.get();
    }
}
