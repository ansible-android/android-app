package org.ansible.tgnet.model.generated

import kotlin.Int
import kotlin.UInt
import org.ansible.tgnet.OutputSerializedData
import org.ansible.tgnet.model.TlGen_Object
import org.ansible.tgnet.model.TlGen_Vector

public sealed class TlGen_OutboxReadDate : TlGen_Object {
  public data class TL_outboxReadDate(
    public val date: Int,
  ) : TlGen_OutboxReadDate() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      stream.writeInt32(date)
    }

    public companion object {
      public const val MAGIC: UInt = 0x3BB842ACU
    }
  }
}
