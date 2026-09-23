package org.ansible.tgnet.model.generated

import kotlin.String
import kotlin.UInt
import org.ansible.tgnet.OutputSerializedData
import org.ansible.tgnet.model.TlGen_Object
import org.ansible.tgnet.model.TlGen_Vector

public sealed class TlGen_help_InviteText : TlGen_Object {
  public data class TL_help_inviteText(
    public val message: String,
  ) : TlGen_help_InviteText() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      stream.writeString(message)
    }

    public companion object {
      public const val MAGIC: UInt = 0x18CB9F78U
    }
  }
}
