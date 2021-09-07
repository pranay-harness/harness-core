package io.harness.pms.sdk.core.plan.creation.creators;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.UnexpectedException;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.ExceptionManager;
import io.harness.manage.ManagedExecutorService;
import io.harness.pms.contracts.plan.Dependencies;
import io.harness.pms.contracts.plan.ErrorResponse;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.FilterCreationBlobRequest;
import io.harness.pms.contracts.plan.FilterCreationBlobResponse;
import io.harness.pms.contracts.plan.FilterCreationResponse;
import io.harness.pms.contracts.plan.PlanCreationBlobRequest;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.contracts.plan.PlanCreationServiceGrpc.PlanCreationServiceImplBase;
import io.harness.pms.contracts.plan.VariablesCreationBlobRequest;
import io.harness.pms.contracts.plan.VariablesCreationBlobResponse;
import io.harness.pms.contracts.plan.VariablesCreationResponse;
import io.harness.pms.exception.runtime.InvalidYamlRuntimeException;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.pms.plan.creation.PlanCreationBlobResponseUtils;
import io.harness.pms.plan.creation.PlanCreatorUtils;
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
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
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
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
@Singleton
public class PlanCreatorService extends PlanCreationServiceImplBase {
  private final Executor executor = new ManagedExecutorService(Executors.newFixedThreadPool(2));
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
      if (EmptyPredicate.isNotEmpty(finalResponse.getErrorMessages())) {
        planCreationResponse =
            io.harness.pms.contracts.plan.PlanCreationResponse.newBuilder()
                .setErrorResponse(ErrorResponse.newBuilder().addAllMessages(finalResponse.getErrorMessages()).build())
                .build();
      } else {
        planCreationResponse = io.harness.pms.contracts.plan.PlanCreationResponse.newBuilder()
                                   .setBlobResponse(planCreationResponseBlobHelper.toBlobResponse(finalResponse))
                                   .build();
      }
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
    PlanCreationContextValue planCreationContextValue = ctx.getMetadata();
    ByteString gitSyncBranchContext = null;
    if (planCreationContextValue != null) {
      ExecutionMetadata executionMetadata = planCreationContextValue.getMetadata();
      gitSyncBranchContext = executionMetadata.getGitSyncBranchContext();
    }

    try (PmsGitSyncBranchContextGuard ignore =
             pmsGitSyncHelper.createGitSyncBranchContextGuardFromBytes(gitSyncBranchContext, true)) {
      Dependencies dependencies = initialDependencies.toBuilder().build();
      while (!dependencies.getDependenciesMap().isEmpty()) {
        dependencies = createPlanForDependencies(ctx, finalResponse, dependencies);
        removeInitialDependencies(dependencies, initialDependencies);
      }
    }

    if (finalResponse.getDependencies() != null
        && EmptyPredicate.isNotEmpty(finalResponse.getDependencies().getDependenciesMap())) {
      finalResponse.setDependencies(removeInitialDependencies(finalResponse.getDependencies(), initialDependencies));
    }
    return finalResponse;
  }

  private Dependencies removeInitialDependencies(Dependencies dependencies, Dependencies initialDependencies) {
    if (initialDependencies == null || EmptyPredicate.isEmpty(initialDependencies.getDependenciesMap())) {
      return dependencies;
    }
    if (dependencies == null || EmptyPredicate.isEmpty(dependencies.getDependenciesMap())) {
      return dependencies;
    }

    Dependencies.Builder builder = dependencies.toBuilder();
    initialDependencies.getDependenciesMap().keySet().forEach(builder::removeDependencies);
    return builder.build();
  }

  private Dependencies createPlanForDependencies(
      PlanCreationContext ctx, PlanCreationResponse finalResponse, Dependencies dependencies) {
    if (EmptyPredicate.isEmpty(dependencies.getDependenciesMap())) {
      return dependencies;
    }

    String yaml = dependencies.getYaml();
    String updatedYaml = yaml;
    List<Map.Entry<String, String>> dependenciesList = new ArrayList<>(dependencies.getDependenciesMap().entrySet());
    CompletableFutures<PlanCreationResponse> completableFutures = new CompletableFutures<>(executor);
    for (Map.Entry<String, String> entry : dependenciesList) {
      String fieldYamlPath = entry.getValue();
      completableFutures.supplyAsync(() -> {
        YamlField field;
        try {
          field = YamlField.fromYamlPath(yaml, fieldYamlPath);
        } catch (IOException e) {
          String message = format("Invalid yaml path [%s] during execution plan creation", fieldYamlPath);
          log.error(message, e);
          return PlanCreationResponse.builder().errorMessage(message).build();
        }

        Optional<PartialPlanCreator<?>> planCreatorOptional = findPlanCreator(planCreators, field);
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
            throw new InvalidYamlRuntimeException(
                format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(field.getNode(), e)), e,
                field.getNode());
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

    try {
      List<PlanCreationResponse> planCreationResponses = completableFutures.allOf().get(2, TimeUnit.MINUTES);
      List<String> errorMessages =
          planCreationResponses.stream()
              .filter(resp -> resp != null && EmptyPredicate.isNotEmpty(resp.getErrorMessages()))
              .flatMap(resp -> resp.getErrorMessages().stream())
              .collect(Collectors.toList());
      if (EmptyPredicate.isNotEmpty(errorMessages)) {
        finalResponse.setErrorMessages(errorMessages);
        return dependencies.toBuilder().clearDependencies().build();
      }

      Map<String, String> newDependencies = new HashMap<>();
      for (int i = 0; i < dependenciesList.size(); i++) {
        Map.Entry<String, String> entry = dependenciesList.get(i);
        String fieldYamlPath = entry.getValue();
        PlanCreationResponse response = planCreationResponses.get(i);
        if (response == null) {
          finalResponse.addDependency(yaml, entry.getKey(), fieldYamlPath);
          continue;
        }

        finalResponse.addNodes(response.getNodes());
        finalResponse.mergeContext(response.getContextMap());
        finalResponse.mergeLayoutNodeInfo(response.getGraphLayoutResponse());
        finalResponse.mergeStartingNodeId(response.getStartingNodeId());
        if (response.getDependencies() != null
            && EmptyPredicate.isNotEmpty(response.getDependencies().getDependenciesMap())) {
          newDependencies.putAll(response.getDependencies().getDependenciesMap());
        }
        if (response.getYamlUpdates() != null
            && EmptyPredicate.isNotEmpty(response.getYamlUpdates().getFqnToYamlMap())) {
          finalResponse.addYamlUpdates(response.getYamlUpdates());
          updatedYaml =
              PlanCreationBlobResponseUtils.mergeYamlUpdates(yaml, finalResponse.getYamlUpdates().getFqnToYamlMap());
          finalResponse.updateYamlInDependencies(updatedYaml);
        }
      }
      return dependencies.toBuilder()
          .setYaml(updatedYaml)
          .clearDependencies()
          .putAllDependencies(newDependencies)
          .build();
    } catch (Exception ex) {
      log.error(format("Unexpected plan creation error: %s", ex.getMessage()), ex);
      throw new UnexpectedException(format("Unexpected plan creation error: %s", ex.getMessage()), ex);
    }
  }

  private Optional<PartialPlanCreator<?>> findPlanCreator(List<PartialPlanCreator<?>> planCreators, YamlField field) {
    return planCreators.stream()
        .filter(planCreator -> {
          Map<String, Set<String>> supportedTypes = planCreator.getSupportedTypes();
          return PlanCreatorUtils.supportsField(supportedTypes, field);
        })
        .findFirst();
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
}
