package io.harness.ng.userprofile.commons;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.github.GithubAuthenticationDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@OwnedBy(PL)
@JsonTypeName("AZURE_DEV_OPS")
@Data
@Schema(name = "AzureDevOpsSCM", description = "This Contains details of the Azure DevOps Source Code Manager")
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AzureDevOpsSCMDTO extends SourceCodeManagerDTO {
  @JsonProperty("authentication") GithubAuthenticationDTO authentication;
  @Override
  public SCMType getType() {
    return SCMType.AZURE_DEV_OPS;
  }
}
