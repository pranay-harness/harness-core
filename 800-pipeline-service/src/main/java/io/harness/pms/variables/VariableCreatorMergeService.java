/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.variables;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnexpectedException;
import io.harness.pms.contracts.plan.Dependencies;
import io.harness.pms.contracts.plan.ErrorResponse;
import io.harness.pms.contracts.plan.VariablesCreationBlobRequest;
import io.harness.pms.contracts.plan.VariablesCreationBlobResponse;
import io.harness.pms.contracts.plan.VariablesCreationMetadata;
import io.harness.pms.contracts.plan.VariablesCreationResponse;
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
    YamlField topRootFieldInYaml = YamlUtils.getTopRootFieldInYaml(Objects.requireNonNull(processedYaml).getNode());

    Dependencies dependencies =
        Dependencies.newBuilder()
            .setYaml(processedYaml.getNode().getCurrJsonNode().toString())
            .putDependencies(topRootFieldInYaml.getNode().getUuid(), topRootFieldInYaml.getNode().getYamlPath())
            .build();
    VariablesCreationMetadata.Builder metadataBuilder = VariablesCreationMetadata.newBuilder();
    ByteString gitSyncBranchContext = pmsGitSyncHelper.getGitSyncBranchContextBytesThreadLocal();
    if (gitSyncBranchContext != null) {
      metadataBuilder.setGitSyncBranchContext(gitSyncBranchContext);
    }

    VariablesCreationBlobResponse response =
        createVariablesForDependenciesRecursive(services, dependencies, metadataBuilder.build());

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
    Dependencies.Builder unresolvedDependenciesMap = Dependencies.newBuilder();
    for (int i = 0; i < MAX_DEPTH && isNotEmpty(finalResponseBuilder.getDeps().getDependenciesMap()); i++) {
      VariablesCreationBlobResponse variablesCreationBlobResponse =
          obtainVariablesPerIteration(services, finalResponseBuilder, metadata);
      VariableCreationBlobResponseUtils.mergeResolvedDependencies(finalResponseBuilder, variablesCreationBlobResponse);
      unresolvedDependenciesMap.putAllDependencies(finalResponseBuilder.getDeps().getDependenciesMap());
      finalResponseBuilder.setDeps(finalResponseBuilder.getDeps().toBuilder().clearDependencies().build());

      VariableCreationBlobResponseUtils.mergeDependencies(finalResponseBuilder, variablesCreationBlobResponse);
      VariableCreationBlobResponseUtils.mergeYamlProperties(finalResponseBuilder, variablesCreationBlobResponse);
      VariableCreationBlobResponseUtils.mergeYamlOutputProperties(finalResponseBuilder, variablesCreationBlobResponse);
    }

    finalResponseBuilder.setDeps(unresolvedDependenciesMap);
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
