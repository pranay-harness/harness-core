package io.harness.mappers;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.secretmanagerclient.NGSecretManagerMetadata;
import io.harness.secretmanagerclient.dto.AwsKmsConfigDTO;
import io.harness.secretmanagerclient.dto.AwsKmsConfigUpdateDTO;
import io.harness.secretmanagerclient.dto.GcpKmsConfigDTO;
import io.harness.secretmanagerclient.dto.GcpKmsConfigUpdateDTO;
import lombok.experimental.UtilityClass;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.KmsConfig;

import java.util.Optional;

import static io.harness.annotations.dev.HarnessTeam.PL;

@UtilityClass
@OwnedBy(PL)
public class AwsKmsConfigMapper {
  public static KmsConfig fromDTO(AwsKmsConfigDTO awsKmsConfigDTO) {
    KmsConfig kmsConfig = new KmsConfig();
    kmsConfig.setName(awsKmsConfigDTO.getName());
    kmsConfig.setAccessKey(awsKmsConfigDTO.getBaseAwsKmsConfigDTO().getAccessKey());
    //TODO: Shashank : Set all parameters according to JsonSubtype kmsConfig.setName();

    kmsConfig.setNgMetadata(SecretManagerConfigMapper.ngMetaDataFromDto(awsKmsConfigDTO));
    kmsConfig.setAccountId(awsKmsConfigDTO.getAccountIdentifier());
    kmsConfig.setEncryptionType(awsKmsConfigDTO.getEncryptionType());
    kmsConfig.setDefault(awsKmsConfigDTO.isDefault());
    return kmsConfig;
  }

  //TODO:Shashank: Which fields can we update here
  public static KmsConfig applyUpdate(KmsConfig kmsConfig, AwsKmsConfigUpdateDTO awsKmsConfigDTO) {
    kmsConfig.setAccessKey(awsKmsConfigDTO.getBaseAwsKmsConfigDTO().getAccessKey());
    //TODO: Shashank : Set all parameters according to JsonSubtype kmsConfig.setName();

    kmsConfig.setDefault(awsKmsConfigDTO.isDefault());
    if (!Optional.ofNullable(kmsConfig.getNgMetadata()).isPresent()) {
      kmsConfig.setNgMetadata(NGSecretManagerMetadata.builder().build());
    }
    kmsConfig.getNgMetadata().setTags(TagMapper.convertToList(awsKmsConfigDTO.getTags()));
    kmsConfig.getNgMetadata().setDescription(awsKmsConfigDTO.getDescription());
    return kmsConfig;
  }
}
