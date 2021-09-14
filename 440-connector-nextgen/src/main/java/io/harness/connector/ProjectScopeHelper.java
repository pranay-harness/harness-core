/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ng.core.entities.Project.ProjectKeys;

import io.harness.connector.entities.Connector;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.services.ProjectService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class ProjectScopeHelper {
  ProjectService projectService;

  public Map<String, String> createProjectIdentifierProjectNameMap(List<String> projectIdentifiersList) {
    if (isEmpty(projectIdentifiersList)) {
      return Collections.emptyMap();
    }
    Criteria criteria = new Criteria();
    criteria.and(ProjectKeys.identifier).in(projectIdentifiersList);
    List<Project> organizations = projectService.list(criteria, Pageable.unpaged()).toList();
    if (isEmpty(organizations)) {
      return Collections.emptyMap();
    }
    return organizations.stream().collect(Collectors.toMap(Project::getIdentifier, Project::getName));
  }

  public List<String> getProjectIdentifiers(List<Connector> connectors) {
    if (isEmpty(connectors)) {
      return Collections.emptyList();
    }
    return connectors.stream().map(Connector::getProjectIdentifier).collect(Collectors.toList());
  }
}
