package io.harness.delegate.task.aws;

import io.harness.aws.AwsAccessKeyCredential;
import io.harness.aws.AwsConfig;
import io.harness.aws.CrossAccountAccess;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.CrossAccountAccessDTO;
import io.harness.encryption.SecretRefData;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class AwsNgConfigMapper {
  @Inject private SecretDecryptionService secretDecryptionService;

  private CrossAccountAccess mapCrossAccountAccess(CrossAccountAccessDTO crossAccountAccess) {
    if (crossAccountAccess == null) {
      return null;
    }
    return CrossAccountAccess.builder()
        .crossAccountRoleArn(crossAccountAccess.getCrossAccountRoleArn())
        .externalId(crossAccountAccess.getExternalId())
        .build();
  }

  public AwsConfig mapAwsConfigWithDecryption(
      AwsCredentialDTO credential, AwsCredentialType awsCredentialType, List<EncryptedDataDetail> encryptionDetails) {
    AwsConfig awsConfig = null;
    switch (awsCredentialType) {
      case INHERIT_FROM_DELEGATE:
        awsConfig = AwsConfig.builder()
                        .isEc2IamCredentials(true)
                        .crossAccountAccess(mapCrossAccountAccess(credential.getCrossAccountAccess()))
                        .build();
        break;
      case MANUAL_CREDENTIALS:
        final AwsManualConfigSpecDTO config = (AwsManualConfigSpecDTO) credential.getConfig();
        secretDecryptionService.decrypt(config, encryptionDetails);
        final SecretRefData secretKeyRef = config.getSecretKeyRef();
        awsConfig = AwsConfig.builder()
                        .crossAccountAccess(mapCrossAccountAccess(credential.getCrossAccountAccess()))
                        .awsAccessKeyCredential(AwsAccessKeyCredential.builder()
                                                    .accessKey(config.getAccessKey())
                                                    .secretKey(getDecryptedValueWithNullCheck(secretKeyRef))
                                                    .build())
                        .build();
        break;
    }
    return awsConfig;
  }

  @VisibleForTesting
  char[] getDecryptedValueWithNullCheck(SecretRefData passwordRef) {
    if (passwordRef != null) {
      return passwordRef.getDecryptedValue();
    }
    return null;
  }
}
