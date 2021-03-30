package io.harness.ng.userprofile.entities;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.githubconnector.GithubAuthentication;
import io.harness.connector.mappers.githubconnector.GithubDTOToEntity;
import io.harness.connector.mappers.githubconnector.GithubEntityToDTO;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.ng.userprofile.commons.GithubSCMDTO;
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
@EqualsAndHashCode
@FieldNameConstants(innerTypeName = "GithubSCMKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("io.harness.ng.userprofile.entities.GithubSCM")
public class GithubSCM extends SourceCodeManager {
  GitAuthType authType;
  GithubAuthentication authenticationDetails;
  @Override
  public SCMType getType() {
    return SCMType.GITHUB;
  }

  public static class GithubSCMMapper extends SourceCodeManagerMapper<GithubSCMDTO, GithubSCM> {
    @Override
    public GithubSCM toSCMEntity(GithubSCMDTO sourceCodeManagerDTO) {
      GithubSCM githubSCM = GithubSCM.builder()
                                .authType(sourceCodeManagerDTO.getGithubAuthenticationDTO().getAuthType())
                                .authenticationDetails(GithubDTOToEntity.buildAuthenticationDetails(
                                    sourceCodeManagerDTO.getGithubAuthenticationDTO().getAuthType(),
                                    sourceCodeManagerDTO.getGithubAuthenticationDTO().getCredentials()))
                                .build();
      setCommonFieldsEntity(githubSCM, sourceCodeManagerDTO);
      return githubSCM;
    }

    @Override
    public GithubSCMDTO toSCMDTO(GithubSCM sourceCodeManager) {
      GithubSCMDTO githubSCMDTO =
          GithubSCMDTO.builder()
              .githubAuthenticationDTO(GithubEntityToDTO.buildGithubAuthentication(
                  sourceCodeManager.getAuthType(), sourceCodeManager.getAuthenticationDetails()))
              .build();
      setCommonFieldsDTO(sourceCodeManager, githubSCMDTO);
      return githubSCMDTO;
    }
  }
}
