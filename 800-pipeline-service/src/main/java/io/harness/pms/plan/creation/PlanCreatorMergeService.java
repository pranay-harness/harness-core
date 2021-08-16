package io.harness.pms.plan.creation;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.execution.PlanExecutionMetadata;
import io.harness.pms.contracts.plan.ErrorResponse;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.PlanCreationBlobRequest;
import io.harness.pms.contracts.plan.PlanCreationBlobResponse;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.contracts.plan.PlanCreationResponse;
import io.harness.pms.contracts.plan.YamlFieldBlob;
import io.harness.pms.contracts.triggers.TriggerPayload;
import io.harness.pms.exception.PmsExceptionUtils;
import io.harness.pms.sdk.PmsSdkHelper;
import io.harness.pms.utils.CompletableFutures;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class PlanCreatorMergeService {
  private static final int MAX_DEPTH = 10;

  private final Executor executor = Executors.newFixedThreadPool(5);

  private final PmsSdkHelper pmsSdkHelper;

  @Inject
  public PlanCreatorMergeService(PmsSdkHelper pmsSdkHelper) {
    this.pmsSdkHelper = pmsSdkHelper;
  }

  public PlanCreationBlobResponse createPlan(String accountId, String orgIdentifier, String projectIdentifier,
      ExecutionMetadata metadata, PlanExecutionMetadata planExecutionMetadata) throws IOException {
    log.info("Starting plan creation");
    Map<String, PlanCreatorServiceInfo> services = pmsSdkHelper.getServices();

    YamlField pipelineField = YamlUtils.extractPipelineField(planExecutionMetadata.getProcessedYaml());
    Map<String, YamlFieldBlob> dependencies = new HashMap<>();
    dependencies.put(pipelineField.getNode().getUuid(), pipelineField.toFieldBlob());
    PlanCreationBlobResponse finalResponse = createPlanForDependenciesRecursive(accountId, orgIdentifier,
        projectIdentifier, services, dependencies, metadata, planExecutionMetadata.getTriggerPayload());
    validatePlanCreationBlobResponse(finalResponse);
    log.info("Done with plan creation");
    return finalResponse;
  }

  @VisibleForTesting
  Map<String, PlanCreationContextValue> createInitialPlanCreationContext(String accountId, String orgIdentifier,
      String projectIdentifier, ExecutionMetadata metadata, TriggerPayload triggerPayload) {
    Map<String, PlanCreationContextValue> planCreationContextMap = new HashMap<>();
    PlanCreationContextValue.Builder builder = PlanCreationContextValue.newBuilder()
                                                   .setAccountIdentifier(accountId)
                                                   .setOrgIdentifier(orgIdentifier)
                                                   .setProjectIdentifier(projectIdentifier);
    if (metadata != null) {
      builder.setMetadata(metadata);
    }
    if (triggerPayload != null) {
      builder.setTriggerPayload(triggerPayload);
    }
    planCreationContextMap.put("metadata", builder.build());
    return planCreationContextMap;
  }

  private PlanCreationBlobResponse createPlanForDependenciesRecursive(String accountId, String orgIdentifier,
      String projectIdentifier, Map<String, PlanCreatorServiceInfo> services,
      Map<String, YamlFieldBlob> initialDependencies, ExecutionMetadata metadata, TriggerPayload triggerPayload)
      throws IOException {
    PlanCreationBlobResponse.Builder finalResponseBuilder =
        PlanCreationBlobResponse.newBuilder().putAllDependencies(initialDependencies);
    if (EmptyPredicate.isEmpty(services) || EmptyPredicate.isEmpty(initialDependencies)) {
      return finalResponseBuilder.build();
    }
    finalResponseBuilder.putAllContext(
        createInitialPlanCreationContext(accountId, orgIdentifier, projectIdentifier, metadata, triggerPayload));
    for (int i = 0; i < MAX_DEPTH && EmptyPredicate.isNotEmpty(finalResponseBuilder.getDependenciesMap()); i++) {
      PlanCreationBlobResponse currIterationResponse = createPlanForDependencies(services, finalResponseBuilder);
      PlanCreationBlobResponseUtils.addNodes(finalResponseBuilder, currIterationResponse.getNodesMap());
      PlanCreationBlobResponseUtils.mergeStartingNodeId(
          finalResponseBuilder, currIterationResponse.getStartingNodeId());
      PlanCreationBlobResponseUtils.mergeLayoutNodeInfo(finalResponseBuilder, currIterationResponse);
      if (EmptyPredicate.isNotEmpty(finalResponseBuilder.getDependenciesMap())) {
        throw new InvalidRequestException(
            PmsExceptionUtils.getUnresolvedDependencyErrorMessage(finalResponseBuilder.getDependenciesMap().values()));
      }
      PlanCreationBlobResponseUtils.mergeContext(finalResponseBuilder, currIterationResponse.getContextMap());
      PlanCreationBlobResponseUtils.addDependencies(finalResponseBuilder, currIterationResponse.getDependenciesMap());
    }

    return finalResponseBuilder.build();
  }

  private PlanCreationBlobResponse createPlanForDependencies(
      Map<String, PlanCreatorServiceInfo> services, PlanCreationBlobResponse.Builder responseBuilder) {
    PlanCreationBlobResponse.Builder currIterationResponseBuilder = PlanCreationBlobResponse.newBuilder();
    CompletableFutures<PlanCreationResponse> completableFutures = new CompletableFutures<>(executor);

    for (Map.Entry<String, PlanCreatorServiceInfo> serviceEntry : services.entrySet()) {
      if (!pmsSdkHelper.containsSupportedDependency(serviceEntry.getValue(), responseBuilder.getDependenciesMap())) {
        continue;
      }

      completableFutures.supplyAsync(() -> {
        try {
          return serviceEntry.getValue().getPlanCreationClient().createPlan(
              PlanCreationBlobRequest.newBuilder()
                  .putAllDependencies(responseBuilder.getDependenciesMap())
                  .putAllContext(responseBuilder.getContextMap())
                  .build());
        } catch (StatusRuntimeException ex) {
          log.error(
              String.format("Error connecting with service: [%s]. Is this service Running?", serviceEntry.getKey()),
              ex);
          return PlanCreationResponse.newBuilder()
              .setErrorResponse(
                  ErrorResponse.newBuilder()
                      .addMessages(String.format("Error connecting with service: [%s]", serviceEntry.getKey()))
                      .build())
              .build();
        }
      });
    }

    List<ErrorResponse> errorResponses;
    try {
      List<PlanCreationResponse> planCreationResponses = completableFutures.allOf().get(5, TimeUnit.MINUTES);
      errorResponses = planCreationResponses.stream()
                           .filter(resp -> resp.getResponseCase() == PlanCreationResponse.ResponseCase.ERRORRESPONSE)
                           .map(PlanCreationResponse::getErrorResponse)
                           .collect(Collectors.toList());
      if (EmptyPredicate.isEmpty(errorResponses)) {
        planCreationResponses.forEach(
            resp -> PlanCreationBlobResponseUtils.merge(currIterationResponseBuilder, resp.getBlobResponse()));
      }
    } catch (Exception ex) {
      throw new UnexpectedException("Error fetching plan creation response from service", ex);
    }

    PmsExceptionUtils.checkAndThrowPlanCreatorException(errorResponses);
    return currIterationResponseBuilder.build();
  }

  private void validatePlanCreationBlobResponse(PlanCreationBlobResponse finalResponse) {
    if (EmptyPredicate.isNotEmpty(finalResponse.getDependenciesMap())) {
      throw new InvalidRequestException(
          format("Unable to interpret nodes: %s", finalResponse.getDependenciesMap().keySet().toString()));
    }
    if (EmptyPredicate.isEmpty(finalResponse.getStartingNodeId())) {
      throw new InvalidRequestException("Unable to find out starting node");
    }
  }
}
