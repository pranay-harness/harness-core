package io.harness.ng.core.auditevent;

import static io.harness.audit.ResourceTypeConstants.PROJECT;

import io.harness.event.Event;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.dto.ProjectDTO;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProjectDeleteEvent implements Event {
  private ProjectDTO project;
  private String accountIdentifier;

  public ProjectDeleteEvent(String accountIdentifier, ProjectDTO project) {
    this.project = project;
    this.accountIdentifier = accountIdentifier;
  }

  public ResourceScope getResourceScope() {
    return new OrgScope(accountIdentifier, project.getOrgIdentifier());
  }

  public Resource getResource() {
    return Resource.builder().identifier(project.getIdentifier()).type(PROJECT).build();
  }

  public String getEventType() {
    return "ProjectDeleted";
  }
}
