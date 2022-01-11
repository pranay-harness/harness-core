/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.seeddata;

import static java.util.Arrays.asList;

import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.RoleType;
import software.wings.beans.security.UserGroup;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.UserGroupService;
import software.wings.sm.StateType;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PipelineSampleDataProvider {
  @Inject private PipelineService pipelineService;
  @Inject private UserGroupService userGroupService;

  public Pipeline createPipeline(
      String accountId, String appId, String qaWorkflowId, String qaEnvId, String prodWorkflowId, String prodEnvId) {
    UserGroup userGroup = userGroupService.fetchUserGroupByName(accountId, RoleType.ACCOUNT_ADMIN.getDisplayName());
    String userGroupId = userGroup == null ? null : userGroup.getUuid();

    PipelineStage qaStage =
        PipelineStage.builder()
            .name("STAGE 1")
            .pipelineStageElements(asList(PipelineStageElement.builder()
                                              .type(StateType.ENV_STATE.name())
                                              .name(SampleDataProviderConstants.K8S_QA_ENVIRONMENT)
                                              .properties(ImmutableMap.of("workflowId", qaWorkflowId, "envId", qaEnvId))
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

    PipelineStage prodStage = PipelineStage.builder()
                                  .name("STAGE 3")
                                  .pipelineStageElements(asList(
                                      PipelineStageElement.builder()
                                          .type(StateType.ENV_STATE.name())
                                          .name(SampleDataProviderConstants.K8S_PROD_ENVIRONMENT)
                                          .properties(ImmutableMap.of("workflowId", prodWorkflowId, "envId", prodEnvId))
                                          .build()))
                                  .build();

    Pipeline pipeline = Pipeline.builder()
                            .name(SampleDataProviderConstants.K8S_PIPELINE_NAME)
                            .appId(appId)
                            .sample(true)
                            .pipelineStages(asList(qaStage, approvalStage, prodStage))
                            .build();

    return pipelineService.save(pipeline);
  }
}
