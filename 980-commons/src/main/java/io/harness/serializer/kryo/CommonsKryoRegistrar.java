package io.harness.serializer.kryo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.KeyValuePair;
import io.harness.encryption.Scope;
import io.harness.exception.ArtifactServerException;
import io.harness.exception.ArtifactoryServerException;
import io.harness.exception.ContextException;
import io.harness.exception.DelegateErrorHandlerException;
import io.harness.exception.ExceptionHandlerNotFoundException;
import io.harness.exception.GeneralException;
import io.harness.exception.HttpResponseException;
import io.harness.exception.ImageNotFoundException;
import io.harness.exception.InvalidArtifactServerException;
import io.harness.exception.KryoHandlerNotFoundException;
import io.harness.exception.ServiceNowException;
import io.harness.exception.ShellExecutionException;
import io.harness.exception.VerificationOperationException;
import io.harness.logging.LogLevel;
import io.harness.security.PrincipalContextData;
import io.harness.security.SourcePrincipalContextData;
import io.harness.security.dto.Principal;
import io.harness.security.dto.PrincipalType;
import io.harness.security.dto.UserPrincipal;
import io.harness.serializer.KryoRegistrar;
import io.harness.tasks.BinaryResponseData;
import io.harness.tasks.FailureResponseData;

import com.esotericsoftware.kryo.Kryo;
import java.time.ZonedDateTime;

@OwnedBy(HarnessTeam.PL)
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
    kryo.register(ZonedDateTime.class, 71123);

    // Promoted Classes
    kryo.register(ArtifactoryServerException.class, 7214);
    kryo.register(ArtifactServerException.class, 7244);
    kryo.register(InvalidArtifactServerException.class, 7250);
    kryo.register(ShellExecutionException.class, 7473);
    kryo.register(LogLevel.class, 71103);

    kryo.register(java.lang.StackTraceElement[].class, 31010);
    kryo.register(java.lang.StackTraceElement.class, 31011);
    kryo.register(DelegateErrorHandlerException.class, 31012);
    kryo.register(KryoHandlerNotFoundException.class, 31013);
    kryo.register(ExceptionHandlerNotFoundException.class, 31014);
    kryo.register(ImageNotFoundException.class, 31015);
    kryo.register(ContextException.class, 31016);

    kryo.register(PrincipalContextData.class, 980001);
    kryo.register(UserPrincipal.class, 980002);
    kryo.register(SourcePrincipalContextData.class, 980003);
    kryo.register(Principal.class, 980004);
    kryo.register(PrincipalType.class, 980005);
    kryo.register(HttpResponseException.class, 980006);
  }
}
