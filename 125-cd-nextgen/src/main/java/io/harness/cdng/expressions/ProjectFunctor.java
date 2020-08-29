package io.harness.cdng.expressions;

import io.harness.ambiance.Ambiance;
import io.harness.cdng.common.AmbianceHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.expression.LateBindingValue;
import io.harness.ng.core.services.ProjectService;

public class ProjectFunctor implements LateBindingValue {
  private final ProjectService projectService;
  private final Ambiance ambiance;

  public ProjectFunctor(ProjectService projectService, Ambiance ambiance) {
    this.projectService = projectService;
    this.ambiance = ambiance;
  }

  @Override
  public Object bind() {
    String accountId = AmbianceHelper.getAccountId(ambiance);
    String projectIdentifier = AmbianceHelper.getProjectIdentifier(ambiance);
    return EmptyPredicate.isEmpty(accountId) || EmptyPredicate.isEmpty(projectIdentifier)
        ? null
        : projectService.get(accountId, projectIdentifier);
  }
}
