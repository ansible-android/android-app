package org.ansible.ui.Diamonds;

import static org.ansible.messenger.AndroidUtilities.dp;
import static org.ansible.messenger.LocaleController.formatPluralStringComma;
import static org.ansible.messenger.LocaleController.getString;
import static org.ansible.ui.ChatEditActivity.applyNewSpan;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.ansible.messenger.AndroidUtilities;
import org.ansible.messenger.BillingController;
import org.ansible.messenger.FileLog;
import org.ansible.messenger.LocaleController;
import org.ansible.messenger.MessagesController;
import org.ansible.messenger.NotificationCenter;
import org.ansible.messenger.R;
import org.ansible.messenger.UserConfig;
import org.ansible.messenger.UserObject;
import org.ansible.messenger.browser.Browser;
import org.ansible.asnet.ConnectionsManager;
import org.ansible.asnet.TLRPC;
import org.ansible.asnet.tl.TL_account;
import org.ansible.asnet.tl.TL_diamonds;
import org.ansible.ui.ActionBar.ActionBar;
import org.ansible.ui.ActionBar.AlertDialog;
import org.ansible.ui.ActionBar.BackDrawable;
import org.ansible.ui.ActionBar.BaseFragment;
import org.ansible.ui.ActionBar.Theme;
import org.ansible.ui.ChannelMonetizationLayout;
import org.ansible.ui.Components.AnimatedTextView;
import org.ansible.ui.Components.Bulletin;
import org.ansible.ui.Components.BulletinFactory;
import org.ansible.ui.Components.ChatAvatarContainer;
import org.ansible.ui.Components.ColoredImageSpan;
import org.ansible.ui.Components.EditTextBoldCursor;
import org.ansible.ui.Components.LayoutHelper;
import org.ansible.ui.Components.OutlineTextContainerView;
import org.ansible.ui.Components.RecyclerListView;
import org.ansible.ui.Components.SizeNotifierFrameLayout;
import org.ansible.ui.Components.UItem;
import org.ansible.ui.Components.UniversalAdapter;
import org.ansible.ui.Components.UniversalRecyclerView;
import org.ansible.ui.StatisticActivity;
import org.ansible.ui.Stories.recorder.ButtonWithCounterView;
import org.ansible.ui.TwoStepVerificationActivity;
import org.ansible.ui.TwoStepVerificationSetupActivity;
import org.ansible.ui.bots.AffiliateProgramFragment;
import org.ansible.ui.bots.ChannelAffiliateProgramsFragment;

import java.util.ArrayList;
import java.util.Locale;

