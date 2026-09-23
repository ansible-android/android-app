package org.ansible.tgnet.model.generated

import kotlin.String
import kotlin.UInt
import org.ansible.tgnet.OutputSerializedData
import org.ansible.tgnet.model.TlGen_Object
import org.ansible.tgnet.model.TlGen_Vector

public sealed class TlGen_StatsURL : TlGen_Object {
  public data class TL_statsURL(
    public val url: String,
  ) : TlGen_StatsURL() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      stream.writeString(url)
    }

    public companion object {
      public const val MAGIC: UInt = 0x47A971E0U
    }
  }
}
