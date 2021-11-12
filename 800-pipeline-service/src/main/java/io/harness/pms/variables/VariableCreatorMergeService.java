package io.harness.pms.variables;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnexpectedException;
import io.harness.pms.contracts.plan.*;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.pms.plan.creation.PlanCreatorServiceInfo;
import io.harness.pms.sdk.PmsSdkHelper;
import io.harness.pms.utils.CompletableFutures;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
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

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@Slf4j
public class VariableCreatorMergeService {
  private final PmsSdkHelper pmsSdkHelper;
  private final PmsGitSyncHelper pmsGitSyncHelper;
  @Inject private Map<String, List<String>> serviceExpressionMap;

  private static final int MAX_DEPTH = 10;
  private final Executor executor = Executors.newFixedThreadPool(5);

  @Inject
  public VariableCreatorMergeService(PmsSdkHelper pmsSdkHelper, PmsGitSyncHelper pmsGitSyncHelper) {
    this.pmsSdkHelper = pmsSdkHelper;
    this.pmsGitSyncHelper = pmsGitSyncHelper;
  }

  public VariableMergeServiceResponse createVariablesResponse(@NotNull String yaml) throws IOException {
    Map<String, PlanCreatorServiceInfo> services = pmsSdkHelper.getServices();

    YamlField processedYaml = YamlUtils.injectUuidWithLeafUuid(yaml);
    YamlField pipelineField = YamlUtils.getPipelineField(Objects.requireNonNull(processedYaml).getNode());
    Map<String, YamlFieldBlob> dependencies = new HashMap<>();
    dependencies.put(pipelineField.getNode().getUuid(), pipelineField.toFieldBlob());

    Dependencies dependencies1 =
        Dependencies.newBuilder()
            .setYaml(String.valueOf(processedYaml))
            .putDependencies(pipelineField.getNode().getUuid(), pipelineField.getNode().getYamlPath())
            .build();

    VariablesCreationMetadata.Builder metadataBuilder = VariablesCreationMetadata.newBuilder();
    ByteString gitSyncBranchContext = pmsGitSyncHelper.getGitSyncBranchContextBytesThreadLocal();
    if (gitSyncBranchContext != null) {
      metadataBuilder.setGitSyncBranchContext(gitSyncBranchContext);
    }

    VariablesCreationBlobResponse response =
        createVariablesForDependenciesRecursive(services, dependencies1, metadataBuilder.build());

    return VariableCreationBlobResponseUtils.getMergeServiceResponse(
        YamlUtils.writeYamlString(processedYaml), response, serviceExpressionMap);
  }

  private VariablesCreationBlobResponse createVariablesForDependenciesRecursive(
      Map<String, PlanCreatorServiceInfo> services, Dependencies initialDependencies,
      VariablesCreationMetadata metadata) {
    VariablesCreationBlobResponse.Builder finalResponseBuilder =
        VariablesCreationBlobResponse.newBuilder().setDeps(initialDependencies);
    if (isEmpty(services) || isEmpty(initialDependencies.getDependenciesMap())) {
      return finalResponseBuilder.build();
    }

    // This map is for storing those dependencies which cannot be resolved by anyone.
    // We don't want to return early, thus we are storing unresolved dependencies so that variable resolution keeps on
    // working for other entities.
    //    Dependencies unresolvedDependenciesMap = new HashMap<>();
    Dependencies.Builder unresolvedDependencies = Dependencies.newBuilder();
    for (int i = 0; i < MAX_DEPTH && isNotEmpty(finalResponseBuilder.getDeps().getDependenciesMap()); i++) {
      VariablesCreationBlobResponse variablesCreationBlobResponse =
          obtainVariablesPerIteration(services, finalResponseBuilder, metadata);
      VariableCreationBlobResponseUtils.mergeResolvedDependencies(finalResponseBuilder, variablesCreationBlobResponse);
      unresolvedDependencies.putAllDependencies(finalResponseBuilder.getDeps().getDependenciesMap());
      finalResponseBuilder.clearDependencies();
      VariableCreationBlobResponseUtils.mergeDependencies(finalResponseBuilder, variablesCreationBlobResponse);
      VariableCreationBlobResponseUtils.mergeYamlProperties(finalResponseBuilder, variablesCreationBlobResponse);
      VariableCreationBlobResponseUtils.mergeYamlOutputProperties(finalResponseBuilder, variablesCreationBlobResponse);
    }

    finalResponseBuilder.setDeps(unresolvedDependencies);
    return finalResponseBuilder.build();
  }

  private VariablesCreationBlobResponse obtainVariablesPerIteration(Map<String, PlanCreatorServiceInfo> services,
      VariablesCreationBlobResponse.Builder responseBuilder, VariablesCreationMetadata metadata) {
    CompletableFutures<VariablesCreationResponse> completableFutures = new CompletableFutures<>(executor);

    for (Map.Entry<String, PlanCreatorServiceInfo> entry : services.entrySet()) {
      if (!pmsSdkHelper.containsSupportedDependencyByYamlPath(entry.getValue(), responseBuilder.getDeps())) {
        continue;
      }

      completableFutures.supplyAsync(() -> {
        try {
          return entry.getValue().getPlanCreationClient().createVariablesYaml(VariablesCreationBlobRequest.newBuilder()
                                                                                  .setDeps(responseBuilder.getDeps())
                                                                                  .setMetadata(metadata)
                                                                                  .build());
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
