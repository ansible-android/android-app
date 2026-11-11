package org.ansible.ui;

import static org.ansible.messenger.LocaleController.getString;

import android.content.Context;
import android.view.View;

import org.ansible.messenger.R;
import org.ansible.ui.ActionBar.SimpleActionBarDoneButton;
import org.ansible.ui.Components.RecyclerListView;
import org.ansible.ui.Components.UItem;
import org.ansible.ui.Components.UniversalAdapter;
import org.ansible.ui.Components.UniversalFragment;
import org.ansible.utils.settings.SharedSettings;

import java.util.ArrayList;

/** Experimental settings of the simple header: the look of the done button, previewed in this screen's own header. */
public class HeaderSettingsActivity extends UniversalFragment {
    private static final int ID_PREVIEW_DONE = 1;

    private static final int ID_COLOR_GLASS = 10;
    private static final int ID_COLOR_ACCENT = 11;
    private static final int ID_SHAPE_ICON = 20;
    private static final int ID_SHAPE_TEXT = 21;
    private static final int ID_PREVIEW_CHANGES = 30;

    private boolean previewHasChanges = true;

    @Override
    protected boolean useSimpleActionBar() {
        return true;
    }

    @Override
    public View createView(Context context) {
        setHasOwnBackground(true);
        fragmentView = super.createView(context);
        listView.setSections();
        listView.setSectionsDrawBackground(true);
        createPreviewButton();
        return fragmentView;
    }

    private void createPreviewButton() {
        simpleActionBar.removeDoneButton();
        final SimpleActionBarDoneButton button = simpleActionBar.createDoneButton(ID_PREVIEW_DONE, getString(R.string.Save));
        button.setActive(previewHasChanges, false);
    }

    @Override
    protected RecyclerListView getListViewForSimpleGlass() {
        return listView;
    }

    @Override
    public boolean isSupportEdgeToEdge() {
        return true;
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.HeaderSettings);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        final SimpleActionBarDoneButton.Style style = SharedSettings.headerDoneButtonStyle.get();
        final SimpleActionBarDoneButton.Shape shape = SharedSettings.headerDoneButtonShape.get();

        items.add(UItem.asHeader(getString(R.string.HeaderDoneButtonColor)));
        items.add(UItem.asRadio(ID_COLOR_ACCENT, getString(R.string.HeaderDoneButtonColorAccent)).setChecked(style == SimpleActionBarDoneButton.Style.ACCENT));
        items.add(UItem.asRadio(ID_COLOR_GLASS, getString(R.string.HeaderDoneButtonColorGlass)).setChecked(style == SimpleActionBarDoneButton.Style.GLASS));
        items.add(UItem.asShadow(getString(R.string.HeaderDoneButtonColorInfo)));

        items.add(UItem.asHeader(getString(R.string.HeaderDoneButtonShape)));
        items.add(UItem.asRadio(ID_SHAPE_ICON, getString(R.string.HeaderDoneButtonShapeIcon)).setChecked(shape == SimpleActionBarDoneButton.Shape.ICON));
        items.add(UItem.asRadio(ID_SHAPE_TEXT, getString(R.string.HeaderDoneButtonShapeText)).setChecked(shape == SimpleActionBarDoneButton.Shape.TEXT));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(getString(R.string.HeaderDoneButtonPreview)));
        items.add(UItem.asCheck(ID_PREVIEW_CHANGES, getString(R.string.HeaderDoneButtonPreviewChanges)).setChecked(previewHasChanges));
        items.add(UItem.asShadow(getString(R.string.HeaderDoneButtonPreviewInfo)));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_COLOR_GLASS:
            case ID_COLOR_ACCENT:
                SharedSettings.headerDoneButtonStyle.set(item.id == ID_COLOR_GLASS ? SimpleActionBarDoneButton.Style.GLASS : SimpleActionBarDoneButton.Style.ACCENT);
                createPreviewButton();
                break;
            case ID_SHAPE_ICON:
            case ID_SHAPE_TEXT:
                SharedSettings.headerDoneButtonShape.set(item.id == ID_SHAPE_TEXT ? SimpleActionBarDoneButton.Shape.TEXT : SimpleActionBarDoneButton.Shape.ICON);
                createPreviewButton();
                break;
            case ID_PREVIEW_CHANGES:
                previewHasChanges = !previewHasChanges;
                simpleActionBar.getDoneButton().setActive(previewHasChanges, true);
                break;
            default:
                return;
        }
        listView.adapter.update(true);
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
