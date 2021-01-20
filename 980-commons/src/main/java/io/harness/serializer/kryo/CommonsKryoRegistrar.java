package io.harness.serializer.kryo;

import io.harness.beans.KeyValuePair;
import io.harness.encryption.Scope;
import io.harness.exception.ArtifactServerException;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidArtifactServerException;
import io.harness.exception.ServiceNowException;
import io.harness.exception.ShellExecutionException;
import io.harness.exception.VerificationOperationException;
import io.harness.logging.LogLevel;
import io.harness.serializer.KryoRegistrar;
import io.harness.tasks.BinaryResponseData;
import io.harness.tasks.FailureResponseData;

import com.esotericsoftware.kryo.Kryo;

public class CommonsKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(VerificationOperationException.class, 3001);
    kryo.register(ServiceNowException.class, 3002);
    kryo.register(Scope.class, 3004);
    kryo.register(GeneralException.class, 3005);
    kryo.register(BinaryResponseData.class, 3006);
    kryo.register(FailureResponseData.class, 3007);
    kryo.register(KeyValuePair.class, 3008);

    // Promoted Classes
    kryo.register(ArtifactServerException.class, 7244);
    kryo.register(InvalidArtifactServerException.class, 7250);
    kryo.register(ShellExecutionException.class, 7473);
    kryo.register(LogLevel.class, 71103);
  }
}
