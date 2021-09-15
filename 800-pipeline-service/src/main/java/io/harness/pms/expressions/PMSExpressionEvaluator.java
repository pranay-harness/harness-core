package io.harness.pms.expressions;

import io.harness.ModuleType;
import io.harness.account.AccountClient;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.CollectionUtils;
import io.harness.engine.executions.plan.PlanExecutionMetadataService;
import io.harness.engine.expressions.AmbianceExpressionEvaluator;
import io.harness.engine.expressions.functors.NodeExecutionEntityType;
import io.harness.expression.VariableResolverTracker;
import io.harness.ngtriggers.expressions.functors.EventPayloadFunctor;
import io.harness.ngtriggers.expressions.functors.TriggerFunctor;
import io.harness.organization.remote.OrganizationClient;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.expression.RemoteFunctorServiceGrpc.RemoteFunctorServiceBlockingStub;
import io.harness.pms.expressions.functors.AccountFunctor;
import io.harness.pms.expressions.functors.ImagePullSecretFunctor;
import io.harness.pms.expressions.functors.OrgFunctor;
import io.harness.pms.expressions.functors.ProjectFunctor;
import io.harness.pms.expressions.functors.RemoteExpressionFunctor;
import io.harness.pms.expressions.utils.ImagePullSecretUtils;
import io.harness.pms.plan.execution.SetupAbstractionKeys;
import io.harness.pms.sdk.PmsSdkInstance;
import io.harness.pms.sdk.PmsSdkInstanceService;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.project.remote.ProjectClient;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.PIPELINE)
public class PMSExpressionEvaluator extends AmbianceExpressionEvaluator {
  @Inject Map<ModuleType, RemoteFunctorServiceBlockingStub> remoteFunctorServiceBlockingStubMap;
  @Inject @Named("PRIVILEGED") private AccountClient accountClient;
  @Inject @Named("PRIVILEGED") private OrganizationClient organizationClient;
  @Inject @Named("PRIVILEGED") private ProjectClient projectClient;
  @Inject private ImagePullSecretUtils imagePullSecretUtils;
  @Inject private PlanExecutionMetadataService planExecutionMetadataService;
  @Inject PmsSdkInstanceService pmsSdkInstanceService;

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
    addToContext(SetupAbstractionKeys.eventPayload, new EventPayloadFunctor(ambiance, planExecutionMetadataService));
    addToContext(SetupAbstractionKeys.trigger, new TriggerFunctor(ambiance, planExecutionMetadataService));
    List<PmsSdkInstance> pmsSdkInstances = pmsSdkInstanceService.getActiveInstances();

    pmsSdkInstances.forEach(e -> {
      for (Map.Entry<String, String> entry : CollectionUtils.emptyIfNull(e.getStaticAliases()).entrySet()) {
        addStaticAlias(entry.getKey(), entry.getValue());
      }
    });

    pmsSdkInstances.forEach(e -> {
      for (String functorKey : CollectionUtils.emptyIfNull(e.getSdkFunctors())) {
        addToContext(functorKey,
            RemoteExpressionFunctor.builder()
                .ambiance(ambiance)
                .remoteFunctorServiceBlockingStub(
                    remoteFunctorServiceBlockingStubMap.get(ModuleType.fromString(e.getName())))
                .functorKey(functorKey)
                .build());
      }
    });

    // Group aliases
    // TODO: Replace with step category
    addGroupAlias(YAMLFieldNameConstants.STAGE, StepOutcomeGroup.STAGE.name());
    addGroupAlias(YAMLFieldNameConstants.STEP, StepOutcomeGroup.STEP.name());
  }
}
