/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.sdk.execution.events.plan;

import static java.lang.String.format;

import io.harness.async.AsyncCreatorBaseEventHandler;
import io.harness.async.AsyncCreatorResponse;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.UnexpectedException;
import io.harness.exception.WingsException;
import io.harness.manage.ManagedExecutorService;
import io.harness.pms.contracts.plan.CreatePartialPlanEvent;
import io.harness.pms.contracts.plan.ErrorResponse;
import io.harness.pms.contracts.plan.PartialPlanResponse;
import io.harness.pms.contracts.plan.YamlFieldBlob;
import io.harness.pms.exception.runtime.InvalidYamlRuntimeException;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.plan.creation.PlanCreationResponseBlobHelper;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.PartialPlanCreator;
import io.harness.pms.sdk.core.plan.creation.creators.PipelineServiceInfoProvider;
import io.harness.pms.utils.CompletableFutures;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreatePartialPlanEventHandler
    extends AsyncCreatorBaseEventHandler<CreatePartialPlanEvent, PlanCreationContext> {
  private final Executor executor = new ManagedExecutorService(Executors.newFixedThreadPool(2));

  @Inject PartialPlanResponseEventPublisher partialPlanResponseEventPublisher;
  @Inject PlanCreationResponseBlobHelper planCreationResponseBlobHelper;

  @Inject PipelineServiceInfoProvider pipelineServiceInfoProvider;

  @Override
  @NonNull
  protected Map<String, String> extraLogProperties(CreatePartialPlanEvent event) {
    return new HashMap<>();
  }

  @Override
  protected Map<String, YamlFieldBlob> extractDependencies(CreatePartialPlanEvent message) {
    return message.getDependenciesMap();
  }

  @Override
  protected PlanCreationContext extractContext(CreatePartialPlanEvent message) {
    return PlanCreationContext.builder().globalContext(message.getContextMap()).build();
  }

  @Override
  protected void handleResult(CreatePartialPlanEvent event, AsyncCreatorResponse creatorResponse) {
    PlanCreationResponse finalResponse = (PlanCreationResponse) creatorResponse;
    if (EmptyPredicate.isNotEmpty(finalResponse.getErrorMessages())) {
      partialPlanResponseEventPublisher.publishEvent(
          PartialPlanResponse.newBuilder()
              .setNotifyId(event.getNotifyId())
              .setErrorResponse(ErrorResponse.newBuilder().addAllMessages(finalResponse.getErrorMessages()).build())
              .build());
    } else {
      partialPlanResponseEventPublisher.publishEvent(
          PartialPlanResponse.newBuilder()
              .setNotifyId(event.getNotifyId())
              .setBlobResponse(planCreationResponseBlobHelper.toBlobResponse(finalResponse))
              .build());
    }
  }

  @Override
  protected AsyncCreatorResponse createNewAsyncCreatorResponse() {
    return PlanCreationResponse.builder().build();
  }

  @Override
  public void handleDependencies(
      PlanCreationContext ctx, AsyncCreatorResponse finalResponse, Map<String, YamlField> dependencies) {
    PlanCreationResponse planCreationResponse = (PlanCreationResponse) finalResponse;
    if (EmptyPredicate.isEmpty(dependencies)) {
      return;
    }

    List<YamlField> dependenciesList = new ArrayList<>(dependencies.values());
    dependencies.clear();
    CompletableFutures<PlanCreationResponse> completableFutures = new CompletableFutures<>(executor);
    for (YamlField field : dependenciesList) {
      completableFutures.supplyAsync(() -> {
        Optional<PartialPlanCreator<?>> planCreatorOptional = findPlanCreator(field);
        if (!planCreatorOptional.isPresent()) {
          return null;
        }

        PartialPlanCreator planCreator = planCreatorOptional.get();
        Class<?> cls = planCreator.getFieldClass();
        Object obj;
        if (YamlField.class.isAssignableFrom(cls)) {
          obj = field;
        } else {
          try {
            obj = YamlUtils.read(field.getNode().toString(), cls);
          } catch (IOException e) {
            // YamlUtils.getErrorNodePartialFQN() uses exception path to build FQN
            log.error(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(field.getNode(), e)), e);
            throw new InvalidYamlRuntimeException("Invalid yaml in node [%s]", e, field.getNode());
          }
        }

        try {
          return planCreator.createPlanForField(PlanCreationContext.cloneWithCurrentField(ctx, field), obj);
        } catch (Exception ex) {
          log.error(format("Error creating plan for node: %s", YamlUtils.getFullyQualifiedName(field.getNode())), ex);
          return PlanCreationResponse.builder()
              .errorMessage(format("Could not create plan for node [%s]: %s",
                  YamlUtils.getFullyQualifiedName(field.getNode()), ExceptionUtils.getMessage(ex)))
              .build();
        }
      });
    }

    List<PlanCreationResponse> planCreationResponses = null;
    try {
      planCreationResponses = completableFutures.allOf().get(2, TimeUnit.MINUTES);
    } catch (Exception ex) {
      log.error(format("Unexpected plan creation error: %s", ex.getMessage()), ex);
      throw new UnexpectedException(format("Unexpected plan creation error: %s", ex.getMessage()), ex);
    }
    List<String> errorMessages = planCreationResponses.stream()
                                     .filter(resp -> resp != null && EmptyPredicate.isNotEmpty(resp.getErrorMessages()))
                                     .flatMap(resp -> resp.getErrorMessages().stream())
                                     .collect(Collectors.toList());
    if (EmptyPredicate.isNotEmpty(errorMessages)) {
      planCreationResponse.setErrorMessages(errorMessages);
      return;
    }

    for (int i = 0; i < dependenciesList.size(); i++) {
      YamlField field1 = dependenciesList.get(i);
      PlanCreationResponse response = planCreationResponses.get(i);
      if (response == null) {
        //        planCreationResponse.addDependency(field1);
        continue;
      }

      planCreationResponse.addNodes(response.getNodes());
      planCreationResponse.mergeContext(response.getContextMap());
      planCreationResponse.mergeLayoutNodeInfo(response.getGraphLayoutResponse());
      planCreationResponse.mergeStartingNodeId(response.getStartingNodeId());
      //      if (EmptyPredicate.isNotEmpty(response.getDependencies())) {
      //        for (YamlField childField : response.getDependencies().values()) {
      //          dependencies.put(childField.getNode().getUuid(), childField);
      //        }
      //      }
    }
  }

  @Override
  protected void handleException(CreatePartialPlanEvent event, YamlField field, Exception ex) {
    WingsException processedException = exceptionManager.processException(ex);
    partialPlanResponseEventPublisher.publishEvent(
        PartialPlanResponse.newBuilder()
            .setNotifyId(event.getNotifyId())
            .setErrorResponse(
                ErrorResponse.newBuilder().addMessages(ExceptionUtils.getMessage(processedException)).build())
            .build());
  }

  @Override
  protected void handleException(CreatePartialPlanEvent event, Exception ex) {
    WingsException processedException = exceptionManager.processException(ex);
    partialPlanResponseEventPublisher.publishEvent(
        PartialPlanResponse.newBuilder()
            .setNotifyId(event.getNotifyId())
            .setErrorResponse(
                ErrorResponse.newBuilder().addMessages(ExceptionUtils.getMessage(processedException)).build())
            .build());
  }

  private Optional<PartialPlanCreator<?>> findPlanCreator(YamlField field) {
    return pipelineServiceInfoProvider.getPlanCreators()
        .stream()
        .filter(planCreator -> {
          Map<String, Set<String>> supportedTypes = planCreator.getSupportedTypes();
          return PlanCreatorUtils.supportsField(supportedTypes, field);
        })
        .findFirst();
  }
}
