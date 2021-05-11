package io.harness.ngpipeline.expressions.functors;

import io.harness.annotations.dev.ToBeDeleted;
import io.harness.data.structure.EmptyPredicate;
import io.harness.expression.LateBindingValue;
import io.harness.ng.core.services.ProjectService;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;

@Deprecated
@ToBeDeleted
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
    return EmptyPredicate.isEmpty(accountId) || EmptyPredicate.isEmpty(orgIdentifier)
            || EmptyPredicate.isEmpty(projectIdentifier)
        ? null
        : projectService.get(accountId, orgIdentifier, projectIdentifier);
  }
}
