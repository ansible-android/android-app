package org.ansible.asnet.model.generated

import kotlin.String
import kotlin.UInt
import org.ansible.asnet.OutputSerializedData
import org.ansible.asnet.model.TlGen_Object
import org.ansible.asnet.model.TlGen_Vector

public sealed class TlGen_payments_StarsRevenueAdsAccountUrl : TlGen_Object {
  public data class TL_payments_starsRevenueAdsAccountUrl(
    public val url: String,
  ) : TlGen_payments_StarsRevenueAdsAccountUrl() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      stream.writeString(url)
    }

    public companion object {
      public const val MAGIC: UInt = 0x394E7F21U
    }
  }
}
