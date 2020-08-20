package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.exception.ArtifactServerException;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidArtifactServerException;
import io.harness.exception.ServiceNowException;
import io.harness.exception.VerificationOperationException;
import io.harness.serializer.KryoRegistrar;

public class CommonsKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(VerificationOperationException.class, 3001);
    kryo.register(ServiceNowException.class, 3002);
    kryo.register(SecretRefData.class, 3003);
    kryo.register(Scope.class, 3004);
    kryo.register(GeneralException.class, 3005);
    kryo.register(ArtifactServerException.class, 7244);
    kryo.register(InvalidArtifactServerException.class, 7250);
  }
}
