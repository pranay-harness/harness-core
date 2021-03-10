package io.harness.ng.core;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hibernate.validator.constraints.NotEmpty;

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
