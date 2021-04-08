package io.harness.connector.mappers.secretmanagermapper;

import com.google.inject.Singleton;
import io.harness.connector.entities.embedded.awskmsconnector.*;
import io.harness.delegate.beans.connector.awskmsconnector.*;
import lombok.extern.slf4j.Slf4j;

import static io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialType.*;

@Singleton
@Slf4j
public class AwsKmsMappingHelper {

    public AwsKmsConnectorDTO.AwsKmsConnectorDTOBuilder buildFromManualConfig(AwsKmsManualCredential credentialSpec) {
        AwsKmsCredentialSpecManualConfigDTO configDTO = AwsKmsCredentialSpecManualConfigDTO.builder()
                .secretKey(credentialSpec.getSecretKey())
                .accessKey(credentialSpec.getAccessKey())
                .build();

        return populateCredentialDTO(populateCredentialSpec(configDTO, MANUAL_CONFIG));
    }

    public AwsKmsConnectorDTO.AwsKmsConnectorDTOBuilder buildFromIAMConfig(AwsKmsIamCredential credentialSpec) {
        AwsKmsCredentialSpecAssumeIAMDTO configDTO = AwsKmsCredentialSpecAssumeIAMDTO.builder()
                .delegateSelectors(credentialSpec.getDelegateSelectors())
                .build();

        return populateCredentialDTO(populateCredentialSpec(configDTO, ASSUME_IAM_ROLE));
    }

    public AwsKmsConnectorDTO.AwsKmsConnectorDTOBuilder buildFromSTSConfig(AwsKmsStsCredential credentialSpec) {
        AwsKmsCredentialSpecAssumeSTSDTO configDTO = AwsKmsCredentialSpecAssumeSTSDTO.builder()
                .delegateSelectors(credentialSpec.getDelegateSelectors())
                .externalName(credentialSpec.getExternalName())
                .roleArn(credentialSpec.getRoleArn())
                .build();

        return populateCredentialDTO(populateCredentialSpec(configDTO, ASSUME_STS_ROLE));
    }

    private AwsKmsConnectorCredentialDTO populateCredentialSpec(AwsKmsCredentialSpecDTO configDTO, AwsKmsCredentialType manualConfig) {
        return AwsKmsConnectorCredentialDTO.builder()
                .credentialType(manualConfig)
                .config(configDTO)
                .build();
    }

    private AwsKmsConnectorDTO.AwsKmsConnectorDTOBuilder populateCredentialDTO(AwsKmsConnectorCredentialDTO credentialDTO) {
        return AwsKmsConnectorDTO.builder().credential(credentialDTO);
    }

    public AwsKmsConnector.AwsKmsConnectorBuilder buildSTSConfig(AwsKmsCredentialSpecAssumeSTSDTO config) {
        AwsKmsStsCredential build = AwsKmsStsCredential.builder()
                .delegateSelectors(config.getDelegateSelectors())
                .externalName(config.getExternalName())
                .roleArn(config.getRoleArn())
                .build();
        return getAwsKmsConnectorBuilder(build, AwsKmsCredentialType.ASSUME_STS_ROLE);
    }

    public AwsKmsConnector.AwsKmsConnectorBuilder buildIAMConfig(AwsKmsCredentialSpecAssumeIAMDTO config) {
        AwsKmsIamCredential build = AwsKmsIamCredential.builder()
                .delegateSelectors(config.getDelegateSelectors())
                .build();
        return getAwsKmsConnectorBuilder(build, AwsKmsCredentialType.ASSUME_IAM_ROLE);
    }

    public AwsKmsConnector.AwsKmsConnectorBuilder buildManualConfig(AwsKmsCredentialSpecManualConfigDTO credentialConfig) {
        AwsKmsManualCredential build = AwsKmsManualCredential.builder()
                .secretKey(credentialConfig.getSecretKey())
                .accessKey(credentialConfig.getAccessKey())
                .build();
        return getAwsKmsConnectorBuilder(build, AwsKmsCredentialType.MANUAL_CONFIG);
    }

    private AwsKmsConnector.AwsKmsConnectorBuilder getAwsKmsConnectorBuilder(AwsKmsCredentialSpec build, AwsKmsCredentialType manualConfig) {
        return AwsKmsConnector.builder().credentialType(manualConfig).credentialSpec(build);
    }
}
