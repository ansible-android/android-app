package org.ansible.asnet.model.generated

import kotlin.String
import kotlin.UInt
import org.ansible.asnet.OutputSerializedData
import org.ansible.asnet.model.TlGen_Object
import org.ansible.asnet.model.TlGen_Vector

public sealed class TlGen_EmojiURL : TlGen_Object {
  public data class TL_emojiURL(
    public val url: String,
  ) : TlGen_EmojiURL() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      stream.writeString(url)
    }

    public companion object {
      public const val MAGIC: UInt = 0xA575739DU
    }
  }
}
