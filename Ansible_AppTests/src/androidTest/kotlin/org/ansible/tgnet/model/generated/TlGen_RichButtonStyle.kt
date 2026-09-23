package org.ansible.tgnet.model.generated

import kotlin.Boolean
import kotlin.UInt
import org.ansible.tgnet.OutputSerializedData
import org.ansible.tgnet.model.TlGen_Object
import org.ansible.tgnet.model.TlGen_Vector

public sealed class TlGen_RichButtonStyle : TlGen_Object {
  public data class TL_richButtonStyle(
    public val bg_primary: Boolean,
    public val bg_danger: Boolean,
    public val bg_success: Boolean,
    public val link: Boolean,
  ) : TlGen_RichButtonStyle() {
    internal val flags: UInt
      get() {
        var result = 0U
        if (bg_primary) result = result or 1U
        if (bg_danger) result = result or 2U
        if (bg_success) result = result or 4U
        if (link) result = result or 8U
        return result
      }

    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      stream.writeInt32(flags.toInt())
    }

    public companion object {
      public const val MAGIC: UInt = 0x03C610BDU
    }
  }
}
