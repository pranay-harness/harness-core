/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources.secretsmanagement;

import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.ADMIN_SRE;

import io.harness.beans.EncryptedData;
import io.harness.exception.SecretManagementException;
import io.harness.secretmanagerclient.NGEncryptedDataMetadata;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.ValueType;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.SecretTextDTO;
import io.harness.secretmanagerclient.dto.SecretTextUpdateDTO;

import java.util.Optional;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;

@UtilityClass
@Slf4j
public class EncryptedDataMapper {
  public static EncryptedDataDTO toDTO(EncryptedData encryptedData) {
    if (encryptedData == null) {
      return null;
    }
    EncryptedDataDTO dto = EncryptedDataDTO.builder()
                               .name(encryptedData.getName())
                               .value(encryptedData.getPath())
                               .encryptionType(encryptedData.getEncryptionType())
                               .lastUpdatedAt(encryptedData.getLastUpdatedAt())
                               .type(SecretType.fromSettingVariableType(encryptedData.getType()))
                               .build();
    if (SecretType.SecretText == dto.getType()) {
      if (Optional.ofNullable(encryptedData.getPath()).isPresent()) {
        dto.setValue(encryptedData.getPath());
        dto.setValueType(ValueType.Reference);
      } else {
        dto.setValue(null);
        dto.setValueType(ValueType.Inline);
      }
    }
    if (Optional.ofNullable(encryptedData.getNgMetadata()).isPresent()) {
      NGEncryptedDataMetadata metadata = encryptedData.getNgMetadata();
      dto.setAccount(metadata.getAccountIdentifier());
      dto.setOrg(metadata.getOrgIdentifier());
      dto.setProject(metadata.getProjectIdentifier());
      dto.setIdentifier(metadata.getIdentifier());
      dto.setSecretManager(metadata.getSecretManagerIdentifier());
      dto.setSecretManagerName(metadata.getSecretManagerName()); // TODO{phoenikx} Query from DB instead of saving it
      dto.setDescription(metadata.getDescription());
      dto.setTags(metadata.getTags());
      dto.setDraft(metadata.isDraft());
    }
    return dto;
  }

  public static EncryptedData fromDTO(SecretTextDTO dto) {
    EncryptedData encryptedData = EncryptedData.builder()
                                      .name(dto.getName())
                                      .path(dto.getPath())
                                      .accountId(dto.getAccount())
                                      .type(dto.getSettingVariableType())
                                      .enabled(true)
                                      .build();
    NGEncryptedDataMetadata metadata = NGEncryptedDataMetadata.builder()
                                           .identifier(dto.getIdentifier())
                                           .accountIdentifier(dto.getAccount())
                                           .orgIdentifier(dto.getOrg())
                                           .draft(dto.isDraft())
                                           .projectIdentifier(dto.getProject())
                                           .secretManagerIdentifier(dto.getSecretManager())
                                           .description(dto.getDescription())
                                           .tags(dto.getTags())
                                           .build();
    encryptedData.setNgMetadata(metadata);
    return encryptedData;
  }

  public static EncryptedData fromDTO(EncryptedDataDTO dto) {
    if (!Optional.ofNullable(dto).isPresent()) {
      return null;
    }
    EncryptedData encryptedData = EncryptedData.builder()
                                      .name(dto.getName())
                                      .path(dto.getValue())
                                      .accountId(dto.getAccount())
                                      .type(SecretType.toSettingVariableType(dto.getType()))
                                      .enabled(true)
                                      .build();
    NGEncryptedDataMetadata metadata = NGEncryptedDataMetadata.builder()
                                           .identifier(dto.getIdentifier())
                                           .accountIdentifier(dto.getAccount())
                                           .orgIdentifier(dto.getOrg())
                                           .projectIdentifier(dto.getProject())
                                           .secretManagerIdentifier(dto.getSecretManager())
                                           .secretManagerName(dto.getSecretManagerName())
                                           .description(dto.getDescription())
                                           .tags(dto.getTags())
                                           .draft(dto.isDraft())
                                           .build();
    encryptedData.setNgMetadata(metadata);
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
      updatedEncryptedData.setName(dto.getName());
      if (!Optional.ofNullable(updatedEncryptedData.getNgMetadata()).isPresent()) {
        updatedEncryptedData.setNgMetadata(NGEncryptedDataMetadata.builder().build());
      }
      updatedEncryptedData.getNgMetadata().setDescription(dto.getDescription());
      updatedEncryptedData.getNgMetadata().setTags(dto.getTags());
      updatedEncryptedData.getNgMetadata().setDraft(dto.isDraft());
      return updatedEncryptedData;
    } catch (Exception exception) {
      log.error("Exception while copying object.", exception);
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Unable to copy object", ADMIN_SRE);
    }
  }
}
