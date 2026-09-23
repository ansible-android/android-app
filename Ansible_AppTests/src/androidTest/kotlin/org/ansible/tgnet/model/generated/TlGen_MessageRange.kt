package org.ansible.tgnet.model.generated

import kotlin.Int
import kotlin.UInt
import org.ansible.tgnet.OutputSerializedData
import org.ansible.tgnet.model.TlGen_Object
import org.ansible.tgnet.model.TlGen_Vector

public sealed class TlGen_MessageRange : TlGen_Object {
  public data class TL_messageRange(
    public val min_id: Int,
    public val max_id: Int,
  ) : TlGen_MessageRange() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      stream.writeInt32(min_id)
      stream.writeInt32(max_id)
    }

    public companion object {
      public const val MAGIC: UInt = 0x0AE30253U
    }
  }
}
