package org.ansible.ui.Components.Premium.boosts;

import android.app.Activity;

import org.ansible.ui.ActionBar.BaseFragment;
import org.ansible.ui.ActionBar.Theme;
import org.ansible.ui.Stories.DarkThemeResourceProvider;
import org.ansible.ui.WrappedResourceProvider;

public class DarkFragmentWrapper extends BaseFragment {

    private final BaseFragment parentFragment;

    DarkFragmentWrapper(BaseFragment parentFragment) {
        this.parentFragment = parentFragment;
    }

    @Override
    public boolean isLightStatusBar() {
        return false;
    }

    @Override
    public Activity getParentActivity() {
        return parentFragment.getParentActivity();
    }

    @Override
    public Theme.ResourcesProvider getResourceProvider() {
        return new WrappedResourceProvider(new DarkThemeResourceProvider());
    }

    @Override
    public boolean presentFragment(BaseFragment fragment) {
        return false;
    }
}
