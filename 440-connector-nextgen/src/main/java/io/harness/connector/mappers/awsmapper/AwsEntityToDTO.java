/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.awsmapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.awsconnector.AwsAccessKeyCredential;
import io.harness.connector.entities.embedded.awsconnector.AwsConfig;
import io.harness.connector.entities.embedded.awsconnector.AwsIRSACredential;
import io.harness.connector.entities.embedded.awsconnector.AwsIamCredential;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO.AwsCredentialDTOBuilder;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.DX)
@Singleton
public class AwsEntityToDTO implements ConnectorEntityToDTOMapper<AwsConnectorDTO, AwsConfig> {
  @Override
  public AwsConnectorDTO createConnectorDTO(AwsConfig connector) {
    final AwsCredentialType credentialType = connector.getCredentialType();
    AwsCredentialDTOBuilder awsCredentialDTOBuilder;
    switch (credentialType) {
      case INHERIT_FROM_DELEGATE:
        awsCredentialDTOBuilder = buildInheritFromDelegate((AwsIamCredential) connector.getCredential());
        break;
      case MANUAL_CREDENTIALS:
        awsCredentialDTOBuilder = buildManualCredential((AwsAccessKeyCredential) connector.getCredential());
        break;
      case IRSA:
        awsCredentialDTOBuilder = buildIRSA((AwsIRSACredential) connector.getCredential());
        break;
      default:
        throw new InvalidRequestException("Invalid Credential type.");
    }
    return AwsConnectorDTO.builder()
        .credential(awsCredentialDTOBuilder.crossAccountAccess(connector.getCrossAccountAccess()).build())
        .delegateSelectors(connector.getDelegateSelectors())
        .build();
  }

  private AwsCredentialDTOBuilder buildManualCredential(AwsAccessKeyCredential credential) {
    final SecretRefData secretRef = SecretRefHelper.createSecretRef(credential.getSecretKeyRef());
    final SecretRefData accessKeyRef = SecretRefHelper.createSecretRef(credential.getAccessKeyRef());
    final AwsManualConfigSpecDTO awsManualConfigSpecDTO = AwsManualConfigSpecDTO.builder()
                                                              .accessKey(credential.getAccessKey())
                                                              .secretKeyRef(secretRef)
                                                              .accessKeyRef(accessKeyRef)
                                                              .build();
    return AwsCredentialDTO.builder()
        .awsCredentialType(AwsCredentialType.MANUAL_CREDENTIALS)
        .config(awsManualConfigSpecDTO);
  }

  private AwsCredentialDTOBuilder buildInheritFromDelegate(AwsIamCredential credential) {
    return AwsCredentialDTO.builder().awsCredentialType(AwsCredentialType.INHERIT_FROM_DELEGATE).config(null);
  }

  private AwsCredentialDTOBuilder buildIRSA(AwsIRSACredential credential) {
    return AwsCredentialDTO.builder().awsCredentialType(AwsCredentialType.IRSA).config(null);
  }
}
