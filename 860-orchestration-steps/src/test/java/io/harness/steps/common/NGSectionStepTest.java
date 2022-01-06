/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.steps.common;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationStepsTestBase;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponseNotifyData;
import io.harness.rule.Owner;
import io.harness.tasks.ResponseData;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class NGSectionStepTest extends OrchestrationStepsTestBase {
  @Inject private NGSectionStep ngSectionStep;
  private static final String CHILD_ID = generateUuid();

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void shouldTestObtainChildren() {
    Ambiance ambiance = Ambiance.newBuilder().build();
    StepInputPackage inputPackage = StepInputPackage.builder().build();
    NGSectionStepParameters stateParameters =
        NGSectionStepParameters.builder().childNodeId(CHILD_ID).logMessage("log").build();
    ChildExecutableResponse childExecutableResponse =
        ngSectionStep.obtainChild(ambiance, stateParameters, inputPackage);

    assertThat(childExecutableResponse).isNotNull();
    assertThat(childExecutableResponse.getChildNodeId()).isEqualTo(CHILD_ID);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void shouldTestHandleChildResponse() {
    Ambiance ambiance = Ambiance.newBuilder().build();
    NGSectionStepParameters stateParameters =
        NGSectionStepParameters.builder().childNodeId(CHILD_ID).logMessage("log").build();

    Map<String, ResponseData> responseDataMap =
        ImmutableMap.<String, ResponseData>builder()
            .put(CHILD_ID, StepResponseNotifyData.builder().status(Status.FAILED).build())
            .build();

    StepResponse stepResponse = ngSectionStep.handleChildResponse(ambiance, stateParameters, responseDataMap);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.FAILED);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetStepParametersClass() {
    assertThat(ngSectionStep.getStepParametersClass()).isEqualTo(NGSectionStepParameters.class);
  }
}
