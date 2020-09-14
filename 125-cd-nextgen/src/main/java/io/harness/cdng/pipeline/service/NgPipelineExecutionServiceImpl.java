package io.harness.cdng.pipeline.service;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.EmbeddedUser;
import io.harness.cdng.common.beans.SetupAbstractionKeys;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.engine.OrchestrationService;
import io.harness.exception.GeneralException;
import io.harness.execution.PlanExecution;
import io.harness.executionplan.service.ExecutionPlanCreatorService;
import io.harness.plan.Plan;
import io.harness.yaml.utils.YamlPipelineUtils;
import software.wings.beans.User;
import software.wings.security.UserThreadLocal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class NgPipelineExecutionServiceImpl implements NgPipelineExecutionService {
  @Inject private OrchestrationService orchestrationService;
  @Inject private ExecutionPlanCreatorService executionPlanCreatorService;

  @Override
  public PlanExecution triggerPipeline(
      String pipelineYaml, String accountId, String orgId, String projectId, EmbeddedUser user) {
    final CDPipeline cdPipeline;
    try {
      cdPipeline = YamlPipelineUtils.read(pipelineYaml, CDPipeline.class);
      Map<String, Object> contextAttributes = new HashMap<>();

      final Plan planForPipeline =
          executionPlanCreatorService.createPlanForPipeline(cdPipeline, accountId, contextAttributes);
      return orchestrationService.startExecution(planForPipeline,
          ImmutableMap.of(SetupAbstractionKeys.accountId, accountId, SetupAbstractionKeys.orgIdentifier, orgId,
              SetupAbstractionKeys.projectIdentifier, projectId),
          user != null ? user : getEmbeddedUser());
    } catch (IOException e) {
      throw new GeneralException("error while de-serializing Yaml", e);
    }
  }

  private EmbeddedUser getEmbeddedUser() {
    User user = UserThreadLocal.get();
    return EmbeddedUser.builder().uuid(user.getUuid()).email(user.getEmail()).name(user.getName()).build();
  }
}
