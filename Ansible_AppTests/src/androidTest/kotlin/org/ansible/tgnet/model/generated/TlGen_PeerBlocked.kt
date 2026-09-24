package org.ansible.asnet.model.generated

import kotlin.Int
import kotlin.UInt
import org.ansible.asnet.OutputSerializedData
import org.ansible.asnet.model.TlGen_Object
import org.ansible.asnet.model.TlGen_Vector

public sealed class TlGen_PeerBlocked : TlGen_Object {
  public data class TL_peerBlocked(
    public val peer_id: TlGen_Peer,
    public val date: Int,
  ) : TlGen_PeerBlocked() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      peer_id.serializeToStream(stream)
      stream.writeInt32(date)
    }

    public companion object {
      public const val MAGIC: UInt = 0xE8FD8014U
    }
  }
}
