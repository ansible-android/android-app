package org.ansible.tgnet.model.generated

import kotlin.UInt
import kotlin.collections.List
import org.ansible.tgnet.OutputSerializedData
import org.ansible.tgnet.model.TlGen_Object
import org.ansible.tgnet.model.TlGen_Vector

public sealed class TlGen_messages_TranslatedRichMessage : TlGen_Object {
  public data class TL_messages_translatedRichMessage(
    public val result: List<TlGen_RichMessage>,
  ) : TlGen_messages_TranslatedRichMessage() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      TlGen_Vector.serialize(stream, result)
    }

    public companion object {
      public const val MAGIC: UInt = 0x4203998FU
    }
  }
}
