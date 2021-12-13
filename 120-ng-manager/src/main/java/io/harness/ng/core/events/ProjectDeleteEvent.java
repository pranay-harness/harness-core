package io.harness.ng.core.events;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.ResourceType;
import io.harness.event.Event;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.dto.ProjectDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Getter
@NoArgsConstructor
public class ProjectDeleteEvent implements Event {
  private ProjectDTO project;
  private String accountIdentifier;

  public ProjectDeleteEvent(String accountIdentifier, ProjectDTO project) {
    this.project = project;
    this.accountIdentifier = accountIdentifier;
  }

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    return new ProjectScope(accountIdentifier, project.getOrgIdentifier(), project.getIdentifier());
  }

  @JsonIgnore
  @Override
  public Resource getResource() {
    return Resource.builder().identifier(project.getIdentifier()).type(ResourceType.PROJECT.name()).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return "ProjectDeleted";
  }
}
