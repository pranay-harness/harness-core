/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.sdk.core.plan.creation.creators;

import static io.harness.pms.sdk.PmsSdkModuleUtils.PLAN_CREATOR_SERVICE_EXECUTOR;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.ExceptionManager;
import io.harness.pms.contracts.plan.Dependencies;
import io.harness.pms.contracts.plan.Dependency;
import io.harness.pms.contracts.plan.ErrorResponse;
import io.harness.pms.contracts.plan.FilterCreationBlobRequest;
import io.harness.pms.contracts.plan.FilterCreationBlobResponse;
import io.harness.pms.contracts.plan.FilterCreationResponse;
import io.harness.pms.contracts.plan.PlanCreationBlobRequest;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.contracts.plan.PlanCreationServiceGrpc.PlanCreationServiceImplBase;
import io.harness.pms.contracts.plan.VariablesCreationBlobRequest;
import io.harness.pms.contracts.plan.VariablesCreationBlobResponse;
import io.harness.pms.contracts.plan.VariablesCreationResponse;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.pms.sdk.core.pipeline.filters.FilterCreatorService;
import io.harness.pms.sdk.core.plan.creation.PlanCreationResponseBlobHelper;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.variables.VariableCreatorService;
import io.harness.pms.utils.CompletableFutures;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
@Singleton
public class PlanCreatorService extends PlanCreationServiceImplBase {
  @Inject @Named(PLAN_CREATOR_SERVICE_EXECUTOR) private Executor executor;
  @Inject ExceptionManager exceptionManager;

  private final FilterCreatorService filterCreatorService;
  private final VariableCreatorService variableCreatorService;
  private final List<PartialPlanCreator<?>> planCreators;
  private final PlanCreationResponseBlobHelper planCreationResponseBlobHelper;
  private final PmsGitSyncHelper pmsGitSyncHelper;

  @Inject
  public PlanCreatorService(@NotNull PipelineServiceInfoProvider pipelineServiceInfoProvider,
      @NotNull FilterCreatorService filterCreatorService, VariableCreatorService variableCreatorService,
      PlanCreationResponseBlobHelper planCreationResponseBlobHelper, PmsGitSyncHelper pmsGitSyncHelper) {
    this.planCreators = pipelineServiceInfoProvider.getPlanCreators();
    this.filterCreatorService = filterCreatorService;
    this.variableCreatorService = variableCreatorService;
    this.planCreationResponseBlobHelper = planCreationResponseBlobHelper;
    this.pmsGitSyncHelper = pmsGitSyncHelper;
  }

  @Override
  public void createPlan(PlanCreationBlobRequest request,
      StreamObserver<io.harness.pms.contracts.plan.PlanCreationResponse> responseObserver) {
    io.harness.pms.contracts.plan.PlanCreationResponse planCreationResponse;
    try {
      PlanCreationResponse finalResponse =
          createPlanForDependenciesRecursive(request.getDeps(), request.getContextMap());
      planCreationResponse = getPlanCreationResponseFromFinalResponse(finalResponse);
    } catch (Exception ex) {
      log.error(ExceptionUtils.getMessage(ex), ex);
      WingsException processedException = exceptionManager.processException(ex);
      planCreationResponse =
          io.harness.pms.contracts.plan.PlanCreationResponse.newBuilder()
              .setErrorResponse(
                  ErrorResponse.newBuilder().addMessages(ExceptionUtils.getMessage(processedException)).build())
              .build();
    }

    responseObserver.onNext(planCreationResponse);
    responseObserver.onCompleted();
  }

  private PlanCreationResponse createPlanForDependenciesRecursive(
      Dependencies initialDependencies, Map<String, PlanCreationContextValue> context) {
    // TODO: Add patch version before sending the response back
    PlanCreationResponse finalResponse = PlanCreationResponse.builder().build();
    if (EmptyPredicate.isEmpty(planCreators) || EmptyPredicate.isEmpty(initialDependencies.getDependenciesMap())) {
      return finalResponse;
    }

    PlanCreationContext ctx = PlanCreationContext.builder().globalContext(context).build();

    try (PmsGitSyncBranchContextGuard ignore =
             pmsGitSyncHelper.createGitSyncBranchContextGuardFromBytes(ctx.getGitSyncBranchContext(), true)) {
      Dependencies dependencies = initialDependencies.toBuilder().build();
      while (!dependencies.getDependenciesMap().isEmpty()) {
        dependencies = createPlanForDependencies(ctx, finalResponse, dependencies);
        PlanCreatorServiceHelper.removeInitialDependencies(dependencies, initialDependencies);
      }
    }

    if (finalResponse.getDependencies() != null
        && EmptyPredicate.isNotEmpty(finalResponse.getDependencies().getDependenciesMap())) {
      finalResponse.setDependencies(
          PlanCreatorServiceHelper.removeInitialDependencies(finalResponse.getDependencies(), initialDependencies));
    }
    return finalResponse;
  }

