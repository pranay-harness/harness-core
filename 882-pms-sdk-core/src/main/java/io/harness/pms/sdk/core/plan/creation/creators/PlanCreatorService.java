package io.harness.pms.sdk.core.plan.creation.creators;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.manage.ManagedExecutorService;
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
import io.harness.pms.contracts.plan.YamlFieldBlob;
import io.harness.pms.exception.YamlNodeErrorInfo;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.pipeline.filters.FilterCreatorService;
import io.harness.pms.sdk.core.plan.creation.PlanCreationResponseBlobHelper;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.variables.VariableCreatorService;
import io.harness.pms.utils.CompletableFutures;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.serializer.JsonUtils;

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
      Map<String, YamlFieldBlob> dependencyBlobs = request.getDependenciesMap();
      Map<String, YamlField> initialDependencies = new HashMap<>();
      if (EmptyPredicate.isNotEmpty(dependencyBlobs)) {
        try {
          for (Map.Entry<String, YamlFieldBlob> entry : dependencyBlobs.entrySet()) {
            initialDependencies.put(entry.getKey(), YamlField.fromFieldBlob(entry.getValue()));
          }
        } catch (Exception e) {
          throw new InvalidRequestException("Invalid YAML found in dependency blobs");
        }
      }

      PlanCreationResponse finalResponse =
          createPlanForDependenciesRecursive(initialDependencies, request.getContextMap());
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
      planCreationResponse =
          io.harness.pms.contracts.plan.PlanCreationResponse.newBuilder()
              .setErrorResponse(ErrorResponse.newBuilder().addMessages(ExceptionUtils.getMessage(ex)).build())
              .build();
    }

    responseObserver.onNext(planCreationResponse);
    responseObserver.onCompleted();
  }

  private PlanCreationResponse createPlanForDependenciesRecursive(
      Map<String, YamlField> initialDependencies, Map<String, PlanCreationContextValue> context) {
    // TODO: Add patch version before sending the response back
    PlanCreationResponse finalResponse = PlanCreationResponse.builder().build();
    if (EmptyPredicate.isEmpty(planCreators) || EmptyPredicate.isEmpty(initialDependencies)) {
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
      Map<String, YamlField> dependencies = new HashMap<>(initialDependencies);
      while (!dependencies.isEmpty()) {
        createPlanForDependencies(ctx, finalResponse, dependencies);
        initialDependencies.keySet().forEach(dependencies::remove);
      }
    }

    if (EmptyPredicate.isNotEmpty(finalResponse.getDependencies())) {
      initialDependencies.keySet().forEach(k -> finalResponse.getDependencies().remove(k));
    }
    return finalResponse;
  }

  private void createPlanForDependencies(
      PlanCreationContext ctx, PlanCreationResponse finalResponse, Map<String, YamlField> dependencies) {
    if (EmptyPredicate.isEmpty(dependencies)) {
      return;
    }

    List<YamlField> dependenciesList = new ArrayList<>(dependencies.values());
    dependencies.clear();
    CompletableFutures<PlanCreationResponse> completableFutures = new CompletableFutures<>(executor);
    for (YamlField field : dependenciesList) {
      completableFutures.supplyAsync(() -> {
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
            throw new InvalidRequestException(
                format("Invalid yaml in node [%s]", JsonUtils.asJson(YamlNodeErrorInfo.fromField(field))), e);
          }
        }

        try {
          return planCreator.createPlanForField(PlanCreationContext.cloneWithCurrentField(ctx, field), obj);
        } catch (Exception ex) {
          YamlNodeErrorInfo errorInfo = YamlNodeErrorInfo.fromField(field);
          log.error(format("Error creating plan for node: %s", JsonUtils.asJson(errorInfo)), ex);
          return PlanCreationResponse.builder()
              .errorMessage(format("Could not create plan for node [%s]: %s", JsonUtils.asJson(errorInfo),
                  ExceptionUtils.getMessage(ex)))
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
        return;
      }

      for (int i = 0; i < dependenciesList.size(); i++) {
        YamlField field = dependenciesList.get(i);
        PlanCreationResponse response = planCreationResponses.get(i);
        if (response == null) {
          finalResponse.addDependency(field);
          continue;
        }

        finalResponse.addNodes(response.getNodes());
        finalResponse.mergeContext(response.getContextMap());
        finalResponse.mergeLayoutNodeInfo(response.getGraphLayoutResponse());
        finalResponse.mergeStartingNodeId(response.getStartingNodeId());
        if (EmptyPredicate.isNotEmpty(response.getDependencies())) {
          for (YamlField childField : response.getDependencies().values()) {
            dependencies.put(childField.getNode().getUuid(), childField);
          }
        }
      }
    } catch (Exception ex) {
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
      filterCreationResponse =
          FilterCreationResponse.newBuilder()
              .setErrorResponse(ErrorResponse.newBuilder().addMessages(ExceptionUtils.getMessage(ex)).build())
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
      variablesCreationResponse =
          VariablesCreationResponse.newBuilder()
              .setErrorResponse(ErrorResponse.newBuilder().addMessages(ExceptionUtils.getMessage(ex)).build())
              .build();
    }

    responseObserver.onNext(variablesCreationResponse);
    responseObserver.onCompleted();
  }
}
