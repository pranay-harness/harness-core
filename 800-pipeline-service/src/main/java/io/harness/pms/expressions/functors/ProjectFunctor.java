package io.harness.pms.expressions.functors;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.FunctorException;
import io.harness.expression.LateBindingValue;
import io.harness.network.SafeHttpCall;
import io.harness.ng.core.dto.ProjectResponse;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.projectmanagerclient.remote.ProjectManagerClient;

import java.util.Optional;

public class ProjectFunctor implements LateBindingValue {
  private final ProjectManagerClient projectManagerClient;
  private final Ambiance ambiance;

  public ProjectFunctor(ProjectManagerClient projectManagerClient, Ambiance ambiance) {
    this.projectManagerClient = projectManagerClient;
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
          SafeHttpCall.execute(projectManagerClient.getProject(projectIdentifier, accountId, orgIdentifier)).getData();
      return resp.map(ProjectResponse::getProject).orElse(null);
    } catch (Exception ex) {
      throw new FunctorException(String.format("Invalid project: %s", projectIdentifier), ex);
    }
  }
}
