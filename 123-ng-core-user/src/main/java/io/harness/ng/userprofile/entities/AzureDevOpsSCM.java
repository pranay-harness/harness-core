/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.userprofile.entities;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.githubconnector.GithubAuthentication;
import io.harness.connector.mappers.githubconnector.GithubDTOToEntity;
import io.harness.connector.mappers.githubconnector.GithubEntityToDTO;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.ng.userprofile.commons.AzureDevOpsSCMDTO;
import io.harness.ng.userprofile.commons.SCMType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(PL)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "AzureDevOpsSCMKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("io.harness.ng.userprofile.entities.AzureDevOpsSCM")
public class AzureDevOpsSCM extends SourceCodeManager {
  GitAuthType authType;
  GithubAuthentication authenticationDetails;

  @Override
  public SCMType getType() {
    return SCMType.AZURE_DEV_OPS;
  }

  public static class AzureDevOpsSCMMapper extends SourceCodeManagerMapper<AzureDevOpsSCMDTO, AzureDevOpsSCM> {
    @Override
    public AzureDevOpsSCM toSCMEntity(AzureDevOpsSCMDTO sourceCodeManagerDTO) {
      AzureDevOpsSCM azureDevOpsSCM = AzureDevOpsSCM.builder()
                                          .authType(sourceCodeManagerDTO.getAuthentication().getAuthType())
                                          .authenticationDetails(GithubDTOToEntity.buildAuthenticationDetails(
                                              sourceCodeManagerDTO.getAuthentication().getAuthType(),
                                              sourceCodeManagerDTO.getAuthentication().getCredentials()))
                                          .build();
      setCommonFieldsEntity(azureDevOpsSCM, sourceCodeManagerDTO);
      return azureDevOpsSCM;
    }

    @Override
    public AzureDevOpsSCMDTO toSCMDTO(AzureDevOpsSCM sourceCodeManager) {
      AzureDevOpsSCMDTO azureDevOpsSCMDTO =
          AzureDevOpsSCMDTO.builder()
              .authentication(GithubEntityToDTO.buildGithubAuthentication(
                  sourceCodeManager.getAuthType(), sourceCodeManager.getAuthenticationDetails()))
              .build();
      setCommonFieldsDTO(sourceCodeManager, azureDevOpsSCMDTO);
      return azureDevOpsSCMDTO;
    }
  }
}
