package io.harness.pms.plan.creation;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.execution.PlanExecutionMetadata;
import io.harness.pms.contracts.plan.Dependencies;
import io.harness.pms.contracts.plan.ErrorResponse;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.PlanCreationBlobRequest;
import io.harness.pms.contracts.plan.PlanCreationBlobResponse;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.contracts.plan.PlanCreationResponse;
import io.harness.pms.contracts.triggers.TriggerPayload;
import io.harness.pms.exception.PmsExceptionUtils;
import io.harness.pms.sdk.PmsSdkHelper;
import io.harness.pms.utils.CompletableFutures;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;

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

  public PlanCreationBlobResponse createPlan(ExecutionMetadata metadata, PlanExecutionMetadata planExecutionMetadata)
      throws IOException {
    log.info("Starting plan creation");
    Map<String, PlanCreatorServiceInfo> services = pmsSdkHelper.getServices();

    YamlField pipelineField = YamlUtils.extractPipelineField(planExecutionMetadata.getProcessedYaml());
    Dependencies dependencies =
        Dependencies.newBuilder()
            .setYaml(planExecutionMetadata.getProcessedYaml())
            .putDependencies(pipelineField.getNode().getUuid(), pipelineField.getNode().getYamlPath())
            .build();
    PlanCreationBlobResponse finalResponse =
        createPlanForDependenciesRecursive(services, dependencies, metadata, planExecutionMetadata.getTriggerPayload());
    validatePlanCreationBlobResponse(finalResponse);
    log.info("Done with plan creation");
    return finalResponse;
  }

  private Map<String, PlanCreationContextValue> createInitialPlanCreationContext(
      ExecutionMetadata metadata, TriggerPayload triggerPayload) {
    Map<String, PlanCreationContextValue> planCreationContextBuilder = new HashMap<>();
    if (metadata != null) {
      PlanCreationContextValue.Builder builder = PlanCreationContextValue.newBuilder().setMetadata(metadata);
      if (triggerPayload != null) {
        builder.setTriggerPayload(triggerPayload);
      }
      planCreationContextBuilder.put("metadata", builder.build());
    }
    return planCreationContextBuilder;
  }

  private PlanCreationBlobResponse createPlanForDependenciesRecursive(Map<String, PlanCreatorServiceInfo> services,
      Dependencies initialDependencies, ExecutionMetadata metadata, TriggerPayload triggerPayload) {
    PlanCreationBlobResponse.Builder finalResponseBuilder =
        PlanCreationBlobResponse.newBuilder().setDeps(initialDependencies);
    if (EmptyPredicate.isEmpty(services) || initialDependencies == null
        || EmptyPredicate.isEmpty(initialDependencies.getDependenciesMap())) {
      return finalResponseBuilder.build();
    }

    finalResponseBuilder.putAllContext(createInitialPlanCreationContext(metadata, triggerPayload));
    for (int i = 0; i < MAX_DEPTH && EmptyPredicate.isNotEmpty(finalResponseBuilder.getDeps().getDependenciesMap());
         i++) {
      PlanCreationBlobResponse currIterationResponse = createPlanForDependencies(services, finalResponseBuilder);
      PlanCreationBlobResponseUtils.addNodes(finalResponseBuilder, currIterationResponse.getNodesMap());
      PlanCreationBlobResponseUtils.mergeStartingNodeId(
          finalResponseBuilder, currIterationResponse.getStartingNodeId());
      PlanCreationBlobResponseUtils.mergeLayoutNodeInfo(finalResponseBuilder, currIterationResponse);
      if (EmptyPredicate.isNotEmpty(finalResponseBuilder.getDeps().getDependenciesMap())) {
        throw new InvalidRequestException(
            PmsExceptionUtils.getUnresolvedDependencyPathsErrorMessage(finalResponseBuilder.getDeps()));
      }
      PlanCreationBlobResponseUtils.mergeContext(finalResponseBuilder, currIterationResponse.getContextMap());
      PlanCreationBlobResponseUtils.addDependencies(finalResponseBuilder, currIterationResponse.getDeps());
    }

    return finalResponseBuilder.build();
  }

  private PlanCreationBlobResponse createPlanForDependencies(
      Map<String, PlanCreatorServiceInfo> services, PlanCreationBlobResponse.Builder responseBuilder) {
    PlanCreationBlobResponse.Builder currIterationResponseBuilder = PlanCreationBlobResponse.newBuilder();
    CompletableFutures<PlanCreationResponse> completableFutures = new CompletableFutures<>(executor);

    for (Map.Entry<String, PlanCreatorServiceInfo> serviceEntry : services.entrySet()) {
      if (!pmsSdkHelper.containsSupportedDependencyByYamlPath(serviceEntry.getValue(), responseBuilder.getDeps())) {
        continue;
      }

      completableFutures.supplyAsync(() -> {
        try {
          return serviceEntry.getValue().getPlanCreationClient().createPlan(
              PlanCreationBlobRequest.newBuilder()
                  .setDeps(responseBuilder.getDeps())
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
    if (EmptyPredicate.isNotEmpty(finalResponse.getDeps().getDependenciesMap())) {
      throw new InvalidRequestException(
          format("Unable to interpret nodes: %s", finalResponse.getDeps().getDependenciesMap().keySet().toString()));
    }
    if (EmptyPredicate.isEmpty(finalResponse.getStartingNodeId())) {
      throw new InvalidRequestException("Unable to find out starting node");
    }
  }
}
