package org.ansible.tgnet.model.generated

import kotlin.UInt
import org.ansible.tgnet.OutputSerializedData
import org.ansible.tgnet.model.TlGen_Object
import org.ansible.tgnet.model.TlGen_Vector

public sealed class TlGen_Null : TlGen_Object {
  public data object TL_null : TlGen_Null() {
    public const val MAGIC: UInt = 0x56730BCCU

    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
    }
  }
}
