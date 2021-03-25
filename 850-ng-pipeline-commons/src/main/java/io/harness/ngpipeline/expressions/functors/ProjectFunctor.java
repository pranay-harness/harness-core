package io.harness.ngpipeline.expressions.functors;

import static io.harness.data.structure.HasPredicate.hasNone;

import io.harness.expression.LateBindingValue;
import io.harness.ng.core.services.ProjectService;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;

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
    String orgIdentifier = AmbianceHelper.getOrgIdentifier(ambiance);
    return hasNone(accountId) || hasNone(orgIdentifier) || hasNone(projectIdentifier)
        ? null
        : projectService.get(accountId, orgIdentifier, projectIdentifier);
  }
}
