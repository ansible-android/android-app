package org.ansible.ui.Diamonds;

import static org.ansible.messenger.AndroidUtilities.dp;
import static org.ansible.messenger.AndroidUtilities.formatSpannable;
import static org.ansible.messenger.AndroidUtilities.replaceTags;
import static org.ansible.messenger.LocaleController.formatNumber;
import static org.ansible.messenger.LocaleController.formatPluralString;
import static org.ansible.messenger.LocaleController.formatString;
import static org.ansible.messenger.LocaleController.getString;
import static org.ansible.ui.Diamonds.DiamondGiftSheet.addAttributeRow;
import static org.ansible.ui.Diamonds.DiamondsController.findAttribute;
import static org.ansible.ui.Diamonds.DiamondsIntroActivity.replaceDiamonds;
import static org.ansible.ui.Diamonds.DiamondsIntroActivity.replaceDiamondsWithPlain;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.ansible.messenger.AndroidUtilities;
import org.ansible.messenger.AppGlobalConfig;
import org.ansible.messenger.BillingController;
import org.ansible.messenger.DialogObject;
import org.ansible.messenger.LocaleController;
import org.ansible.messenger.MessagesController;
import org.ansible.messenger.R;
import org.ansible.messenger.SendMessagesHelper;
import org.ansible.messenger.Utilities;
import org.ansible.messenger.browser.Browser;
import org.ansible.messenger.utils.tlutils.AmountUtils;
import org.ansible.asnet.ConnectionsManager;
import org.ansible.asnet.TLObject;
import org.ansible.asnet.TLRPC;
import org.ansible.asnet.tl.TL_payments;
import org.ansible.asnet.tl.TL_diamonds;
import org.ansible.ui.AccountFrozenAlert;
import org.ansible.ui.ActionBar.AlertDialog;
import org.ansible.ui.ActionBar.BaseFragment;
import org.ansible.ui.ActionBar.Theme;
import org.ansible.ui.ChatActivity;
import org.ansible.ui.Components.AlertsCreator;
import org.ansible.ui.Components.AnimatedTextView;
import org.ansible.ui.Components.BottomSheetWithRecyclerListView;
import org.ansible.ui.Components.BulletinFactory;
import org.ansible.ui.Components.ButtonSpan;
import org.ansible.ui.Components.ColoredImageSpan;
import org.ansible.ui.Components.EditTextBoldCursor;
import org.ansible.ui.Components.Forum.ForumUtilities;
import org.ansible.ui.Components.LayoutHelper;
import org.ansible.ui.Components.OutlineTextContainerView;
import org.ansible.ui.Components.RecyclerListView;
import org.ansible.ui.Components.ScaleStateListAnimator;
import org.ansible.ui.Components.TableView;
import org.ansible.ui.Components.UItem;
import org.ansible.ui.Components.UniversalAdapter;
import org.ansible.ui.LaunchActivity;
import org.ansible.ui.Stories.recorder.ButtonWithCounterView;
import org.ansible.ui.Stories.recorder.HintView2;

import java.util.ArrayList;

public class GiftOfferSheet extends BottomSheetWithRecyclerListView {
    private final @Nullable BalanceCloud balanceCloud;
    private final TL_diamonds.TL_starGiftUnique giftUnique;
    private final String giftName;
    private final long dialogId;

    private final OutlineTextContainerView diamondsCountEditOutline;
    private final EditTextBoldCursor diamondsCountEditField;
    private final TextView diamondsCountEditHint;

    private final EditTextBoldCursor publishingTimeField;
    private final TextView publishingTimeHint;
    private final ButtonWithCounterView buttonView;
    private final AnimatedTextView dollarsEqView;
    private final ImageView iconDiamonds;

    private final AmountUtils.AmountLimits inputAmountLimits = new AmountUtils.AmountLimits();
    private AmountUtils.Amount inputAmount;

    private int selectedDuration;

