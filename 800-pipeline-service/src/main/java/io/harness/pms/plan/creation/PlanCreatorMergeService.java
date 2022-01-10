/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.plan.creation;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.async.plan.PlanNotifyEventConsumer.PMS_PLAN_CREATION;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.pms.commons.events.PmsEventSender;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.YamlException;
import io.harness.execution.PlanExecutionMetadata;
import io.harness.pms.async.plan.PartialPlanResponseCallback;
import io.harness.pms.contracts.plan.CreatePartialPlanEvent;
import io.harness.pms.contracts.plan.Dependencies;
import io.harness.pms.contracts.plan.ErrorResponse;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.PartialPlanResponse;
import io.harness.pms.contracts.plan.PlanCreationBlobRequest;
import io.harness.pms.contracts.plan.PlanCreationBlobResponse;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.contracts.plan.PlanCreationResponse;
import io.harness.pms.contracts.triggers.TriggerPayload;
import io.harness.pms.events.base.PmsEventCategory;
import io.harness.pms.exception.PmsExceptionUtils;
import io.harness.pms.plan.creation.validator.PlanCreationValidator;
import io.harness.pms.sdk.PmsSdkHelper;
import io.harness.pms.utils.CompletableFutures;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.waiter.WaitNotifyEngine;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class PlanCreatorMergeService {
  private static final int MAX_DEPTH = 10;

  private final Executor executor;

  private final PmsSdkHelper pmsSdkHelper;
  private final WaitNotifyEngine waitNotifyEngine;
  PmsEventSender pmsEventSender;
  PlanCreationValidator planCreationValidator;

  @Inject
  public PlanCreatorMergeService(PmsSdkHelper pmsSdkHelper, PmsEventSender pmsEventSender,
      WaitNotifyEngine waitNotifyEngine, PlanCreationValidator planCreationValidator,
      @Named("PlanCreatorMergeExecutorService") Executor executor) {
    this.pmsSdkHelper = pmsSdkHelper;
    this.pmsEventSender = pmsEventSender;
    this.waitNotifyEngine = waitNotifyEngine;
    this.planCreationValidator = planCreationValidator;
    this.executor = executor;
  }

  public String getPublisher() {
    return PMS_PLAN_CREATION;
  }

  public void createPlanV2(String accountId, String orgIdentifier, String projectIdentifier, String planUuid,
      ExecutionMetadata metadata, PlanExecutionMetadata planExecutionMetadata) throws IOException {
    log.info("Starting plan creation");
    YamlField pipelineField = YamlUtils.extractPipelineField(planExecutionMetadata.getProcessedYaml());
    String notifyId = generateUuid();
    Dependencies dependencies =
        Dependencies.newBuilder()
            .setYaml(planExecutionMetadata.getProcessedYaml())
            .putDependencies(pipelineField.getNode().getUuid(), pipelineField.getNode().getYamlPath())
            .build();
    pmsEventSender.sendEvent(CreatePartialPlanEvent.newBuilder()
                                 .setDeps(dependencies)
                                 .putAllContext(createInitialPlanCreationContext(accountId, orgIdentifier,
                                     projectIdentifier, metadata, planExecutionMetadata.getTriggerPayload()))
                                 .setNotifyId(notifyId)
                                 .build()
                                 .toByteString(),
        new HashMap<>(), PmsEventCategory.CREATE_PARTIAL_PLAN, "pms");
    waitNotifyEngine.waitForAllOnInList(getPublisher(),
        PartialPlanResponseCallback.builder()
            .planUuid(planUuid)
            .depth(0)
            .finalResponse(PartialPlanResponse.newBuilder()
                               .setBlobResponse(PlanCreationBlobResponse.newBuilder().setDeps(dependencies).build())
                               .build())
            .build(),
        Collections.singletonList(notifyId), Duration.ofMinutes(2));
  }

  public PlanCreationBlobResponse createPlan(String accountId, String orgIdentifier, String projectIdentifier,
      ExecutionMetadata metadata, PlanExecutionMetadata planExecutionMetadata) throws IOException {
    log.info("Starting plan creation");
    Map<String, PlanCreatorServiceInfo> services = pmsSdkHelper.getServices();

    YamlField pipelineField = YamlUtils.extractPipelineField(planExecutionMetadata.getProcessedYaml());
    if (pipelineField.getNode().getUuid() == null) {
      throw new YamlException("Processed pipeline yaml does not have uuid for the pipeline field");
    }
    Dependencies dependencies =
        Dependencies.newBuilder()
            .setYaml(planExecutionMetadata.getProcessedYaml())
            .putDependencies(pipelineField.getNode().getUuid(), pipelineField.getNode().getYamlPath())
            .build();
    PlanCreationBlobResponse finalResponse = createPlanForDependenciesRecursive(accountId, orgIdentifier,
        projectIdentifier, services, dependencies, metadata, planExecutionMetadata.getTriggerPayload());
    planCreationValidator.validate(accountId, finalResponse);
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
      String projectIdentifier, Map<String, PlanCreatorServiceInfo> services, Dependencies initialDependencies,
      ExecutionMetadata metadata, TriggerPayload triggerPayload) {
    PlanCreationBlobResponse.Builder finalResponseBuilder =
        PlanCreationBlobResponse.newBuilder().setDeps(initialDependencies);
    if (EmptyPredicate.isEmpty(services) || EmptyPredicate.isEmpty(initialDependencies.getDependenciesMap())) {
      return finalResponseBuilder.build();
    }

    finalResponseBuilder.putAllContext(
        createInitialPlanCreationContext(accountId, orgIdentifier, projectIdentifier, metadata, triggerPayload));

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
      PlanCreationBlobResponseUtils.addDependenciesV2(finalResponseBuilder, currIterationResponse);
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
}
