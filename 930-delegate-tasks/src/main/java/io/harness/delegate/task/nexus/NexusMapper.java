/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.nexus;

import static io.harness.utils.FieldWithPlainTextOrSecretValueHelper.getSecretAsStringFromPlainTextOrSecretRef;

import io.harness.delegate.beans.connector.nexusconnector.NexusAuthType;
import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusUsernamePasswordAuthDTO;
import io.harness.encryption.SecretRefData;
import io.harness.nexus.NexusRequest;
import io.harness.nexus.NexusRequest.NexusRequestBuilder;

import com.google.inject.Singleton;

@Singleton
public class NexusMapper {
  public NexusRequest toNexusRequest(NexusConnectorDTO nexusConnectorDTO) {
    final NexusRequestBuilder nexusRequestBuilder =
        NexusRequest.builder()
            .nexusUrl(nexusConnectorDTO.getNexusServerUrl())
            .version(nexusConnectorDTO.getVersion())
            // Setting as false since we are not taking this in yaml as of now.
            .isCertValidationRequired(false);
    final NexusAuthType authType = nexusConnectorDTO.getAuth().getAuthType();

    if (authType == NexusAuthType.ANONYMOUS) {
      return nexusRequestBuilder.hasCredentials(false).build();
    }
    final NexusUsernamePasswordAuthDTO credentials =
        (NexusUsernamePasswordAuthDTO) nexusConnectorDTO.getAuth().getCredentials();
    final SecretRefData passwordRef = credentials.getPasswordRef();
    final String username =
        getSecretAsStringFromPlainTextOrSecretRef(credentials.getUsername(), credentials.getUsernameRef());
    return nexusRequestBuilder.hasCredentials(true)
        .username(username)
        .password(passwordRef.getDecryptedValue())
        .build();
  }
}
