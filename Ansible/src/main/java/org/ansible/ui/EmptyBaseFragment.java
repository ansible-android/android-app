package org.ansible.ui;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import org.ansible.ui.ActionBar.BaseFragment;
import org.ansible.ui.Components.SizeNotifierFrameLayout;

public class EmptyBaseFragment extends BaseFragment {

    @Override
    public View createView(Context context) {
        return fragmentView = new SizeNotifierFrameLayout(context);
    }

}
