package org.ansible.asnet.model

import org.ansible.asnet.OutputSerializedData

public interface TlGen_Object {
    fun serializeToStream(stream: OutputSerializedData)
}