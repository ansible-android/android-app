package org.ansible.tgnet.model.generated

import kotlin.Int
import kotlin.UInt
import org.ansible.tgnet.OutputSerializedData
import org.ansible.tgnet.model.TlGen_Object
import org.ansible.tgnet.model.TlGen_Vector

public sealed class TlGen_StatsDateRangeDays : TlGen_Object {
  public data class TL_statsDateRangeDays(
    public val min_date: Int,
    public val max_date: Int,
  ) : TlGen_StatsDateRangeDays() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      stream.writeInt32(min_date)
      stream.writeInt32(max_date)
    }

    public companion object {
      public const val MAGIC: UInt = 0xB637EDAFU
    }
  }
}
