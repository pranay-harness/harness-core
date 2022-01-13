/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.metadata;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.PipelineStageExecution;

import java.util.Collections;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SkipConditionMetadataTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFromPipelineStageExecution() {
    assertThat(SkipConditionMetadata.fromPipelineStageExecution(null, PipelineStage.builder().build())).isNull();
    assertThat(SkipConditionMetadata.fromPipelineStageExecution(PipelineStageExecution.builder().build(), null))
        .isNull();
    assertThat(SkipConditionMetadata.fromPipelineStageExecution(
                   PipelineStageExecution.builder().build(), PipelineStage.builder().build()))
        .isNull();

    SkipConditionMetadata skipConditionMetadata = SkipConditionMetadata.fromPipelineStageExecution(
        PipelineStageExecution.builder().status(ExecutionStatus.SUCCESS).build(),
        PipelineStage.builder()
            .pipelineStageElements(Collections.singletonList(PipelineStageElement.builder().build()))
            .build());
    assertThat(skipConditionMetadata).isNull();

    skipConditionMetadata = SkipConditionMetadata.fromPipelineStageExecution(
        PipelineStageExecution.builder().status(ExecutionStatus.SUCCESS).build(),
        PipelineStage.builder()
            .pipelineStageElements(
                Collections.singletonList(PipelineStageElement.builder().disableAssertion("false").build()))
            .build());
    assertThat(skipConditionMetadata).isNull();

    skipConditionMetadata = SkipConditionMetadata.fromPipelineStageExecution(
        PipelineStageExecution.builder().status(ExecutionStatus.SKIPPED).build(),
        PipelineStage.builder()
            .pipelineStageElements(
                Collections.singletonList(PipelineStageElement.builder().disableAssertion("true").build()))
            .build());
    assertThat(skipConditionMetadata).isNotNull();
    assertThat(skipConditionMetadata.getAssertion()).isEqualTo("SKIP_ALWAYS");
    assertThat(skipConditionMetadata.isSkipped()).isTrue();

    String assertion = "${var} == 'qa'";
    skipConditionMetadata = SkipConditionMetadata.fromPipelineStageExecution(
        PipelineStageExecution.builder().status(ExecutionStatus.SUCCESS).build(),
        PipelineStage.builder()
            .pipelineStageElements(
                Collections.singletonList(PipelineStageElement.builder().disableAssertion(assertion).build()))
            .build());
    assertThat(skipConditionMetadata).isNotNull();
    assertThat(skipConditionMetadata.getAssertion()).isEqualTo(assertion);
    assertThat(skipConditionMetadata.isSkipped()).isFalse();
  }
}
