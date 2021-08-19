package io.harness.pms.async.plan;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.OrchestrationPublisherName;
import io.harness.async.AsyncResponseCallback;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.executions.plan.PlanService;
import io.harness.engine.pms.commons.events.PmsEventSender;
import io.harness.plan.Plan;
import io.harness.pms.contracts.plan.CreatePartialPlanEvent;
import io.harness.pms.contracts.plan.ErrorResponse;
import io.harness.pms.contracts.plan.PartialPlanResponse;
import io.harness.pms.contracts.plan.PlanCreationBlobResponse;
import io.harness.pms.events.base.PmsEventCategory;
import io.harness.pms.exception.PmsExceptionUtils;
import io.harness.pms.plan.creation.PlanCreationBlobResponseUtils;
import io.harness.pms.plan.creation.PlanCreatorServiceInfo;
import io.harness.pms.plan.execution.PlanExecutionUtils;
import io.harness.pms.sdk.PmsSdkHelper;
import io.harness.tasks.ErrorResponseData;
import io.harness.tasks.ResponseData;
import io.harness.waiter.OldNotifyCallback;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.SuperBuilder;

@Value
@SuperBuilder
public class PartialPlanResponseCallback extends AsyncResponseCallback<PartialPlanResponse> {
  String accountId;
  String orgIdentifier;
  String projectIdentifier;
  String planUuid;

  @Inject PmsSdkHelper pmsSdkHelper;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;
  @Inject PlanService planService;
  @Inject PmsEventSender pmsEventSender;

  @Override
  public void handleMaxDepthExceeded() {
    PartialPlanResponse.Builder finalResponseBuilder = finalResponse.toBuilder();
    finalResponseBuilder.setErrorResponse(
        ErrorResponse.newBuilder().addMessages("Unable to resolve all dependencies").build());
    Plan plan = Plan.builder().valid(false).errorResponse(finalResponseBuilder.getErrorResponse()).build();
    planService.save(plan);
  }

  @SneakyThrows
  @Override
  public PartialPlanResponse handleResponseData(Map<String, ResponseData> responseDataMap) {
    PartialPlanResponse.Builder finalResponseBuilder = PartialPlanResponse.newBuilder();

    for (Map.Entry<String, ResponseData> entry : responseDataMap.entrySet()) {
      try {
        PartialPlanCreatorResponseData responseData = (PartialPlanCreatorResponseData) entry.getValue();
        PartialPlanResponse partialPlanResponse = responseData.getPartialPlanResponse();
        if (partialPlanResponse.hasBlobResponse()) {
          PlanCreationBlobResponse currIterationResponse = partialPlanResponse.getBlobResponse();
          PlanCreationBlobResponseUtils.addNodes(
              finalResponseBuilder.getBlobResponseBuilder(), currIterationResponse.getNodesMap());
          PlanCreationBlobResponseUtils.mergeStartingNodeId(
              finalResponseBuilder.getBlobResponseBuilder(), currIterationResponse.getStartingNodeId());
          PlanCreationBlobResponseUtils.mergeLayoutNodeInfo(
              finalResponseBuilder.getBlobResponseBuilder(), currIterationResponse);
          if (EmptyPredicate.isNotEmpty(finalResponseBuilder.getBlobResponseBuilder().getDependenciesMap())) {
            finalResponseBuilder.setErrorResponse(
                ErrorResponse.newBuilder()
                    .addMessages(PmsExceptionUtils.getUnresolvedDependencyErrorMessage(
                        finalResponseBuilder.getBlobResponse().getDependenciesMap().values()))
                    .build());
          }
          PlanCreationBlobResponseUtils.mergeContext(
              finalResponseBuilder.getBlobResponseBuilder(), currIterationResponse.getContextMap());
          PlanCreationBlobResponseUtils.addDependencies(
              finalResponseBuilder.getBlobResponseBuilder(), currIterationResponse.getDependenciesMap());
        } else {
          finalResponseBuilder.setErrorResponse(
              ErrorResponse.newBuilder()
                  .addMessages(PmsExceptionUtils.getUnresolvedDependencyErrorMessage(
                      finalResponseBuilder.getBlobResponse().getDependenciesMap().values()))
                  .build());
        }
      } catch (Exception ex) {
        finalResponseBuilder.setErrorResponse(
            ErrorResponse.newBuilder()
                .addMessages(PmsExceptionUtils.getUnresolvedDependencyErrorMessage(
                    finalResponseBuilder.getBlobResponse().getDependenciesMap().values()))
                .build());
      }
    }
    return finalResponseBuilder.build();
  }

