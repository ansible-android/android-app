package org.ansible.asnet.model.generated

import kotlin.Int
import kotlin.Long
import kotlin.UInt
import org.ansible.asnet.OutputSerializedData
import org.ansible.asnet.model.TlGen_Object
import org.ansible.asnet.model.TlGen_Vector

public sealed class TlGen_StarsSubscriptionPricing : TlGen_Object {
  public data class TL_starsSubscriptionPricing(
    public val period: Int,
    public val amount: Long,
  ) : TlGen_StarsSubscriptionPricing() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      stream.writeInt32(period)
      stream.writeInt64(amount)
    }

    public companion object {
      public const val MAGIC: UInt = 0x05416D58U
    }
  }
}
