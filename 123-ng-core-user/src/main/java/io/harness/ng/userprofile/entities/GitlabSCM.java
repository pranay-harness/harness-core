/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.userprofile.entities;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabAuthentication;
import io.harness.connector.mappers.gitlabconnector.GitlabDTOToEntity;
import io.harness.connector.mappers.gitlabconnector.GitlabEntityToDTO;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.ng.userprofile.commons.GitlabSCMDTO;
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
@FieldNameConstants(innerTypeName = "GitlabSCMKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("io.harness.ng.userprofile.entities.GitlabSCM")
public class GitlabSCM extends SourceCodeManager {
  GitAuthType authType;
  GitlabAuthentication authenticationDetails;
  @Override
  public SCMType getType() {
    return SCMType.GITLAB;
  }

  public static class GitlabSCMMapper extends SourceCodeManagerMapper<GitlabSCMDTO, GitlabSCM> {
    @Override
    public GitlabSCM toSCMEntity(GitlabSCMDTO sourceCodeManagerDTO) {
      GitlabSCM gitlabSCM = GitlabSCM.builder()
                                .authType(sourceCodeManagerDTO.getAuthentication().getAuthType())
                                .authenticationDetails(GitlabDTOToEntity.buildAuthenticationDetails(
                                    sourceCodeManagerDTO.getAuthentication().getAuthType(),
                                    sourceCodeManagerDTO.getAuthentication().getCredentials()))
                                .build();
      setCommonFieldsEntity(gitlabSCM, sourceCodeManagerDTO);
      return gitlabSCM;
    }

    @Override
    public GitlabSCMDTO toSCMDTO(GitlabSCM sourceCodeManager) {
      GitlabSCMDTO gitlabSCMDTO =
          GitlabSCMDTO.builder()
              .authentication(GitlabEntityToDTO.buildGitlabAuthentication(
                  sourceCodeManager.getAuthType(), sourceCodeManager.getAuthenticationDetails()))
              .build();
      setCommonFieldsDTO(sourceCodeManager, gitlabSCMDTO);
      return gitlabSCMDTO;
    }
  }
}
