package org.ansible.ui.Diamonds;

import static org.ansible.messenger.AndroidUtilities.dp;
import static org.ansible.messenger.LocaleController.getString;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.ansible.messenger.AndroidUtilities;
import org.ansible.messenger.AppGlobalConfig;
import org.ansible.messenger.BillingController;
import org.ansible.messenger.LocaleController;
import org.ansible.messenger.MessagesController;
import org.ansible.messenger.R;
import org.ansible.messenger.Utilities;
import org.ansible.messenger.utils.tlutils.AmountUtils;
import org.ansible.ui.ActionBar.BottomSheet;
import org.ansible.ui.ActionBar.Theme;
import org.ansible.ui.Components.AnimatedTextView;
import org.ansible.ui.Components.EditTextBoldCursor;
import org.ansible.ui.Components.LayoutHelper;
import org.ansible.ui.Components.OutlineTextContainerView;
import org.ansible.ui.Stories.recorder.ButtonWithCounterView;

public class SellGiftEnterPriceSheet extends BottomSheet {
    private final OutlineTextContainerView diamondsCountEditOutline;
    private final EditTextBoldCursor diamondsCountEditField;
    private final TextView diamondsCountEditHint;

    private final AnimatedTextView titleView;
    private final ButtonWithCounterView buttonView;
    private final AnimatedTextView dollarsEqView;
    private final ImageView iconDiamonds;

    private final AmountUtils.Amount inputAmountMinDiamonds;
    private final AmountUtils.Amount inputAmountMaxDiamonds;
    private AmountUtils.Amount inputAmount;

    private static final int ERROR_FLAG_INCORRECT_INPUT = 1;
    private static final int ERROR_FLAG_AMOUNT_TOO_SMALL = 1 << 1;
    private static final int ERROR_FLAG_AMOUNT_TOO_BIG = 1 << 2;
    private static final int ERROR_FLAG_AMOUNT_NOT_ENOUGH = 1 << 3;
    private int inputAmountError;


    public SellGiftEnterPriceSheet(
        Context context,
        Theme.ResourcesProvider resourcesProvider,
        int currentAccount,
        AmountUtils.Amount startParams,
        Utilities.Callback<AmountUtils.Amount> callback
    ) {
        super(context, true, resourcesProvider);
        this.currentAccount = currentAccount;
        smoothKeyboardAnimationEnabled = true;
        waitingKeyboard = true;

        final AppGlobalConfig config = MessagesController.getInstance(currentAccount).config;
        inputAmountMinDiamonds = AmountUtils.Amount.fromDecimal(config.diamondsDiamondGiftResaleAmountMin.get(), AmountUtils.Currency.STARS);
        inputAmountMaxDiamonds = AmountUtils.Amount.fromDecimal(config.diamondsDiamondGiftResaleAmountMax.get(), AmountUtils.Currency.STARS);

        fixNavigationBar(Theme.getColor(Theme.key_dialogBackground, resourcesProvider));



        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        /* Header */

        LinearLayout headerLayout = new LinearLayout(context);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        layout.addView(headerLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56, Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 0, 0, 0));

        titleView = new AnimatedTextView(context);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setTextSize(dp(20));
        titleView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        titleView.setTypeface(AndroidUtilities.bold());
        // titleView.setEllipsize(TextUtils.TruncateAt.END);
        headerLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, 1f, Gravity.FILL, 18 + 4, 0, 18 + 4, 0));

        /* Body */

        LinearLayout bodyLayout = new LinearLayout(context);
        bodyLayout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(bodyLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 1f));

        {
            diamondsCountEditOutline = new OutlineTextContainerView(context);
            diamondsCountEditField = new EditTextBoldCursor(context);
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
            diamondsCountEditOutline.animateSelection(true, startParams != null && !startParams.isZero(), false);
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
            diamondsCountEditHint.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            diamondsCountEditHint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            bodyLayout.addView(diamondsCountEditHint, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.FILL_HORIZONTAL, 33, 4, 33, 0));
        }

        /* Footer */

        LinearLayout footerLayout = new LinearLayout(context);
        footerLayout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(footerLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM));

        buttonView = new ButtonWithCounterView(context, resourcesProvider).setRound();
        buttonView.setOnClickListener(v -> {
            if (!buttonView.isEnabled() || buttonView.isLoading()) {
                return;
            }
            AndroidUtilities.hideKeyboard(diamondsCountEditField);
            buttonView.setLoading(true);
            callback.run(inputAmount);
        });

        buttonView.setText(getString(R.string.ResellGiftButton), false);
        footerLayout.addView(buttonView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 18, 0, 18, 8));

        if (startParams != null) {
            setAmount(AmountUtils.Amount.fromNano(startParams.asNano(), startParams.currency), !startParams.isZero(), true, false);
        } else {
            setAmount(AmountUtils.Amount.fromNano(0, AmountUtils.Currency.STARS), false, true, false);
        }
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
        titleView.setText(getString(R.string.ResellGiftTitle), animated);

        diamondsCountEditField.setInputType(InputType.TYPE_CLASS_NUMBER);
        diamondsCountEditField.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(Long.toString(getInputAmountMax().asDecimal()).length())
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

    private void checkButtonEnabled(boolean animated) {
        final boolean newEnabled = inputAmountError == 0 && (inputAmount.asNano() > 0);
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
        if ((inputAmountError & ERROR_FLAG_AMOUNT_TOO_BIG) != 0) {
            diamondsCountEditOutline.setText(LocaleController.formatString(R.string.ResellGiftPriceTooMuch, getInputAmountMax().formatAsDecimalSpaced()));
        } else if ((inputAmountError & ERROR_FLAG_AMOUNT_TOO_SMALL) != 0) {
            diamondsCountEditOutline.setText(LocaleController.formatString(R.string.ResellGiftPriceTooSmall, getInputAmountMin().formatAsDecimalSpaced()));
        } else {
            diamondsCountEditOutline.setText(getString(R.string.ResellGiftPriceTitle));
        }
    }

    private void checkRateText(boolean animated) {
        final AppGlobalConfig config = MessagesController.getInstance(currentAccount).config;

        final AmountUtils.Amount amount = inputAmount.applyPerMille(config.diamondsDiamondGiftResaleCommissionPermille.get());
        final CharSequence s = AndroidUtilities.replaceTags(LocaleController.formatPluralString("ResellGiftInfo", (int) amount.asDecimal()));
        diamondsCountEditHint.setText(s);


        final StringBuilder sb = new StringBuilder(10).append('~');

        final double rate = MessagesController.getInstance(currentAccount).diamondsUsdWithdrawRate1000 * 0.00001;

        sb.append(BillingController.getInstance().formatCurrency((long) (inputAmount.asDouble() * rate * 100), "USD", 2));

        dollarsEqView.setText(sb, animated);
    }

    private AmountUtils.Amount getInputAmountMin() {
        return inputAmountMinDiamonds;
    }

    private AmountUtils.Amount getInputAmountMax() {
        return inputAmountMaxDiamonds;
    }

    @Override
    public void show() {
        super.show();
        AndroidUtilities.runOnUIThread(() -> AndroidUtilities.showKeyboard(diamondsCountEditField), 50);
    }
}