  @Override
  public List<String> handleUnresolvedDependencies() {
    PlanCreationBlobResponse planCreationBlobResponse = finalResponse.getBlobResponse();
    List<String> waitIds = new ArrayList<>();
    Map<String, PlanCreatorServiceInfo> services = pmsSdkHelper.getServices();
    for (Map.Entry<String, PlanCreatorServiceInfo> serviceEntry : services.entrySet()) {
      if (!pmsSdkHelper.containsSupportedDependency(
              serviceEntry.getValue(), planCreationBlobResponse.getDependenciesMap())) {
        continue;
      }
      String waitId = generateUuid();
      waitIds.add(waitId);
      pmsEventSender.sendEvent(CreatePartialPlanEvent.newBuilder()
                                   .putAllDependencies(planCreationBlobResponse.getDependenciesMap())
                                   .putAllContext(planCreationBlobResponse.getContextMap())
                                   .setNotifyId(waitId)
                                   .build()
                                   .toByteString(),
          new HashMap<>(), PmsEventCategory.CREATE_PARTIAL_PLAN, serviceEntry.getKey());
    }
    return waitIds;
  }

  @Override
  public boolean hasUnresolvedDependency() {
    return !finalResponse.getBlobResponse().getDependenciesMap().isEmpty();
  }

  @Override
  public PartialPlanResponse mergeResponses(PartialPlanResponse finalResponse, PartialPlanResponse interimResponse) {
    PartialPlanResponse.Builder finalResponseBuilder = finalResponse.toBuilder();
    PlanCreationBlobResponse currIterationResponse = interimResponse.getBlobResponse();
    PlanCreationBlobResponseUtils.addNodes(
        finalResponseBuilder.getBlobResponseBuilder(), currIterationResponse.getNodesMap());
    PlanCreationBlobResponseUtils.mergeStartingNodeId(
        finalResponseBuilder.getBlobResponseBuilder(), currIterationResponse.getStartingNodeId());
    PlanCreationBlobResponseUtils.mergeLayoutNodeInfo(
        finalResponseBuilder.getBlobResponseBuilder(), currIterationResponse);
    PlanCreationBlobResponseUtils.mergeContext(
        finalResponseBuilder.getBlobResponseBuilder(), currIterationResponse.getContextMap());
    PlanCreationBlobResponseUtils.addDependencies(
        finalResponseBuilder.getBlobResponseBuilder(), currIterationResponse.getDependenciesMap());
    if (interimResponse.hasErrorResponse()) {
      ErrorResponse.Builder errorResponseBuilder = ErrorResponse.newBuilder();
      if (finalResponse.hasErrorResponse()) {
        errorResponseBuilder = finalResponse.getErrorResponse().toBuilder();
      }
      for (String message : interimResponse.getErrorResponse().getMessagesList()) {
        errorResponseBuilder.addMessages(message);
      }
      finalResponseBuilder.setErrorResponse(errorResponseBuilder.build());
    }
    return finalResponseBuilder.build();
  }

  @Override
  public void finalizeCreation() {
    Plan plan = PlanExecutionUtils.extractPlan(planUuid, finalResponse.getBlobResponse());
    planService.save(plan);
  }

  @Override
  public boolean hasErrorResponse(PartialPlanResponse finalResponse) {
    return finalResponse.hasErrorResponse();
  }

  @Override
  public boolean handleError(PartialPlanResponse finalResponse) {
    PartialPlanResponse.Builder finalResponseBuilder = finalResponse.toBuilder();
    finalResponseBuilder.setErrorResponse(
        ErrorResponse.newBuilder().addMessages("Unable to resolve all dependencies").build());
    Plan plan = Plan.builder().valid(false).errorResponse(finalResponseBuilder.getErrorResponse()).build();
    planService.save(plan);
    return true;
  }

  @Override
  public OldNotifyCallback clone() {
    return PartialPlanResponseCallback.builder()
        .planUuid(planUuid)
        .depth(depth + 1)
        .projectIdentifier(projectIdentifier)
        .orgIdentifier(orgIdentifier)
        .accountId(accountId)
        .finalResponse(finalResponse)
        .build();
  }

  @SneakyThrows
  @Override
  public void notifyError(Map<String, ResponseData> response) {
    PartialPlanResponse.Builder finalResponseBuilder = finalResponse.toBuilder();
    for (Map.Entry<String, ResponseData> entry : response.entrySet()) {
      if (entry.getValue() instanceof ErrorResponseData) {
        finalResponseBuilder.setErrorResponse(
            ErrorResponse.newBuilder()
                .addMessages(PmsExceptionUtils.getUnresolvedDependencyErrorMessage(
                    finalResponseBuilder.getBlobResponse().getDependenciesMap().values()))
                .build());
      }
    }
    Plan plan = Plan.builder().valid(true).errorResponse(finalResponseBuilder.getErrorResponse()).build();
    planService.save(plan);
  }

  @Override
  public void notifyTimeout(Map<String, ResponseData> responseMap) {
    PartialPlanResponse.Builder finalResponseBuilder = finalResponse.toBuilder();
    finalResponseBuilder.setErrorResponse(
        ErrorResponse.newBuilder().addMessages("Plan Creation timed out. Please try again").build());
    Plan plan = Plan.builder().valid(false).errorResponse(finalResponseBuilder.getErrorResponse()).build();
    planService.save(plan);
  }
}
