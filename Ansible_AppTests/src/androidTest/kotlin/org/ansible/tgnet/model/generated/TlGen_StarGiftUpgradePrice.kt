package org.ansible.tgnet.model.generated

import kotlin.Int
import kotlin.Long
import kotlin.UInt
import org.ansible.tgnet.OutputSerializedData
import org.ansible.tgnet.model.TlGen_Object
import org.ansible.tgnet.model.TlGen_Vector

public sealed class TlGen_StarGiftUpgradePrice : TlGen_Object {
  public data class TL_starGiftUpgradePrice(
    public val date: Int,
    public val upgrade_stars: Long,
  ) : TlGen_StarGiftUpgradePrice() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      stream.writeInt32(date)
      stream.writeInt64(upgrade_stars)
    }

    public companion object {
      public const val MAGIC: UInt = 0x99EA331DU
    }
  }
}
