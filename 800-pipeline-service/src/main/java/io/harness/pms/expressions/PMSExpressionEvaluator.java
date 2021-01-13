package io.harness.pms.expressions;

import io.harness.engine.expressions.AmbianceExpressionEvaluator;
import io.harness.engine.expressions.functors.NodeExecutionEntityType;
import io.harness.expression.VariableResolverTracker;
import io.harness.ngpipeline.expressions.functors.EventPayloadFunctor;
import io.harness.organizationmanagerclient.remote.OrganizationManagerClient;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.expressions.functors.OrgFunctor;
import io.harness.pms.expressions.functors.ProjectFunctor;
import io.harness.pms.plan.execution.SetupAbstractionKeys;
import io.harness.projectmanagerclient.remote.ProjectManagerClient;

import com.google.inject.Inject;
import java.util.Set;

public class PMSExpressionEvaluator extends AmbianceExpressionEvaluator {
  @Inject private OrganizationManagerClient organizationManagerClient;
  @Inject private ProjectManagerClient projectManagerClient;

  public PMSExpressionEvaluator(VariableResolverTracker variableResolverTracker, Ambiance ambiance,
      Set<NodeExecutionEntityType> entityTypes, boolean refObjectSpecific) {
    super(variableResolverTracker, ambiance, entityTypes, refObjectSpecific);
  }

  @Override
  protected void initialize() {
    super.initialize();
    addToContext("org", new OrgFunctor(organizationManagerClient, ambiance));
    addToContext("project", new ProjectFunctor(projectManagerClient, ambiance));
    addToContext(SetupAbstractionKeys.eventPayload, new EventPayloadFunctor(ambiance));
  }
}
