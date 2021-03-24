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
public class ProjectUpdateEvent implements Event {
  private ProjectDTO newProjectDTO;
  private ProjectDTO oldProjectDTO;

  private String accountIdentifier;

  public ProjectUpdateEvent(String accountIdentifier, ProjectDTO newProjectDTO, ProjectDTO oldProjectDTO) {
    this.newProjectDTO = newProjectDTO;
    this.oldProjectDTO = oldProjectDTO;
    this.accountIdentifier = accountIdentifier;
  }

  public ResourceScope getResourceScope() {
    return new OrgScope(accountIdentifier, oldProjectDTO.getOrgIdentifier());
  }

  public Resource getResource() {
    return Resource.builder().identifier(oldProjectDTO.getIdentifier()).type(PROJECT).build();
  }

  public String getEventType() {
    return "ProjectUpdated";
  }
}
