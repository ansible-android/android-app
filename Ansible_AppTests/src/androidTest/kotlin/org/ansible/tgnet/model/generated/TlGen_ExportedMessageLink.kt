package org.ansible.tgnet.model.generated

import kotlin.String
import kotlin.UInt
import org.ansible.tgnet.OutputSerializedData
import org.ansible.tgnet.model.TlGen_Object
import org.ansible.tgnet.model.TlGen_Vector

public sealed class TlGen_ExportedMessageLink : TlGen_Object {
  public data class TL_exportedMessageLink(
    public val link: String,
    public val html: String,
  ) : TlGen_ExportedMessageLink() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      stream.writeString(link)
      stream.writeString(html)
    }

    public companion object {
      public const val MAGIC: UInt = 0x5DAB1AF4U
    }
  }
}
