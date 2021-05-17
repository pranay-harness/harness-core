package io.harness.pms.expressions;

import io.harness.account.AccountClient;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.expressions.AmbianceExpressionEvaluator;
import io.harness.engine.expressions.OrchestrationConstants;
import io.harness.engine.expressions.functors.NodeExecutionEntityType;
import io.harness.expression.VariableResolverTracker;
import io.harness.ngtriggers.expressions.functors.EventPayloadFunctor;
import io.harness.ngtriggers.expressions.functors.TriggerFunctor;
import io.harness.organization.remote.OrganizationClient;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.expressions.functors.AccountFunctor;
import io.harness.pms.expressions.functors.ImagePullSecretFunctor;
import io.harness.pms.expressions.functors.OrgFunctor;
import io.harness.pms.expressions.functors.ProjectFunctor;
import io.harness.pms.expressions.utils.ImagePullSecretUtils;
import io.harness.pms.plan.execution.SetupAbstractionKeys;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.project.remote.ProjectClient;
import io.harness.steps.StepOutcomeGroup;

import com.google.inject.Inject;
import java.util.Set;

@OwnedBy(HarnessTeam.PIPELINE)
public class PMSExpressionEvaluator extends AmbianceExpressionEvaluator {
  @Inject private AccountClient accountClient;
  @Inject private OrganizationClient organizationClient;
  @Inject private ProjectClient projectClient;
  @Inject private ImagePullSecretUtils imagePullSecretUtils;

  public PMSExpressionEvaluator(VariableResolverTracker variableResolverTracker, Ambiance ambiance,
      Set<NodeExecutionEntityType> entityTypes, boolean refObjectSpecific) {
    super(variableResolverTracker, ambiance, entityTypes, refObjectSpecific);
  }

  @Override
  protected void initialize() {
    super.initialize();
    // NG access functors
    addToContext("account", new AccountFunctor(accountClient, ambiance));
    addToContext("org", new OrgFunctor(organizationClient, ambiance));
    addToContext("project", new ProjectFunctor(projectClient, ambiance));

    // Artifact pull secret functor
    addToContext(ImagePullSecretFunctor.IMAGE_PULL_SECRET,
        ImagePullSecretFunctor.builder()
            .imagePullSecretUtils(imagePullSecretUtils)
            .pmsOutcomeService(getPmsOutcomeService())
            .ambiance(ambiance)
            .build());

    // Trigger functors
    addToContext(SetupAbstractionKeys.eventPayload, new EventPayloadFunctor(ambiance));
    addToContext(SetupAbstractionKeys.trigger, new TriggerFunctor(ambiance));

    // Service aliases
    addStaticAlias("serviceConfig", "stage.spec.serviceConfig");
    addStaticAlias("serviceDefinition", "stage.spec.serviceConfig.serviceDefinition");
    addStaticAlias("artifact", "stage.spec.serviceConfig.serviceDefinition.spec.artifacts.primary.output");
    addStaticAlias("infra", "stage.spec.infrastructure.output");

    // Status aliases
    addStaticAlias(OrchestrationConstants.STAGE_SUCCESS, "<+stage.currentStatus> == \"SUCCEEDED\"");
    addStaticAlias(OrchestrationConstants.STAGE_FAILURE,
        "<+stage.currentStatus> == \"FAILED\" || <+stage.currentStatus> == \"ERRORED\" || <+stage.currentStatus> == \"EXPIRED\"");
    addStaticAlias(OrchestrationConstants.PIPELINE_FAILURE,
        "<+pipeline.currentStatus> == \"FAILED\" || <+pipeline.currentStatus> == \"ERRORED\" || <+pipeline.currentStatus> == \"EXPIRED\"");
    addStaticAlias(OrchestrationConstants.PIPELINE_SUCCESS, "<+pipeline.currentStatus> == \"SUCCEEDED\"");
    addStaticAlias(OrchestrationConstants.ALWAYS, "true");

    // Group aliases
    addGroupAlias(YAMLFieldNameConstants.STAGE, StepOutcomeGroup.STAGE.name());
    addGroupAlias(YAMLFieldNameConstants.STEP, StepOutcomeGroup.STEP.name());
  }
}
