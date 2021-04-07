package io.harness.ng.userprofile.entities;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitAuthentication;
import io.harness.connector.mappers.awscodecommit.AwsCodeCommitDTOToEntity;
import io.harness.connector.mappers.awscodecommit.AwsCodeCommitEntityToDTO;
import io.harness.ng.userprofile.commons.AwsCodeCommitSCMDTO;
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
@FieldNameConstants(innerTypeName = "AwsCodeCommitSCMKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("io.harness.ng.userprofile.entities.AwsCodeCommitSCM")
public class AwsCodeCommitSCM extends SourceCodeManager {
  AwsCodeCommitAuthentication authentication;

  @Override
  public SCMType getType() {
    return SCMType.AWS_CODE_COMMIT;
  }

  public static class AwsCodeCommitSCMMapper extends SourceCodeManagerMapper<AwsCodeCommitSCMDTO, AwsCodeCommitSCM> {
    @Override
    public AwsCodeCommitSCM toSCMEntity(AwsCodeCommitSCMDTO sourceCodeManagerDTO) {
      AwsCodeCommitSCM awsCodeCommitSCM = AwsCodeCommitSCM.builder()
                                              .authentication(AwsCodeCommitDTOToEntity.buildAwsCodeCommitAuthentication(
                                                  sourceCodeManagerDTO.getAuthentication()))
                                              .build();
      setCommonFieldsEntity(awsCodeCommitSCM, sourceCodeManagerDTO);
      return awsCodeCommitSCM;
    }

    @Override
    public AwsCodeCommitSCMDTO toSCMDTO(AwsCodeCommitSCM sourceCodeManager) {
      AwsCodeCommitSCMDTO awsCodeCommitSCMDTO =
          AwsCodeCommitSCMDTO.builder()
              .authentication(
                  AwsCodeCommitEntityToDTO.buildAwsCodeCommitAuthenticationDTO(sourceCodeManager.getAuthentication()))
              .build();
      setCommonFieldsDTO(sourceCodeManager, awsCodeCommitSCMDTO);
      return awsCodeCommitSCMDTO;
    }
  }
}