    private static final int ERROR_FLAG_INCORRECT_INPUT = 1;
    private static final int ERROR_FLAG_AMOUNT_TOO_SMALL = 1 << 1;
    private static final int ERROR_FLAG_AMOUNT_TOO_BIG = 1 << 2;
    private static final int ERROR_FLAG_AMOUNT_NOT_ENOUGH = 1 << 3;

    private static final int[] ALLOWED_DURATIONS = { /*120,*/ 21600, 43200, 86400, 129600, 172800, 259200 };

    private int inputAmountError;
    private boolean balanceCloudVisible;

    @Override
    protected boolean isTouchOutside(float x, float y) {
        if (balanceCloudVisible && balanceCloud != null && x >= balanceCloud.getX() && x <= balanceCloud.getX() + balanceCloud.getWidth() && y >= balanceCloud.getY() && y <= balanceCloud.getY() + balanceCloud.getHeight())
            return false;
        return super.isTouchOutside(x, y);
    }

    private final Runnable closeParentSheet;

    public GiftOfferSheet(
        Context context,
        int currentAccount,
        long dialogId,
        TL_diamonds.TL_starGiftUnique giftUnique,
        Theme.ResourcesProvider resourcesProvider,
        Runnable closeParentSheet
    ) {
        super(context, null, true, false,
            false, false, ActionBarType.SLIDING, resourcesProvider);
        ignoreTouchActionBar = false;
        headerMoveTop = dp(12);
        topPadding = 0.2f;

        this.dialogId = dialogId;
        this.giftUnique = giftUnique;
        this.giftName = giftUnique.title + " #" + LocaleController.formatNumber(giftUnique.num, ',');
        this.closeParentSheet = closeParentSheet;

        waitingKeyboard = true;
        smoothKeyboardAnimationEnabled = true;

        if (dialogId > 0) {
            final TLRPC.UserFull userFull = MessagesController.getInstance(currentAccount).getUserFull(dialogId);
            if (userFull == null) {
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialogId);
                if (user != null) {
                    MessagesController.getInstance(currentAccount).loadFullUser(user, 0, false);
                }
            }
        }

        final AppGlobalConfig config = MessagesController.getInstance(currentAccount).config;

        final AmountUtils.Amount minOfferDiamonds = AmountUtils.Amount.fromDecimal(giftUnique.offer_min_stars, AmountUtils.Currency.STARS);
        final AmountUtils.Amount maxOfferDiamonds = AmountUtils.Amount.fromDecimal(Math.max(
            minOfferDiamonds.asDecimal() * 2,
            config.diamondsDiamondGiftResaleAmountMax.get()
        ), AmountUtils.Currency.STARS);

        inputAmountLimits.set(minOfferDiamonds, maxOfferDiamonds);

