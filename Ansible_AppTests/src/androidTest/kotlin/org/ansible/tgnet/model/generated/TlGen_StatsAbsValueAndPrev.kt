package org.ansible.tgnet.model.generated

import kotlin.Double
import kotlin.UInt
import org.ansible.tgnet.OutputSerializedData
import org.ansible.tgnet.model.TlGen_Object
import org.ansible.tgnet.model.TlGen_Vector

public sealed class TlGen_StatsAbsValueAndPrev : TlGen_Object {
  public data class TL_statsAbsValueAndPrev(
    public val current: Double,
    public val previous: Double,
  ) : TlGen_StatsAbsValueAndPrev() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      stream.writeDouble(current)
      stream.writeDouble(previous)
    }

    public companion object {
      public const val MAGIC: UInt = 0xCB43ACDEU
    }
  }
}
