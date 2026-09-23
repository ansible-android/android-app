package org.ansible.messenger;

import org.ansible.messenger.regular.BuildConfig;

public class ApplicationLoaderImpl extends ApplicationLoader {
    @Override
    protected String onGetApplicationId() {
        return BuildConfig.APPLICATION_ID;
    }
}
