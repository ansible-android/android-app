package org.ansible.ui;

import static org.ansible.messenger.AndroidUtilities.REPLACING_TAG_TYPE_LINK_NBSP;
import static org.ansible.messenger.AndroidUtilities.dp;
import static org.ansible.messenger.AndroidUtilities.makeBlurBitmap;
import static org.ansible.messenger.LocaleController.formatPluralString;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.ansible.messenger.AndroidUtilities;
import org.ansible.messenger.BillingController;
import org.ansible.messenger.BuildVars;
import org.ansible.messenger.ChatObject;
import org.ansible.messenger.FileLog;
import org.ansible.messenger.LocaleController;
import org.ansible.messenger.MessagesController;
import org.ansible.messenger.R;
import org.ansible.messenger.UserConfig;
import org.ansible.messenger.browser.Browser;
import org.ansible.asnet.ConnectionsManager;
import org.ansible.asnet.TLRPC;
import org.ansible.asnet.tl.TL_account;
import org.ansible.asnet.tl.TL_diamonds;
import org.ansible.ui.ActionBar.ActionBar;
import org.ansible.ui.ActionBar.AlertDialog;
import org.ansible.ui.ActionBar.BaseFragment;
import org.ansible.ui.ActionBar.Theme;
import org.ansible.ui.Components.AnimatedEmojiSpan;
import org.ansible.ui.Components.AnimatedTextView;
import org.ansible.ui.Components.Bulletin;
import org.ansible.ui.Components.BulletinFactory;
import org.ansible.ui.Components.ColoredImageSpan;
import org.ansible.ui.Components.CubicBezierInterpolator;
import org.ansible.ui.Components.EditTextBoldCursor;
import org.ansible.ui.Components.FlickerLoadingView;
import org.ansible.ui.Components.LayoutHelper;
import org.ansible.ui.Components.LinkSpanDrawable;
import org.ansible.ui.Components.OutlineTextContainerView;
import org.ansible.ui.Components.RLottieImageView;
import org.ansible.ui.Components.RecyclerListView;
import org.ansible.ui.Components.SizeNotifierFrameLayout;
import org.ansible.ui.Components.UItem;
import org.ansible.ui.Components.UniversalAdapter;
import org.ansible.ui.Components.UniversalRecyclerView;
import org.ansible.ui.Components.ViewPagerFixed;
import org.ansible.ui.Components.blur3.capture.IBlur3Capture;
import org.ansible.ui.Diamonds.BotDiamondsActivity;
import org.ansible.ui.Diamonds.BotDiamondsController;
import org.ansible.ui.Diamonds.DiamondsIntroActivity;
import org.ansible.ui.Stories.recorder.ButtonWithCounterView;
import org.ansible.ui.bots.AffiliateProgramFragment;
import org.ansible.ui.bots.ChannelAffiliateProgramsFragment;

import java.util.ArrayList;

public class ChannelMonetizationLayout extends SizeNotifierFrameLayout implements NestedScrollingParent3 {

    public static ChannelMonetizationLayout instance;

    private final BaseFragment fragment;
    private final Theme.ResourcesProvider resourcesProvider;
    private final int currentAccount;
    public final long dialogId;

    private final CharSequence proceedsInfo;
    private final CharSequence diamondsBalanceInfo;

    private int shakeDp = 4;

    private int diamondsBalanceBlockedUntil;
    private final LinearLayout diamondsBalanceLayout;
    private final RelativeSizeSpan diamondsBalanceTitleSizeSpan;
    private TL_diamonds.StarsAmount diamondsBalance = TL_diamonds.StarsAmount.ofDiamonds(0);
    private final AnimatedTextView diamondsBalanceTitle;
    private final AnimatedTextView diamondsBalanceSubtitle;
    private final ButtonWithCounterView diamondsBalanceButton;
    private ColoredImageSpan[] diamondRef = new ColoredImageSpan[1];
    private final LinearLayout diamondsBalanceButtonsLayout;
    private final ButtonWithCounterView diamondsAdsButton;
    private OutlineTextContainerView diamondsBalanceEditTextContainer;
    private boolean diamondsBalanceEditTextIgnore = false;
    private boolean diamondsBalanceEditTextAll = true;
    private long diamondsBalanceEditTextValue;
    private EditTextBoldCursor diamondsBalanceEditText;

    private Bulletin withdrawalBulletin;

    private boolean transfering;

    public final UniversalRecyclerView listView;
    public IBlur3Capture iBlur3Capture;
    private final FrameLayout progress;

    private final ChannelTransactionsView transactionsLayout;
    public void updateList() {
        if (listView != null) {
            listView.adapter.update(true);
        }
    }

    public final boolean diamondsRevenueAvailable;

    public ChannelMonetizationLayout(
        Context context,
        BaseFragment fragment,
        int currentAccount,
        long dialogId,
        Theme.ResourcesProvider resourcesProvider,

        boolean diamondsRevenueAvailable
    ) {
        super(context);

        this.diamondsRevenueAvailable = diamondsRevenueAvailable;

        this.fragment = fragment;
        this.resourcesProvider = resourcesProvider;

        this.currentAccount = currentAccount;
        this.dialogId = dialogId;
        initLevel();

        final TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);

