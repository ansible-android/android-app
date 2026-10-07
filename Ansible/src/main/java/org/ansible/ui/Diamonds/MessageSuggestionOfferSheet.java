package org.ansible.ui.Diamonds;

import static org.ansible.messenger.AndroidUtilities.dp;
import static org.ansible.messenger.LocaleController.getString;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.ansible.messenger.AndroidUtilities;
import org.ansible.messenger.AppGlobalConfig;
import org.ansible.messenger.BillingController;
import org.ansible.messenger.ChatObject;
import org.ansible.messenger.LocaleController;
import org.ansible.messenger.MessageSuggestionParams;
import org.ansible.messenger.MessagesController;
import org.ansible.messenger.R;
import org.ansible.messenger.Utilities;
import org.ansible.messenger.utils.tlutils.AmountUtils;
import org.ansible.ui.AccountFrozenAlert;
import org.ansible.ui.ActionBar.BottomSheet;
import org.ansible.ui.ActionBar.Theme;
import org.ansible.ui.ChatActivity;
import org.ansible.ui.Components.AlertsCreator;
import org.ansible.ui.Components.AnimatedTextView;
import org.ansible.ui.Components.ColoredImageSpan;
import org.ansible.ui.Components.EditTextBoldCursor;
import org.ansible.ui.Components.Forum.ForumUtilities;
import org.ansible.ui.Components.HorizontalRoundTabsLayout;
import org.ansible.ui.Components.LayoutHelper;
import org.ansible.ui.Components.OutlineTextContainerView;
import org.ansible.ui.Components.ScaleStateListAnimator;
import org.ansible.ui.Stories.recorder.ButtonWithCounterView;
import org.ansible.ui.TON.TONIntroActivity;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class MessageSuggestionOfferSheet extends BottomSheet {
    private final @Nullable BalanceCloud balanceCloud;
    private final boolean isMonoForumAdmin;
    private final int mode;

    private final @Nullable HorizontalRoundTabsLayout currencyTabsView;
    private final OutlineTextContainerView diamondsCountEditOutline;
    private final EditTextBoldCursor diamondsCountEditField;
    private final TextView diamondsCountEditHint;

    private final EditTextBoldCursor publishingTimeField;
    private final ButtonWithCounterView buttonView;
    private final AnimatedTextView dollarsEqView;
    private final ImageView iconDiamonds;
    private final ImageView iconTon;

    private final AmountUtils.Amount inputAmountMinDiamonds;
    private final AmountUtils.Amount inputAmountMaxDiamonds;

    private final AmountUtils.Amount inputAmountMinTON;
    private final AmountUtils.Amount inputAmountMaxTON;
    private AmountUtils.Amount inputAmount;

    private long selectedTime = -1;

    private static final int ERROR_FLAG_INCORRECT_INPUT = 1;
    private static final int ERROR_FLAG_AMOUNT_TOO_SMALL = 1 << 1;
    private static final int ERROR_FLAG_AMOUNT_TOO_BIG = 1 << 2;
    private static final int ERROR_FLAG_AMOUNT_NOT_ENOUGH = 1 << 3;

    private int inputAmountError;
    private boolean balanceCloudVisible;

    @Override
    protected boolean isTouchOutside(float x, float y) {
        if (balanceCloudVisible && balanceCloud != null && x >= balanceCloud.getX() && x <= balanceCloud.getX() + balanceCloud.getWidth() && y >= balanceCloud.getY() && y <= balanceCloud.getY() + balanceCloud.getHeight())
            return false;
        return super.isTouchOutside(x, y);
    }

    public static final int MODE_INPUT = 0;
    public static final int MODE_EDIT = 1;

    public MessageSuggestionOfferSheet(
        Context context,
        int currentAccount,
        long dialogId,
        MessageSuggestionParams startParams,
        ChatActivity chatActivity,
        Theme.ResourcesProvider resourcesProvider,
        int mode,
        Utilities.Callback<MessageSuggestionParams> callback
    ) {
        super(context, true, resourcesProvider);
        this.mode = mode;

        waitingKeyboard = true;
        smoothKeyboardAnimationEnabled = true;
        isMonoForumAdmin = ChatObject.canManageMonoForum(currentAccount, dialogId);
        boolean allowTON = isMonoForumAdmin || DiamondsController.getTonInstance(currentAccount).canUseTon();

       final AppGlobalConfig config = MessagesController.getInstance(currentAccount).config;
        inputAmountMinTON = AmountUtils.Amount.fromNano(
            config.tonSuggestedPostAmountMin.get(),
            AmountUtils.Currency.TON);
        inputAmountMaxTON = AmountUtils.Amount.fromNano(
            config.tonSuggestedPostAmountMax.get(),
            AmountUtils.Currency.TON);

        inputAmountMinDiamonds = AmountUtils.Amount.fromDecimal(
            config.diamondsSuggestedPostAmountMin.get(),
            AmountUtils.Currency.STARS);
        inputAmountMaxDiamonds = AmountUtils.Amount.fromDecimal(
            config.diamondsSuggestedPostAmountMax.get(),
            AmountUtils.Currency.STARS);

        if (!isMonoForumAdmin) {
            balanceCloud = new BalanceCloud(context, currentAccount, resourcesProvider);
            balanceCloud.setScaleX(0.6f);
            balanceCloud.setScaleY(0.6f);
            balanceCloud.setAlpha(0.0f);
            balanceCloud.setEnabled(false);
            balanceCloud.setClickable(false);
            container.addView(balanceCloud, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 48, 0, 0));
            ScaleStateListAnimator.apply(balanceCloud);
            balanceCloud.setOnClickListener(v -> {
                if (inputAmount.currency == AmountUtils.Currency.STARS) {
                    new DiamondsIntroActivity.DiamondsOptionsSheet(context, resourcesProvider).show();
                }
            });
        } else {
            balanceCloud = null;
        }

        fixNavigationBar(Theme.getColor(Theme.key_dialogBackground, resourcesProvider));

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);


        /* Header */

        LinearLayout headerLayout = new LinearLayout(context);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        layout.addView(headerLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56, Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 0, 0, 0));

        TextView titleView = new TextView(context);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        titleView.setText(getString(mode == MODE_INPUT ?
            R.string.PostSuggestionsOfferTitle:
            R.string.PostSuggestionsOfferChangeTitle
        ));
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        headerLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, 1f, Gravity.FILL, 18 + 4, 0, 18 + 4, 0));

        ImageView closeView = new ImageView(context);
        closeView.setScaleType(ImageView.ScaleType.CENTER);
        closeView.setImageResource(R.drawable.ic_close_white);
        closeView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_dialogEmptyImage, resourcesProvider), PorterDuff.Mode.SRC_IN));
        ScaleStateListAnimator.apply(closeView);
        closeView.setOnClickListener(v -> dismiss());
        headerLayout.addView(closeView, LayoutHelper.createLinear(48, 48, 0, Gravity.CENTER_VERTICAL | Gravity.RIGHT, 0, 0, 6, 0));

        diamondsCountEditField = new EditTextBoldCursor(context);

        /* Tabs */

        if (allowTON) {
            currencyTabsView = new HorizontalRoundTabsLayout(context, resourcesProvider);
            ArrayList<CharSequence> tabs = new ArrayList<>();
            tabs.add(getString(R.string.SuggestedOfferDiamonds));
            tabs.add(getString(R.string.SuggestedOfferTON));
            currencyTabsView.setTabs(tabs, x -> {
                final AmountUtils.Currency currency = x == 0 ?
                        AmountUtils.Currency.STARS :
                        AmountUtils.Currency.TON;

                setAmount(AmountUtils.Amount.fromNano(0, currency), true, false, true);
                diamondsCountEditField.setText("");
            });
            layout.addView(currencyTabsView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 18, 0, 18, 12));
        } else {
            currencyTabsView = null;
        }

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
            diamondsCountEditOutline.animateSelection(true, startParams.amount != null && !startParams.amount.isZero(), false);
            diamondsCountEditOutline.setForceUseCenter2(true);

            diamondsCountEditField.setOnFocusChangeListener((v, hasFocus) ->
                diamondsCountEditOutline.animateSelection(hasFocus, !TextUtils.isEmpty(diamondsCountEditField.getText())));

            diamondsCountEditOutline.addView(diamondsCountEditField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));
            bodyLayout.addView(diamondsCountEditOutline, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 58, 18, 0, 18, 0));

            iconDiamonds = new ImageView(context);
            iconDiamonds.setImageResource(R.drawable.diamond);
            diamondsCountEditOutline.addView(iconDiamonds, LayoutHelper.createFrame(22, 22, Gravity.LEFT | Gravity.CENTER_VERTICAL, 14, 0, 0, 0));

            iconTon = new ImageView(context);
            iconTon.setImageResource(R.drawable.mini_gram_72);
            iconTon.setColorFilter(0xFF3391d4);
            diamondsCountEditOutline.addView(iconTon, LayoutHelper.createFrame(22, 22, Gravity.LEFT | Gravity.CENTER_VERTICAL, 14, 0, 0, 0));

            dollarsEqView = new AnimatedTextView(context);
            dollarsEqView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            dollarsEqView.setTextSize(dp(13));
            dollarsEqView.setGravity(Gravity.RIGHT);
            diamondsCountEditOutline.addView(dollarsEqView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 16, 0));

            diamondsCountEditHint = new TextView(context);
            diamondsCountEditHint.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
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
            publishingTimeOutline.setText(getString(R.string.PostSuggestionsOfferTitleTime));
            publishingTimeOutline.attachEditText(publishingTimeField);
            publishingTimeOutline.addView(publishingTimeField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 0, 0, 48, 0));
            ScaleStateListAnimator.apply(publishingTimeOutline, .02f, 1.2f);
            publishingTimeOutline.setOnClickListener(v -> AlertsCreator.createSuggestedMessageDatePickerDialog(context, selectedTime, (notify, scheduleDate, scheduleRepeatPeriod) -> {
                if (notify) {
                    setSelectedTime(scheduleDate, true);
                }
            }, resourcesProvider, AlertsCreator.SUGGEST_DATE_PICKER_MODE_EDIT).show());
            bodyLayout.addView(publishingTimeOutline, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 58, 18, 24, 18, 0));

            ImageView iconArrow = new ImageView(context);
            iconArrow.setImageResource(R.drawable.arrow_more);
            iconArrow.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_dialogEmptyImage, resourcesProvider), PorterDuff.Mode.SRC_IN));
            publishingTimeOutline.addView(iconArrow, LayoutHelper.createFrame(24, 24, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 14, 0));

            TextView publishingTimeHint = new TextView(context);
            publishingTimeHint.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            publishingTimeHint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);

            SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append(AndroidUtilities.replaceTags(LocaleController.getString(R.string.PostSuggestionsAddTimeHint)));
            ssb.append(' ');
            ssb.append(AndroidUtilities.replaceTags(LocaleController.formatString(R.string.PostSuggestionsAddTimeHint2, MessagesController.getInstance(currentAccount).config.diamondsSuggestedPostAgeMin.get(TimeUnit.HOURS))));
            publishingTimeHint.setText(ssb);

            bodyLayout.addView(publishingTimeHint, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.FILL_HORIZONTAL, 33, 4, 33, 24));
        }



        /* Footer */

        LinearLayout footerLayout = new LinearLayout(context);
        footerLayout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(footerLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM));

        buttonView = new ButtonWithCounterView(context, resourcesProvider);
        buttonView.setOnClickListener(v -> {
            if (chatActivity == null || !buttonView.isEnabled()) {
                return;
            }
            if (MessagesController.getInstance(currentAccount).isFrozen()) {
                AccountFrozenAlert.show(currentAccount);
                return;
            }

            final DiamondsController diamondsController = DiamondsController.getInstance(currentAccount, inputAmount.currency);
            final AmountUtils.Amount balance = diamondsController.balanceAvailable() ?
                AmountUtils.Amount.of(diamondsController.getBalance()) : null;

            if (!isMonoForumAdmin && (balance == null || balance.asNano() < inputAmount.asNano())) {
                if (inputAmount.currency == AmountUtils.Currency.STARS) {
                    new DiamondsIntroActivity.DiamondsNeededSheet(context, resourcesProvider, inputAmount.asDecimal(), DiamondsIntroActivity.DiamondsNeededSheet.TYPE_PRIVATE_MESSAGE, ForumUtilities.getMonoForumTitle(currentAccount, dialogId, true), null, dialogId).show();
                } else if (inputAmount.currency == AmountUtils.Currency.TON){
                    new TONIntroActivity.DiamondsNeededSheet(context, resourcesProvider, inputAmount, true, null).show();
                }
            } else {
                callback.run(MessageSuggestionParams.of(inputAmount, selectedTime));
                dismiss();
            }
        });
        if (mode == MODE_EDIT) {
            buttonView.setText(getString(R.string.PostSuggestionsOfferChangeUpdateTerms), false);
        }
        footerLayout.addView(buttonView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 18, 0, 18, 8));

        if (startParams.amount != null) {
            setAmount(AmountUtils.Amount.fromNano(startParams.amount.asNano(), startParams.amount.currency), !startParams.amount.isZero(), true, false);
        } else {
            setAmount(AmountUtils.Amount.fromNano(0, AmountUtils.Currency.STARS), false, true, false);
        }
        setSelectedTime(startParams.time, false);
        setCustomView(layout);

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
    }

    private void setSelectedTime(long selectedTime, boolean animated) {
        if (this.selectedTime != selectedTime) {
            this.selectedTime = selectedTime;
            this.publishingTimeField.setText(formatDateTime(selectedTime));
        }
        checkButtonEnabled(animated);
    }

    private final ColoredImageSpan[] spanRefDiamonds = new ColoredImageSpan[1];
    private final ColoredImageSpan[] spanRefTon = new ColoredImageSpan[1];

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

        if (getInputAmountMax().asNano() < inputAmount.asNano()) {
            inputAmountError |= ERROR_FLAG_AMOUNT_TOO_BIG;
        }
        if (!inputAmount.isZero() && getInputAmountMin().asNano() > inputAmount.asNano()) {
            inputAmountError |= ERROR_FLAG_AMOUNT_TOO_SMALL;
        }

        /*if (!isMonoForumAdmin && !TONIntroActivity.allowTopUp() && inputAmount.currency == AmountUtils.Currency.TON) {
            if (StarsController.getTonInstance(currentAccount).balanceAvailable()) {
                if (inputAmount.asNano() > StarsController.getTonInstance(currentAccount).getBalanceAmount().asNano()) {
                    inputAmountError |= ERROR_FLAG_AMOUNT_NOT_ENOUGH;
                }
            }
        }*/




        final boolean currencyChanged = force || oldAmount.currency != inputAmount.currency;
        final boolean amountChanged = force || oldAmount.asNano() != inputAmount.asNano();
        final boolean amountErrorChanged = force || oldAmountError != inputAmountError;

        if (amountErrorChanged) {
            diamondsCountEditOutline.animateError((inputAmountError & (~ERROR_FLAG_AMOUNT_NOT_ENOUGH)) == 0 ? 0 : 1);
        }
        if (currencyChanged) {
            onCurrencyChanged(animated);
        }
        if (currencyChanged || amountErrorChanged) {
            checkAmountInputText(animated);
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
        if (currencyTabsView != null) {
            currencyTabsView.setSelectedIndex(inputAmount.currency == AmountUtils.Currency.STARS ? 0 : 1, animated);
        }
        if (inputAmount.currency == AmountUtils.Currency.STARS) {
            diamondsCountEditHint.setText(getString(R.string.PostSuggestionsOfferSubtitleDiamonds));
            diamondsCountEditField.setInputType(InputType.TYPE_CLASS_NUMBER);
            diamondsCountEditField.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(Long.toString(getInputAmountMax().asDecimal()).length())
            });
        } else if (inputAmount.currency == AmountUtils.Currency.TON) {
            diamondsCountEditHint.setText(getString(R.string.PostSuggestionsOfferSubtitleTON));
            diamondsCountEditField.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            diamondsCountEditField.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(Long.toString(getInputAmountMax().asDecimal()).length() + 3)
            });
        }

        if (animated) {
            iconDiamonds.animate()
                    .alpha(inputAmount.currency == AmountUtils.Currency.STARS ? 1f : 0f)
                    .scaleX(inputAmount.currency == AmountUtils.Currency.STARS ? 1f : 0f)
                    .scaleY(inputAmount.currency == AmountUtils.Currency.STARS ? 1f : 0f)
                    .setDuration(180L)
                    .start();
            iconTon.animate()
                    .alpha(inputAmount.currency == AmountUtils.Currency.TON ? 1f : 0f)
                    .scaleX(inputAmount.currency == AmountUtils.Currency.TON ? 1f : 0f)
                    .scaleY(inputAmount.currency == AmountUtils.Currency.TON ? 1f : 0f)
                    .setDuration(180L)
                    .start();
        } else {
            iconDiamonds.setAlpha(inputAmount.currency == AmountUtils.Currency.STARS ? 1f : 0f);
            iconTon.setAlpha(inputAmount.currency == AmountUtils.Currency.TON ? 1f : 0f);
        }

        if (balanceCloud != null) {
            balanceCloud.setCurrency(inputAmount.currency, animated);
        }
    }

    private boolean isFullyVisible;

    @Override
    public void onOpenAnimationEnd() {
        super.onOpenAnimationEnd();
        isFullyVisible = true;
        checkBalanceCloudVisibility();
    }

    @Override
    protected void onContainerTranslationYChanged(float translationY) {
        super.onContainerTranslationYChanged(translationY);
        checkBalanceCloudVisibility();
    }

    private void checkBalanceCloudVisibility() {
        final boolean balanceCloudVisible = isFullyVisible && !isDismissed() && balanceCloud != null && containerView.getY() > dp(32) || currencyTabsView == null;
        if (this.balanceCloudVisible != balanceCloudVisible)  {
            this.balanceCloudVisible = balanceCloudVisible;
            if (balanceCloud != null) {
                balanceCloud.setEnabled(balanceCloudVisible);
                balanceCloud.setClickable(balanceCloudVisible);
                balanceCloud.animate()
                    .scaleX(balanceCloudVisible ? 1f : 0.6f)
                    .scaleY(balanceCloudVisible ? 1f : 0.6f)
                    .alpha(balanceCloudVisible ? 1f : 0f)
                    .setDuration(180L)
                    .start();
            }
        }
    }

    private void checkButtonOfferText(boolean animated) {
        if (mode == MODE_INPUT) {
            if (!inputAmount.isZero()) {
                final boolean isTon = inputAmount.currency == AmountUtils.Currency.TON;
                buttonView.setText(DiamondsIntroActivity.replaceDiamonds(isTon,
                    LocaleController.formatString(R.string.PostSuggestionsOfferDiamonds, isTon ? inputAmount.asDecimalString() :
                        LocaleController.formatNumber(inputAmount.asDecimal(), ',')),
                    isTon ? spanRefTon: spanRefDiamonds
                ), animated);
            } else {
                buttonView.setText(LocaleController.getString(R.string.PostSuggestionsOfferForFree), animated);
            }
        } else {
            buttonView.setText(getString(R.string.PostSuggestionsOfferChangeUpdateTerms), animated);
        }
    }

    private void checkButtonEnabled(boolean animated) {
        final boolean newEnabled = inputAmountError == 0 && (inputAmount.asNano() >= 0 || selectedTime > 0);
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

    private void checkAmountInputText(boolean animated) {
        if ((inputAmountError & ERROR_FLAG_AMOUNT_TOO_BIG) != 0) {
            diamondsCountEditOutline.setText(LocaleController.formatString(R.string.SuggestAPostTooMuch, getInputAmountMax().formatAsDecimalSpaced()));
        } else if ((inputAmountError & ERROR_FLAG_AMOUNT_TOO_SMALL) != 0) {
            diamondsCountEditOutline.setText(LocaleController.formatString(R.string.SuggestAPostTooSmall, getInputAmountMin().formatAsDecimalSpaced()));
        } else {
            final int key = inputAmount.currency == AmountUtils.Currency.STARS ?
                R.string.PostSuggestionsOfferTitlePriceDiamonds:
                R.string.PostSuggestionsOfferTitlePriceTON;

            diamondsCountEditOutline.setText(getString(key));
        }
    }

    private void checkRateText(boolean animated) {
        final StringBuilder sb = new StringBuilder(10).append('~');

        final double rate = inputAmount.currency == AmountUtils.Currency.TON ?
                (MessagesController.getInstance(currentAccount).config.tonUsdRate.get()):
                (MessagesController.getInstance(currentAccount).diamondsUsdWithdrawRate1000 * 0.00001);

        sb.append(BillingController.getInstance().formatCurrency((long) (inputAmount.asDouble() * rate * 100), "USD", 2));

        dollarsEqView.setText(sb, animated);
    }

    private AmountUtils.Amount getInputAmountMin() {
        return inputAmount.currency == AmountUtils.Currency.TON ? inputAmountMinTON : inputAmountMinDiamonds;
    }

    private AmountUtils.Amount getInputAmountMax() {
        return inputAmount.currency == AmountUtils.Currency.TON ? inputAmountMaxTON : inputAmountMaxDiamonds;
    }

    @Override
    public void show() {
        super.show();
        AndroidUtilities.runOnUIThread(() -> AndroidUtilities.showKeyboard(diamondsCountEditField), 50);
    }

    public static String formatDateTime(long time) {
        if (time <= 0) {
            return LocaleController.getString(R.string.PostSuggestionsAnytime);
        } else {
            final String s = LocaleController.formatDateTime(time, true);
            if (!s.isEmpty())
                return Character.toUpperCase(s.charAt(0)) + s.substring(1);
            return s;
        }
    }
}
