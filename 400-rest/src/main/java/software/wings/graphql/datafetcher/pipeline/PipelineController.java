/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.pipeline;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.Pipeline;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.type.QLPipeline.QLPipelineBuilder;

import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class PipelineController {
  public static void populatePipeline(Pipeline pipeline, QLPipelineBuilder builder) {
    builder.id(pipeline.getUuid())
        .name(pipeline.getName())
        .appId(pipeline.getAppId())
        .description(pipeline.getDescription())
        .createdAt(pipeline.getCreatedAt())
        .createdBy(UserController.populateUser(pipeline.getCreatedBy()));
  }
}
