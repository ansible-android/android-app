package org.ansible.tgnet.model.generated

import kotlin.Int
import kotlin.Long
import kotlin.UInt
import org.ansible.tgnet.OutputSerializedData
import org.ansible.tgnet.model.TlGen_Object
import org.ansible.tgnet.model.TlGen_Vector

public sealed class TlGen_ReadParticipantDate : TlGen_Object {
  public data class TL_readParticipantDate(
    public val user_id: Long,
    public val date: Int,
  ) : TlGen_ReadParticipantDate() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      stream.writeInt64(user_id)
      stream.writeInt32(date)
    }

    public companion object {
      public const val MAGIC: UInt = 0x4A4FF172U
    }
  }
}
