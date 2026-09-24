package org.ansible.asnet.model.generated

import kotlin.String
import kotlin.UInt
import org.ansible.asnet.OutputSerializedData
import org.ansible.asnet.model.TlGen_Object
import org.ansible.asnet.model.TlGen_Vector

public sealed class TlGen_auth_PasswordRecovery : TlGen_Object {
  public data class TL_auth_passwordRecovery(
    public val email_pattern: String,
  ) : TlGen_auth_PasswordRecovery() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      stream.writeString(email_pattern)
    }

    public companion object {
      public const val MAGIC: UInt = 0x137948A5U
    }
  }
}
