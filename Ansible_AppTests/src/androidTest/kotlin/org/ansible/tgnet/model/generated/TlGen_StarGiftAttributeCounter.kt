package org.ansible.tgnet.model.generated

import kotlin.Int
import kotlin.UInt
import org.ansible.tgnet.OutputSerializedData
import org.ansible.tgnet.model.TlGen_Object
import org.ansible.tgnet.model.TlGen_Vector

public sealed class TlGen_StarGiftAttributeCounter : TlGen_Object {
  public data class TL_starGiftAttributeCounter(
    public val attribute: TlGen_StarGiftAttributeId,
    public val count: Int,
  ) : TlGen_StarGiftAttributeCounter() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      attribute.serializeToStream(stream)
      stream.writeInt32(count)
    }

    public companion object {
      public const val MAGIC: UInt = 0x2EB1B658U
    }
  }
}
