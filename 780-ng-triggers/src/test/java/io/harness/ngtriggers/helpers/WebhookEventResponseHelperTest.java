package io.harness.ngtriggers.helpers;

import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.INVALID_PAYLOAD;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.INVALID_RUNTIME_INPUT_YAML;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.NO_ENABLED_TRIGGER_FOUND_FOR_REPO;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.NO_MATCHING_TRIGGER_FOR_REPO;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.SCM_SERVICE_CONNECTION_FAILED;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.TARGET_DID_NOT_EXECUTE;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.TARGET_EXECUTION_REQUESTED;
import static io.harness.ngtriggers.beans.target.TargetType.PIPELINE;
import static io.harness.pms.contracts.execution.Status.RUNNING;
import static io.harness.rule.OwnerRule.ADWAIT;

import static io.grpc.Status.UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.PlanExecution;
import io.harness.ngpipeline.pipeline.beans.resources.NGPipelineExecutionResponseDTO;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.response.TargetExecutionSummary;
import io.harness.ngtriggers.beans.response.WebhookEventResponse;
import io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus;
import io.harness.ngtriggers.beans.scm.ParsePayloadResponse;
import io.harness.ngtriggers.beans.scm.ParsePayloadResponse.ParsePayloadResponseBuilder;
import io.harness.rule.Owner;

