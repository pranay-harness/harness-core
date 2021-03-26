package io.harness.ng.core;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
@Getter
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("project")
public class ProjectScope extends ResourceScope {
  @NotEmpty String accountIdentifier;
  @NotEmpty String orgIdentifier;
  @NotEmpty String projectIdentifier;

  public ProjectScope(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    super("project");
    this.accountIdentifier = accountIdentifier;
    this.orgIdentifier = orgIdentifier;
    this.projectIdentifier = projectIdentifier;
  }
}
