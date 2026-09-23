package org.ansible.tgnet.model.generated

import kotlin.UInt
import org.ansible.tgnet.OutputSerializedData
import org.ansible.tgnet.model.TlGen_Object
import org.ansible.tgnet.model.TlGen_Vector

public sealed class TlGen_messages_ComposedRichMessageWithAI : TlGen_Object {
  public data class TL_messages_composedRichMessageWithAI(
    public val result: TlGen_RichMessage,
  ) : TlGen_messages_ComposedRichMessageWithAI() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      result.serializeToStream(stream)
    }

    public companion object {
      public const val MAGIC: UInt = 0x4C4537C8U
    }
  }
}
