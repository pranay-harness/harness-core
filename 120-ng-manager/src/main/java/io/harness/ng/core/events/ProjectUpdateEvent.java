/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.events;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.ResourceTypeConstants.PROJECT;

import io.harness.annotations.dev.OwnedBy;
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
public class ProjectUpdateEvent implements Event {
  private ProjectDTO newProject;
  private ProjectDTO oldProject;

  private String accountIdentifier;

  public ProjectUpdateEvent(String accountIdentifier, ProjectDTO newProject, ProjectDTO oldProject) {
    this.newProject = newProject;
    this.oldProject = oldProject;
    this.accountIdentifier = accountIdentifier;
  }

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    return new ProjectScope(accountIdentifier, newProject.getOrgIdentifier(), newProject.getIdentifier());
  }

  @JsonIgnore
  @Override
  public Resource getResource() {
    return Resource.builder().identifier(oldProject.getIdentifier()).type(PROJECT).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return "ProjectUpdated";
  }
}