        final int proceedsInfoText = R.string.MonetizationProceedsDiamondsInfo;
        final int proceedsInfoLink = R.string.MonetizationProceedsDiamondsInfoLink;
        proceedsInfo = AndroidUtilities.replaceArrows(AndroidUtilities.replaceSingleTag(getString(proceedsInfoText), -1, REPLACING_TAG_TYPE_LINK_NBSP, () -> {
            Browser.openUrl(getContext(), getString(proceedsInfoLink));
        }, resourcesProvider), true);
        diamondsBalanceInfo = AndroidUtilities.replaceArrows(AndroidUtilities.replaceSingleTag(getString(ChatObject.isChannelAndNotMegaGroup(chat) ? R.string.MonetizationDiamondsInfo : R.string.MonetizationDiamondsInfoGroup), () -> {
            Browser.openUrl(getContext(), getString(R.string.MonetizationDiamondsInfoLink));
        }), true);

        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray, resourcesProvider));

        transactionsLayout = new ChannelTransactionsView(context, currentAccount, dialogId, fragment.getClassGuid(), this::updateList, resourcesProvider);

        diamondsBalanceLayout = new LinearLayout(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(
                        MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                        heightMeasureSpec
                );
            }
        };
        diamondsBalanceLayout.setOrientation(LinearLayout.VERTICAL);
        diamondsBalanceLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider));
        diamondsBalanceLayout.setPadding(0, 0, 0, dp(17));

        diamondsBalanceTitle = new AnimatedTextView(context, false, true, true);
        diamondsBalanceTitle.setTypeface(AndroidUtilities.bold());
        diamondsBalanceTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        diamondsBalanceTitle.setTextSize(dp(32));
        diamondsBalanceTitle.setGravity(Gravity.CENTER);
        diamondsBalanceTitleSizeSpan = new RelativeSizeSpan(65f / 96f);
        diamondsBalanceLayout.addView(diamondsBalanceTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 38, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 22, 15, 22, 0));

        diamondsBalanceSubtitle = new AnimatedTextView(context, true, true, true);
        diamondsBalanceSubtitle.setGravity(Gravity.CENTER);
        diamondsBalanceSubtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
        diamondsBalanceSubtitle.setTextSize(dp(14));
        diamondsBalanceLayout.addView(diamondsBalanceSubtitle, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 17, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 22, 4, 22, 0));

        diamondsBalanceEditTextContainer = new OutlineTextContainerView(context) {
            @Override
            public boolean dispatchTouchEvent(MotionEvent event) {
                if (diamondsBalanceEditText != null && !diamondsBalanceEditText.isFocusable()) {
                    diamondsBalanceEditText.setFocusable(true);
                    diamondsBalanceEditText.setFocusableInTouchMode(true);
                    int position = listView.findPositionByItemId(STARS_BALANCE);
                    if (position >= 0 && position < listView.adapter.getItemCount()) {
                        listView.stopScroll();
                        listView.smoothScrollToPosition(position);
                    }
                    diamondsBalanceEditText.requestFocus();
                }
                return super.dispatchTouchEvent(event);
            }
        };
        diamondsBalanceEditTextContainer.setVisibility(GONE);
        diamondsBalanceEditTextContainer.setText(getString(R.string.BotDiamondsWithdrawPlaceholder));
        diamondsBalanceEditTextContainer.setLeftPadding(dp(14 + 22));
        diamondsBalanceEditText = new EditTextBoldCursor(context) {
            @Override
            protected void onDetachedFromWindow() {
                super.onDetachedFromWindow();
                AndroidUtilities.hideKeyboard(this);
            }
        };
        diamondsBalanceEditText.setFocusable(false);
        diamondsBalanceEditText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        diamondsBalanceEditText.setCursorSize(AndroidUtilities.dp(20));
        diamondsBalanceEditText.setCursorWidth(1.5f);
        diamondsBalanceEditText.setBackground(null);
        diamondsBalanceEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        diamondsBalanceEditText.setMaxLines(1);
        int padding = AndroidUtilities.dp(16);
        diamondsBalanceEditText.setPadding(dp(6), padding, padding, padding);
        diamondsBalanceEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        diamondsBalanceEditText.setTypeface(Typeface.DEFAULT);
        diamondsBalanceEditText.setHighlightColor(Theme.getColor(Theme.key_chat_inTextSelectionHighlight, resourcesProvider));
        diamondsBalanceEditText.setHandlesColor(Theme.getColor(Theme.key_chat_TextSelectionCursor, resourcesProvider));
        diamondsBalanceEditText.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        diamondsBalanceEditText.setOnFocusChangeListener((v, hasFocus) -> diamondsBalanceEditTextContainer.animateSelection(hasFocus ? 1f : 0f));
        diamondsBalanceEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (diamondsBalanceEditTextIgnore) return;
                diamondsBalanceEditTextValue = TextUtils.isEmpty(s) ? 0 : Long.parseLong(s.toString());
                if (diamondsBalanceEditTextValue > diamondsBalance.amount) {
                    diamondsBalanceEditTextValue = diamondsBalance.amount;
                    diamondsBalanceEditTextIgnore = true;
                    diamondsBalanceEditText.setText(Long.toString(diamondsBalanceEditTextValue));
                    diamondsBalanceEditText.setSelection(diamondsBalanceEditText.getText().length());
                    diamondsBalanceEditTextIgnore = false;
                }
                diamondsBalanceEditTextAll = diamondsBalanceEditTextValue == diamondsBalance.amount;
                AndroidUtilities.cancelRunOnUIThread(setDiamondsBalanceButtonText);
                setDiamondsBalanceButtonText.run();
                diamondsBalanceEditTextAll = false;
            }
        });
        LinearLayout balanceEditTextLayout = new LinearLayout(context);
        balanceEditTextLayout.setOrientation(LinearLayout.HORIZONTAL);
        ImageView diamondImage = new ImageView(context);
        diamondImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        diamondImage.setImageResource(R.drawable.diamond);
        balanceEditTextLayout.addView(diamondImage, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, Gravity.LEFT | Gravity.CENTER_VERTICAL, 14, 0, 0, 0));
        balanceEditTextLayout.addView(diamondsBalanceEditText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 1, Gravity.FILL));
        diamondsBalanceEditTextContainer.attachEditText(diamondsBalanceEditText);
        diamondsBalanceEditTextContainer.addView(balanceEditTextLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));
        diamondsBalanceLayout.addView(diamondsBalanceEditTextContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 18, 14, 18, 2));

        diamondsBalanceButtonsLayout = new LinearLayout(context);
        diamondsBalanceButtonsLayout.setOrientation(LinearLayout.HORIZONTAL);

        diamondsBalanceButton = new ButtonWithCounterView(context, resourcesProvider) {
            @Override
            protected boolean subTextSplitToWords() {
                return false;
            }
        }.setRound();
        diamondsBalanceButton.setEnabled(false);
        diamondsBalanceButton.setText(formatPluralString("MonetizationDiamondsWithdraw", 0), false);
        diamondsBalanceButton.setVisibility(View.VISIBLE);
        diamondsBalanceButton.setOnClickListener(v -> {
            if (!v.isEnabled() || diamondsBalanceButton.isLoading()) {
                return;
            }

            final int now = ConnectionsManager.getInstance(currentAccount).getCurrentTime();
            if (diamondsBalanceBlockedUntil > now) {
                withdrawalBulletin = BulletinFactory.of(fragment).createSimpleBulletin(R.raw.timer_3, AndroidUtilities.replaceTags(LocaleController.formatString(R.string.BotDiamondsWithdrawalToast, BotDiamondsActivity.untilString(diamondsBalanceBlockedUntil - now)))).show();
                return;
            }

            if (diamondsBalanceEditTextValue < MessagesController.getInstance(currentAccount).diamondsRevenueWithdrawalMin) {
                Drawable diamondDrawable = getContext().getResources().getDrawable(R.drawable.diamond).mutate();
                BulletinFactory.of(fragment).createSimpleBulletin(diamondDrawable, AndroidUtilities.replaceSingleTag(LocaleController.formatPluralString("BotDiamondsWithdrawMinLimit", (int) MessagesController.getInstance(currentAccount).diamondsRevenueWithdrawalMin), () -> {
                    Bulletin.hideVisible();
                    if (diamondsBalance.amount < MessagesController.getInstance(currentAccount).diamondsRevenueWithdrawalMin) {
                        diamondsBalanceEditTextAll = true;
                        diamondsBalanceEditTextValue = diamondsBalance.amount;
                    } else {
                        diamondsBalanceEditTextAll = false;
                        diamondsBalanceEditTextValue = MessagesController.getInstance(currentAccount).diamondsRevenueWithdrawalMin;
                    }
                    diamondsBalanceEditTextIgnore = true;
                    diamondsBalanceEditText.setText(Long.toString(diamondsBalanceEditTextValue));
                    diamondsBalanceEditText.setSelection(diamondsBalanceEditText.getText().length());
                    diamondsBalanceEditTextIgnore = false;

                    AndroidUtilities.cancelRunOnUIThread(setDiamondsBalanceButtonText);
                    setDiamondsBalanceButtonText.run();
                })).show();
                return;
            }

            TwoStepVerificationActivity passwordFragment = new TwoStepVerificationActivity();
            passwordFragment.setDelegate(1, password -> initWithdraw(password, passwordFragment));
            diamondsBalanceButton.setLoading(true);
            passwordFragment.preload(() -> {
                diamondsBalanceButton.setLoading(false);
                fragment.presentFragment(passwordFragment);;
            });
        });

        diamondsAdsButton = new ButtonWithCounterView(context, resourcesProvider).setRound();
        diamondsAdsButton.setEnabled(false);
        diamondsAdsButton.setText(getString(R.string.MonetizationDiamondsAds), false);
        diamondsAdsButton.setOnClickListener(v -> {
            if (!v.isEnabled() || diamondsAdsButton.isLoading()) return;

            diamondsAdsButton.setLoading(true);
            TLRPC.TL_payments_getDiamondsRevenueAdsAccountUrl req = new TLRPC.TL_payments_getDiamondsRevenueAdsAccountUrl();
            req.peer = MessagesController.getInstance(currentAccount).getInputPeer(dialogId);
            ConnectionsManager.getInstance(currentAccount).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                if (res instanceof TLRPC.TL_payments_starsRevenueAdsAccountUrl) {
                    Browser.openUrl(context, ((TLRPC.TL_payments_starsRevenueAdsAccountUrl) res).url);
                }
                AndroidUtilities.runOnUIThread(() -> {
                    diamondsAdsButton.setLoading(false);
                }, 1000);
            }));
        });

        diamondsBalanceButtonsLayout.addView(diamondsBalanceButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 1, Gravity.FILL));
        if (ChatObject.isChannelAndNotMegaGroup(chat)) {
            diamondsBalanceButtonsLayout.addView(new Space(context), LayoutHelper.createLinear(8, 48, 0, Gravity.FILL));
            diamondsBalanceButtonsLayout.addView(diamondsAdsButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 1, Gravity.FILL));
        }
        diamondsBalanceLayout.addView(diamondsBalanceButtonsLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.TOP | Gravity.FILL_HORIZONTAL, 18, 13, 18, 0));

        diamondsBalanceEditText.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_NEXT) {
                TwoStepVerificationActivity passwordFragment = new TwoStepVerificationActivity();
                passwordFragment.setDelegate(1, password -> initWithdraw(password, passwordFragment));
                diamondsBalanceButton.setLoading(true);
                passwordFragment.preload(() -> {
                    diamondsBalanceButton.setLoading(false);
                    fragment.presentFragment(passwordFragment);;
                });
                return true;
            }
            return false;
        });
        setDiamondsBalanceButtonText = () -> {
            final int now = ConnectionsManager.getInstance(currentAccount).getCurrentTime();
            diamondsBalanceButton.setEnabled(diamondsBalanceEditTextValue > 0 || diamondsBalanceBlockedUntil > now);
            if (now < diamondsBalanceBlockedUntil) {
                diamondsBalanceButton.setText(getString(R.string.MonetizationDiamondsWithdrawUntil), true);

                if (lock == null) {
                    lock = new SpannableStringBuilder("l");
                    ColoredImageSpan coloredImageSpan = new ColoredImageSpan(R.drawable.mini_switch_lock);
                    coloredImageSpan.setTopOffset(1);
                    lock.setSpan(coloredImageSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                SpannableStringBuilder buttonLockedText = new SpannableStringBuilder();
                buttonLockedText.append(lock).append(BotDiamondsActivity.untilString(diamondsBalanceBlockedUntil - now));
                diamondsBalanceButton.setSubText(buttonLockedText, true);

                if (withdrawalBulletin != null && withdrawalBulletin.getLayout() instanceof Bulletin.LottieLayout && withdrawalBulletin.getLayout().isAttachedToWindow()) {
                    ((Bulletin.LottieLayout) withdrawalBulletin.getLayout()).textView.setText(AndroidUtilities.replaceTags(LocaleController.formatString(R.string.BotDiamondsWithdrawalToast, BotDiamondsActivity.untilString(diamondsBalanceBlockedUntil - now))));
                }

                AndroidUtilities.cancelRunOnUIThread(this.setDiamondsBalanceButtonText);
                AndroidUtilities.runOnUIThread(this.setDiamondsBalanceButtonText, 1000);
            } else {
                diamondsBalanceButton.setSubText(null, true);
                diamondsBalanceButton.setText(DiamondsIntroActivity.replaceDiamonds(diamondsBalanceEditTextAll ? getString(R.string.MonetizationDiamondsWithdrawAll) : LocaleController.formatPluralStringSpaced("MonetizationDiamondsWithdraw", (int) diamondsBalanceEditTextValue), diamondRef), true);
            }
        };

        listView = new UniversalRecyclerView(fragment, this::fillItems, this::onClick, this::onLongClick);
        listView.setClipToPadding(false);
        listView.setSections();
        addView(listView);

        LinearLayout progressLayout = new LinearLayout(context);
        progressLayout.setOrientation(LinearLayout.VERTICAL);

        progress = new FrameLayout(context);
        progress.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray, resourcesProvider));
        progress.addView(progressLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        RLottieImageView imageView = new RLottieImageView(context);
        imageView.setAutoRepeat(true);
        imageView.setAnimation(R.raw.statistic_preload, 120, 120);
        imageView.playAnimation();

        TextView loadingTitle = new TextView(context);
        loadingTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        loadingTitle.setTypeface(AndroidUtilities.bold());
        loadingTitle.setTextColor(Theme.getColor(Theme.key_player_actionBarTitle));
        loadingTitle.setTag(Theme.key_player_actionBarTitle);
        loadingTitle.setText(getString("LoadingStats", R.string.LoadingStats));
        loadingTitle.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView loadingSubtitle = new TextView(context);
        loadingSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        loadingSubtitle.setTextColor(Theme.getColor(Theme.key_player_actionBarSubtitle));
        loadingSubtitle.setTag(Theme.key_player_actionBarSubtitle);
        loadingSubtitle.setText(getString(R.string.LoadingStatsDescription));
        loadingSubtitle.setGravity(Gravity.CENTER_HORIZONTAL);

        progressLayout.addView(imageView, LayoutHelper.createLinear(120, 120, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 20));
        progressLayout.addView(loadingTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 10));
        progressLayout.addView(loadingSubtitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));

        addView(progress, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL));
    }

    private void initWithdraw(TLRPC.InputCheckPasswordSRP password, TwoStepVerificationActivity passwordFragment) {
        if (fragment == null) return;
        Activity parentActivity = fragment.getParentActivity();
        TLRPC.User currentUser = UserConfig.getInstance(currentAccount).getCurrentUser();
        if (parentActivity == null || currentUser == null) return;

        TLRPC.TL_payments_getDiamondsRevenueWithdrawalUrl req = new TLRPC.TL_payments_getDiamondsRevenueWithdrawalUrl();
        req.peer = MessagesController.getInstance(currentAccount).getInputPeer(dialogId);
        req.password = password != null ? password : new TLRPC.TL_inputCheckPasswordEmpty();
        req.flags |= 2;
        req.amount = diamondsBalanceEditTextValue;
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
                        builder.setPositiveButton(LocaleController.getString(R.string.EditAdminTransferSetPassword), (dialogInterface, i) -> fragment.presentFragment(new TwoStepVerificationSetupActivity(TwoStepVerificationSetupActivity.TYPE_INTRO, null)));
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
                        fragment.showDialog(builder.create());
                    }
                } else if ("SRP_ID_INVALID".equals(error.text)) {
                    TL_account.getPassword getPasswordReq = new TL_account.getPassword();
                    ConnectionsManager.getInstance(currentAccount).sendRequest(getPasswordReq, (response2, error2) -> AndroidUtilities.runOnUIThread(() -> {
                        if (error2 == null) {
                            TL_account.Password currentPassword = (TL_account.Password) response2;
                            passwordFragment.setCurrentPasswordInfo(null, currentPassword);
                            TwoStepVerificationActivity.initPasswordNewAlgo(currentPassword);
                            initWithdraw(passwordFragment.getNewSrpPassword(), passwordFragment);
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
                    Browser.openUrl(getContext(), ((TLRPC.TL_payments_starsRevenueWithdrawalUrl) response).url);
                    loadDiamondsStats(true);
                }
                reloadTransactions();
            }
        }));
    }

    private void setDiamondsBalance(TL_diamonds.StarsAmount amount, int blockedUntil) {
        if (diamondsBalanceTitle == null || diamondsBalanceSubtitle == null)
            return;
//        long amount = (long) (stars_rate * crypto_amount * 100.0);
        SpannableStringBuilder ssb = new SpannableStringBuilder(DiamondsIntroActivity.replaceDiamondsWithPlain(TextUtils.concat("XTR ", DiamondsIntroActivity.formatDiamondsAmount(amount, 0.8f, ' ')), 1f));
        int index = TextUtils.indexOf(ssb, ".");
        if (index >= 0) {
            ssb.setSpan(diamondsBalanceTitleSizeSpan, index, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        diamondsBalance = amount;
        diamondsBalanceTitle.setText(ssb);
        diamondsBalanceSubtitle.setText("≈" + BillingController.getInstance().formatCurrency((long) (stars_rate * amount.amount * 100.0), "USD"));
        diamondsBalanceEditTextContainer.setVisibility(amount.amount > 0 ? VISIBLE : GONE);
        if (diamondsBalanceEditTextAll) {
            diamondsBalanceEditTextIgnore = true;
            diamondsBalanceEditText.setText(Long.toString(diamondsBalanceEditTextValue = amount.amount));
            diamondsBalanceEditText.setSelection(diamondsBalanceEditText.getText().length());
            diamondsBalanceEditTextIgnore = false;

            diamondsBalanceButton.setEnabled(diamondsBalanceEditTextValue > 0);
        }
        if (diamondsAdsButton != null) {
            diamondsAdsButton.setEnabled(amount.amount > 0);
        }
        diamondsBalanceBlockedUntil = blockedUntil;

        AndroidUtilities.cancelRunOnUIThread(setDiamondsBalanceButtonText);
        setDiamondsBalanceButtonText.run();
    }


    private SpannableStringBuilder lock;
    private Runnable setDiamondsBalanceButtonText;

    private double stars_rate;

    private void loadDiamondsStats(boolean force) {
        if (!diamondsRevenueAvailable) return;

        TLRPC.TL_payments_starsRevenueStats cachedStats = BotDiamondsController.getInstance(currentAccount).getStarsRevenueStats(dialogId, force);
        if (cachedStats != null) {
            AndroidUtilities.runOnUIThread(() -> {
                applyDiamondsStats(cachedStats);
            });
        } else {
            TLRPC.TL_payments_getDiamondsRevenueStats req2 = new TLRPC.TL_payments_getDiamondsRevenueStats();
            req2.peer = MessagesController.getInstance(currentAccount).getInputPeer(dialogId);
            req2.dark = Theme.isCurrentThemeDark();
            ConnectionsManager.getInstance(currentAccount).sendRequest(req2, (res2, err) -> AndroidUtilities.runOnUIThread(() -> {
                if (res2 instanceof TLRPC.TL_payments_starsRevenueStats) {
                    TLRPC.TL_payments_starsRevenueStats stats = (TLRPC.TL_payments_starsRevenueStats) res2;
                    applyDiamondsStats(stats);
                }
            }));
        }
    }

    private void applyDiamondsStats(TLRPC.TL_payments_starsRevenueStats stats) {
        final boolean first = diamondsRevenueChart == null;
        stars_rate = stats.usd_rate;
        diamondsRevenueChart = StatisticActivity.createViewData(stats.revenue_graph, getString(R.string.MonetizationGraphDiamondsRevenue), 2);
        if (diamondsRevenueChart != null && diamondsRevenueChart.chartData != null && diamondsRevenueChart.chartData.lines != null && !diamondsRevenueChart.chartData.lines.isEmpty() && diamondsRevenueChart.chartData.lines.get(0) != null) {
            diamondsRevenueChart.chartData.lines.get(0).colorKey = Theme.key_statisticChartLine_golden;
            diamondsRevenueChart.chartData.yRate = (float) (1.0 / stars_rate / 100.0);
        }
        setupBalances(stats.status);

        if (progress != null) {
            progress.animate().alpha(0).setDuration(380).setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT).withEndAction(() -> {
                progress.setVisibility(View.GONE);
            }).start();
        }

        if (listView != null) {
            listView.adapter.update(!first);
            if (first) {
                listView.scrollToPosition(0);
            }
        }
    }

    private void initLevel() {
        loadDiamondsStats(false);
    }

    public void setupBalances(TLRPC.TL_starsRevenueStatus balances) {
        if (stars_rate == 0) {
            return;
        }
        availableValue.contains2 = true;
        availableValue.crypto_amount2 = balances.available_balance;
        availableValue.amount2 = (long) (availableValue.crypto_amount2.amount * stars_rate * 100.0);
        setDiamondsBalance(availableValue.crypto_amount2, balances.next_withdrawal_at);
        availableValue.currency = "USD";
        lastWithdrawalValue.contains2 = true;
        lastWithdrawalValue.crypto_amount2 = balances.current_balance;
        lastWithdrawalValue.amount2 = (long) (lastWithdrawalValue.crypto_amount2.amount * stars_rate * 100.0);
        lastWithdrawalValue.currency = "USD";
        lifetimeValue.contains2 = true;
        lifetimeValue.crypto_amount2 = balances.overall_revenue;
        lifetimeValue.amount2 = (long) (lifetimeValue.crypto_amount2.amount * stars_rate * 100.0);
        lifetimeValue.currency = "USD";
        proceedsAvailable = true;
        if (diamondsBalanceButtonsLayout != null) {
            diamondsBalanceButtonsLayout.setVisibility(balances.withdrawal_enabled ? View.VISIBLE : View.GONE);
        }
        if (diamondsBalanceButton != null) {
            diamondsBalanceButton.setVisibility(balances.available_balance.amount > 0 || BuildVars.DEBUG_PRIVATE_VERSION ? View.VISIBLE : View.GONE);
        }

        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    public void reloadTransactions() {
        transactionsLayout.reloadTransactions();
    }

    @Override
    protected void onAttachedToWindow() {
        instance = this;
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        instance = null;
        super.onDetachedFromWindow();
        if (actionBar != null) {
            actionBar.setCastShadows(true);
        }
    }

    private ActionBar actionBar;
    public void setActionBar(ActionBar actionBar) {
        this.actionBar = actionBar;
    }

    private StatisticActivity.ChartViewData diamondsRevenueChart;
    private boolean proceedsAvailable = false;
    private final ProceedOverview availableValue =      ProceedOverview.as(null, "XTR", getString(R.string.MonetizationOverviewAvailable));
    private final ProceedOverview lastWithdrawalValue = ProceedOverview.as(null, "XTR", getString(R.string.MonetizationOverviewLastWithdrawal));
    private final ProceedOverview lifetimeValue =       ProceedOverview.as(null, "XTR", getString(R.string.MonetizationOverviewTotal));

    private final static int BUTTON_LOAD_MORE_TRANSACTIONS = 2;
    private final static int STARS_BALANCE = 3;
    private final static int BUTTON_AFFILIATE =4;

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        int stats_dc = -1;
        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
        TLRPC.ChatFull chatFull = MessagesController.getInstance(currentAccount).getChatFull(-dialogId);
        if (chatFull != null) {
            stats_dc = chatFull.stats_dc;
        }
        if (diamondsRevenueAvailable && diamondsRevenueChart != null && !diamondsRevenueChart.isEmpty) {
            items.add(UItem.asChart(StatisticActivity.VIEW_TYPE_STACKBAR, stats_dc, diamondsRevenueChart));
            items.add(UItem.asShadow(-3, null));
        }
        if (proceedsAvailable) {
            items.add(UItem.asBlackHeader(getString(R.string.MonetizationOverview)));
            items.add(UItem.asProceedOverview(availableValue));
            items.add(UItem.asProceedOverview(lastWithdrawalValue));
            items.add(UItem.asProceedOverview(lifetimeValue));
            items.add(UItem.asShadow(-4, proceedsInfo));
        }
        if (chat != null && chat.creator) {
            if (diamondsRevenueAvailable) {
                items.add(UItem.asBlackHeader(getString(R.string.MonetizationDiamondsBalance)));
                items.add(UItem.asCustom(STARS_BALANCE, diamondsBalanceLayout));
                items.add(UItem.asShadow(-6, diamondsBalanceInfo));
            }
        }
        if (ChatObject.isChannelAndNotMegaGroup(MessagesController.getInstance(currentAccount).getChat(-dialogId)) && MessagesController.getInstance(currentAccount).starrefConnectAllowed) {
            items.add(AffiliateProgramFragment.ColorfulTextCell.Factory.as(BUTTON_AFFILIATE, Theme.getColor(Theme.key_color_green, resourcesProvider), R.drawable.filled_earn_stars, applyNewSpan(getString(R.string.ChannelAffiliateProgramRowTitle)), getString(R.string.ChannelAffiliateProgramRowText)));
            items.add(UItem.asShadow(-7, null));
        }
        if (transactionsLayout.hasTransactions()) {
            items.add(UItem.asFullscreenCustom(transactionsLayout, dp(24), true));
        } else {
            items.add(UItem.asShadow(-10, null));
        }
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == BUTTON_AFFILIATE) {
            fragment.presentFragment(new ChannelAffiliateProgramsFragment(dialogId));
        }
    }

    private boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    private static final long DIAMOND_EMOJI = 5471952986970267163L;

    public static class ProceedOverviewCell extends LinearLayout {

        private final Theme.ResourcesProvider resourcesProvider;

        private final LinearLayout layout;
        private final LinearLayout[] amountContainer = new LinearLayout[2];
        private final AnimatedEmojiSpan.TextViewEmojis[] cryptoAmountView = new AnimatedEmojiSpan.TextViewEmojis[2];
        private final TextView amountView[] = new TextView[2];
        private final TextView titleView;

        public ProceedOverviewCell(Context context, Theme.ResourcesProvider resourcesProvider) {
            super(context);
            this.resourcesProvider = resourcesProvider;

            setOrientation(VERTICAL);

            layout = new LinearLayout(context);
            layout.setOrientation(VERTICAL);
            addView(layout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 22, 9, 22, 0));

            for (int i = 0; i < 2; ++i) {
                amountContainer[i] = new LinearLayout(context);
                amountContainer[i].setOrientation(HORIZONTAL);
                layout.addView(amountContainer[i], LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 1, Gravity.FILL));

                cryptoAmountView[i] = new AnimatedEmojiSpan.TextViewEmojis(context);
                cryptoAmountView[i].setTypeface(AndroidUtilities.bold());
                cryptoAmountView[i].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                cryptoAmountView[i].setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
                amountContainer[i].addView(cryptoAmountView[i], LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM, 0, 0, 5, 0));

                amountView[i] = new AnimatedEmojiSpan.TextViewEmojis(context);
                amountView[i].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f);
                amountView[i].setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
                amountContainer[i].addView(amountView[i], LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM));
            }

            titleView = new TextView(context);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
            addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.FILL_HORIZONTAL, 22, 5, 22, 9));
        }

        public void set(ProceedOverview value) {
            titleView.setText(value.text);

            for (int i = 0; i < 2; ++i) {
                final String crypto_currency = i == 0 ? value.crypto_currency : value.crypto_currency2;
//                final long crypto_amount = i == 0 ? value.crypto_amount : value.crypto_amount2;
//                CharSequence cryptoAmount;
//                if (i == 0) {
//                    cryptoAmount
//                }
                final long amount = i == 0 ? value.amount : value.amount2;

                if (i == 0 && !value.contains1) {
                    amountContainer[i].setVisibility(View.GONE);
                    continue;
                }
                if (i == 1 && !value.contains2) {
                    amountContainer[i].setVisibility(View.GONE);
                    continue;
                }

                SpannableStringBuilder s = new SpannableStringBuilder(crypto_currency + " ");
                CharSequence finalS;
                if ("XTR".equalsIgnoreCase(crypto_currency)) {
                    if (i == 0) {
                        s.append(LocaleController.formatNumber(value.crypto_amount, ' '));
                    } else {
                        s.append(DiamondsIntroActivity.formatDiamondsAmount(value.crypto_amount2, .8f, ' '));
                    }
                    finalS = DiamondsIntroActivity.replaceDiamondsWithPlain(s, .7f);
                } else {
                    s.append(Long.toString(value.crypto_amount));
                    finalS = s;
                }
                SpannableStringBuilder cryptoAmount = new SpannableStringBuilder(finalS);
                amountContainer[i].setVisibility(View.VISIBLE);
                cryptoAmountView[i].setText(cryptoAmount);
                amountView[i].setText("≈" + BillingController.getInstance().formatCurrency(amount, value.currency));
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), heightMeasureSpec);
        }
    }

    public static class ProceedOverview {

        public boolean contains1 = true;
        public String crypto_currency;
        public CharSequence text;
        public long crypto_amount;
        public long amount;
        public String currency;

        public boolean contains2;
        public String crypto_currency2;
        public TL_diamonds.StarsAmount crypto_amount2 = TL_diamonds.StarsAmount.ofDiamonds(0);
        public long amount2;

        public static ProceedOverview as(String cryptoCurrency, CharSequence text) {
            ProceedOverview o = new ProceedOverview();
            o.crypto_currency = cryptoCurrency;
            o.text = text;
            return o;
        }

        public static ProceedOverview as(String cryptoCurrency, String cryptoCurrency2, CharSequence text) {
            ProceedOverview o = new ProceedOverview();
            o.contains1 = false;
            o.crypto_currency = cryptoCurrency;
            o.crypto_currency2 = cryptoCurrency2;
            o.text = text;
            return o;
        }
    }

    public static class FeatureCell extends FrameLayout {
        public FeatureCell(Context context, int icon, CharSequence header, CharSequence text, Theme.ResourcesProvider resourcesProvider) {
            super(context);

            ImageView imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            imageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider), PorterDuff.Mode.SRC_IN));
            imageView.setImageResource(icon);
            addView(imageView, LayoutHelper.createFrame(24, 24, Gravity.TOP | Gravity.LEFT, 0, 5, 18, 0));

            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            addView(layout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.FILL_HORIZONTAL, 42, 0, 0, 0));

            LinkSpanDrawable.LinksTextView textView = new LinkSpanDrawable.LinksTextView(context);
            textView.setTypeface(AndroidUtilities.bold());
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
            textView.setLinkTextColor(Theme.getColor(Theme.key_chat_messageLinkIn, resourcesProvider));
            textView.setText(header);
            layout.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 0, 0, 2));

            textView = new LinkSpanDrawable.LinksTextView(context);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
            textView.setLinkTextColor(Theme.getColor(Theme.key_chat_messageLinkIn, resourcesProvider));
            textView.setText(text);
            layout.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 0, 0, 0));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(Math.min(MeasureSpec.getSize(widthMeasureSpec), dp(325)), MeasureSpec.getMode(widthMeasureSpec)), heightMeasureSpec);
        }
    }

    public class ChannelTransactionsView extends LinearLayout {

        private final int currentAccount;
        private final ViewPagerFixed viewPager;
        private final PageAdapter adapter;
        private final ViewPagerFixed.TabsView tabsView;
        private final long dialogId;
        private final Runnable updateParentList;

        public static final int STARS_TRANSACTIONS = 0;

        private final ArrayList<TL_diamonds.StarsTransaction> diamondsTransactions = new ArrayList<>();
        private String diamondsLastOffset = "";

        private class PageAdapter extends ViewPagerFixed.Adapter {

            private final Context context;
            private final int currentAccount;
            private final int classGuid;
            private final Theme.ResourcesProvider resourcesProvider;
            private final long dialogId;

            public PageAdapter(Context context, int currentAccount, long dialogId, int classGuid, Theme.ResourcesProvider resourcesProvider) {
                this.context = context;
                this.currentAccount = currentAccount;
                this.classGuid = classGuid;
                this.resourcesProvider = resourcesProvider;
                this.dialogId = dialogId;
                fill();
            }

            private final ArrayList<UItem> items = new ArrayList<>();

            public void fill() {
                items.clear();
                if (!diamondsTransactions.isEmpty())
                    items.add(UItem.asSpace(STARS_TRANSACTIONS));
            }

            @Override
            public int getItemCount() {
                return items.size();
            }

            @Override
            public View createView(int viewType) {
                return new Page(context, dialogId, viewType, currentAccount, classGuid, () -> loadTransactions(viewType), resourcesProvider);
            }

            @Override
            public void bindView(View view, int position, int viewType) {}

            @Override
            public int getItemViewType(int position) {
                if (position < 0 || position >= items.size())
                    return STARS_TRANSACTIONS;
                return items.get(position).intValue;
            }

            @Override
            public String getItemTitle(int position) {
                final int viewType = getItemViewType(position);
                switch (viewType) {
                    case STARS_TRANSACTIONS: return getString(R.string.MonetizationTransactionsDiamonds);
                    default: return "";
                }
            }
        }

        public RecyclerListView getCurrentListView() {
            View currentView = viewPager.getCurrentView();
            if (!(currentView instanceof Page)) return null;
            return ((Page) currentView).listView;
        }

        public ChannelTransactionsView(Context context, int currentAccount, long dialogId, int classGuid, Runnable updateList, Theme.ResourcesProvider resourcesProvider) {
            super(context);
            this.currentAccount = currentAccount;
            this.dialogId = dialogId;
            this.updateParentList = updateList;

            setOrientation(VERTICAL);

            viewPager = new ViewPagerFixed(context);
            viewPager.setAdapter(adapter = new PageAdapter(context, currentAccount, dialogId, classGuid, resourcesProvider));
            tabsView = viewPager.createTabsView(true, 3);

            View separatorView = new View(context);
            separatorView.setBackgroundColor(Theme.getColor(Theme.key_divider, resourcesProvider));

            addView(tabsView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
            addView(separatorView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1f / AndroidUtilities.density));
            addView(viewPager, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            setBackgroundColor(Theme.getColor(Theme.key_dialogBackground, resourcesProvider));

            loadTransactions(STARS_TRANSACTIONS);
        }

        private void updateTabs() {
            adapter.fill();
            viewPager.fillTabs(false);
            viewPager.updateCurrent();
        }

        public void reloadTransactions() {
            final boolean hadTransactions = hasTransactions();
            if (loadingTransactions[STARS_TRANSACTIONS]) return;
            diamondsTransactions.clear();
            diamondsLastOffset = "";
            loadingTransactions[STARS_TRANSACTIONS] = false;
            loadTransactions(STARS_TRANSACTIONS);
            if (hasTransactions() != hadTransactions && updateParentList != null) {
                updateTabs();
                updateParentList.run();
            }
        }

        private void updateLists(boolean animated, boolean checkMore) {
            for (int i = 0; i < viewPager.getViewPages().length; ++i) {
                View page = viewPager.getViewPages()[i];
                if (page instanceof Page) {
                    ((Page) page).listView.adapter.update(animated);
                    if (checkMore) {
                        ((Page) page).checkMore();
                    }
                }
            }
        }

        public boolean hasTransactions() {
            return !diamondsTransactions.isEmpty();
        }
        public boolean hasTransactions(int type) {
            if (type == STARS_TRANSACTIONS) return !diamondsTransactions.isEmpty();
            return false;
        }

        private boolean[] loadingTransactions = new boolean[] { false };
        private void loadTransactions(int type) {
            if (loadingTransactions[type]) return;

            final boolean hadTransactions = hasTransactions();
            final boolean hadTheseTransactions = hasTransactions(type);
            if (type == STARS_TRANSACTIONS) {
                if (diamondsLastOffset == null || !diamondsRevenueAvailable)
                    return;
                loadingTransactions[type] = true;
                TL_diamonds.TL_payments_getDiamondsTransactions req = new TL_diamonds.TL_payments_getDiamondsTransactions();
                req.peer = MessagesController.getInstance(currentAccount).getInputPeer(dialogId);
                req.offset = diamondsLastOffset;
                req.limit = diamondsTransactions.isEmpty() ? 5 : 20;
                ConnectionsManager.getInstance(currentAccount).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                    if (res instanceof TL_diamonds.StarsStatus) {
                        TL_diamonds.StarsStatus r = (TL_diamonds.StarsStatus) res;
                        MessagesController.getInstance(currentAccount).putUsers(r.users, false);
                        MessagesController.getInstance(currentAccount).putChats(r.chats, false);
                        diamondsTransactions.addAll(r.history);
                        diamondsLastOffset = r.next_offset;
                        loadingTransactions[type] = false;
                        updateLists(true, true);
                    } else if (err != null) {
                        BulletinFactory.showError(err);
                    }
                    if (hasTransactions() != hadTransactions && updateParentList != null) {
                        updateParentList.run();
                    }
                    if (hasTransactions(type) != hadTheseTransactions) {
                        updateTabs();
                    }
                }));
            }
        }

        public class Page extends FrameLayout {

            private final UniversalRecyclerView listView;
            private final Theme.ResourcesProvider resourcesProvider;
            private final int currentAccount;
            private final int type;
            private final long bot_id;
            private final Runnable loadMore;

            public Page(Context context, long bot_id, int type, int currentAccount, int classGuid, Runnable loadMore, Theme.ResourcesProvider resourcesProvider) {
                super(context);

                this.type = type;
                this.currentAccount = currentAccount;
                this.bot_id = bot_id;
                this.resourcesProvider = resourcesProvider;
                this.loadMore = loadMore;

                listView = new UniversalRecyclerView(context, currentAccount, classGuid, true, this::fillItems, this::onClick, null, resourcesProvider);
                addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
                listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        if (!Page.this.listView.canScrollVertically(1) || isLoadingVisible()) {
                            loadMore.run();
                        }
                    }
                });
            }

            public void checkMore() {
                if (!Page.this.listView.canScrollVertically(1) || isLoadingVisible()) {
                    loadMore.run();
                }
            }

            public boolean isLoadingVisible() {
                for (int i = 0; i < listView.getChildCount(); ++i) {
                    if (listView.getChildAt(i) instanceof FlickerLoadingView)
                        return true;
                }
                return false;
            }

            @Override
            protected void onAttachedToWindow() {
                super.onAttachedToWindow();
                listView.adapter.update(false);
            }

            private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
                if (type == STARS_TRANSACTIONS) {
                    for (TL_diamonds.StarsTransaction t : diamondsTransactions) {
                        items.add(DiamondsIntroActivity.DiamondsTransactionView.Factory.asTransaction(t, true));
                    }
                    if (!TextUtils.isEmpty(diamondsLastOffset)) {
                        items.add(UItem.asFlicker(items.size(), FlickerLoadingView.DIALOG_CELL_TYPE));
                        items.add(UItem.asFlicker(items.size(), FlickerLoadingView.DIALOG_CELL_TYPE));
                        items.add(UItem.asFlicker(items.size(), FlickerLoadingView.DIALOG_CELL_TYPE));
                    }
                }
            }

            private void onClick(UItem item, View view, int position, float x, float y) {
                if (item.object instanceof TL_diamonds.StarsTransaction) {
                    DiamondsIntroActivity.showTransactionSheet(getContext(), true, dialogId, currentAccount, (TL_diamonds.StarsTransaction) item.object, resourcesProvider);
                }
            }

        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private NestedScrollingParentHelper nestedScrollingParentHelper = new NestedScrollingParentHelper(this);

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type, int[] consumed) {
        try {
            if (target == listView && transactionsLayout.isAttachedToWindow()) {
                RecyclerListView innerListView = transactionsLayout.getCurrentListView();
                int bottom = ((View) transactionsLayout.getParent()).getBottom();
                if (actionBar != null) {
                    actionBar.setCastShadows(!isAttachedToWindow() || listView.getHeight() - bottom < 0);
                }
                if (listView.getHeight() - bottom >= (listView.getPaddingBottom() + dp(8))) {
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
            boolean searchVisible = false;
            int t = ((View) transactionsLayout.getParent()).getTop() - AndroidUtilities.statusBarHeight - ActionBar.getCurrentActionBarHeight();
            int bottom = ((View) transactionsLayout.getParent()).getBottom();
            if (dy < 0) {
                boolean scrolledInner = false;
                if (actionBar != null) {
                    actionBar.setCastShadows(!isAttachedToWindow() || listView.getHeight() - bottom < 0);
                }
                if (listView.getHeight() - bottom >= (listView.getPaddingBottom() + dp(8))) {
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
                    if (listView.getHeight() - bottom >= (listView.getPaddingBottom() + dp(8)) && innerListView != null && !innerListView.canScrollVertically(1)) {
                        consumed[1] = dy;
                        listView.stopScroll();
                    }
                }
            }
        }
    }

    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes, int type) {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes, int type) {
        nestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
    }

    @Override
    public void onStopNestedScroll(@NonNull View target, int type) {
        nestedScrollingParentHelper.onStopNestedScroll(target);
    }

    @Override
    public void onStopNestedScroll(@NonNull View child) {

    }


}
