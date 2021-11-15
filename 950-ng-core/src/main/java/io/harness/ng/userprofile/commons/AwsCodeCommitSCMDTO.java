package io.harness.ng.userprofile.commons;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthentication;

import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@OwnedBy(PL)
@JsonTypeName("AWS_CODE_COMMIT")
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AwsCodeCommitSCMDTO extends SourceCodeManagerDTO {
  @Valid @NotNull AwsCodeCommitAuthentication authentication;

  @Override
  public SCMType getType() {
    return SCMType.AWS_CODE_COMMIT;
  }
}
