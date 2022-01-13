/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.k8Connector;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@JsonSubTypes({
  @JsonSubTypes.Type(value = KubernetesUserNamePasswordDTO.class, name = KubernetesConfigConstants.USERNAME_PASSWORD)
  , @JsonSubTypes.Type(value = KubernetesServiceAccountDTO.class, name = KubernetesConfigConstants.SERVICE_ACCOUNT),
      @JsonSubTypes.Type(value = KubernetesOpenIdConnectDTO.class, name = KubernetesConfigConstants.OPENID_CONNECT),
      @JsonSubTypes.Type(value = KubernetesClientKeyCertDTO.class, name = KubernetesConfigConstants.CLIENT_KEY_CERT)
})
@Schema(name = "KubernetesAuthCredential", description = "This contains kubernetes auth credentials")
public abstract class KubernetesAuthCredentialDTO implements DecryptableEntity {}
