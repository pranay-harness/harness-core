package io.harness.pms.variables;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.exception.UnexpectedException;
import io.harness.pms.contracts.plan.ErrorResponse;
import io.harness.pms.contracts.plan.VariablesCreationBlobRequest;
import io.harness.pms.contracts.plan.VariablesCreationBlobResponse;
import io.harness.pms.contracts.plan.VariablesCreationResponse;
import io.harness.pms.contracts.plan.YamlFieldBlob;
import io.harness.pms.plan.creation.PlanCreatorServiceInfo;
import io.harness.pms.sdk.PmsSdkHelper;
import io.harness.pms.utils.CompletableFutures;
import io.harness.pms.yaml.PmsYamlUtils;
import io.harness.pms.yaml.YamlField;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class VariableCreatorMergeService {
  private final PmsSdkHelper pmsSdkHelper;

  private static final int MAX_DEPTH = 10;
  private final Executor executor = Executors.newFixedThreadPool(5);

  @Inject
  public VariableCreatorMergeService(PmsSdkHelper pmsSdkHelper) {
    this.pmsSdkHelper = pmsSdkHelper;
  }

  public VariableMergeServiceResponse createVariablesResponse(@NotNull String yaml) throws IOException {
    Map<String, PlanCreatorServiceInfo> services = pmsSdkHelper.getServices();

    YamlField processedYaml = PmsYamlUtils.injectUuidWithLeafUuid(yaml);
    YamlField pipelineField = PmsYamlUtils.getPipelineField(Objects.requireNonNull(processedYaml).getNode());
    Map<String, YamlFieldBlob> dependencies = new HashMap<>();
    dependencies.put(pipelineField.getNode().getUuid(), pipelineField.toFieldBlob());

    VariablesCreationBlobResponse response = createVariablesForDependenciesRecursive(services, dependencies);

    return VariableCreationBlobResponseUtils.getMergeServiceResponse(
        PmsYamlUtils.writeYamlString(processedYaml), response);
  }

  private VariablesCreationBlobResponse createVariablesForDependenciesRecursive(
      Map<String, PlanCreatorServiceInfo> services, Map<String, YamlFieldBlob> dependencies) {
    VariablesCreationBlobResponse.Builder responseBuilder =
        VariablesCreationBlobResponse.newBuilder().putAllDependencies(dependencies);
    if (isEmpty(services) || isEmpty(dependencies)) {
      return responseBuilder.build();
    }

    for (int i = 0; i < MAX_DEPTH && isNotEmpty(responseBuilder.getDependenciesMap()); i++) {
      VariablesCreationBlobResponse variablesCreationBlobResponse =
          obtainVariablesPerIteration(services, responseBuilder.getDependenciesMap());
      VariableCreationBlobResponseUtils.mergeResolvedDependencies(responseBuilder, variablesCreationBlobResponse);
      if (isNotEmpty(responseBuilder.getDependenciesMap())) {
        VariableCreationBlobResponseUtils.mergeYamlProperties(responseBuilder, variablesCreationBlobResponse);
        VariableCreationBlobResponseUtils.mergeErrorResponses(responseBuilder, variablesCreationBlobResponse);
        return responseBuilder.build();
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
      if (!pmsSdkHelper.containsSupportedDependency(entry.getValue(), dependencies)) {
        continue;
      }

      completableFutures.supplyAsync(() -> {
        try {
          return entry.getValue().getPlanCreationClient().createVariablesYaml(
              VariablesCreationBlobRequest.newBuilder().putAllDependencies(dependencies).build());
        } catch (Exception ex) {
          log.error(String.format("Error connecting with service: [%s]. Is this service Running?", entry.getKey()), ex);
          ErrorResponse errorResponse = ErrorResponse.newBuilder()
                                            .addMessages(format("Error connecting with service: [%s]", entry.getKey()))
                                            .build();
          VariablesCreationBlobResponse blobResponse =
              VariablesCreationBlobResponse.newBuilder().addErrorResponse(errorResponse).build();
          return VariablesCreationResponse.newBuilder().setBlobResponse(blobResponse).build();
        }
      });
    }

    try {
      VariablesCreationBlobResponse.Builder builder = VariablesCreationBlobResponse.newBuilder();
      List<VariablesCreationResponse> variablesCreationResponses = completableFutures.allOf().get(5, TimeUnit.MINUTES);
      variablesCreationResponses.forEach(response -> {
        VariableCreationBlobResponseUtils.mergeResponses(builder, response.getBlobResponse());
        if (response.getResponseCase() == VariablesCreationResponse.ResponseCase.ERRORRESPONSE) {
          builder.addErrorResponse(response.getErrorResponse());
        }
      });
      return builder.build();
    } catch (Exception ex) {
      throw new UnexpectedException("Error fetching variables creation response from service", ex);
    }
  }
}
