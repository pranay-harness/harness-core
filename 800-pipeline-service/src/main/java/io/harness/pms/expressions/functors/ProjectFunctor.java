/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.expressions.functors;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.EngineFunctorException;
import io.harness.expression.LateBindingValue;
import io.harness.network.SafeHttpCall;
import io.harness.ng.core.dto.ProjectResponse;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.project.remote.ProjectClient;

import java.util.Optional;

@OwnedBy(PIPELINE)
public class ProjectFunctor implements LateBindingValue {
  private final ProjectClient projectClient;
  private final Ambiance ambiance;

  public ProjectFunctor(ProjectClient projectClient, Ambiance ambiance) {
    this.projectClient = projectClient;
    this.ambiance = ambiance;
  }

  @Override
  public Object bind() {
    String accountId = AmbianceHelper.getAccountId(ambiance);
    String orgIdentifier = AmbianceHelper.getOrgIdentifier(ambiance);
    String projectIdentifier = AmbianceHelper.getProjectIdentifier(ambiance);
    if (EmptyPredicate.isEmpty(accountId) || EmptyPredicate.isEmpty(orgIdentifier)
        || EmptyPredicate.isEmpty(projectIdentifier)) {
      return null;
    }

    try {
      Optional<ProjectResponse> resp =
          SafeHttpCall.execute(projectClient.getProject(projectIdentifier, accountId, orgIdentifier)).getData();
      return resp.map(ProjectResponse::getProject).orElse(null);
    } catch (Exception ex) {
      throw new EngineFunctorException(String.format("Invalid project: %s", projectIdentifier), ex);
    }
  }
}