import io.grpc.StatusRuntimeException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class WebhookEventResponseHelperTest extends CategoryTest {
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void toResponse() {
    FinalStatus status = TARGET_EXECUTION_REQUESTED;
    NGTriggerEntity ngTriggerEntity = NGTriggerEntity.builder()
                                          .accountId("accountId")
                                          .projectIdentifier("projectId")
                                          .orgIdentifier("orgId")
                                          .identifier("triggerId")
                                          .targetIdentifier("targetId")
                                          .targetType(PIPELINE)
                                          .build();
    TriggerWebhookEvent triggerWebhookEvent =
        TriggerWebhookEvent.builder().createdAt(123l).payload("payload").accountId("accountId").build();

    NGPipelineExecutionResponseDTO ngPipelineExecutionResponseDTO = NGPipelineExecutionResponseDTO.builder().build();
    TargetExecutionSummary summary = TargetExecutionSummary.builder().build();
    String message = "msg";

    WebhookEventResponse webhookEventResponse = WebhookEventResponseHelper.toResponse(
        status, triggerWebhookEvent, ngPipelineExecutionResponseDTO, ngTriggerEntity, message, summary);

    assertThat(webhookEventResponse).isNotNull();
    assertThat(webhookEventResponse.getAccountId()).isEqualTo("accountId");
    assertThat(webhookEventResponse.getOrgIdentifier()).isEqualTo("orgId");
    assertThat(webhookEventResponse.getProjectIdentifier()).isEqualTo("projectId");
    assertThat(webhookEventResponse.getTargetIdentifier()).isEqualTo("targetId");
    assertThat(webhookEventResponse.getTriggerIdentifier()).isEqualTo("triggerId");

    assertThat(webhookEventResponse.getPayload()).isEqualTo("payload");
    assertThat(webhookEventResponse.getCreatedAt()).isEqualTo(123l);
    assertThat(webhookEventResponse.getMessage()).isEqualTo("msg");

    assertThat(webhookEventResponse.getTargetExecutionSummary()).isEqualTo(summary);
    assertThat(webhookEventResponse.isExceptionOccurred()).isFalse();

    webhookEventResponse =
        WebhookEventResponseHelper.toResponse(status, triggerWebhookEvent, null, ngTriggerEntity, message, summary);
    assertThat(webhookEventResponse.isExceptionOccurred()).isTrue();

    // test is NgTriggerEntity is null, no NPE is thrown
    webhookEventResponse =
        WebhookEventResponseHelper.toResponse(status, triggerWebhookEvent, null, null, message, summary);
    assertThat(webhookEventResponse).isNotNull();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testIsFinalStatusAnEvent() {
    assertThat(WebhookEventResponseHelper.isFinalStatusAnEvent(INVALID_PAYLOAD)).isTrue();
    assertThat(WebhookEventResponseHelper.isFinalStatusAnEvent(NO_MATCHING_TRIGGER_FOR_REPO)).isTrue();
    assertThat(WebhookEventResponseHelper.isFinalStatusAnEvent(NO_ENABLED_TRIGGER_FOUND_FOR_REPO)).isTrue();
    assertThat(WebhookEventResponseHelper.isFinalStatusAnEvent(INVALID_RUNTIME_INPUT_YAML)).isTrue();
    assertThat(WebhookEventResponseHelper.isFinalStatusAnEvent(TARGET_DID_NOT_EXECUTE)).isTrue();
    assertThat(WebhookEventResponseHelper.isFinalStatusAnEvent(TARGET_EXECUTION_REQUESTED)).isTrue();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testPrepareResponseForScmException() {
    TriggerWebhookEvent event =
        TriggerWebhookEvent.builder().createdAt(123l).payload("payload").accountId("accountId").build();

    ParsePayloadResponseBuilder parsePayloadResponse =
        ParsePayloadResponse.builder().originalEvent(event).exceptionOccured(true).exception(
            new InvalidRequestException("test"));

    WebhookEventResponse webhookEventResponse =
        WebhookEventResponseHelper.prepareResponseForScmException(parsePayloadResponse.build());
    assertThat(webhookEventResponse.isExceptionOccurred()).isTrue();
    assertThat(webhookEventResponse.getMessage()).isEqualTo("test");
    assertThat(webhookEventResponse.getFinalStatus()).isEqualTo(INVALID_PAYLOAD);
    assertThat(webhookEventResponse.getPayload()).isEqualTo("payload");

    parsePayloadResponse.exception(new StatusRuntimeException(UNAVAILABLE));
    webhookEventResponse = WebhookEventResponseHelper.prepareResponseForScmException(parsePayloadResponse.build());
    assertThat(webhookEventResponse.isExceptionOccurred()).isTrue();
    assertThat(webhookEventResponse.getFinalStatus()).isEqualTo(SCM_SERVICE_CONNECTION_FAILED);
    assertThat(webhookEventResponse.getPayload()).isEqualTo("payload");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testPrepareTargetExecutionSummary() {
    TriggerDetails triggerDetails = TriggerDetails.builder()
                                        .ngTriggerEntity(NGTriggerEntity.builder()
                                                             .accountId("accountId")
                                                             .projectIdentifier("projectId")
                                                             .orgIdentifier("orgId")
                                                             .identifier("triggerId")
                                                             .targetIdentifier("targetId")
                                                             .targetType(PIPELINE)
                                                             .build())
                                        .build();

    String runtimeInputYaml = "runtime";

    TargetExecutionSummary targetExecutionSummary =
        WebhookEventResponseHelper.prepareTargetExecutionSummary(null, triggerDetails, runtimeInputYaml);
    assertThat(targetExecutionSummary).isNotNull();
    assertThat(targetExecutionSummary.getTriggerId()).isEqualTo("triggerId");
    assertThat(targetExecutionSummary.getTargetId()).isEqualTo("targetId");

    targetExecutionSummary = WebhookEventResponseHelper.prepareTargetExecutionSummary(
        NGPipelineExecutionResponseDTO.builder()
            .planExecution(PlanExecution.builder().uuid("planUuid").startTs(123l).status(RUNNING).build())
            .build(),
        triggerDetails, runtimeInputYaml);
    assertThat(targetExecutionSummary).isNotNull();
    assertThat(targetExecutionSummary.getTriggerId()).isEqualTo("triggerId");
    assertThat(targetExecutionSummary.getTargetId()).isEqualTo("targetId");
    assertThat(targetExecutionSummary.getPlanExecutionId()).isEqualTo("planUuid");
    assertThat(targetExecutionSummary.getExecutionStatus()).isEqualTo(RUNNING.name());
    assertThat(targetExecutionSummary.getStartTs()).isEqualTo(123l);
  }
}
