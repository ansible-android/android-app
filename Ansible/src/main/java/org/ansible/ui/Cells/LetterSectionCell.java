/*
 * This is the source code of Ansible for Android.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.ansible.ui.Cells;

import static org.ansible.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.ansible.messenger.AndroidUtilities;
import org.ansible.ui.ActionBar.Theme;
import org.ansible.ui.Components.LayoutHelper;

public class LetterSectionCell extends FrameLayout {

    private TextView textView;

    public LetterSectionCell(Context context) {
        super(context);
        setLayoutParams(new ViewGroup.LayoutParams(dp(12 + 64), dp(64)));

        textView = new TextView(getContext());
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        textView.setTypeface(AndroidUtilities.bold());
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4));
        textView.setGravity(Gravity.CENTER);
        addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL, 12, 0, 0, 0));
    }

    public void setLetter(String letter) {
        textView.setText(letter.toUpperCase());
    }

    public void setCellHeight(int height) {
        setLayoutParams(new ViewGroup.LayoutParams(dp(54), height));
    }
}
