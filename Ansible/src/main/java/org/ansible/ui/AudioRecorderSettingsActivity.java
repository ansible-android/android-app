package org.ansible.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.ansible.messenger.LocaleController;
import org.ansible.messenger.MediaController;
import org.ansible.messenger.R;
import org.ansible.ui.ActionBar.ActionBar;
import org.ansible.ui.ActionBar.AlertDialog;
import org.ansible.ui.ActionBar.BaseFragment;
import org.ansible.ui.ActionBar.Theme;
import org.ansible.ui.ActionBar.ThemeBind;
import org.ansible.ui.Cells.HeaderCell;
import org.ansible.ui.Cells.TextCheckCell;
import org.ansible.ui.Cells.TextInfoPrivacyCell;
import org.ansible.ui.Cells.TextSettingsCell;
import org.ansible.ui.Components.LayoutHelper;
import org.ansible.ui.Components.RecyclerListView;
import org.ansible.utils.recorder.audio.AudioRecordingSession;
import org.ansible.utils.settings.SharedSettings;

/** Configures the second-generation voice-message recorder. */
public class AudioRecorderSettingsActivity extends BaseFragment {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_CHECK = 1;
    private static final int TYPE_VALUE = 2;
    private static final int TYPE_INFO = 3;

    private static final int ROW_GENERAL_HEADER = 0;
    private static final int ROW_ENABLED = 1;
    private static final int ROW_NOISE_SUPPRESSION = 2;
    private static final int ROW_GENERAL_INFO = 3;
    private static final int ROW_COUNT = 4;

    private RecyclerListView listView;
    private ListAdapter adapter;

    @Override
    protected boolean useSimpleActionBar() {
        return true;
    }

    @Override
    public View createView(Context context) {
        setHasOwnBackground(true);
        simpleActionBar.setBackButtonImage(R.drawable.ic_ab_back);
        simpleActionBar.setTitle(LocaleController.getString(R.string.AudioRecorderSettings));
        simpleActionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout frameLayout = new FrameLayout(context);
        fragmentView = frameLayout;

        listView = new RecyclerListView(context);
        listView.setSectionsDrawBackground(true);
        listView.setSections();
        listView.setLayoutManager(new LinearLayoutManager(context));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(adapter = new ListAdapter(context));
        listView.setOnItemClickListener((view, position) -> onRowClicked(position));
        frameLayout.addView(listView, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT,
                LayoutHelper.MATCH_PARENT
        ));
        return fragmentView;
    }

    private void onRowClicked(int position) {
        if (position == ROW_ENABLED) {
            SharedSettings.audioRecorderV2Enabled.set(!MediaController.shouldUseAudioRecorderV2(currentAccount));
            adapter.notifyDataSetChanged();
            return;
        }
        if (!MediaController.shouldUseAudioRecorderV2(currentAccount)) {
            return;
        }
        if (position == ROW_NOISE_SUPPRESSION) {
            showNoiseSuppressionChoice();
        }
    }

    private void showNoiseSuppressionChoice() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.AudioRecorderNoiseSuppression));
        builder.setItems(new CharSequence[]{
                LocaleController.getString(R.string.AudioRecorderNoiseSuppressionOff),
                LocaleController.getString(R.string.AudioRecorderNoiseSuppressionRNNoise)
        }, (dialog, index) -> {
            SharedSettings.audioRecorderNoiseSuppression.set(index == 0
                    ? AudioRecordingSession.NoiseSuppression.DISABLED
                    : AudioRecordingSession.NoiseSuppression.RNNOISE);
            adapter.notifyItemChanged(ROW_NOISE_SUPPRESSION);
        });
        showDialog(builder.create());
    }

    private static String noiseSuppressionLabel(
            AudioRecordingSession.NoiseSuppression noiseSuppression
    ) {
        return LocaleController.getString(noiseSuppression
                == AudioRecordingSession.NoiseSuppression.RNNOISE
                ? R.string.AudioRecorderNoiseSuppressionRNNoise
                : R.string.AudioRecorderNoiseSuppressionOff);
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context context;

        private ListAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getItemCount() {
            return ROW_COUNT;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            if (position == ROW_ENABLED) {
                return true;
            }
            return MediaController.shouldUseAudioRecorderV2(currentAccount)
                    && position == ROW_NOISE_SUPPRESSION;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == TYPE_HEADER) {
                view = new HeaderCell(context);
            } else if (viewType == TYPE_CHECK) {
                view = new TextCheckCell(context);
            } else if (viewType == TYPE_VALUE) {
                TextSettingsCell cell = new TextSettingsCell(context);
                cell.setCanDisable(true);
                view = cell;
            } else {
                view = new TextInfoPrivacyCell(context);
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
            ));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            boolean enabled = MediaController.shouldUseAudioRecorderV2(currentAccount);
            if (holder.getItemViewType() == TYPE_HEADER) {
                ((HeaderCell) holder.itemView).setText(
                        LocaleController.getString(R.string.AudioRecorderGeneral)
                );
            } else if (holder.getItemViewType() == TYPE_CHECK) {
                TextCheckCell cell = (TextCheckCell) holder.itemView;
                cell.setTextAndCheck(
                        LocaleController.getString(R.string.AudioRecorderUseNewRecorder),
                        enabled,
                        false
                );
            } else if (holder.getItemViewType() == TYPE_VALUE) {
                TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                cell.setEnabled(enabled);
                cell.setTextAndValue(
                        LocaleController.getString(R.string.AudioRecorderNoiseSuppression),
                        noiseSuppressionLabel(
                                SharedSettings.audioRecorderNoiseSuppression.get()
                        ),
                        false
                );
            } else {
                ((TextInfoPrivacyCell) holder.itemView).setText(
                        LocaleController.getString(R.string.AudioRecorderGeneralInfo)
                );
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == ROW_GENERAL_HEADER) {
                return TYPE_HEADER;
            }
            if (position == ROW_ENABLED) {
                return TYPE_CHECK;
            }
            if (position == ROW_NOISE_SUPPRESSION) {
                return TYPE_VALUE;
            }
            return TYPE_INFO;
        }
    }

    @Override
    protected RecyclerListView getListViewForSimpleGlass() {
        return listView;
    }

    @Override
    public boolean isSupportEdgeToEdge() {
        return true;
    }
}
