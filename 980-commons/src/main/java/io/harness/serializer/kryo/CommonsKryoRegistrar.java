/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.serializer.kryo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.KeyValuePair;
import io.harness.encryption.Scope;
import io.harness.exception.ArtifactServerException;
import io.harness.exception.ArtifactoryServerException;
import io.harness.exception.AuthenticationException;
import io.harness.exception.AuthorizationException;
import io.harness.exception.ConnectException;
import io.harness.exception.ContextException;
import io.harness.exception.DelegateErrorHandlerException;
import io.harness.exception.DelegateNotAvailableException;
import io.harness.exception.EngineExpressionEvaluationException;
import io.harness.exception.EngineFunctorException;
import io.harness.exception.ExceptionHandlerNotFoundException;
import io.harness.exception.GcpServerException;
import io.harness.exception.GeneralException;
import io.harness.exception.GitOperationException;
import io.harness.exception.HttpResponseException;
import io.harness.exception.IllegalArgumentException;
import io.harness.exception.ImageNotFoundException;
import io.harness.exception.InvalidArtifactServerException;
import io.harness.exception.InvalidCredentialsException;
import io.harness.exception.InvalidTagException;
import io.harness.exception.InvalidThirdPartyCredentialsException;
import io.harness.exception.InvalidYamlException;
import io.harness.exception.JiraClientException;
import io.harness.exception.KryoHandlerNotFoundException;
import io.harness.exception.KubernetesApiTaskException;
import io.harness.exception.KubernetesTaskException;
import io.harness.exception.KubernetesYamlException;
import io.harness.exception.SecretNotFoundException;
import io.harness.exception.ServiceNowException;
import io.harness.exception.ShellExecutionException;
import io.harness.exception.TerraformCommandExecutionException;
import io.harness.exception.UnresolvedExpressionsException;
import io.harness.exception.VerificationOperationException;
import io.harness.logging.LogLevel;
import io.harness.security.PrincipalContextData;
import io.harness.security.SimpleEncryption;
import io.harness.security.SourcePrincipalContextData;
import io.harness.security.dto.Principal;
import io.harness.security.dto.PrincipalType;
import io.harness.security.dto.UserPrincipal;
import io.harness.serializer.KryoRegistrar;
import io.harness.tasks.BinaryResponseData;
import io.harness.tasks.FailureResponseData;

import software.wings.beans.appmanifest.StoreType;

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
    kryo.register(StoreType.class, 5540);

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
    kryo.register(HttpResponseException.class, 31016);
    kryo.register(GcpServerException.class, 31017);
    kryo.register(InvalidCredentialsException.class, 31018);
    kryo.register(ContextException.class, 31019);
    kryo.register(InvalidTagException.class, 31020);
    kryo.register(SecretNotFoundException.class, 31021);
    kryo.register(DelegateNotAvailableException.class, 31022);
    kryo.register(IllegalArgumentException.class, 31023);
    kryo.register(InvalidThirdPartyCredentialsException.class, 31024);
    kryo.register(ConnectException.class, 31025);

    kryo.register(PrincipalContextData.class, 980001);
    kryo.register(UserPrincipal.class, 980002);
    kryo.register(SourcePrincipalContextData.class, 980003);
    kryo.register(Principal.class, 980004);
    kryo.register(PrincipalType.class, 980005);
    kryo.register(EngineExpressionEvaluationException.class, 980006);
    kryo.register(EngineFunctorException.class, 980007);
    kryo.register(UnresolvedExpressionsException.class, 980008);
    kryo.register(JiraClientException.class, 980009);
    kryo.register(InvalidYamlException.class, 980010);
    kryo.register(AuthenticationException.class, 980011);
    kryo.register(AuthorizationException.class, 980012);
    kryo.register(KubernetesApiTaskException.class, 980014);
    kryo.register(KubernetesTaskException.class, 980015);
    kryo.register(KubernetesYamlException.class, 980016);
    kryo.register(GitOperationException.class, 980017);
    kryo.register(TerraformCommandExecutionException.class, 980018);
    kryo.register(SimpleEncryption.class, 980019);
  }
}