        balanceCloud = new BalanceCloud(context, currentAccount, resourcesProvider);
        balanceCloud.setScaleX(0.6f);
        balanceCloud.setScaleY(0.6f);
        balanceCloud.setAlpha(0.0f);
        balanceCloud.setEnabled(false);
        balanceCloud.setClickable(false);
        container.addView(balanceCloud, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 48, 0, 0));
        ScaleStateListAnimator.apply(balanceCloud);
        balanceCloud.setOnClickListener(v -> {
            new DiamondsIntroActivity.DiamondsOptionsSheet(context, resourcesProvider).show();
        });

        fixNavigationBar(Theme.getColor(Theme.key_dialogBackground, resourcesProvider));

        LinearLayout layout = new LinearLayout(context);
        layout.setClickable(true);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, dp(12), 0, dp(16));



        diamondsCountEditField = new EditTextBoldCursor(context);

        /* Body */

        LinearLayout bodyLayout = new LinearLayout(context);
        bodyLayout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(bodyLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 1f));



        {
            diamondsCountEditOutline = new OutlineTextContainerView(context);
            diamondsCountEditField.setCursorSize(dp(20));
            diamondsCountEditField.setCursorWidth(1.5f);
            diamondsCountEditField.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
            diamondsCountEditField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
            diamondsCountEditField.setMaxLines(1);
            diamondsCountEditField.setBackground(null);
            diamondsCountEditField.setPadding(dp(42), dp(16), dp(16), dp(16));
            diamondsCountEditField.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            diamondsCountEditField.requestFocus();

            diamondsCountEditOutline.setLeftPadding(dp(28));
            diamondsCountEditOutline.attachEditText(diamondsCountEditField);
            diamondsCountEditOutline.animateSelection(true, false, false);
            diamondsCountEditOutline.setForceUseCenter2(true);

            diamondsCountEditField.setOnFocusChangeListener((v, hasFocus) ->
                diamondsCountEditOutline.animateSelection(hasFocus, !TextUtils.isEmpty(diamondsCountEditField.getText())));

            diamondsCountEditOutline.addView(diamondsCountEditField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));
            bodyLayout.addView(diamondsCountEditOutline, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 58, 18, 0, 18, 0));

            iconDiamonds = new ImageView(context);
            iconDiamonds.setImageResource(R.drawable.diamond);
            diamondsCountEditOutline.addView(iconDiamonds, LayoutHelper.createFrame(22, 22, Gravity.LEFT | Gravity.CENTER_VERTICAL, 14, 0, 0, 0));

            dollarsEqView = new AnimatedTextView(context);
            dollarsEqView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            dollarsEqView.setTextSize(dp(13));
            dollarsEqView.setGravity(Gravity.RIGHT);
            diamondsCountEditOutline.addView(dollarsEqView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 16, 0));

            diamondsCountEditHint = new TextView(context);
            diamondsCountEditHint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            bodyLayout.addView(diamondsCountEditHint, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.FILL_HORIZONTAL, 33, 4, 33, 0));
        }
        {
            publishingTimeField = new EditTextBoldCursor(context) {
                @Override
                public boolean dispatchTouchEvent(MotionEvent event) {
                    return false;
                }
            };
            publishingTimeField.setCursorSize(dp(20));
            publishingTimeField.setCursorWidth(1.5f);
            publishingTimeField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
            publishingTimeField.setMaxLines(1);
            publishingTimeField.setBackground(null);
            publishingTimeField.setPadding(dp(16), dp(16), dp(16), dp(16));
            publishingTimeField.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            publishingTimeField.setFocusable(false);
            publishingTimeField.setClickable(false);
            publishingTimeField.setEnabled(false);

            OutlineTextContainerView publishingTimeOutline = new OutlineTextContainerView(context);
            publishingTimeOutline.setText(getString(R.string.GiftOfferDuration));
            publishingTimeOutline.attachEditText(publishingTimeField);
            publishingTimeOutline.addView(publishingTimeField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 0, 0, 48, 0));
            ScaleStateListAnimator.apply(publishingTimeOutline, .02f, 1.2f);
            publishingTimeOutline.setOnClickListener(v -> {
                int index = 0;
                String[] positions = new String[ALLOWED_DURATIONS.length];
                for (int a = 0; a < ALLOWED_DURATIONS.length; a++) {
                    positions[a] = formatPluralString("GiftOfferHours", ALLOWED_DURATIONS[a] / 3600);
                    if (ALLOWED_DURATIONS[a] == selectedDuration) {
                        index = a;
                    }
                }

                AlertsCreator.createCustomPicker(context, getString(R.string.GiftOfferDuration), index, positions, s -> {
                    setSelectedDuration(ALLOWED_DURATIONS[s], true);
                });
            });
            bodyLayout.addView(publishingTimeOutline, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 58, 18, 18, 18, 0));

            ImageView iconArrow = new ImageView(context);
            iconArrow.setImageResource(R.drawable.arrow_more);
            iconArrow.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_dialogEmptyImage, resourcesProvider), PorterDuff.Mode.SRC_IN));
            publishingTimeOutline.addView(iconArrow, LayoutHelper.createFrame(24, 24, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 14, 0));

            publishingTimeHint = new TextView(context);
            publishingTimeHint.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            publishingTimeHint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);

            bodyLayout.addView(publishingTimeHint, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.FILL_HORIZONTAL, 33, 4, 33, 0));
        }



        /* Footer */

        buttonView = new ButtonWithCounterView(context, resourcesProvider);
        buttonView.setOnClickListener(v -> {
            if (!buttonView.isEnabled()) {
                return;
            }
            if (MessagesController.getInstance(currentAccount).isFrozen()) {
                AccountFrozenAlert.show(currentAccount);
                return;
            }

            final DiamondsController diamondsController = DiamondsController.getInstance(currentAccount);
            final AmountUtils.Amount balance = diamondsController.balanceAvailable() ?
                AmountUtils.Amount.of(diamondsController.getBalance()) : null;

            if ((balance == null || balance.asNano() < inputAmount.asNano())) {
                new DiamondsIntroActivity.DiamondsNeededSheet(context, resourcesProvider, inputAmount.asDecimal(), DiamondsIntroActivity.DiamondsNeededSheet.TYPE_STAR_GIFT_BUY_RESALE, null, null, dialogId).show();
            } else {
                openConfirmAlert();
            }
        });

        setAmount(AmountUtils.Amount.fromNano(0, AmountUtils.Currency.STARS), false, true, false);
        setSelectedDuration(86400, false);

        diamondsCountEditField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                final boolean isEmpty = s == null || s.toString().isEmpty() || ".".equals(s.toString());

                if (!isEmpty) {
                    String str = s.toString();

                    int dotIndex = str.indexOf('.');
                    if (dotIndex >= 0) {
                        int decimals = str.length() - dotIndex - 1;
                        if (decimals > 2) {
                            s.delete(dotIndex + 2 + 1, str.length());
                        }
                    }
                }

                final AmountUtils.Amount newAmount = !isEmpty ?
                        AmountUtils.Amount.fromDecimal(s.toString(), inputAmount.currency):
                        AmountUtils.Amount.fromNano(0, inputAmount.currency);

                setAmount(newAmount, false, false, true);
                diamondsCountEditOutline.animateSelection(diamondsCountEditField.isFocused(), !TextUtils.isEmpty(diamondsCountEditField.getText()));
            }
        });



        FrameLayout.LayoutParams lp = LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM, 16, 16, 16, 16);
        lp.leftMargin += backgroundPaddingLeft;
        lp.rightMargin += backgroundPaddingLeft;
        containerView.addView(buttonView, lp);

        recyclerListView.setPadding(backgroundPaddingLeft, 0, backgroundPaddingLeft, dp(16 + 48));
        recyclerListView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        mainItem = UItem.asCustom(layout);
        adapter.update(false);
    }

    private void setSelectedDuration(int selectedDuration, boolean animated) {
        if (this.selectedDuration != selectedDuration) {
            this.selectedDuration = selectedDuration;
            this.publishingTimeField.setText(formatPluralString("GiftOfferHours", selectedDuration / 3600));
        }
        checkButtonEnabled(animated);
    }

    private final ColoredImageSpan[] spanRefDiamonds = new ColoredImageSpan[1];

    private void setAmount(@Nullable AmountUtils.Amount amount, boolean updateEditField, boolean force, boolean animated) {
        AmountUtils.Amount oldAmount = inputAmount;
        int oldAmountError = inputAmountError;

        inputAmountError = 0;
        if (amount != null) {
            inputAmount = amount;
        } else {
            inputAmount = AmountUtils.Amount.fromNano(0, inputAmount.currency);
            inputAmountError |= ERROR_FLAG_INCORRECT_INPUT;
        }

        if (inputAmountLimits.getMax(inputAmount.currency).asNano() < inputAmount.asNano()) {
            inputAmountError |= ERROR_FLAG_AMOUNT_TOO_BIG;
        }
        if (!inputAmount.isZero() && inputAmountLimits.getMin(inputAmount.currency).asNano() > inputAmount.asNano()) {
            inputAmountError |= ERROR_FLAG_AMOUNT_TOO_SMALL;
        }

        final boolean currencyChanged = force || oldAmount.currency != inputAmount.currency;
        final boolean amountChanged = force || oldAmount.asNano() != inputAmount.asNano();
        final boolean amountErrorChanged = force || oldAmountError != inputAmountError;

        if (currencyChanged) {
            onCurrencyChanged(animated);
        }
        if (currencyChanged || amountErrorChanged) {
            checkAmountInputText(animated);
            checkAmountInputTextHint(animated);
        }
        if (currencyChanged || amountChanged || amountErrorChanged) {
            checkButtonOfferText(animated);
            checkButtonEnabled(animated);
        }
        if (currencyChanged || amountChanged) {
            checkRateText(animated);
        }

        if (updateEditField && amountChanged) {
            String textToSet = inputAmount.asDecimalString();
            diamondsCountEditField.setText(textToSet);
            diamondsCountEditField.setSelection(textToSet.length());
        }
    }

    private void onCurrencyChanged(boolean animated) {
        final String userName = DialogObject.getShortName(dialogId);
        publishingTimeHint.setText(replaceTags(formatString(R.string.GiftOfferDurationInfoDiamonds, userName)));

        diamondsCountEditField.setInputType(InputType.TYPE_CLASS_NUMBER);
        diamondsCountEditField.setFilters(new InputFilter[]{
            new InputFilter.LengthFilter(Long.toString(inputAmountLimits.getMax(inputAmount.currency).asDecimal()).length())
        });

        if (animated) {
            iconDiamonds.animate()
                    .alpha(inputAmount.currency == AmountUtils.Currency.STARS ? 1f : 0f)
                    .scaleX(inputAmount.currency == AmountUtils.Currency.STARS ? 1f : 0f)
                    .scaleY(inputAmount.currency == AmountUtils.Currency.STARS ? 1f : 0f)
                    .setDuration(180L)
                    .start();
        } else {
            iconDiamonds.setAlpha(inputAmount.currency == AmountUtils.Currency.STARS ? 1f : 0f);
        }
    }

    @Override
    public void onOpenAnimationEnd() {
        super.onOpenAnimationEnd();
        checkBalanceCloudVisibility();
    }

    @Override
    protected void onContainerTranslationYChanged(float translationY) {
        super.onContainerTranslationYChanged(translationY);
        checkBalanceCloudVisibility();
    }

    private void checkBalanceCloudVisibility() {
        if (!balanceCloudVisible) {
            balanceCloudVisible = true;
            if (balanceCloud != null) {
                balanceCloud.setEnabled(true);
                balanceCloud.setClickable(true);
                balanceCloud.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(180L)
                    .start();
            }
        }
    }

    private void checkButtonOfferText(boolean animated) {
        buttonView.setText(DiamondsIntroActivity.replaceDiamonds(
            LocaleController.formatString(R.string.GiftOfferButtonDiamonds,
                LocaleController.formatNumber(inputAmount.asDecimal(), ',')),
            spanRefDiamonds
        ), animated);
    }

    private void checkButtonEnabled(boolean animated) {
        final boolean newEnabled = inputAmountError == 0 && inputAmount.asNano() > 0;
        if (buttonView.isEnabled() != newEnabled) {
            this.buttonView.setEnabled(newEnabled);
            this.buttonView.setClickable(newEnabled);
            if (animated) {
                this.buttonView.animate().alpha(newEnabled ? 1f : 0.6f).setDuration(180L).start();
            } else {
                this.buttonView.setAlpha(newEnabled ? 1f : 0.6f);
            }
        }
    }

    private void checkAmountInputText(boolean ignoredAnimated) {
        diamondsCountEditOutline.setText(getString(R.string.GiftOfferDiamondsToOffer));
    }

    private void checkAmountInputTextHint(boolean ignoredAnimated) {
        final AmountUtils.Currency currency = inputAmount.currency;

        if ((inputAmountError & ERROR_FLAG_AMOUNT_TOO_BIG) != 0) {
            diamondsCountEditHint.setText(replaceTags(formatString(R.string.GiftOfferDiamondsToOfferInfoIsHigh,
                inputAmountLimits.getMax(currency).asFormatString(), giftName)));
        } else if ((inputAmountError & ERROR_FLAG_AMOUNT_TOO_SMALL) != 0) {
            diamondsCountEditHint.setText(replaceTags(formatString(R.string.GiftOfferDiamondsToOfferInfoIsLow,
                inputAmountLimits.getMin(currency).asFormatString(), giftName)));
        } else {
            diamondsCountEditHint.setText(replaceTags(formatString(R.string.GiftOfferDiamondsToOfferInfo, giftName)));
        }

        diamondsCountEditHint.setTextColor(getThemedColor((inputAmountError & (~ERROR_FLAG_AMOUNT_NOT_ENOUGH)) == 0 ?
            Theme.key_windowBackgroundWhiteGrayText : Theme.key_text_RedBold));
    }

    private void checkRateText(boolean animated) {
        final StringBuilder sb = new StringBuilder(10).append('~');

        final double rate = MessagesController.getInstance(currentAccount).diamondsUsdWithdrawRate1000 * 0.00001;

        sb.append(BillingController.getInstance().formatCurrency((long) (inputAmount.asDouble() * rate * 100), "USD", 2));

        dollarsEqView.setText(sb, animated);
    }

    @Override
    public void show() {
        super.show();
        AndroidUtilities.runOnUIThread(() -> AndroidUtilities.showKeyboard(diamondsCountEditField), 50);
    }

    /* * */

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.GiftOfferToBuyTitle);
    }

    private UniversalAdapter adapter;
    private final UItem mainItem;

    @Override
    protected RecyclerListView.SelectionAdapter createAdapter(RecyclerListView listView) {
        adapter = new UniversalAdapter(recyclerListView, getContext(), currentAccount, 0, true, this::fillItems, resourcesProvider);
        adapter.setApplyBackground(false);
        return adapter;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (mainItem != null) {
            items.add(mainItem);
        }
    }



    private void openConfirmAlert() {
        final String amountFmt = inputAmount.asFormatString();
        // final String amountFmtFee = getFee().asFormatString();
        // final String amountFmtFull = getFullOfferWithFee().asFormatString();

        final LinearLayout topView = new LinearLayout(getContext());
        topView.setOrientation(LinearLayout.VERTICAL);

        final TextView titleView = new TextView(getContext());
        titleView.setText(getString(R.string.GiftOfferConfirmSend));
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleView.setTypeface(AndroidUtilities.bold());
        topView.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 24, 4, 24, 14));

        final TextView textView = new TextView(getContext());
        textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView.setText(AndroidUtilities.replaceTags(
            formatString(R.string.GiftOfferTransferInfoTextDiamonds, amountFmt, DialogObject.getShortName(dialogId), giftName)
        ));
        topView.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 24, 4, 24, 4));
        final TableView tableView = new TableView(getContext(), resourcesProvider);

        final long paywall = MessagesController.getInstance(currentAccount).getSendPaidMessagesDiamonds(dialogId);
        final AmountUtils.Amount paywallAmount = AmountUtils.Amount.fromDecimal(paywall, AmountUtils.Currency.STARS);

        tableView.addRow(
            getString(R.string.GiftOfferRowOffer),
            replaceDiamondsWithPlain(formatString(R.string.GiftOfferAmount, amountFmt), 0.8f));
        if (paywall > 0) {
            tableView.addRow(getString(R.string.GiftOfferRowFee),
                replaceDiamondsWithPlain(formatString(R.string.GiftOfferAmount, paywallAmount.asFormatString()), 0.8f));
        }
        tableView.addRow(
            getString(R.string.GiftOfferRowDuration),
            formatPluralString("GiftOfferHours", selectedDuration / 3600));
        topView.addView(tableView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 23, 16, 23, 4));

        final long randomId = SendMessagesHelper.getInstance(currentAccount).getNextRandomId();

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        if (paywall == 0) {
            ssb.append(replaceDiamonds(formatString(R.string.GiftOfferPay, amountFmt)));
        } else {
            String fmt = AmountUtils.Amount.fromNano(inputAmount.asNano() + paywallAmount.asNano(), AmountUtils.Currency.STARS).asFormatString();
            ssb.append(replaceDiamonds(formatString(R.string.GiftOfferPay, fmt)));
        }

        new AlertDialog.Builder(getContext(), resourcesProvider)
            .setView(topView)
            .setPositiveButton(ssb, (di, w) -> {
                if (paywall > 0) {
                    final DiamondsController diamondsController = DiamondsController.getInstance(currentAccount);
                    final AmountUtils.Amount balance = diamondsController.balanceAvailable() ? AmountUtils.Amount.of(diamondsController.getBalance()) : null;
                    final AmountUtils.Amount needed = AmountUtils.Amount.fromNano(inputAmount.asNano() + paywallAmount.asNano(), AmountUtils.Currency.STARS);
                    if ((balance == null || balance.asNano() < needed.asNano())) {
                        new DiamondsIntroActivity.DiamondsNeededSheet(getContext(), resourcesProvider, needed.asDecimal(), DiamondsIntroActivity.DiamondsNeededSheet.TYPE_STAR_GIFT_BUY_RESALE, null, null, dialogId).show();
                        return;
                    }
                }


                final Browser.Progress progress = di.makeButtonLoading(AlertDialog.BUTTON_POSITIVE);
                progress.init();

                TL_payments.TL_sendDiamondGiftOffer req = new TL_payments.TL_sendDiamondGiftOffer();
                req.price = inputAmount.toTl();
                req.peer = MessagesController.getInstance(currentAccount).getInputPeer(dialogId);
                req.duration = selectedDuration;
                req.slug = giftUnique.slug;
                req.random_id = randomId;
                if (paywall > 0) {
                    req.flags |= TLObject.FLAG_0;
                    req.allow_paid_stars = paywall;
                }

                ConnectionsManager.getInstance(currentAccount).sendRequestTyped(req, (res, err) -> {
                    if (res != null && err == null) {
                        MessagesController.getInstance(currentAccount).processUpdates(res, false);
                    }
                    AndroidUtilities.runOnUIThread(() -> {
                        if (closeParentSheet != null) {
                            closeParentSheet.run();
                        }
                        progress.end();
                        di.dismiss();
                        dismiss();

                        BaseFragment lastFragment = LaunchActivity.getSafeLastFragment();
                        if (lastFragment != null) {
                            if (res != null) {
                                BulletinFactory.of(lastFragment)
                                        .createSimpleBulletin(R.raw.forward, getString(R.string.GiftOfferSentTitle), AndroidUtilities.replaceTags(formatString(R.string.GiftOfferSentText, giftName, DialogObject.getShortName(dialogId))))
                                        .ignoreDetach()
                                        .show();
                            } else {
                                BulletinFactory.of(lastFragment).showForError(err);
                            }
                        }
                    });
                });
            })
            .setNegativeButton(getString(R.string.Cancel), null)
            .create()
            .setShowDiamondsBalance(true)
            .show();
    }

    public static void openOfferAcceptAlert(BaseFragment fragment, Context context, Theme.ResourcesProvider resourcesProvider, int currentAccount, long dialogId, int msgId, TLRPC.TL_messageActionDiamondGiftPurchaseOffer offer) {
        final AmountUtils.Amount amount = AmountUtils.Amount.ofSafe(offer.price);
        final AmountUtils.Amount amountWithFee = getAmountMinusFee(currentAccount, amount);

        final TL_diamonds.StarGift gift = offer.gift;
        final String giftName = gift.title + " #" + LocaleController.formatNumber(gift.num, ',');

        final TLObject obj;
        if (dialogId >= 0) {
            obj = MessagesController.getInstance(currentAccount).getUser(dialogId);
        } else {
            obj = MessagesController.getInstance(currentAccount).getChat(-dialogId);
        }


        final String amountFmt = amount.asFormatString();
        final String amountMinusFeeFmt = amountWithFee.asFormatString();

        final LinearLayout topView = new LinearLayout(context);
        topView.setOrientation(LinearLayout.VERTICAL);
        topView.addView(new DiamondGiftSheet.GiftTransferTopView(context, gift, obj), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 0, -4, 0, 0));

        final TextView textView = new TextView(context);
        textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView.setText(AndroidUtilities.replaceTags(
                formatString(R.string.GiftOfferTransferInfoTextSellDiamonds, amountFmt, DialogObject.getShortName(dialogId), giftName, amountMinusFeeFmt)
        ));
        topView.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 24, 4, 24, 4));

        final FrameLayout tableLayout = new FrameLayout(context);
        tableLayout.setClipChildren(false);
        tableLayout.setClipToPadding(false);
        final TableView tableView = new TableView(context, resourcesProvider);
        tableLayout.addView(tableView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL));


        /**/

        addAttributeRow(tableView, findAttribute(gift.attributes, TL_diamonds.starGiftAttributeModel.class));
        addAttributeRow(tableView, findAttribute(gift.attributes, TL_diamonds.starGiftAttributeBackdrop.class));
        addAttributeRow(tableView, findAttribute(gift.attributes, TL_diamonds.starGiftAttributePattern.class));

        topView.addView(tableLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 23, 16, 23, 4));

        final double exp = Math.pow(10, BillingController.getInstance().getCurrencyExp("USD"));
        final double usd = gift.value_usd_amount / exp;
        final AmountUtils.Amount value = AmountUtils.Amount.fromUsd(usd, amount.currency);
        if (value.asDouble() > 0 && gift.value_usd_amount > 0) {
            final CharSequence buttonHint;
            final int percent;
            final boolean badBrice;
            if (value.asNano() >= amountWithFee.asNano()) {
                percent = (int) Math.round((1 - amountWithFee.asDouble() / value.asDouble()) * 100);
                buttonHint = replaceTags(formatString(R.string.GiftOfferAmountLowerHint2, percent + "%", gift.title));
                badBrice = percent > 10;
            } else {
                percent = (int) Math.round((amountWithFee.asDouble() / value.asDouble() - 1) * 100);
                buttonHint = replaceTags(formatString(R.string.GiftOfferAmountHigherHint2, percent + "%", gift.title));
                badBrice = false;
            }

            final TextView hintView = new TextView(context);
            hintView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            hintView.setGravity(Gravity.CENTER);
            hintView.setText(buttonHint);
            hintView.setTextColor(Theme.getColor(badBrice ? Theme.key_text_RedRegular : Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
            topView.addView(hintView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 40, 12, 40, 9));
        }







        new AlertDialog.Builder(context, resourcesProvider)
                .setView(topView)
                .setPositiveButton(replaceDiamonds(formatString(R.string.GiftOfferSellFor, amountMinusFeeFmt)), (di, w) -> {
                    final Browser.Progress progress = di.makeButtonLoading(AlertDialog.BUTTON_POSITIVE);
                    progress.init();

                    TL_payments.TL_resolveDiamondGiftOffer req = new TL_payments.TL_resolveDiamondGiftOffer();
                    req.offer_msg_id = msgId;

                    ConnectionsManager.getInstance(currentAccount).sendRequestTyped(req, (res, err) -> {
                        if (res != null && err == null) {
                            MessagesController.getInstance(currentAccount).processUpdates(res, false);
                        }
                        AndroidUtilities.runOnUIThread(() -> {
                            if (fragment != null && err != null) {
                                BulletinFactory.of(fragment).showForError(err);
                            }
                            if (fragment instanceof ChatActivity && err == null) {
                                ((ChatActivity) fragment).startFireworks();
                            }

                            progress.end();
                            di.dismiss();
                        });
                    });
                })
                .setNegativeButton(getString(R.string.Cancel), null)
                .create()
                .show();
    }

    private static AmountUtils.Amount getAmountMinusFee(int currentAccount, AmountUtils.Amount amount) {
        final AmountUtils.Currency currency = amount.currency;
        final int permille = MessagesController.getInstance(currentAccount).config.diamondsDiamondGiftResaleCommissionPermille.get();

        return AmountUtils.Amount.fromNano(amount.asNano() * permille / 1000, currency);
    }
}
