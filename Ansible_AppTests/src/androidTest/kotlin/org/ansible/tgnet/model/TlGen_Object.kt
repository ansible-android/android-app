package org.ansible.tgnet.model

import org.ansible.tgnet.OutputSerializedData

public interface TlGen_Object {
    fun serializeToStream(stream: OutputSerializedData)
}