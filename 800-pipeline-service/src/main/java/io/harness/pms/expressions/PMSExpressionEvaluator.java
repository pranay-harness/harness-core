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
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.projectmanagerclient.remote.ProjectManagerClient;
import io.harness.steps.StepOutcomeGroup;

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
    addStaticAlias("artifact", "service.artifacts.primary.output");
    addStaticAlias("serviceVariables", "service.variables.output");
    addStaticAlias("env", "infrastructure.environment");
    addGroupAlias(YAMLFieldNameConstants.STAGE, StepOutcomeGroup.STAGE.name());
    addGroupAlias(YAMLFieldNameConstants.STEP, StepOutcomeGroup.STEP.name());
  }
}
