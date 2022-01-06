/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.search.entities.pipeline;

import io.harness.mongo.changestreams.ChangeEvent;
import io.harness.mongo.changestreams.ChangeEvent.ChangeEventBuilder;
import io.harness.mongo.changestreams.ChangeType;
import io.harness.seeddata.SampleDataProviderConstants;

import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.sm.StateType;

import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import java.util.Arrays;

public class PipelineEntityTestUtils {
  public static Pipeline createPipeline(
      String accountId, String appId, String pipelineId, String pipelineName, String envId, String workflowId) {
    PipelineStage pipelineStage = PipelineStage.builder()
                                      .name("STAGE 1")
                                      .pipelineStageElements(Arrays.asList(
                                          PipelineStageElement.builder()
                                              .type(StateType.ENV_STATE.name())
                                              .name(SampleDataProviderConstants.K8S_QA_ENVIRONMENT)
                                              .properties(ImmutableMap.of("workflowId", workflowId, "envId", envId))
                                              .build()))
                                      .build();

    return Pipeline.builder()
        .name(pipelineName)
        .uuid(pipelineId)
        .accountId(accountId)
        .appId(appId)
        .sample(true)
        .pipelineStages(Arrays.asList(pipelineStage))
        .build();
  }

  private static DBObject getPipelineChanges() {
    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("name", "edited_name");
    basicDBObject.put("appId", "appId");
    basicDBObject.put("pipelineStages", "pipelineStages");
    return basicDBObject;
  }

  public static ChangeEvent createPipelineChangeEvent(Pipeline pipeline, ChangeType changeType) {
    ChangeEventBuilder changeEventBuilder = ChangeEvent.builder();
    changeEventBuilder = changeEventBuilder.changeType(changeType)
                             .fullDocument(pipeline)
                             .token("token")
                             .uuid(pipeline.getUuid())
                             .entityType(Pipeline.class);

    if (changeType == ChangeType.UPDATE) {
      changeEventBuilder = changeEventBuilder.changes(getPipelineChanges());
    }

    return changeEventBuilder.build();
  }
}
