package org.ansible.ui;

import android.view.View;

import org.ansible.messenger.utils.ViewOutlineProviderImpl;

public class BadWayToMakeButtonRound {
    public static void round(View view) {
        view.setOutlineProvider(ViewOutlineProviderImpl.BOUNDS_ROUND_RECT);
        view.setClipToOutline(true);
    }
}
