/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.mappers.secretmanagermapper;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.awssecretmanager.AwsSecretManagerConnector;
import io.harness.connector.entities.embedded.awssecretmanager.AwsSecretManagerConnector.AwsSecretManagerConnectorBuilder;
import io.harness.connector.entities.embedded.awssecretmanager.AwsSecretManagerCredentialSpec;
import io.harness.connector.entities.embedded.awssecretmanager.AwsSecretManagerIAMCredential;
import io.harness.connector.entities.embedded.awssecretmanager.AwsSecretManagerManualCredential;
import io.harness.connector.entities.embedded.awssecretmanager.AwsSecretManagerSTSCredential;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSMCredentialSpecAssumeIAMDTO;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSMCredentialSpecAssumeSTSDTO;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSMCredentialSpecManualConfigDTO;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSecretManagerCredentialDTO;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSecretManagerCredentialSpecDTO;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSecretManagerCredentialType;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSecretManagerDTO;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSecretManagerDTO.AwsSecretManagerDTOBuilder;
import io.harness.encryption.SecretRefHelper;

import com.amazonaws.auth.STSSessionCredentialsProvider;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@UtilityClass
@Slf4j
public class AwsSecretManagerMapperHelper {
  public AwsSecretManagerConnectorBuilder buildManualConfig(AwsSMCredentialSpecManualConfigDTO credentialConfig) {
    AwsSecretManagerManualCredential spec =
        AwsSecretManagerManualCredential.builder()
            .secretKey(SecretRefHelper.getSecretConfigString(credentialConfig.getSecretKey()))
            .accessKey(SecretRefHelper.getSecretConfigString(credentialConfig.getAccessKey()))
            .build();
    return getAwsSMConnectorBuilder(spec, AwsSecretManagerCredentialType.MANUAL_CONFIG);
  }

  public static AwsSecretManagerConnectorBuilder buildIAMConfig(AwsSMCredentialSpecAssumeIAMDTO config) {
    return getAwsSMConnectorBuilder(null, AwsSecretManagerCredentialType.ASSUME_IAM_ROLE);
  }

  public static AwsSecretManagerConnectorBuilder buildSTSConfig(AwsSMCredentialSpecAssumeSTSDTO config) {
    int assumeStsRoleDuration = config.getAssumeStsRoleDuration();
    if (assumeStsRoleDuration == 0) {
      assumeStsRoleDuration = STSSessionCredentialsProvider.DEFAULT_DURATION_SECONDS;
    }

    AwsSecretManagerSTSCredential spec = AwsSecretManagerSTSCredential.builder()
                                             .externalId(config.getExternalId())
                                             .roleArn(config.getRoleArn())
                                             .assumeStsRoleDuration(assumeStsRoleDuration)
                                             .build();
    return getAwsSMConnectorBuilder(spec, AwsSecretManagerCredentialType.ASSUME_STS_ROLE);
  }

  private AwsSecretManagerConnectorBuilder getAwsSMConnectorBuilder(
      AwsSecretManagerCredentialSpec spec, AwsSecretManagerCredentialType type) {
    return AwsSecretManagerConnector.builder().credentialType(type).credentialSpec(spec);
  }

  public static AwsSecretManagerDTOBuilder buildFromManualConfig(AwsSecretManagerManualCredential credentialSpec) {
    AwsSMCredentialSpecManualConfigDTO config =
        AwsSMCredentialSpecManualConfigDTO.builder()
            .secretKey(SecretRefHelper.createSecretRef(credentialSpec.getSecretKey()))
            .accessKey(SecretRefHelper.createSecretRef(credentialSpec.getAccessKey()))
            .build();
    return AwsSecretManagerDTO.builder().credential(
        populateCredentialSpec(config, AwsSecretManagerCredentialType.MANUAL_CONFIG));
  }

  public static AwsSecretManagerDTOBuilder buildFromIAMConfig(AwsSecretManagerIAMCredential credentialSpec) {
    return AwsSecretManagerDTO.builder().credential(
        populateCredentialSpec(null, AwsSecretManagerCredentialType.ASSUME_IAM_ROLE));
  }

  public static AwsSecretManagerDTOBuilder buildFromSTSConfig(AwsSecretManagerSTSCredential credentialSpec) {
    AwsSMCredentialSpecAssumeSTSDTO configDTO = AwsSMCredentialSpecAssumeSTSDTO.builder()
                                                    .externalId(credentialSpec.getExternalId())
                                                    .roleArn(credentialSpec.getRoleArn())
                                                    .assumeStsRoleDuration(credentialSpec.getAssumeStsRoleDuration())
                                                    .build();

    return AwsSecretManagerDTO.builder().credential(
        populateCredentialSpec(configDTO, AwsSecretManagerCredentialType.ASSUME_STS_ROLE));
  }

  private AwsSecretManagerCredentialDTO populateCredentialSpec(
      AwsSecretManagerCredentialSpecDTO configDTO, AwsSecretManagerCredentialType type) {
    return AwsSecretManagerCredentialDTO.builder().credentialType(type).config(configDTO).build();
  }
}
