package io.harness.pms.variables;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.pms.contracts.plan.*;
import io.harness.pms.exception.PmsExceptionUtil;
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
    if (EmptyPredicate.isNotEmpty(planCreatorServices) && EmptyPredicate.isNotEmpty(sdkInstances)) {
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

    for (int i = 0; i < MAX_DEPTH && EmptyPredicate.isNotEmpty(responseBuilder.getDependenciesMap()); i++) {
      VariablesCreationBlobResponse variablesCreationBlobResponse = obtainVariablesPerIteration(services, dependencies);
      VariableCreationBlobResponseUtils.mergeResolvedDependencies(responseBuilder, variablesCreationBlobResponse);
      if (isNotEmpty(responseBuilder.getDependenciesMap())) {
        throw new InvalidRequestException(
            PmsExceptionUtil.getUnresolvedDependencyErrorMessage(responseBuilder.getDependenciesMap().values()));
      }
      VariableCreationBlobResponseUtils.mergeDependencies(responseBuilder, variablesCreationBlobResponse);
      VariableCreationBlobResponseUtils.mergeYamlProperties(responseBuilder, variablesCreationBlobResponse);
    }

    return responseBuilder.build();
  }

  private VariablesCreationBlobResponse obtainVariablesPerIteration(
      Map<String, PlanCreatorServiceInfo> services, Map<String, YamlFieldBlob> dependencies) {
    CompletableFutures<VariablesCreationBlobResponse> completableFutures = new CompletableFutures<>(executor);
    for (Map.Entry<String, PlanCreatorServiceInfo> entry : services.entrySet()) {
      completableFutures.supplyAsync(() -> {
        try {
          return entry.getValue().getPlanCreationClient().createVariablesYaml(
              VariablesCreationBlobRequest.newBuilder().putAllDependencies(dependencies).build());
        } catch (Exception ex) {
          log.error("Error fetching Variables Response from service " + entry.getKey(), ex);
          return null;
        }
      });
    }

    VariablesCreationBlobResponse.Builder builder = VariablesCreationBlobResponse.newBuilder();

    try {
      List<VariablesCreationBlobResponse> variablesCreationBlobResponses =
          completableFutures.allOf().get(5, TimeUnit.MINUTES);
      variablesCreationBlobResponses.forEach(
          response -> VariableCreationBlobResponseUtils.mergeResponses(builder, response));
    } catch (Exception ex) {
      throw new UnexpectedException("Error fetching variables creation response from service", ex);
    }

    return builder.build();
  }

  private void validateVariableCreationResponse(VariablesCreationBlobResponse finalResponse) {
    if (isNotEmpty(finalResponse.getDependenciesMap())) {
      throw new InvalidRequestException(
          format("Unable to resolve all dependencies: %s", finalResponse.getDependenciesMap().keySet().toString()));
    }
  }
}
