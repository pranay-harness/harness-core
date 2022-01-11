/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.filter.creation;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.pms.contracts.plan.Dependencies;
import io.harness.pms.contracts.plan.ErrorResponse;
import io.harness.pms.contracts.plan.FilterCreationBlobRequest;
import io.harness.pms.contracts.plan.FilterCreationBlobResponse;
import io.harness.pms.contracts.plan.FilterCreationResponse;
import io.harness.pms.contracts.plan.SetupMetadata;
import io.harness.pms.exception.PmsExceptionUtils;
import io.harness.pms.filter.creation.FilterCreationResponseWrapper.FilterCreationResponseWrapperBuilder;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineSetupUsageHelper;
import io.harness.pms.plan.creation.PlanCreatorServiceInfo;
import io.harness.pms.sdk.PmsSdkHelper;
import io.harness.pms.utils.CompletableFutures;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@Slf4j
public class FilterCreatorMergeService {
  private final PmsSdkHelper pmsSdkHelper;
  private final PipelineSetupUsageHelper pipelineSetupUsageHelper;
  private final PmsGitSyncHelper pmsGitSyncHelper;

  public static final int MAX_DEPTH = 10;
  private final Executor executor = Executors.newFixedThreadPool(5);

  @Inject
  public FilterCreatorMergeService(
      PmsSdkHelper pmsSdkHelper, PipelineSetupUsageHelper pipelineSetupUsageHelper, PmsGitSyncHelper pmsGitSyncHelper) {
    this.pmsSdkHelper = pmsSdkHelper;
    this.pipelineSetupUsageHelper = pipelineSetupUsageHelper;
    this.pmsGitSyncHelper = pmsGitSyncHelper;
  }

  public FilterCreatorMergeServiceResponse getPipelineInfo(PipelineEntity pipelineEntity) throws IOException {
    Map<String, PlanCreatorServiceInfo> services = pmsSdkHelper.getServices();
    String yaml = pipelineEntity.getYaml();
    String processedYaml = YamlUtils.injectUuid(yaml);
    YamlField pipelineField = YamlUtils.extractPipelineField(processedYaml);

    Dependencies dependencies =
        Dependencies.newBuilder()
            .setYaml(processedYaml)
            .putDependencies(pipelineField.getNode().getUuid(), pipelineField.getNode().getYamlPath())
            .build();

    Map<String, String> filters = new HashMap<>();
    SetupMetadata.Builder setupMetadataBuilder = SetupMetadata.newBuilder()
                                                     .setAccountId(pipelineEntity.getAccountId())
                                                     .setProjectId(pipelineEntity.getProjectIdentifier())
                                                     .setOrgId(pipelineEntity.getOrgIdentifier());
    ByteString gitSyncBranchContext = pmsGitSyncHelper.getGitSyncBranchContextBytesThreadLocal();
    if (gitSyncBranchContext != null) {
      setupMetadataBuilder.setGitSyncBranchContext(gitSyncBranchContext);
    }
    FilterCreationBlobResponse response =
        obtainFiltersRecursively(services, dependencies, filters, setupMetadataBuilder.build());
    validateFilterCreationBlobResponse(response);
    pipelineSetupUsageHelper.publishSetupUsageEvent(pipelineEntity, response.getReferredEntitiesList());
    return FilterCreatorMergeServiceResponse.builder()
        .filters(filters)
        .stageCount(response.getStageCount())
        .stageNames(new ArrayList<>(response.getStageNamesList()))
        .build();
  }

  @VisibleForTesting
  public void validateFilterCreationBlobResponse(FilterCreationBlobResponse response) throws IOException {
    if (isNotEmpty(response.getDeps().getDependenciesMap())) {
      throw new InvalidRequestException(PmsExceptionUtils.getUnresolvedDependencyPathsErrorMessage(response.getDeps()));
    }
  }