public class BotDiamondsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    public static final int TYPE_STARS = 0;

    public final int type;
    public final long bot_id;
    public final boolean self;

    private ChatAvatarContainer avatarContainer;
    private UniversalRecyclerView listView;

    private TLRPC.TL_payments_starsRevenueStats lastStats;
    private TLRPC.TL_starsRevenueStatus lastStatsStatus;

    private StatisticActivity.ChartViewData revenueChartData;
    private final ChannelMonetizationLayout.ProceedOverview availableValue = ChannelMonetizationLayout.ProceedOverview.as("XTR", getString(R.string.BotDiamondsOverviewAvailableBalance));
    private final ChannelMonetizationLayout.ProceedOverview totalValue = ChannelMonetizationLayout.ProceedOverview.as("XTR", getString(R.string.BotDiamondsOverviewTotalBalance));
    private final ChannelMonetizationLayout.ProceedOverview totalProceedsValue =     ChannelMonetizationLayout.ProceedOverview.as("XTR", getString(R.string.BotDiamondsOverviewTotalProceeds));

    private final CharSequence withdrawInfo;

    private DiamondsIntroActivity.DiamondsTransactionsLayout transactionsLayout;

    private int balanceBlockedUntil;
    private LinearLayout balanceLayout;
    private LinearLayout balanceButtonsLayout;
    private RelativeSizeSpan balanceTitleSizeSpan;
    private AnimatedTextView balanceTitle;
    private AnimatedTextView balanceSubtitle;
    private OutlineTextContainerView balanceEditTextContainer;
    private boolean balanceEditTextIgnore = false;
    private boolean balanceEditTextAll = true;
    private long balanceEditTextValue;
    private EditTextBoldCursor balanceEditText;
    private ButtonWithCounterView balanceButton, adsButton;
    private ColoredImageSpan[] diamondRef = new ColoredImageSpan[1];
    private int shakeDp = 4;

    private double rate;

    public BotDiamondsActivity(int type, long botId) {
        this.type = type;
        this.bot_id = botId;
        this.self = botId == getUserConfig().getClientUserId();

        if (type == TYPE_STARS) {
            BotDiamondsController.getInstance(currentAccount).preloadDiamondsStats(bot_id);
            if (!self) {
                BotDiamondsController.getInstance(currentAccount).invalidateTransactions(bot_id, true);
            }
        }

        withdrawInfo = AndroidUtilities.replaceArrows(AndroidUtilities.replaceSingleTag(self ? formatPluralStringComma("SelfDiamondsWithdrawInfo", (int) getMessagesController().diamondsRevenueWithdrawalMin) : getString(R.string.BotDiamondsWithdrawInfo), () -> {
            Browser.openUrl(getContext(), getString(R.string.BotDiamondsWithdrawInfoLink));
        }), true);
    }

    @Override
    public View createView(Context context) {

        NestedFrameLayout frameLayout = new NestedFrameLayout(context);

        avatarContainer = new ChatAvatarContainer(context, null, false);
        avatarContainer.setOccupyStatusBar(!AndroidUtilities.isTablet());
        avatarContainer.getAvatarImageView().setScaleX(0.9f);
        avatarContainer.getAvatarImageView().setScaleY(0.9f);
        avatarContainer.setRightAvatarPadding(-dp(3));
        actionBar.addView(avatarContainer, 0, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT, !inPreviewMode ? 50 : 0, 0, 40, 0));

        TLRPC.User bot = getMessagesController().getUser(bot_id);
        avatarContainer.setUserAvatar(bot, true);
        avatarContainer.setTitle(UserObject.getUserName(bot));
        avatarContainer.setSubtitle(LocaleController.getString(R.string.BotStatsDiamonds));

        actionBar.setBackButtonDrawable(new BackDrawable(false));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(final int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });
        avatarContainer.setTitleColors(Theme.getColor(Theme.key_player_actionBarTitle), Theme.getColor(Theme.key_player_actionBarSubtitle));
        actionBar.setItemsColor(Theme.getColor(Theme.key_player_actionBarTitle), false);
        actionBar.setItemsColor(Theme.getColor(Theme.key_player_actionBarTitle), true);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarActionModeDefaultSelector), false);
        actionBar.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        transactionsLayout = new DiamondsIntroActivity.DiamondsTransactionsLayout(context, currentAccount, bot_id, getClassGuid(), getResourceProvider());

        balanceLayout = new LinearLayout(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(
                    MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                    heightMeasureSpec
                );
            }
        };
        balanceLayout.setOrientation(LinearLayout.VERTICAL);
        balanceLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite, getResourceProvider()));
        balanceLayout.setPadding(0, 0, 0, dp(17));

        balanceTitle = new AnimatedTextView(context, false, true, true);
        balanceTitle.setTypeface(AndroidUtilities.bold());
        balanceTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        balanceTitle.setTextSize(dp(32));
        balanceTitle.setGravity(Gravity.CENTER);
        balanceTitleSizeSpan = new RelativeSizeSpan(65f / 96f);
        balanceLayout.addView(balanceTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 38, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 22, 15, 22, 0));

        balanceSubtitle = new AnimatedTextView(context, true, true, true);
        balanceSubtitle.setGravity(Gravity.CENTER);
        balanceSubtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, getResourceProvider()));
        balanceSubtitle.setTextSize(dp(14));
        balanceLayout.addView(balanceSubtitle, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 17, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 22, 4, 22, 0));

        balanceEditTextContainer = new OutlineTextContainerView(context) {
            @Override
            public boolean dispatchTouchEvent(MotionEvent event) {
                if (balanceEditText != null && !balanceEditText.isFocusable()) {
                    balanceEditText.setFocusable(true);
                    balanceEditText.setFocusableInTouchMode(true);
                    int position = listView.findPositionByItemId(BALANCE);
                    if (position >= 0 && position < listView.adapter.getItemCount()) {
                        listView.stopScroll();
                        listView.smoothScrollToPosition(position);
                    }
                    balanceEditText.requestFocus();
                }
                return super.dispatchTouchEvent(event);
            }
        };
        balanceEditTextContainer.setText(getString(R.string.BotDiamondsWithdrawPlaceholder));
        balanceEditTextContainer.setLeftPadding(dp(14 + 22));
        balanceEditText = new EditTextBoldCursor(context) {
            @Override
            protected void onDetachedFromWindow() {
                super.onDetachedFromWindow();
                AndroidUtilities.hideKeyboard(this);
            }
        };
        balanceEditText.setFocusable(false);
        balanceEditText.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        balanceEditText.setCursorSize(AndroidUtilities.dp(20));
        balanceEditText.setCursorWidth(1.5f);
        balanceEditText.setBackground(null);
        balanceEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        balanceEditText.setMaxLines(1);
        int padding = AndroidUtilities.dp(16);
        balanceEditText.setPadding(dp(6), padding, padding, padding);
        balanceEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        balanceEditText.setTypeface(Typeface.DEFAULT);
        balanceEditText.setHighlightColor(getThemedColor(Theme.key_chat_inTextSelectionHighlight));
        balanceEditText.setHandlesColor(getThemedColor(Theme.key_chat_TextSelectionCursor));
        balanceEditText.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        balanceEditText.setOnFocusChangeListener((v, hasFocus) -> balanceEditTextContainer.animateSelection(hasFocus ? 1f : 0f));
        balanceEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                long balance = BotDiamondsController.getInstance(currentAccount).getAvailableBalance(bot_id);
                balanceEditTextValue = TextUtils.isEmpty(s) ? 0 : Long.parseLong(s.toString());
                if (balanceEditTextValue > balance) {
                    balanceEditTextValue = balance;
                    balanceEditTextIgnore = true;
                    balanceEditText.setText(Long.toString(balanceEditTextValue));
                    balanceEditText.setSelection(balanceEditText.getText().length());
                    balanceEditTextIgnore = false;
                }
                balanceEditTextAll = balanceEditTextValue == balance;
                AndroidUtilities.cancelRunOnUIThread(setBalanceButtonText);
                setBalanceButtonText.run();
                if (balanceEditTextIgnore) return;
                balanceEditTextAll = false;
            }
        });
        LinearLayout balanceEditTextLayout = new LinearLayout(context);
        balanceEditTextLayout.setOrientation(LinearLayout.HORIZONTAL);
        ImageView diamondImage = new ImageView(context);
        diamondImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        diamondImage.setImageResource(R.drawable.diamond);
        balanceEditTextLayout.addView(diamondImage, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, Gravity.LEFT | Gravity.CENTER_VERTICAL, 14, 0, 0, 0));
        balanceEditTextLayout.addView(balanceEditText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 1, Gravity.FILL));
        balanceEditTextContainer.attachEditText(balanceEditText);
        balanceEditTextContainer.addView(balanceEditTextLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));
        balanceEditText.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_NEXT) {
                withdraw();
                return true;
            }
            return false;
        });
        balanceLayout.addView(balanceEditTextContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 18, 14, 18, 2));
        balanceEditTextContainer.setVisibility(View.GONE);

        balanceButtonsLayout = new LinearLayout(context);
        balanceButtonsLayout.setOrientation(LinearLayout.HORIZONTAL);

        balanceButton = new ButtonWithCounterView(context, getResourceProvider()) {
            @Override
            protected boolean subTextSplitToWords() {
                return false;
            }
        }.setRound();
        balanceButton.setEnabled(MessagesController.getInstance(currentAccount).channelRevenueWithdrawalEnabled);
        balanceButton.setText(getString(R.string.BotDiamondsButtonWithdrawShortAll), false);
        balanceButton.setOnClickListener(v -> {
            withdraw();
        });

        adsButton = new ButtonWithCounterView(context, getResourceProvider()).setRound();
        adsButton.setEnabled(true);
        adsButton.setText(getString(R.string.MonetizationDiamondsAds), false);
        adsButton.setOnClickListener(v -> {
            if (!v.isEnabled() || adsButton.isLoading()) return;

            adsButton.setLoading(true);
            TLRPC.TL_payments_getDiamondsRevenueAdsAccountUrl req = new TLRPC.TL_payments_getDiamondsRevenueAdsAccountUrl();
            req.peer = MessagesController.getInstance(currentAccount).getInputPeer(bot_id);
            ConnectionsManager.getInstance(currentAccount).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                if (res instanceof TLRPC.TL_payments_starsRevenueAdsAccountUrl) {
                    Browser.openUrl(context, ((TLRPC.TL_payments_starsRevenueAdsAccountUrl) res).url);
                }
                AndroidUtilities.runOnUIThread(() -> {
                    adsButton.setLoading(false);
                }, 1000);
            }));
        });

        balanceButtonsLayout.addView(balanceButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 1, Gravity.FILL));
        if (!self) {
            balanceButtonsLayout.addView(new Space(context), LayoutHelper.createLinear(8, 48, 0, Gravity.FILL));
            balanceButtonsLayout.addView(adsButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 1, Gravity.FILL));
        }
        balanceLayout.addView(balanceButtonsLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.TOP | Gravity.FILL_HORIZONTAL, 18, 13, 18, 0));

        listView = new UniversalRecyclerView(this, this::fillItems, this::onItemClick, this::onItemLongClick);
        listView.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));
        listView.setSections();
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        actionBar.setAdaptiveBackground(listView);

        return fragmentView = frameLayout;
    }

    private Bulletin withdrawalBulletin;
    private void withdraw() {
        if (!balanceButton.isEnabled() || balanceButton.isLoading()) {
            return;
        }

        final int now = getConnectionsManager().getCurrentTime();
        if (balanceBlockedUntil > now) {
            withdrawalBulletin = BulletinFactory.of(this).createSimpleBulletin(R.raw.timer_3, AndroidUtilities.replaceTags(LocaleController.formatString(R.string.BotDiamondsWithdrawalToast, untilString(balanceBlockedUntil - now)))).show();
            return;
        }

        if (balanceEditTextValue < getMessagesController().diamondsRevenueWithdrawalMin) {
            Drawable diamondDrawable = getContext().getResources().getDrawable(R.drawable.diamond).mutate();
            BulletinFactory.of(this).createSimpleBulletin(diamondDrawable, AndroidUtilities.replaceSingleTag(LocaleController.formatPluralString("BotDiamondsWithdrawMinLimit", (int) getMessagesController().diamondsRevenueWithdrawalMin), () -> {
                Bulletin.hideVisible();
                long balance = BotDiamondsController.getInstance(currentAccount).getAvailableBalance(bot_id);
                if (balance < getMessagesController().diamondsRevenueWithdrawalMin) {
                    balanceEditTextAll = true;
                    balanceEditTextValue = balance;
                } else {
                    balanceEditTextAll = false;
                    balanceEditTextValue = getMessagesController().diamondsRevenueWithdrawalMin;
                }
                balanceEditTextIgnore = true;
                balanceEditText.setText(Long.toString(balanceEditTextValue));
                balanceEditText.setSelection(balanceEditText.getText().length());
                balanceEditTextIgnore = false;

                AndroidUtilities.cancelRunOnUIThread(setBalanceButtonText);
                setBalanceButtonText.run();
            })).show();
            return;
        }

        final long stars = balanceEditTextValue;
        TwoStepVerificationActivity passwordFragment = new TwoStepVerificationActivity();
        passwordFragment.setDelegate(1, password -> initWithdraw(stars, password, passwordFragment));
        balanceButton.setLoading(true);
        passwordFragment.preload(() -> {
            balanceButton.setLoading(false);
            presentFragment(passwordFragment);;
        });
    }

    private final int BALANCE = 1;
    private final int BUTTON_AFFILIATE = 2;

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        final BotDiamondsController s = BotDiamondsController.getInstance(currentAccount);
        if (type == TYPE_STARS) {
            items.add(UItem.asChart(StatisticActivity.VIEW_TYPE_STACKBAR, stats_dc, revenueChartData));
            items.add(UItem.asShadow(-1, null));
            items.add(UItem.asBlackHeader(getString(R.string.BotDiamondsOverview)));
            TLRPC.TL_payments_starsRevenueStats stats = s.getStarsRevenueStats(bot_id);
            if (stats != null && stats.status != null) {
                availableValue.contains1 = false;
                availableValue.contains2 = true;
                availableValue.crypto_amount2 = stats.status.available_balance;
                availableValue.crypto_currency2 = "XTR";
                availableValue.currency = "USD";
                availableValue.amount2 = (long) (stats.status.available_balance.amount * rate * 100.0);
                totalValue.contains1 = false;
                totalValue.contains2 = true;
                totalValue.crypto_amount2 = stats.status.current_balance;
                totalValue.crypto_currency2 = "XTR";
                totalValue.amount2 = (long) (stats.status.current_balance.amount * rate * 100.0);
                totalValue.currency = "USD";
                totalProceedsValue.contains1 = false;
                totalProceedsValue.contains2 = true;
                totalProceedsValue.crypto_amount2 = stats.status.overall_revenue;
                totalProceedsValue.crypto_currency2 = "XTR";
                totalProceedsValue.amount2 = (long) (stats.status.overall_revenue.amount * rate * 100.0);
                totalProceedsValue.currency = "USD";
                setDiamondsBalance(stats.status.available_balance, stats.status.next_withdrawal_at);

                balanceButtonsLayout.setVisibility(stats.status.withdrawal_enabled ? View.VISIBLE : View.GONE);
            }
            items.add(UItem.asProceedOverview(availableValue));
            items.add(UItem.asProceedOverview(totalValue));
            items.add(UItem.asProceedOverview(totalProceedsValue));
            items.add(UItem.asShadow(-2, getString(self ? R.string.SelfDiamondsOverviewInfo : R.string.BotDiamondsOverviewInfo)));
            items.add(UItem.asBlackHeader(getString(R.string.BotDiamondsAvailableBalance)));
            items.add(UItem.asCustom(BALANCE, balanceLayout));
            items.add(UItem.asShadow(-3, withdrawInfo));
            if (!self) {
                if (getMessagesController().starrefConnectAllowed) {
                    items.add(AffiliateProgramFragment.ColorfulTextCell.Factory.as(BUTTON_AFFILIATE, Theme.getColor(Theme.key_color_green, resourceProvider), R.drawable.filled_earn_stars, applyNewSpan(getString(R.string.BotAffiliateProgramRowTitle)), getString(R.string.BotAffiliateProgramRowText)));
                    items.add(UItem.asShadow(-4, null));
                }
                items.add(UItem.asFullscreenCustom(transactionsLayout, 0));
            }
        }
    }

    private void onItemClick(UItem item, View view, int pos, float x, float y) {
        if (item.instanceOf(DiamondsIntroActivity.DiamondsTransactionView.Factory.class)) {
            TL_diamonds.StarsTransaction t = (TL_diamonds.StarsTransaction) item.object;
            DiamondsIntroActivity.showTransactionSheet(getContext(), true, bot_id, currentAccount, t, getResourceProvider());
        } else if (item.id == BUTTON_AFFILIATE) {
            presentFragment(new ChannelAffiliateProgramsFragment(bot_id));
        }
    }

    private void setDiamondsBalance(TL_diamonds.StarsAmount crypto_amount, int blockedUntil) {
        if (balanceTitle == null || balanceSubtitle == null)
            return;
        long amount = (long) (rate * crypto_amount.amount * 100.0);
        SpannableStringBuilder ssb = new SpannableStringBuilder(DiamondsIntroActivity.replaceDiamondsWithPlain(TextUtils.concat("XTR ", DiamondsIntroActivity.formatDiamondsAmount(crypto_amount, 0.8f, ' ')), 1f));
        int index = TextUtils.indexOf(ssb, ".");
        if (index >= 0) {
            ssb.setSpan(balanceTitleSizeSpan, index, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        balanceTitle.setText(ssb);
        balanceSubtitle.setText("≈" + BillingController.getInstance().formatCurrency(amount, "USD"));
        balanceEditTextContainer.setVisibility(amount > 0 ? View.VISIBLE : View.GONE);
        if (balanceEditTextAll) {
            balanceEditTextIgnore = true;
            balanceEditText.setText(Long.toString(balanceEditTextValue = crypto_amount.amount));
            balanceEditText.setSelection(balanceEditText.getText().length());
            balanceEditTextIgnore = false;

            balanceButton.setEnabled(balanceEditTextValue > 0);
        }
        balanceBlockedUntil = blockedUntil;

        AndroidUtilities.cancelRunOnUIThread(setBalanceButtonText);
        setBalanceButtonText.run();
    }

    private SpannableStringBuilder lock;
    private Runnable setBalanceButtonText = () -> {
        final int now = getConnectionsManager().getCurrentTime();
        balanceButton.setEnabled(balanceEditTextValue > 0 || balanceBlockedUntil > now);
        if (now < balanceBlockedUntil) {
            balanceButton.setText(getString(R.string.BotDiamondsButtonWithdrawShortUntil), true);

            if (lock == null) {
                lock = new SpannableStringBuilder("l");
                ColoredImageSpan coloredImageSpan = new ColoredImageSpan(R.drawable.mini_switch_lock);
                coloredImageSpan.setTopOffset(1);
                lock.setSpan(coloredImageSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            SpannableStringBuilder buttonLockedText = new SpannableStringBuilder();
            buttonLockedText.append(lock).append(untilString(balanceBlockedUntil - now));
            balanceButton.setSubText(buttonLockedText, true);

            if (withdrawalBulletin != null && withdrawalBulletin.getLayout() instanceof Bulletin.LottieLayout && withdrawalBulletin.getLayout().isAttachedToWindow()) {
                ((Bulletin.LottieLayout) withdrawalBulletin.getLayout()).textView.setText(AndroidUtilities.replaceTags(LocaleController.formatString(R.string.BotDiamondsWithdrawalToast, untilString(balanceBlockedUntil - now))));
            }

            AndroidUtilities.cancelRunOnUIThread(this.setBalanceButtonText);
            AndroidUtilities.runOnUIThread(this.setBalanceButtonText, 1000);
        } else {
            balanceButton.setSubText(null, true);
            balanceButton.setText(DiamondsIntroActivity.replaceDiamonds(balanceEditTextAll ? getString(R.string.BotDiamondsButtonWithdrawShortAll) : LocaleController.formatPluralStringSpaced("BotDiamondsButtonWithdrawShort", (int) balanceEditTextValue), diamondRef), true);
        }
    };

    public static String untilString(int t) {
        final int d = t / (60 * 60 * 24);
        t -= d * (60 * 60 * 24);
        final int h = t / (60 * 60);
        t -= h * (60 * 60);
        final int m = t / 60;
        t -= m * 60;
        final int s = t;

        if (d == 0) {
            if (h == 0) {
                return String.format(Locale.ENGLISH, "%02d:%02d", m, s);
            }
            return String.format(Locale.ENGLISH, "%02d:%02d:%02d", h, m, s);
        }
        return LocaleController.formatString(R.string.PeriodDHM, String.format(Locale.ENGLISH, "%02d", d), String.format(Locale.ENGLISH, "%02d", h), String.format(Locale.ENGLISH, "%02d", m));
    }

    private boolean onItemLongClick(UItem item, View view, int pos, float x, float y) {
        return false;
    }

    private int stats_dc = -1;
    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.botDiamondsUpdated);
        checkStats();
        return super.onFragmentCreate();
    }

    private void checkStats() {
        TLRPC.TL_payments_starsRevenueStats stats = BotDiamondsController.getInstance(currentAccount).getStarsRevenueStats(bot_id);
        if (stats == lastStats && (stats == null ? null : stats.status) == lastStatsStatus) {
            return;
        }

        lastStats = stats;
        lastStatsStatus = stats == null ? null : stats.status;
        if (stats != null) {
            rate = stats.usd_rate;
            revenueChartData = StatisticActivity.createViewData(stats.revenue_graph, getString(R.string.BotDiamondsChartRevenue), 2);
            if (revenueChartData != null && revenueChartData.chartData != null && revenueChartData.chartData.lines != null && !revenueChartData.chartData.lines.isEmpty() && revenueChartData.chartData.lines.get(0) != null) {
                revenueChartData.showAll = true;
                revenueChartData.chartData.lines.get(0).colorKey = Theme.key_color_yellow;
                revenueChartData.chartData.yRate = (float) (1.0 / rate / 100.0);
            }
            setDiamondsBalance(stats.status.available_balance, stats.status.next_withdrawal_at);
            if (listView != null) {
                listView.adapter.update(true);
            }
        }
    }

    @Override
    public void onFragmentDestroy() {
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.botDiamondsUpdated);
        super.onFragmentDestroy();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.botDiamondsUpdated) {
            if ((long) args[0] == bot_id) {
                checkStats();
            }
        }
    }

    @Override
    public boolean isLightStatusBar() {
        return AndroidUtilities.computePerceivedBrightness(Theme.getColor(Theme.key_windowBackgroundWhite)) > 0.721f;
    }


    private class NestedFrameLayout extends SizeNotifierFrameLayout implements NestedScrollingParent3 {

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        private NestedScrollingParentHelper nestedScrollingParentHelper;

        public NestedFrameLayout(Context context) {
            super(context);
            nestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        }

        @Override
        public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type, int[] consumed) {
            try {
                if (target == listView && transactionsLayout.isAttachedToWindow()) {
                    RecyclerListView innerListView = transactionsLayout.getCurrentListView();
                    int bottom = ((View) transactionsLayout.getParent()).getBottom();
                    actionBar.setCastShadows(listView.getHeight() - bottom < 0);
                    if (listView.getHeight() - bottom >= 0) {
                        consumed[1] = dyUnconsumed;
                        innerListView.scrollBy(0, dyUnconsumed);
                    }
                }
            } catch (Throwable e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(() -> {
                    try {
                        RecyclerListView innerListView = transactionsLayout.getCurrentListView();
                        if (innerListView != null && innerListView.getAdapter() != null) {
                            innerListView.getAdapter().notifyDataSetChanged();
                        }
                    } catch (Throwable e2) {

                    }
                });
            }
        }

        @Override
        public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {

        }

        @Override
        public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
            return super.onNestedPreFling(target, velocityX, velocityY);
        }

        @Override
        public void onNestedPreScroll(View target, int dx, int dy, int[] consumed, int type) {
            if (target == listView && transactionsLayout.isAttachedToWindow()) {
                boolean searchVisible = actionBar.isSearchFieldVisible();
                int t = ((View) transactionsLayout.getParent()).getTop() - AndroidUtilities.statusBarHeight - ActionBar.getCurrentActionBarHeight();
                int bottom = ((View) transactionsLayout.getParent()).getBottom();
                if (dy < 0) {
                    boolean scrolledInner = false;
                    actionBar.setCastShadows(listView.getHeight() - bottom < 0);
                    if (listView.getHeight() - bottom >= 0) {
                        RecyclerListView innerListView = transactionsLayout.getCurrentListView();
                        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) innerListView.getLayoutManager();
                        int pos = linearLayoutManager.findFirstVisibleItemPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            RecyclerView.ViewHolder holder = innerListView.findViewHolderForAdapterPosition(pos);
                            int top = holder != null ? holder.itemView.getTop() : -1;
                            int paddingTop = innerListView.getPaddingTop();
                            if (top != paddingTop || pos != 0) {
                                consumed[1] = pos != 0 ? dy : Math.max(dy, (top - paddingTop));
                                innerListView.scrollBy(0, dy);
                                scrolledInner = true;
                            }
                        }
                    }
                    if (searchVisible) {
                        if (!scrolledInner && t < 0) {
                            consumed[1] = dy - Math.max(t, dy);
                        } else {
                            consumed[1] = dy;
                        }
                    }
                } else {
                    if (searchVisible) {
                        RecyclerListView innerListView = transactionsLayout.getCurrentListView();
                        consumed[1] = dy;
                        if (t > 0) {
                            consumed[1] -= dy;
                        }
                        if (innerListView != null && consumed[1] > 0) {
                            innerListView.scrollBy(0, consumed[1]);
                        }
                    } else if (dy > 0) {
                        RecyclerListView innerListView = transactionsLayout.getCurrentListView();
                        if (listView.getHeight() - bottom >= 0 && innerListView != null && !innerListView.canScrollVertically(1)) {
                            consumed[1] = dy;
                            listView.stopScroll();
                        }
                    }
                }
            }
        }

        @Override
        public boolean onStartNestedScroll(View child, View target, int axes, int type) {
            return axes == ViewCompat.SCROLL_AXIS_VERTICAL;
        }

        @Override
        public void onNestedScrollAccepted(View child, View target, int axes, int type) {
            nestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        }

        @Override
        public void onStopNestedScroll(View target, int type) {
            nestedScrollingParentHelper.onStopNestedScroll(target);
        }

        @Override
        public void onStopNestedScroll(View child) {

        }
    }

    private void initWithdraw(long stars_amount, TLRPC.InputCheckPasswordSRP password, TwoStepVerificationActivity passwordFragment) {
        Activity parentActivity = getParentActivity();
        TLRPC.User currentUser = UserConfig.getInstance(currentAccount).getCurrentUser();
        if (parentActivity == null || currentUser == null) return;

        TLRPC.TL_payments_getDiamondsRevenueWithdrawalUrl req = new TLRPC.TL_payments_getDiamondsRevenueWithdrawalUrl();
        req.peer = MessagesController.getInstance(currentAccount).getInputPeer(bot_id);
        req.password = password != null ? password : new TLRPC.TL_inputCheckPasswordEmpty();
        req.flags |= 2;
        req.amount = stars_amount;
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error != null) {
                if ("PASSWORD_MISSING".equals(error.text) || error.text.startsWith("PASSWORD_TOO_FRESH_") || error.text.startsWith("SESSION_TOO_FRESH_")) {
                    if (passwordFragment != null) {
                        passwordFragment.needHideProgress();
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(parentActivity);
                    builder.setTitle(LocaleController.getString(R.string.EditAdminTransferAlertTitle));

                    LinearLayout linearLayout = new LinearLayout(parentActivity);
                    linearLayout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(2), AndroidUtilities.dp(24), 0);
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    builder.setView(linearLayout);

                    TextView messageTextView = new TextView(parentActivity);
                    messageTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
                    messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                    messageTextView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
                    messageTextView.setText(AndroidUtilities.replaceTags(LocaleController.getString(R.string.WithdrawChannelAlertText)));
                    linearLayout.addView(messageTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

                    LinearLayout linearLayout2 = new LinearLayout(parentActivity);
                    linearLayout2.setOrientation(LinearLayout.HORIZONTAL);
                    linearLayout.addView(linearLayout2, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 11, 0, 0));

                    ImageView dotImageView = new ImageView(parentActivity);
                    dotImageView.setImageResource(R.drawable.list_circle);
                    dotImageView.setPadding(LocaleController.isRTL ? AndroidUtilities.dp(11) : 0, AndroidUtilities.dp(9), LocaleController.isRTL ? 0 : AndroidUtilities.dp(11), 0);
                    dotImageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_dialogTextBlack), PorterDuff.Mode.MULTIPLY));

                    messageTextView = new TextView(parentActivity);
                    messageTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
                    messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                    messageTextView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
                    messageTextView.setText(AndroidUtilities.replaceTags(LocaleController.getString(R.string.EditAdminTransferAlertText1)));
                    if (LocaleController.isRTL) {
                        linearLayout2.addView(messageTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
                        linearLayout2.addView(dotImageView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT));
                    } else {
                        linearLayout2.addView(dotImageView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
                        linearLayout2.addView(messageTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
                    }

                    linearLayout2 = new LinearLayout(parentActivity);
                    linearLayout2.setOrientation(LinearLayout.HORIZONTAL);
                    linearLayout.addView(linearLayout2, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 11, 0, 0));

                    dotImageView = new ImageView(parentActivity);
                    dotImageView.setImageResource(R.drawable.list_circle);
                    dotImageView.setPadding(LocaleController.isRTL ? AndroidUtilities.dp(11) : 0, AndroidUtilities.dp(9), LocaleController.isRTL ? 0 : AndroidUtilities.dp(11), 0);
                    dotImageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_dialogTextBlack), PorterDuff.Mode.MULTIPLY));

                    messageTextView = new TextView(parentActivity);
                    messageTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
                    messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                    messageTextView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
                    messageTextView.setText(AndroidUtilities.replaceTags(LocaleController.getString(R.string.EditAdminTransferAlertText2)));
                    if (LocaleController.isRTL) {
                        linearLayout2.addView(messageTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
                        linearLayout2.addView(dotImageView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT));
                    } else {
                        linearLayout2.addView(dotImageView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
                        linearLayout2.addView(messageTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
                    }

                    if ("PASSWORD_MISSING".equals(error.text)) {
                        builder.setPositiveButton(LocaleController.getString(R.string.EditAdminTransferSetPassword), (dialogInterface, i) -> presentFragment(new TwoStepVerificationSetupActivity(TwoStepVerificationSetupActivity.TYPE_INTRO, null)));
                        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                    } else {
                        messageTextView = new TextView(parentActivity);
                        messageTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
                        messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                        messageTextView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
                        messageTextView.setText(LocaleController.getString(R.string.EditAdminTransferAlertText3));
                        linearLayout.addView(messageTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 11, 0, 0));

                        builder.setNegativeButton(LocaleController.getString(R.string.OK), null);
                    }
                    if (passwordFragment != null) {
                        passwordFragment.showDialog(builder.create());
                    } else {
                        showDialog(builder.create());
                    }
                } else if ("SRP_ID_INVALID".equals(error.text)) {
                    TL_account.getPassword getPasswordReq = new TL_account.getPassword();
                    ConnectionsManager.getInstance(currentAccount).sendRequest(getPasswordReq, (response2, error2) -> AndroidUtilities.runOnUIThread(() -> {
                        if (error2 == null) {
                            TL_account.Password currentPassword = (TL_account.Password) response2;
                            passwordFragment.setCurrentPasswordInfo(null, currentPassword);
                            TwoStepVerificationActivity.initPasswordNewAlgo(currentPassword);
                            initWithdraw(stars_amount, passwordFragment.getNewSrpPassword(), passwordFragment);
                        }
                    }), ConnectionsManager.RequestFlagWithoutLogin);
                } else {
                    if (passwordFragment != null) {
                        passwordFragment.needHideProgress();
                        passwordFragment.finishFragment();
                    }
                    BulletinFactory.showError(error);
                }
            } else {
                passwordFragment.needHideProgress();
                passwordFragment.finishFragment();
                if (response instanceof TLRPC.TL_payments_starsRevenueWithdrawalUrl) {
                    balanceEditTextAll = true;
                    Browser.openUrlInSystemBrowser(getContext(), ((TLRPC.TL_payments_starsRevenueWithdrawalUrl) response).url);
                }
            }
        }));
    }
}
