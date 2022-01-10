/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.citasks.cik8handler.helper;

import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.delegate.beans.ci.pod.SecretParams.Type.TEXT;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ci.pod.SecretParams;
import io.harness.security.ServiceTokenGenerator;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;

@OwnedBy(HarnessTeam.CI)
@Builder
public class DelegateServiceTokenHelper {
  private final ServiceTokenGenerator serviceTokenGenerator;
  private final String accountSecret;
  private static final String DELEGATE_SERVICE_TOKEN_VARIABLE = "DELEGATE_SERVICE_TOKEN";

  public DelegateServiceTokenHelper(ServiceTokenGenerator serviceTokenGenerator, String accountSecret) {
    this.serviceTokenGenerator = serviceTokenGenerator;
    this.accountSecret = accountSecret;
  }

  private String getServiceToken() {
    return serviceTokenGenerator.getServiceTokenWithDuration(accountSecret, Duration.ofHours(12));
  }

  public Map<String, SecretParams> getServiceTokenSecretParams() {
    Map<String, SecretParams> serviceTokenSecretParams = new HashMap<>();
    serviceTokenSecretParams.put(DELEGATE_SERVICE_TOKEN_VARIABLE,
        SecretParams.builder()
            .secretKey(DELEGATE_SERVICE_TOKEN_VARIABLE)
            .value(encodeBase64(getServiceToken()))
            .type(TEXT)
            .build());
    return serviceTokenSecretParams;
  }
}
