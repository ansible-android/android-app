package org.ansible.asnet.model.generated

import kotlin.String
import kotlin.UInt
import org.ansible.asnet.OutputSerializedData
import org.ansible.asnet.model.TlGen_Object
import org.ansible.asnet.model.TlGen_Vector

public sealed class TlGen_messages_BotWidgetQueryAnswer : TlGen_Object {
  public data class TL_messages_botWidgetQueryAnswer(
    public val result: String?,
  ) : TlGen_messages_BotWidgetQueryAnswer() {
    internal val flags: UInt
      get() {
        var result = 0U
        if (this.result != null) result = result or 1U
        return result
      }

    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      stream.writeInt32(flags.toInt())
      result?.let { stream.writeString(it) }
    }

    public companion object {
      public const val MAGIC: UInt = 0x95B73023U
    }
  }
}
