package software.wings.resources.secretsmanagement;

import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.ADMIN_SRE;

import io.harness.secretmanagerclient.NGEncryptedDataMetadata;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.SecretTextCreateDTO;
import io.harness.secretmanagerclient.dto.SecretTextUpdateDTO;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretManagementException;
import software.wings.settings.SettingVariableTypes;

import java.util.Optional;
import javax.validation.constraints.NotNull;

@UtilityClass
@Slf4j
public class EncryptedDataMapper {
  public static EncryptedDataDTO toDTO(@NotNull EncryptedData encryptedData) {
    EncryptedDataDTO dto = EncryptedDataDTO.builder()
                               .name(encryptedData.getName())
                               .encryptionType(encryptedData.getEncryptionType())
                               .fileSize(encryptedData.getFileSize())
                               .id(encryptedData.getUuid())
                               .path(encryptedData.getPath())
                               .secretManagerId(encryptedData.getKmsId())
                               .lastUpdatedAt(encryptedData.getLastUpdatedAt())
                               .type(encryptedData.getType())
                               .build();
    if (Optional.ofNullable(encryptedData.getNgMetadata()).isPresent()) {
      NGEncryptedDataMetadata metadata = encryptedData.getNgMetadata();
      dto.setIdentifier(metadata.getIdentifier());
      dto.setAccountIdentifier(metadata.getAccountIdentifier());
      dto.setOrgIdentifier(metadata.getOrgIdentifier());
      dto.setSecretManagerIdentifier(metadata.getSecretManagerIdentifier());
      dto.setProjectIdentifier(metadata.getProjectIdentifier());
      dto.setSecretManagerName(metadata.getSecretManagerName());
      dto.setDescription(metadata.getDescription());
      dto.setTags(metadata.getTags());
    }
    return dto;
  }

  public static EncryptedData fromDTO(SecretTextCreateDTO dto) {
    EncryptedData encryptedData = EncryptedData.builder()
                                      .name(dto.getName())
                                      .path(dto.getPath())
                                      .accountId(dto.getAccountIdentifier())
                                      .type(SettingVariableTypes.SECRET_TEXT)
                                      .enabled(true)
                                      .build();
    NGEncryptedDataMetadata metadata = NGEncryptedDataMetadata.builder()
                                           .identifier(dto.getIdentifier())
                                           .accountIdentifier(dto.getAccountIdentifier())
                                           .orgIdentifier(dto.getOrgIdentifier())
                                           .projectIdentifier(dto.getProjectIdentifier())
                                           .secretManagerIdentifier(dto.getSecretManagerIdentifier())
                                           .secretManagerName(dto.getSecretManagerName())
                                           .description(dto.getDescription())
                                           .tags(dto.getTags())
                                           .build();
    encryptedData.setNgMetadata(metadata);
    return encryptedData;
  }

  public static EncryptedData fromDTO(EncryptedDataDTO dto) {
    EncryptedData encryptedData = EncryptedData.builder()
                                      .name(dto.getName())
                                      .path(dto.getPath())
                                      .accountId(dto.getAccountIdentifier())
                                      .type(dto.getType())
                                      .enabled(true)
                                      .build();
    NGEncryptedDataMetadata metadata = NGEncryptedDataMetadata.builder()
                                           .identifier(dto.getIdentifier())
                                           .accountIdentifier(dto.getAccountIdentifier())
                                           .orgIdentifier(dto.getOrgIdentifier())
                                           .projectIdentifier(dto.getProjectIdentifier())
                                           .secretManagerIdentifier(dto.getSecretManagerIdentifier())
                                           .secretManagerName(dto.getSecretManagerName())
                                           .description(dto.getDescription())
                                           .tags(dto.getTags())
                                           .build();
    encryptedData.setNgMetadata(metadata);
    encryptedData.setFileSize(dto.getFileSize());
    encryptedData.setLastUpdatedAt(dto.getLastUpdatedAt());
    return encryptedData;
  }

  public static EncryptedData applyUpdate(SecretTextUpdateDTO dto, EncryptedData encryptedData) {
    if (dto == null || encryptedData == null) {
      return null;
    }
    EncryptedData updatedEncryptedData;
    try {
      updatedEncryptedData = (EncryptedData) BeanUtils.cloneBean(encryptedData);
      updatedEncryptedData.setPath(dto.getPath());
      if (!Optional.ofNullable(updatedEncryptedData.getNgMetadata()).isPresent()) {
        updatedEncryptedData.setNgMetadata(NGEncryptedDataMetadata.builder().build());
      }
      updatedEncryptedData.getNgMetadata().setDescription(dto.getDescription());
      return updatedEncryptedData;
    } catch (Exception exception) {
      logger.error("Exception while copying object.", exception);
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Unable to copy object", ADMIN_SRE);
    }
  }
}
