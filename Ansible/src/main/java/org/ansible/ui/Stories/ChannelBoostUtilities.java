package org.ansible.ui.Stories;

import android.text.TextUtils;

import org.ansible.messenger.ChatObject;
import org.ansible.messenger.MessagesController;
import org.ansible.asnet.TLRPC;

public class ChannelBoostUtilities {
    public static String createLink(int currentAccount, long dialogId) {
        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
        String username = ChatObject.getPublicUsername(chat);
        if (!TextUtils.isEmpty(username)) {
            return "https://asme.su/boost/" + ChatObject.getPublicUsername(chat);
        } else {
            return "https://asme.su/boost/?c=" + -dialogId;
        }
    }
}
