package io.harness.seeddata;

import static java.util.Arrays.asList;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.RoleType;
import software.wings.beans.Workflow;
import software.wings.beans.security.UserGroup;
import software.wings.service.impl.workflow.WorkflowServiceTemplateHelper;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.UserGroupService;
import software.wings.sm.StateType;

@Singleton
public class PipelineSeedDataProvider {
  @Inject private PipelineService pipelineService;
  @Inject private UserGroupService userGroupService;

  public Pipeline createPipeline(String accountId, String appId, Workflow workflow, String qaEnvId,
      String qaInframappingId, String prodEnvId, String profInframappingId) {
    UserGroup userGroup = userGroupService.fetchUserGroupByName(accountId, RoleType.ACCOUNT_ADMIN.getDisplayName());
    String userGroupId = userGroup == null ? null : userGroup.getUuid();

    String envName = WorkflowServiceTemplateHelper.getTemplatizedEnvVariableName(
        workflow.getOrchestrationWorkflow().getUserVariables());

    String serviceInfraName =
        WorkflowServiceTemplateHelper
            .getServiceInfrastructureWorkflowVariables(workflow.getOrchestrationWorkflow().getUserVariables())
            .get(0);

    PipelineStage qaStage =
        PipelineStage.builder()
            .name("STAGE 1")
            .pipelineStageElements(
                asList(PipelineStageElement.builder()
                           .type(StateType.ENV_STATE.name())
                           .name(SeedDataProviderConstants.KUBE_QA_ENVIRONMENT)
                           .properties(ImmutableMap.of("workflowId", workflow.getUuid(), "envId", qaEnvId))
                           .workflowVariables(ImmutableMap.of(envName, qaEnvId, serviceInfraName, qaInframappingId))
                           .build()))
            .build();

    PipelineStage approvalStage =
        PipelineStage.builder()
            .name("STAGE 2")
            .pipelineStageElements(asList(PipelineStageElement.builder()
                                              .name("Approval 1")
                                              .properties(ImmutableMap.of("userGroups", asList(userGroupId)))
                                              .type(StateType.APPROVAL.name())
                                              .build()))
            .build();

    PipelineStage prodStage =
        PipelineStage.builder()
            .name("STAGE 3")
            .pipelineStageElements(
                asList(PipelineStageElement.builder()
                           .type(StateType.ENV_STATE.name())
                           .name(SeedDataProviderConstants.KUBE_PROD_ENVIRONMENT)
                           .properties(ImmutableMap.of("workflowId", workflow.getUuid(), "envId", prodEnvId))
                           .workflowVariables(ImmutableMap.of(envName, prodEnvId, serviceInfraName, profInframappingId))
                           .build()))
            .build();

    Pipeline pipeline = Pipeline.builder()
                            .name(SeedDataProviderConstants.KUBE_PIPELINE_NAME)
                            .appId(appId)
                            .pipelineStages(asList(qaStage, approvalStage, prodStage))
                            .build();

    return pipelineService.save(pipeline);
  }
}
