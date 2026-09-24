package org.ansible.asnet.model.generated

import kotlin.UInt
import kotlin.collections.List
import org.ansible.asnet.OutputSerializedData
import org.ansible.asnet.model.TlGen_Object
import org.ansible.asnet.model.TlGen_Vector

public sealed class TlGen_photos_Photo : TlGen_Object {
  public data class TL_photos_photo(
    public val photo: TlGen_Photo,
    public val users: List<TlGen_User>,
  ) : TlGen_photos_Photo() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      photo.serializeToStream(stream)
      TlGen_Vector.serialize(stream, users)
    }

    public companion object {
      public const val MAGIC: UInt = 0x20212CA8U
    }
  }
}
