package org.ansible.asnet.model.generated

import kotlin.String
import kotlin.UInt
import org.ansible.asnet.OutputSerializedData
import org.ansible.asnet.model.TlGen_Object
import org.ansible.asnet.model.TlGen_Vector

public sealed class TlGen_ExportedStoryLink : TlGen_Object {
  public data class TL_exportedStoryLink(
    public val link: String,
  ) : TlGen_ExportedStoryLink() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      stream.writeString(link)
    }

    public companion object {
      public const val MAGIC: UInt = 0x3FC9053BU
    }
  }
}
