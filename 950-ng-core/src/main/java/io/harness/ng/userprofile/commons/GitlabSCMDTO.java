package io.harness.ng.userprofile.commons;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabAuthenticationDTO;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@OwnedBy(PL)
@JsonTypeName("GITLAB")
@Data
@Schema(name = "GitlabSCM", description = "This Contains details of the Gitlab Source Code Manager")
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GitlabSCMDTO extends SourceCodeManagerDTO {
  @Valid @NotNull GitlabAuthenticationDTO authentication;
  @Override
  public SCMType getType() {
    return SCMType.GITLAB;
  }
}
