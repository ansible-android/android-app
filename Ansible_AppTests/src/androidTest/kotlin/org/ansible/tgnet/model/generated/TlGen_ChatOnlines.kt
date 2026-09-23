package org.ansible.tgnet.model.generated

import kotlin.Int
import kotlin.UInt
import org.ansible.tgnet.OutputSerializedData
import org.ansible.tgnet.model.TlGen_Object
import org.ansible.tgnet.model.TlGen_Vector

public sealed class TlGen_ChatOnlines : TlGen_Object {
  public data class TL_chatOnlines(
    public val onlines: Int,
  ) : TlGen_ChatOnlines() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      stream.writeInt32(onlines)
    }

    public companion object {
      public const val MAGIC: UInt = 0xF041E250U
    }
  }
}
