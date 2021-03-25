package io.harness.pms.plan.creation;

import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.data.structure.HasPredicate.hasSome;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static java.lang.String.format;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.pms.contracts.plan.ErrorResponse;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.PlanCreationBlobRequest;
import io.harness.pms.contracts.plan.PlanCreationBlobResponse;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.contracts.plan.PlanCreationResponse;
import io.harness.pms.contracts.plan.PlanCreationServiceGrpc.PlanCreationServiceBlockingStub;
import io.harness.pms.contracts.plan.YamlFieldBlob;
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
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class PlanCreatorMergeService {
  private static final int MAX_DEPTH = 10;

  private final Executor executor = Executors.newFixedThreadPool(5);

  private final Map<String, PlanCreationServiceBlockingStub> planCreatorServices;
  private final PmsSdkHelper pmsSdkHelper;

  @Inject
  public PlanCreatorMergeService(
      Map<String, PlanCreationServiceBlockingStub> planCreatorServices, PmsSdkHelper pmsSdkHelper) {
    this.planCreatorServices = planCreatorServices;
    this.pmsSdkHelper = pmsSdkHelper;
  }

  public PlanCreationBlobResponse createPlan(@NotNull String content) throws IOException {
    return createPlan(content, ExecutionMetadata.newBuilder().setExecutionUuid(generateUuid()).build());
  }

  public PlanCreationBlobResponse createPlan(@NotNull String content, ExecutionMetadata metadata) throws IOException {
    log.info("Starting plan creation");
    Map<String, PlanCreatorServiceInfo> services = pmsSdkHelper.getServices();

    String processedYaml = YamlUtils.injectUuid(content);
    YamlField pipelineField = YamlUtils.extractPipelineField(processedYaml);
    Map<String, YamlFieldBlob> dependencies = new HashMap<>();
    dependencies.put(pipelineField.getNode().getUuid(), pipelineField.toFieldBlob());
    PlanCreationBlobResponse finalResponse = createPlanForDependenciesRecursive(services, dependencies, metadata);
    validatePlanCreationBlobResponse(finalResponse);
    log.info("Done with plan creation");
    return finalResponse;
  }

  private Map<String, PlanCreationContextValue> createInitialPlanCreationContext(ExecutionMetadata metadata) {
    Map<String, PlanCreationContextValue> planCreationContextBuilder = new HashMap<>();
    if (metadata != null) {
      planCreationContextBuilder.put("metadata", PlanCreationContextValue.newBuilder().setMetadata(metadata).build());
    }
    return planCreationContextBuilder;
  }

  private PlanCreationBlobResponse createPlanForDependenciesRecursive(Map<String, PlanCreatorServiceInfo> services,
      Map<String, YamlFieldBlob> initialDependencies, ExecutionMetadata metadata) throws IOException {
    PlanCreationBlobResponse.Builder finalResponseBuilder =
        PlanCreationBlobResponse.newBuilder().putAllDependencies(initialDependencies);
    if (hasNone(services) || hasNone(initialDependencies)) {
      return finalResponseBuilder.build();
    }
    finalResponseBuilder.putAllContext(createInitialPlanCreationContext(metadata));
    for (int i = 0; i < MAX_DEPTH && hasSome(finalResponseBuilder.getDependenciesMap()); i++) {
      PlanCreationBlobResponse currIterationResponse = createPlanForDependencies(services, finalResponseBuilder);
      PlanCreationBlobResponseUtils.addNodes(finalResponseBuilder, currIterationResponse.getNodesMap());
      PlanCreationBlobResponseUtils.mergeStartingNodeId(
          finalResponseBuilder, currIterationResponse.getStartingNodeId());
      PlanCreationBlobResponseUtils.mergeLayoutNodeInfo(finalResponseBuilder, currIterationResponse);
      if (hasSome(finalResponseBuilder.getDependenciesMap())) {
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
      if (hasNone(errorResponses)) {
        planCreationResponses.forEach(
            resp -> PlanCreationBlobResponseUtils.merge(currIterationResponseBuilder, resp.getBlobResponse()));
      }
    } catch (Exception ex) {
      throw new UnexpectedException("Error fetching plan creation response from service", ex);
    }

    PmsExceptionUtils.checkAndThrowErrorResponseException("Error creating plan", errorResponses);
    return currIterationResponseBuilder.build();
  }

  private void validatePlanCreationBlobResponse(PlanCreationBlobResponse finalResponse) {
    if (hasSome(finalResponse.getDependenciesMap())) {
      throw new InvalidRequestException(
          format("Unable to interpret nodes: %s", finalResponse.getDependenciesMap().keySet().toString()));
    }
    if (hasNone(finalResponse.getStartingNodeId())) {
      throw new InvalidRequestException("Unable to find out starting node");
    }
  }
}
