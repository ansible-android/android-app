package org.ansible.tgnet.model.generated

import kotlin.Int
import kotlin.UInt
import org.ansible.tgnet.OutputSerializedData
import org.ansible.tgnet.model.TlGen_Object
import org.ansible.tgnet.model.TlGen_Vector

public sealed class TlGen_InputChatlist : TlGen_Object {
  public data class TL_inputChatlistDialogFilter(
    public val filter_id: Int,
  ) : TlGen_InputChatlist() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      stream.writeInt32(filter_id)
    }

    public companion object {
      public const val MAGIC: UInt = 0xF3E0DA33U
    }
  }
}