  public Dependencies createPlanForDependencies(
      PlanCreationContext ctx, PlanCreationResponse finalResponse, Dependencies dependencies) {
    if (EmptyPredicate.isEmpty(dependencies.getDependenciesMap())) {
      return dependencies;
    }
    CompletableFutures<PlanCreationResponse> completableFutures = new CompletableFutures<>(executor);
    List<Map.Entry<String, String>> dependenciesList = new ArrayList<>(dependencies.getDependenciesMap().entrySet());
    String currentYaml = dependencies.getYaml();

    YamlField fullField;
    try {
      fullField = YamlUtils.readTree(currentYaml);
    } catch (IOException ex) {
      String message = "Invalid yaml during plan creation";
      log.error(message, ex);
      throw new InvalidRequestException(message);
    }

    dependenciesList.forEach(key -> completableFutures.supplyAsync(() -> {
      try {
        return createPlanForDependencyInternal(currentYaml, fullField.fromYamlPath(key.getValue()), ctx,
            dependencies.getDependencyMetadataMap().get(key.getKey()));
      } catch (IOException e) {
        throw new InvalidRequestException("Unable to parse the field in the path:" + key.getValue());
      }
    }));

    try {
      List<PlanCreationResponse> planCreationResponses = completableFutures.allOf().get(2, TimeUnit.MINUTES);
      return PlanCreatorServiceHelper.handlePlanCreationResponses(
          planCreationResponses, finalResponse, currentYaml, dependencies, dependenciesList);
    } catch (Exception ex) {
      log.error(format("Unexpected plan creation error: %s", ex.getMessage()), ex);
      throw new UnexpectedException(format("Unexpected plan creation error: %s", ex.getMessage()), ex);
    }
  }

  @Override
  public void createFilter(FilterCreationBlobRequest request, StreamObserver<FilterCreationResponse> responseObserver) {
    FilterCreationResponse filterCreationResponse;
    try {
      FilterCreationBlobResponse response = filterCreatorService.createFilterBlobResponse(request);
      filterCreationResponse = FilterCreationResponse.newBuilder().setBlobResponse(response).build();
    } catch (Exception ex) {
      log.error(ExceptionUtils.getMessage(ex), ex);
      WingsException processedException = exceptionManager.processException(ex);
      filterCreationResponse =
          FilterCreationResponse.newBuilder()
              .setErrorResponse(
                  ErrorResponse.newBuilder().addMessages(ExceptionUtils.getMessage(processedException)).build())
              .build();
    }

    responseObserver.onNext(filterCreationResponse);
    responseObserver.onCompleted();
  }

  @Override
  public void createVariablesYaml(
      VariablesCreationBlobRequest request, StreamObserver<VariablesCreationResponse> responseObserver) {
    VariablesCreationResponse variablesCreationResponse;
    try {
      VariablesCreationBlobResponse response = variableCreatorService.createVariablesResponse(request);
      variablesCreationResponse = VariablesCreationResponse.newBuilder().setBlobResponse(response).build();
    } catch (Exception ex) {
      log.error(ExceptionUtils.getMessage(ex), ex);
      variablesCreationResponse =
          VariablesCreationResponse.newBuilder()
              .setErrorResponse(ErrorResponse.newBuilder().addMessages(ExceptionUtils.getMessage(ex)).build())
              .build();
    }

    responseObserver.onNext(variablesCreationResponse);
    responseObserver.onCompleted();
  }

  private io.harness.pms.contracts.plan.PlanCreationResponse getPlanCreationResponseFromFinalResponse(
      PlanCreationResponse finalResponse) {
    if (EmptyPredicate.isNotEmpty(finalResponse.getErrorMessages())) {
      return io.harness.pms.contracts.plan.PlanCreationResponse.newBuilder()
          .setErrorResponse(ErrorResponse.newBuilder().addAllMessages(finalResponse.getErrorMessages()).build())
          .build();
    }
    return io.harness.pms.contracts.plan.PlanCreationResponse.newBuilder()
        .setBlobResponse(planCreationResponseBlobHelper.toBlobResponse(finalResponse))
        .build();
  }

  private PlanCreationResponse createPlanForDependencyInternal(
      String currentYaml, YamlField field, PlanCreationContext ctx, Dependency dependency) {
    try {
      Optional<PartialPlanCreator<?>> planCreatorOptional =
          PlanCreatorServiceHelper.findPlanCreator(planCreators, field);
      if (!planCreatorOptional.isPresent()) {
        return null;
      }

      PartialPlanCreator planCreator = planCreatorOptional.get();
      Class<?> cls = planCreator.getFieldClass();
      Object obj = YamlField.class.isAssignableFrom(cls) ? field : YamlUtils.read(field.getNode().toString(), cls);

      try {
        PlanCreationResponse planForField = planCreator.createPlanForField(
            PlanCreationContext.cloneWithCurrentField(ctx, field, currentYaml, dependency), obj);
        PlanCreatorServiceHelper.decorateNodesWithStageFqn(field, planForField);
        return planForField;
      } catch (Exception ex) {
        log.error(format("Error creating plan for node: %s", YamlUtils.getFullyQualifiedName(field.getNode())), ex);
        return PlanCreationResponse.builder()
            .errorMessage(format("Could not create plan for node [%s]: %s",
                YamlUtils.getFullyQualifiedName(field.getNode()), ExceptionUtils.getMessage(ex)))
            .build();
      }
    } catch (IOException ex) {
      String message = format("Invalid yaml path [%s] during execution plan creation", field.getYamlPath());
      log.error(message, ex);
      return PlanCreationResponse.builder().errorMessage(message).build();
    }
  }
}