  @VisibleForTesting
  public FilterCreationBlobResponse obtainFiltersRecursively(Map<String, PlanCreatorServiceInfo> services,
      Dependencies initialDependencies, Map<String, String> filters, SetupMetadata setupMetadata) throws IOException {
    Dependencies initialDependenciesWithoutTemplates =
        FilterCreationBlobResponseUtils.removeTemplateDependencies(initialDependencies);
    FilterCreationBlobResponse.Builder finalResponseBuilder =
        FilterCreationBlobResponse.newBuilder().setDeps(initialDependenciesWithoutTemplates);

    if (isEmpty(services) || isEmpty(initialDependenciesWithoutTemplates.getDependenciesMap())) {
      return finalResponseBuilder.build();
    }

    for (int i = 0; i < MAX_DEPTH && EmptyPredicate.isNotEmpty(finalResponseBuilder.getDeps().getDependenciesMap());
         i++) {
      FilterCreationBlobResponse currIterResponse =
          obtainFiltersPerIteration(services, finalResponseBuilder, filters, setupMetadata);

      FilterCreationBlobResponseUtils.mergeResolvedDependencies(finalResponseBuilder, currIterResponse);
      if (isNotEmpty(finalResponseBuilder.getDeps().getDependenciesMap())) {
        throw new InvalidRequestException(
            PmsExceptionUtils.getUnresolvedDependencyPathsErrorMessage(finalResponseBuilder.getDeps()));
      }
      FilterCreationBlobResponseUtils.mergeDependencies(finalResponseBuilder, currIterResponse);
      FilterCreationBlobResponseUtils.updateStageCount(finalResponseBuilder, currIterResponse);
      FilterCreationBlobResponseUtils.mergeReferredEntities(finalResponseBuilder, currIterResponse);
      FilterCreationBlobResponseUtils.mergeStageNames(finalResponseBuilder, currIterResponse);
    }

    return finalResponseBuilder.build();
  }

  @VisibleForTesting
  public FilterCreationBlobResponse obtainFiltersPerIteration(Map<String, PlanCreatorServiceInfo> services,
      FilterCreationBlobResponse.Builder responseBuilder, Map<String, String> filters, SetupMetadata setupMetadata) {
    CompletableFutures<FilterCreationResponseWrapper> completableFutures = new CompletableFutures<>(executor);
    for (Map.Entry<String, PlanCreatorServiceInfo> serviceEntry : services.entrySet()) {
      if (!pmsSdkHelper.containsSupportedDependencyByYamlPath(serviceEntry.getValue(), responseBuilder.getDeps())) {
        continue;
      }

      completableFutures.supplyAsync(() -> {
        FilterCreationResponseWrapperBuilder builder =
            FilterCreationResponseWrapper.builder().serviceName(serviceEntry.getKey());
        try {
          FilterCreationResponse filterCreationResponse =
              serviceEntry.getValue().getPlanCreationClient().createFilter(FilterCreationBlobRequest.newBuilder()
                                                                               .setDeps(responseBuilder.getDeps())
                                                                               .setSetupMetadata(setupMetadata)
                                                                               .build());
          if (filterCreationResponse.getResponseCase() == FilterCreationResponse.ResponseCase.ERRORRESPONSE) {
            builder.errorResponse(filterCreationResponse.getErrorResponse());
          } else {
            builder.response(filterCreationResponse.getBlobResponse());
          }
        } catch (StatusRuntimeException ex) {
          log.error(
              String.format("Error connecting with service: [%s]. Is this service Running?", serviceEntry.getKey()),
              ex);
          builder.errorResponse(
              ErrorResponse.newBuilder()
                  .addMessages(String.format("Error connecting with service: [%s]", serviceEntry.getKey()))
                  .build());
        }
        return builder.build();
      });
    }

    List<ErrorResponse> errorResponses;
    FilterCreationBlobResponse.Builder currentIteration = FilterCreationBlobResponse.newBuilder();
    try {
      List<FilterCreationResponseWrapper> filterCreationResponseWrappers =
          completableFutures.allOf().get(5, TimeUnit.MINUTES);
      errorResponses = filterCreationResponseWrappers.stream()
                           .filter(resp -> resp != null && resp.getErrorResponse() != null)
                           .map(FilterCreationResponseWrapper::getErrorResponse)
                           .collect(Collectors.toList());
      if (EmptyPredicate.isEmpty(errorResponses)) {
        filterCreationResponseWrappers.forEach(
            response -> FilterCreationBlobResponseUtils.mergeResponses(currentIteration, response, filters));
      }
    } catch (Exception ex) {
      throw new UnexpectedException("Error fetching filter creation response from service", ex);
    }

    PmsExceptionUtils.checkAndThrowFilterCreatorException(errorResponses);
    return currentIteration.build();
  }
}
