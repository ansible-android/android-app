package org.ansible.tgnet.model.generated

import kotlin.Int
import kotlin.UInt
import kotlin.collections.List
import org.ansible.tgnet.OutputSerializedData
import org.ansible.tgnet.model.TlGen_Object
import org.ansible.tgnet.model.TlGen_Vector

public sealed class TlGen_messages_SearchResultsPositions : TlGen_Object {
  public data class TL_messages_searchResultsPositions(
    public val count: Int,
    public val positions: List<TlGen_SearchResultsPosition>,
  ) : TlGen_messages_SearchResultsPositions() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      stream.writeInt32(count)
      TlGen_Vector.serialize(stream, positions)
    }

    public companion object {
      public const val MAGIC: UInt = 0x53B22BAFU
    }
  }
}
