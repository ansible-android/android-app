package org.ansible.asnet.model.generated

import kotlin.String
import kotlin.UInt
import org.ansible.asnet.OutputSerializedData
import org.ansible.asnet.model.TlGen_Object
import org.ansible.asnet.model.TlGen_Vector

public sealed class TlGen_auth_FirebasePnvIntent : TlGen_Object {
  public data class TL_auth_firebasePnvIntent(
    public val nonce: String,
    public val digital_credential_payload: String,
  ) : TlGen_auth_FirebasePnvIntent() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      stream.writeString(nonce)
      stream.writeString(digital_credential_payload)
    }

    public companion object {
      public const val MAGIC: UInt = 0xDF5AC00CU
    }
  }
}
