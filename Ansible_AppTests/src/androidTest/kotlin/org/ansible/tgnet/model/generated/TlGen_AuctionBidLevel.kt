package org.ansible.tgnet.model.generated

import kotlin.Int
import kotlin.Long
import kotlin.UInt
import org.ansible.tgnet.OutputSerializedData
import org.ansible.tgnet.model.TlGen_Object
import org.ansible.tgnet.model.TlGen_Vector

public sealed class TlGen_AuctionBidLevel : TlGen_Object {
  public data class TL_auctionBidLevel(
    public val pos: Int,
    public val amount: Long,
    public val date: Int,
  ) : TlGen_AuctionBidLevel() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      stream.writeInt32(pos)
      stream.writeInt64(amount)
      stream.writeInt32(date)
    }

    public companion object {
      public const val MAGIC: UInt = 0x310240CCU
    }
  }
}
