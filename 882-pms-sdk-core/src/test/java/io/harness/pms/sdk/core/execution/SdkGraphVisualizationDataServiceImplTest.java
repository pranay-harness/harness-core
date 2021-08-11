package io.harness.pms.sdk.core.execution;

import static io.harness.pms.sdk.core.AmbianceTestUtils.EXECUTION_INSTANCE_ID;
import static io.harness.pms.sdk.core.AmbianceTestUtils.SECTION_RUNTIME_ID;
import static io.harness.rule.OwnerRule.SAHIL;

import static org.mockito.Mockito.verify;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.events.AddStepDetailsInstanceRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.contracts.execution.events.SdkResponseEventType;
import io.harness.pms.sdk.core.AmbianceTestUtils;
import io.harness.pms.sdk.core.PmsSdkCoreTestBase;
import io.harness.pms.sdk.core.response.publishers.SdkResponseEventPublisher;
import io.harness.pms.sdk.core.steps.executables.StepDetailsInfo;
import io.harness.rule.Owner;

import lombok.Builder;
import lombok.Data;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.PIPELINE)
public class SdkGraphVisualizationDataServiceImplTest extends PmsSdkCoreTestBase {
  @Mock SdkResponseEventPublisher sdkResponseEventPublisher;

  @InjectMocks SdkGraphVisualizationDataServiceImpl sdkGraphVisualizationDataService;

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testPublishStepDetailInformation() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    sdkGraphVisualizationDataService.publishStepDetailInformation(
        ambiance, TestStepDetailsInfo.builder().build(), "test");
    AddStepDetailsInstanceRequest addStepDetailsInstanceRequest =
        AddStepDetailsInstanceRequest.newBuilder()
            .setStepDetails(TestStepDetailsInfo.builder().build().toViewJson())
            .setName("test")
            .build();

    SdkResponseEventProto sdkResponseEvent =
        SdkResponseEventProto.newBuilder()
            .setSdkResponseEventType(SdkResponseEventType.ADD_STEP_DETAILS_INSTANCE_REQUEST)
            .setNodeExecutionId(SECTION_RUNTIME_ID)
            .setPlanExecutionId(EXECUTION_INSTANCE_ID)
            .setStepDetailsInstanceRequest(addStepDetailsInstanceRequest)
            .build();
    verify(sdkResponseEventPublisher).publishEvent(sdkResponseEvent);
  }

  @Data
  @Builder
  private static class TestStepDetailsInfo implements StepDetailsInfo {}
}