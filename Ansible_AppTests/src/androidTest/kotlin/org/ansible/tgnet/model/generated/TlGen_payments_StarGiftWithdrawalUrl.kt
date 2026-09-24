package org.ansible.asnet.model.generated

import kotlin.String
import kotlin.UInt
import org.ansible.asnet.OutputSerializedData
import org.ansible.asnet.model.TlGen_Object
import org.ansible.asnet.model.TlGen_Vector

public sealed class TlGen_payments_StarGiftWithdrawalUrl : TlGen_Object {
  public data class TL_payments_starGiftWithdrawalUrl(
    public val url: String,
  ) : TlGen_payments_StarGiftWithdrawalUrl() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      stream.writeString(url)
    }

    public companion object {
      public const val MAGIC: UInt = 0x84AA3A9CU
    }
  }
}
