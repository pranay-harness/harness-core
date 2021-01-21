package io.harness.pms.variables;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.pms.contracts.plan.ErrorResponse;
import io.harness.pms.contracts.plan.PlanCreationServiceGrpc;
import io.harness.pms.contracts.plan.VariablesCreationBlobRequest;
import io.harness.pms.contracts.plan.VariablesCreationBlobResponse;
import io.harness.pms.contracts.plan.VariablesCreationResponse;
import io.harness.pms.contracts.plan.YamlFieldBlob;
import io.harness.pms.exception.PmsExceptionUtils;
import io.harness.pms.plan.creation.PlanCreatorServiceInfo;
import io.harness.pms.sdk.PmsSdkInstanceService;
import io.harness.pms.utils.CompletableFutures;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class VariableCreatorMergeService {
  private Map<String, PlanCreationServiceGrpc.PlanCreationServiceBlockingStub> planCreatorServices;
  private final PmsSdkInstanceService pmsSdkInstanceService;

  private static final int MAX_DEPTH = 10;
  private final Executor executor = Executors.newFixedThreadPool(5);

  @Inject
  public VariableCreatorMergeService(
      Map<String, PlanCreationServiceGrpc.PlanCreationServiceBlockingStub> planCreatorServices,
      PmsSdkInstanceService pmsSdkInstanceService) {
    this.planCreatorServices = planCreatorServices;
    this.pmsSdkInstanceService = pmsSdkInstanceService;
  }

  public VariableMergeServiceResponse createVariablesResponse(@NotNull String yaml) throws IOException {
    Map<String, Map<String, Set<String>>> sdkInstances = pmsSdkInstanceService.getInstanceNameToSupportedTypes();
    Map<String, PlanCreatorServiceInfo> services = new HashMap<>();
    if (isNotEmpty(planCreatorServices) && isNotEmpty(sdkInstances)) {
      sdkInstances.forEach((k, v) -> {
        if (planCreatorServices.containsKey(k)) {
          services.put(k, new PlanCreatorServiceInfo(v, planCreatorServices.get(k)));
        }
      });
    }

    YamlField processedYaml = YamlUtils.injectUuidWithLeafUuid(yaml);
    YamlField pipelineField = YamlUtils.getPipelineField(processedYaml.getNode());
    Map<String, YamlFieldBlob> dependencies = new HashMap<>();
    dependencies.put(pipelineField.getNode().getUuid(), pipelineField.toFieldBlob());

    VariablesCreationBlobResponse response = createVariablesForDependenciesRecursive(services, dependencies);
    validateVariableCreationResponse(response);

    return VariableCreationBlobResponseUtils.getMergeServiceResponse(
        YamlUtils.writeYamlString(processedYaml), response);
  }

  private VariablesCreationBlobResponse createVariablesForDependenciesRecursive(
      Map<String, PlanCreatorServiceInfo> services, Map<String, YamlFieldBlob> dependencies) throws IOException {
    VariablesCreationBlobResponse.Builder responseBuilder =
        VariablesCreationBlobResponse.newBuilder().putAllDependencies(dependencies);
    if (isEmpty(services) || isEmpty(dependencies)) {
      return responseBuilder.build();
    }

    for (int i = 0; i < MAX_DEPTH && isNotEmpty(responseBuilder.getDependenciesMap()); i++) {
      VariablesCreationBlobResponse variablesCreationBlobResponse = obtainVariablesPerIteration(services, dependencies);
      VariableCreationBlobResponseUtils.mergeResolvedDependencies(responseBuilder, variablesCreationBlobResponse);
      if (isNotEmpty(responseBuilder.getDependenciesMap())) {
        throw new InvalidRequestException(
            PmsExceptionUtils.getUnresolvedDependencyErrorMessage(responseBuilder.getDependenciesMap().values()));
      }
      VariableCreationBlobResponseUtils.mergeDependencies(responseBuilder, variablesCreationBlobResponse);
      VariableCreationBlobResponseUtils.mergeYamlProperties(responseBuilder, variablesCreationBlobResponse);
    }

    return responseBuilder.build();
  }

  private VariablesCreationBlobResponse obtainVariablesPerIteration(
      Map<String, PlanCreatorServiceInfo> services, Map<String, YamlFieldBlob> dependencies) {
    CompletableFutures<VariablesCreationResponse> completableFutures = new CompletableFutures<>(executor);
    for (Map.Entry<String, PlanCreatorServiceInfo> entry : services.entrySet()) {
      completableFutures.supplyAsync(() -> {
        try {
          return entry.getValue().getPlanCreationClient().createVariablesYaml(
              VariablesCreationBlobRequest.newBuilder().putAllDependencies(dependencies).build());
        } catch (Exception ex) {
          log.error(String.format("Error connecting with service: [%s]. Is this service Running?", entry.getKey()), ex);
          return VariablesCreationResponse.newBuilder()
              .setErrorResponse(ErrorResponse.newBuilder()
                                    .addMessages(String.format("Error connecting with service: [%s]", entry.getKey()))
                                    .build())
              .build();
        }
      });
    }

    List<ErrorResponse> errorResponses;
    VariablesCreationBlobResponse.Builder builder = VariablesCreationBlobResponse.newBuilder();
    try {
      List<VariablesCreationResponse> variablesCreationResponses = completableFutures.allOf().get(5, TimeUnit.MINUTES);
      errorResponses =
          variablesCreationResponses.stream()
              .filter(resp
                  -> resp != null && resp.getResponseCase() == VariablesCreationResponse.ResponseCase.ERRORRESPONSE)
              .map(VariablesCreationResponse::getErrorResponse)
              .collect(Collectors.toList());
      if (EmptyPredicate.isEmpty(errorResponses)) {
        variablesCreationResponses.forEach(
            response -> VariableCreationBlobResponseUtils.mergeResponses(builder, response.getBlobResponse()));
      }
    } catch (Exception ex) {
      throw new UnexpectedException("Error fetching variables creation response from service", ex);
    }

    PmsExceptionUtils.checkAndThrowErrorResponseException("Error creating variables", errorResponses);
    return builder.build();
  }

  private void validateVariableCreationResponse(VariablesCreationBlobResponse finalResponse) {
    if (isNotEmpty(finalResponse.getDependenciesMap())) {
      throw new InvalidRequestException(
          format("Unable to resolve all dependencies: %s", finalResponse.getDependenciesMap().keySet().toString()));
    }
  }
}
