package org.ansible.asnet.model.generated

import kotlin.UInt
import org.ansible.asnet.OutputSerializedData
import org.ansible.asnet.model.TlGen_Object
import org.ansible.asnet.model.TlGen_Vector

public sealed class TlGen_stats_PollStats : TlGen_Object {
  public data class TL_stats_pollStats(
    public val votes_graph: TlGen_StatsGraph,
  ) : TlGen_stats_PollStats() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      votes_graph.serializeToStream(stream)
    }

    public companion object {
      public const val MAGIC: UInt = 0x2999BEEDU
    }
  }
}
