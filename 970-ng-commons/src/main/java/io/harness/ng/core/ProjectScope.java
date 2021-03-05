package io.harness.ng.core;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("project")
public class ProjectScope extends ResourceScope {
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;

  public ProjectScope(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    super("project");
    this.accountIdentifier = accountIdentifier;
    this.orgIdentifier = orgIdentifier;
    this.projectIdentifier = projectIdentifier;
  }
}
