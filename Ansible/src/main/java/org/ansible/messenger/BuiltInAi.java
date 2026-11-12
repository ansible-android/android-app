package org.ansible.messenger;

import org.ansible.asnet.TLObject;
import org.ansible.asnet.TLRPC;
import org.ansible.asnet.tl.TL_aicompose;

/** Product policy for the retired built-in AI service. Wire DTOs stay compatible. */
public final class BuiltInAi {
    private BuiltInAi() {}

    public static boolean isEnabled() {
        return false;
    }

    public static boolean rejectsRequest(TLObject request) {
        return !isEnabled() && (
            request instanceof TLRPC.TL_messages_composeMessageWithAI ||
            request instanceof TLRPC.TL_messages_composeRichMessageWithAI ||
            request instanceof TLRPC.TL_messages_summarizeText ||
            request instanceof TL_aicompose.createTone ||
            request instanceof TL_aicompose.updateTone ||
            request instanceof TL_aicompose.saveTone ||
            request instanceof TL_aicompose.deleteTone ||
            request instanceof TL_aicompose.getTone ||
            request instanceof TL_aicompose.getTones ||
            request instanceof TL_aicompose.getToneExample
        );
    }
}
