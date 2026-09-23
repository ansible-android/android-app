package org.ansible.ui.Components.poll.sheets;

import static org.ansible.messenger.AndroidUtilities.dp;
import static org.ansible.messenger.LocaleController.getString;

import android.content.Context;

import org.ansible.messenger.AndroidUtilities;
import org.ansible.messenger.LocaleController;
import org.ansible.messenger.MessagesController;
import org.ansible.messenger.R;
import org.ansible.messenger.Utilities;
import org.ansible.tgnet.ConnectionsManager;
import org.ansible.tgnet.tl.TL_stats;
import org.ansible.ui.ActionBar.ActionBarMenu;
import org.ansible.ui.ActionBar.Theme;
import org.ansible.ui.Components.BottomSheetWithRecyclerListView;
import org.ansible.ui.Components.RecyclerListView;
import org.ansible.ui.Components.UItem;
import org.ansible.ui.Components.UniversalAdapter;
import org.ansible.ui.StatisticActivity;

import java.util.ArrayList;

public class PollStatisticsBottomSheet extends BottomSheetWithRecyclerListView {

    private UniversalAdapter adapter;
    private final StatisticActivity.ChartViewData chartViewData;

    public PollStatisticsBottomSheet(Context context, Theme.ResourcesProvider resourcesProvider, TL_stats.TL_statsPollStats stats) {
        super(context, null, true, false, false, false, ActionBarType.SLIDING, resourcesProvider);
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray, resourcesProvider));
        occupyNavigationBar = true;
        drawNavigationBar = false;
        ignoreTouchActionBar = false;
        headerMoveTop = dp(12);

        chartViewData = StatisticActivity.createViewData(stats.votes_graph, getString(R.string.PollV2StatsVoteTimeline), 2);

        recyclerListView.setPadding(backgroundPaddingLeft, 0, backgroundPaddingLeft, AndroidUtilities.navigationBarHeight);
        recyclerListView.setClipToPadding(false);
        recyclerListView.setSections(true);

        ActionBarMenu m = actionBar.createMenu();
        m.addItem(-1, R.drawable.ic_close_white);
        m.setTranslationX(-dp(5));

        adapter.update(false);
    }

    public static int loadStatistics(int currentAccount, long dialogId, int messageId, Utilities.Callback<TL_stats.TL_statsPollStats> onResult) {
        final TL_stats.TL_statsGetPollStats req = new TL_stats.TL_statsGetPollStats();
        req.peer = MessagesController.getInstance(currentAccount).getInputPeer(dialogId);
        req.msg_id = messageId;
        return ConnectionsManager.getInstance(currentAccount).sendRequestTyped(req, AndroidUtilities::runOnUIThread, (res, err) -> {
            onResult.run(res);
        });
    }

    @Override
    protected CharSequence getTitle() {
        return LocaleController.getString(R.string.PollV2StatsPollStats);
    }

    @Override
    protected RecyclerListView.SelectionAdapter createAdapter(RecyclerListView listView) {
        adapter = new UniversalAdapter(listView, getContext(), currentAccount, 0, true, this::fillItems, resourcesProvider);
        adapter.setApplyBackground(false);
        return adapter;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (chartViewData != null) {
            items.add(UItem.asSpace(dp(12)));
            items.add(UItem.asChart(StatisticActivity.VIEW_TYPE_LINEAR, 0, chartViewData));
        }
    }
}
