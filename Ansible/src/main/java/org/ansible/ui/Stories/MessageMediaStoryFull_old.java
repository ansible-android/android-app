package org.ansible.ui.Stories;

import org.ansible.tgnet.AbstractSerializedData;
import org.ansible.tgnet.InputSerializedData;
import org.ansible.tgnet.OutputSerializedData;
import org.ansible.tgnet.TLRPC;
import org.ansible.tgnet.tl.TL_stories;

public class MessageMediaStoryFull_old extends TLRPC.TL_messageMediaStory {

    public MessageMediaStoryFull_old() {

    }

    public static int constructor = 0xc79aee1f;

    public void readParams(InputSerializedData stream, boolean exception) {
        user_id = stream.readInt64(exception);
        id = stream.readInt32(exception);
        storyItem = TL_stories.StoryItem.TLdeserialize(stream, stream.readInt32(exception), exception);
    }

    public void serializeToStream(OutputSerializedData stream) {
        stream.writeInt32(constructor);
        stream.writeInt64(user_id);
        stream.writeInt32(id);
        storyItem.serializeToStream(stream);
    }
}