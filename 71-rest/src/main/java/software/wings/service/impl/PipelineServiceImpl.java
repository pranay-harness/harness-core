package software.wings.service.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.beans.OrchestrationWorkflowType.ROLLING;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.CollectionUtils.trimmedLowercaseSet;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.eraro.ErrorCode.PIPELINE_EXECUTION_IN_PROGRESS;
import static io.harness.exception.WingsException.USER;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.validation.Validator.notEmptyCheck;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.EntityType.APPDYNAMICS_APPID;
import static software.wings.beans.EntityType.APPDYNAMICS_CONFIGID;
import static software.wings.beans.EntityType.ARTIFACT;
import static software.wings.beans.EntityType.ELK_CONFIGID;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.INFRASTRUCTURE_DEFINITION;
import static software.wings.beans.EntityType.INFRASTRUCTURE_MAPPING;
import static software.wings.beans.EntityType.NEWRELIC_CONFIGID;
import static software.wings.beans.EntityType.NEWRELIC_MARKER_CONFIGID;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.EntityType.SPLUNK_CONFIGID;
import static software.wings.beans.PipelineExecution.PIPELINE_ID_KEY;
import static software.wings.expression.ManagerExpressionEvaluator.getName;
import static software.wings.expression.ManagerExpressionEvaluator.matchesVariablePattern;
import static software.wings.sm.StateType.APPROVAL;
import static software.wings.sm.StateType.ENV_STATE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.LimitCheckerFactory;
import io.harness.limits.LimitEnforcementUtils;
import io.harness.limits.checker.StaticLimitCheckerWithDecrement;
import io.harness.limits.counter.service.CounterSyncer;
import io.harness.persistence.HIterator;
import io.harness.queue.QueuePublisher;
import io.harness.validation.Create;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.UpdateOperations;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.api.DeploymentType;
import software.wings.beans.AccountEvent;
import software.wings.beans.AccountEventType;
import software.wings.beans.ArtifactVariable;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.EntityType;
import software.wings.beans.Event.Type;
import software.wings.beans.FailureStrategy;
import software.wings.beans.FeatureName;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.beans.PipelineExecution;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Service;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.deployment.DeploymentMetadata;
import software.wings.beans.deployment.DeploymentMetadata.Include;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.Trigger.TriggerKeys;
import software.wings.dl.WingsPersistence;
import software.wings.prune.PruneEntityListener;
import software.wings.prune.PruneEvent;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.HarnessTagService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ResourceLookupService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.ownership.OwnedByPipeline;
import software.wings.service.intfc.trigger.DeploymentTriggerService;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.sm.StateMachine;
import software.wings.sm.states.ApprovalState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 10/26/16.
 */
@Singleton
@ValidateOnExecution
@Slf4j
public class PipelineServiceImpl implements PipelineService {
  private static final Set<Character> ALLOWED_CHARS_SET_PIPELINE_STAGE =
      Sets.newHashSet(Lists.charactersOf("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_ ()"));

  private static final String PIPELINE_ENV_STATE_VALIDATION_MESSAGE =
      "Some workflows %s are found to be invalid/incomplete.";

  @Inject private AppService appService;
  @Inject private ExecutorService executorService;
  @Inject private TriggerService triggerService;
  @Inject private DeploymentTriggerService deploymentTriggerService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private YamlPushService yamlPushService;
  @Inject private WorkflowServiceHelper workflowServiceHelper;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;
  @Inject private LimitCheckerFactory limitCheckerFactory;
  @Inject private AuditServiceHelper auditServiceHelper;
  @Inject private CounterSyncer counterSyncer;
  @Inject private EventPublishHelper eventPublishHelper;

  @Inject private QueuePublisher<PruneEvent> pruneQueue;
  @Inject private HarnessTagService harnessTagService;
  @Inject private ResourceLookupService resourceLookupService;
  @Inject FeatureFlagService featureFlagService;

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<Pipeline> listPipelines(PageRequest<Pipeline> pageRequest) {
    return wingsPersistence.query(Pipeline.class, pageRequest);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<Pipeline> listPipelines(PageRequest<Pipeline> pageRequest, boolean withDetails,
      Integer previousExecutionsCount, boolean withTags, String tagFilter) {
    PageResponse<Pipeline> res =
        resourceLookupService.listWithTagFilters(pageRequest, tagFilter, EntityType.PIPELINE, withTags);

    List<Pipeline> pipelines = res.getResponse();
    if (withDetails) {
      setPipelineDetails(pipelines, false);
    }
    if (previousExecutionsCount != null && previousExecutionsCount > 0) {
      for (Pipeline pipeline : pipelines) {
        try {
          List<WorkflowExecution> workflowExecutions =
              workflowExecutionService
                  .listExecutions(aPageRequest()
                                      .withLimit(previousExecutionsCount.toString())
                                      .addFilter("workflowId", EQ, pipeline.getUuid())
                                      .addFilter("appId", EQ, pipeline.getAppId())
                                      .build(),
                      false, false, false, false)
                  .getResponse();
          pipeline.setWorkflowExecutions(workflowExecutions);
        } catch (Exception e) {
          logger.error("Failed to fetch recent executions for pipeline {}", pipeline, e);
        }
      }
    }
    return res;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Pipeline update(Pipeline pipeline, boolean migration) {
    Pipeline savedPipeline = wingsPersistence.getWithAppId(Pipeline.class, pipeline.getAppId(), pipeline.getUuid());
    notNullCheck("Pipeline not saved", savedPipeline, USER);

    Set<String> keywords = pipeline.generateKeywords();
    ensurePipelineStageUuidAndParallelIndex(pipeline);

    validatePipeline(pipeline, keywords);
    checkUniquePipelineStepName(pipeline);

    // TODO: remove this when all the needed verification is done from validatePipeline
    new StateMachine(pipeline, workflowService.stencilMap(pipeline.getAppId()));

    UpdateOperations<Pipeline> ops = wingsPersistence.createUpdateOperations(Pipeline.class);
    setUnset(ops, "description", pipeline.getDescription());
    setUnset(ops, "name", pipeline.getName());
    setUnset(ops, "pipelineStages", pipeline.getPipelineStages());
    setUnset(ops, "failureStrategies", pipeline.getFailureStrategies());
    setUnset(ops, "keywords", trimmedLowercaseSet(keywords));

    wingsPersistence.update(wingsPersistence.createQuery(Pipeline.class)
                                .filter("appId", pipeline.getAppId())
                                .filter(ID_KEY, pipeline.getUuid()),
        ops);

    Pipeline updatedPipeline = wingsPersistence.getWithAppId(Pipeline.class, pipeline.getAppId(), pipeline.getUuid());

    String accountId = appService.getAccountIdByAppId(pipeline.getAppId());
    boolean isRename = !savedPipeline.getName().equals(pipeline.getName());

    if (isRename) {
      executorService.submit(() -> triggerService.updateByApp(pipeline.getAppId()));
    }

    if (!migration) {
      yamlPushService.pushYamlChangeSet(
          accountId, savedPipeline, updatedPipeline, Type.UPDATE, pipeline.isSyncFromGit(), isRename);
    }

    return updatedPipeline;
  }

  public static void ensurePipelineStageUuidAndParallelIndex(Pipeline pipeline) {
    List<PipelineStage> pipelineStages = pipeline.getPipelineStages();
    if (isEmpty(pipelineStages)) {
      return;
    }

    int parallelIndex = 0;
    String firstParallelStageName = pipelineStages.get(0).getName();
    for (PipelineStage stage : pipelineStages) {
      if (!stage.isParallel()) {
        ++parallelIndex;
      }
      if (isEmpty(stage.getName())) {
        stage.setName("STAGE " + parallelIndex);
      }
      if (!stage.isParallel()) {
        firstParallelStageName = stage.getName();
      } else {
        stage.setName(firstParallelStageName);
      }
      for (PipelineStageElement element : stage.getPipelineStageElements()) {
        // The UI or other agents my try to update the pipeline with new stages without uuid. This makes sure that
        // they all will have one.
        if (element.getUuid() == null) {
          element.setUuid(generateUuid());
        }
        // The parallel index is important to be correct, lets enforce it
        element.setParallelIndex(parallelIndex);
      }
    }
  }

  @Override
  public List<FailureStrategy> updateFailureStrategies(
      String appId, String pipelineId, List<FailureStrategy> failureStrategies) {
    Pipeline savedPipeline = wingsPersistence.getWithAppId(Pipeline.class, appId, pipelineId);
    notNullCheck("pipeline", savedPipeline);

    savedPipeline.setFailureStrategies(failureStrategies);
    Pipeline pipeline = update(savedPipeline, false);
    return pipeline.getFailureStrategies();
  }

  @Override
  public List<String> obtainPipelineNamesReferencedByEnvironment(String appId, String envId) {
    List<String> referencedPipelines = new ArrayList<>();
    try (HIterator<Pipeline> pipelineHIterator =
             new HIterator<>(wingsPersistence.createQuery(Pipeline.class).filter(PipelineKeys.appId, appId).fetch())) {
      while (pipelineHIterator.hasNext()) {
        Pipeline pipeline = pipelineHIterator.next();
      PIPELINE_STAGE_LOOP:
        for (PipelineStage pipelineStage : pipeline.getPipelineStages()) {
          for (PipelineStageElement pipelineStageElement : pipelineStage.getPipelineStageElements()) {
            // Env Id in pipeline
            if (ENV_STATE.name().equals(pipelineStageElement.getType())) {
              Workflow workflow = wingsPersistence.getWithAppId(
                  Workflow.class, appId, (String) pipelineStageElement.getProperties().get("workflowId"));
              if (!workflow.checkEnvironmentTemplatized()) {
                if (envId.equals(workflow.getEnvId())) {
                  referencedPipelines.add(pipeline.getName());
                  break PIPELINE_STAGE_LOOP;
                }
              } else if (pipelineStageElement.getWorkflowVariables() != null
                  && pipelineStageElement.getWorkflowVariables().values().contains(envId)) {
                referencedPipelines.add(pipeline.getName());
                break PIPELINE_STAGE_LOOP;
              }
            }
          }
        }
      }
    }
    return referencedPipelines;
  }

  // TODO: Add unit tests for this function
  private void ensurePipelineSafeToDelete(Pipeline pipeline) {
    PageRequest<PipelineExecution> pageRequest = aPageRequest()
                                                     .addFilter(PipelineKeys.appId, EQ, pipeline.getAppId())
                                                     .addFilter(PIPELINE_ID_KEY, EQ, pipeline.getUuid())
                                                     .build();
    PageResponse<PipelineExecution> pageResponse = wingsPersistence.query(PipelineExecution.class, pageRequest);
    if (pageResponse == null || isEmpty(pageResponse.getResponse())
        || pageResponse.getResponse().stream().allMatch(
               pipelineExecution -> ExecutionStatus.isFinalStatus(pipelineExecution.getStatus()))) {
      List<String> triggerNames;
      if (featureFlagService.isEnabled(FeatureName.TRIGGER_REFACTOR, pipeline.getAccountId())) {
        triggerNames = deploymentTriggerService.getTriggersHasPipelineAction(pipeline.getAppId(), pipeline.getUuid());
      } else {
        List<Trigger> triggers = triggerService.getTriggersHasPipelineAction(pipeline.getAppId(), pipeline.getUuid());
        if (isEmpty(triggers)) {
          return;
        }
        triggerNames = triggers.stream().map(Trigger::getName).collect(toList());
      }
      throw new InvalidRequestException(
          format("Pipeline associated as a trigger action to triggers [%s]", join(", ", triggerNames)), USER);
    }
    throw new InvalidRequestException(
        format("Pipeline:[%s] couldn't be deleted", pipeline.getName()), PIPELINE_EXECUTION_IN_PROGRESS, USER);
  }

  @Override
  public boolean deletePipeline(String appId, String pipelineId) {
    return deletePipeline(appId, pipelineId, false, false);
  }

  private boolean deletePipeline(String appId, String pipelineId, boolean forceDelete, boolean syncFromGit) {
    Pipeline pipeline = wingsPersistence.getWithAppId(Pipeline.class, appId, pipelineId);
    if (pipeline == null) {
      return true;
    }
    String accountId = appService.getAccountIdByAppId(pipeline.getAppId());
    StaticLimitCheckerWithDecrement checker = (StaticLimitCheckerWithDecrement) limitCheckerFactory.getInstance(
        new Action(accountId, ActionType.CREATE_PIPELINE));

    return LimitEnforcementUtils.withCounterDecrement(checker, () -> {
      if (!forceDelete) {
        ensurePipelineSafeToDelete(pipeline);
      }

      yamlPushService.pushYamlChangeSet(accountId, pipeline, null, Type.DELETE, syncFromGit, false);

      return prunePipeline(appId, pipelineId);
    });
  }

  @Override
  public void deleteByYamlGit(String appId, String pipelineId, boolean syncFromGit) {
    deletePipeline(appId, pipelineId, false, syncFromGit);
  }

  @Override
  public String fetchPipelineName(String appId, String pipelineId) {
    Pipeline pipeline = wingsPersistence.createQuery(Pipeline.class)
                            .project(Pipeline.NAME_KEY, true)
                            .filter(PipelineKeys.appId, appId)
                            .filter(Pipeline.ID_KEY, pipelineId)
                            .get();
    notNullCheck("Pipeline does not exist", pipeline, USER);
    return pipeline.getName();
  }

  @Override
  public void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String pipelineId) {
    List<OwnedByPipeline> services =
        ServiceClassLocator.descendingServices(this, PipelineServiceImpl.class, OwnedByPipeline.class);
    PruneEntityListener.pruneDescendingEntities(services, descending -> descending.pruneByPipeline(appId, pipelineId));
  }

  @Override
  public void pruneByApplication(String appId) {
    List<Pipeline> pipelines = wingsPersistence.createQuery(Pipeline.class)
                                   .filter(PipelineKeys.appId, appId)
                                   .project(PipelineKeys.name, true)
                                   .project(PipelineKeys.accountId, true)
                                   .project(PipelineKeys.appId, true)
                                   .project(PipelineKeys.uuid, true)
                                   .asList();

    String accountId = null;
    for (Pipeline pipeline : pipelines) {
      accountId = pipeline.getAccountId();
      if (prunePipeline(appId, pipeline.getUuid())) {
        auditServiceHelper.reportDeleteForAuditing(appId, pipeline);
      }
      harnessTagService.pruneTagLinks(pipeline.getAccountId(), pipeline.getUuid());
    }

    if (StringUtils.isNotEmpty(accountId)) {
      counterSyncer.syncPipelineCount(accountId);
    }
  }

  @Override
  public Pipeline clonePipeline(Pipeline originalPipeline, Pipeline pipeline) {
    Pipeline clonedPipeline = originalPipeline.cloneInternal();
    clonedPipeline.setName(pipeline.getName());
    clonedPipeline.setDescription(pipeline.getDescription());
    return save(clonedPipeline);
  }

  @Override
  public List<EntityType> getRequiredEntities(String appId, String pipelineId) {
    Pipeline pipeline = wingsPersistence.getWithAppId(Pipeline.class, appId, pipelineId);
    notNullCheck("pipeline", pipeline, USER);
    boolean infraRefactor = featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, pipeline.getAccountId());
    List<EntityType> entityTypes = new ArrayList<>();
    for (PipelineStage pipelineStage : pipeline.getPipelineStages()) {
      for (PipelineStageElement pipelineStageElement : pipelineStage.getPipelineStageElements()) {
        if (pipelineStageElement.checkDisableAssertion()) {
          continue;
        }
        if (ENV_STATE.name().equals(pipelineStageElement.getType())) {
          Workflow workflow = workflowService.readWorkflowWithoutServices(
              pipeline.getAppId(), (String) pipelineStageElement.getProperties().get("workflowId"), infraRefactor);
          OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
          if (orchestrationWorkflow != null) {
            if (BUILD.equals(orchestrationWorkflow.getOrchestrationWorkflowType())) {
              return new ArrayList<>();
            }
            if (!entityTypes.contains(ARTIFACT)) {
              DeploymentMetadata deploymentMetadata = workflowService.fetchDeploymentMetadata(pipeline.getAppId(),
                  workflow, pipelineStageElement.getWorkflowVariables(), null, null, Include.ARTIFACT_SERVICE);
              if (deploymentMetadata != null) {
                if (isNotEmpty(deploymentMetadata.getArtifactRequiredServiceIds())) {
                  entityTypes.add(ARTIFACT);
                }
              }
            }
          }
        }
      }
    }
    return entityTypes;
  }

  /**
   * Read Pipeline with Services. It filters out the services that does not need artifact
   * @param appId        the app id
   * @param pipelineId   the pipeline id
   * @param withServices the with services
   * @return
   */
  @Override
  public Pipeline readPipeline(String appId, String pipelineId, boolean withServices) {
    Pipeline pipeline = wingsPersistence.getWithAppId(Pipeline.class, appId, pipelineId);
    notNullCheck("Pipeline does not exist", pipeline, USER);
    if (withServices) {
      // Note: It filters out the services that does need artifact
      setServicesAndPipelineVariables(pipeline);
    }
    return pipeline;
  }

  @Override
  public boolean pipelineExists(String appId, String pipelineId) {
    return wingsPersistence.createQuery(Trigger.class)
               .filter(TriggerKeys.appId, appId)
               .filter(TriggerKeys.pipelineId, pipelineId)
               .getKey()
        != null;
  }

  /**
   * Read Pipeline with Services. It filters out the services that does not need artifact
   * @param appId        the app id
   * @param pipelineId   the pipeline id
   * @return
   */
  @Override
  public Pipeline readPipelineWithVariables(String appId, String pipelineId) {
    Pipeline pipeline = wingsPersistence.getWithAppId(Pipeline.class, appId, pipelineId);
    notNullCheck("Pipeline does not exist", pipeline, USER);
    // Note: It filters out the services that does need artifact
    setPipelineDetails(Collections.singletonList(pipeline), true);
    return pipeline;
  }

  @Override
  public Pipeline readPipelineWithResolvedVariables(
      String appId, String pipelineId, Map<String, String> pipelineVariables) {
    return readPipelineWithResolvedVariables(appId, pipelineId, pipelineVariables, null);
  }

  @Override
  public Pipeline readPipelineWithResolvedVariables(
      String appId, String pipelineId, Map<String, String> pipelineVariables, Map<String, Workflow> workflowCache) {
    Pipeline pipeline = wingsPersistence.getWithAppId(Pipeline.class, appId, pipelineId);
    notNullCheck("Pipeline does not exist", pipeline, USER);
    List<Service> services = new ArrayList<>();
    List<String> serviceIds = new ArrayList<>();
    List<String> envIds = new ArrayList<>();
    List<String> workflowIds = new ArrayList<>();
    List<String> infraMappingIds = new ArrayList<>();
    List<String> infraDefinitionIds = new ArrayList<>();
    if (workflowCache == null) {
      workflowCache = new HashMap<>();
    }
    for (PipelineStage pipelineStage : pipeline.getPipelineStages()) {
      for (PipelineStageElement pipelineStageElement : pipelineStage.getPipelineStageElements()) {
        if (ENV_STATE.name().equals(pipelineStageElement.getType())) {
          String workflowId = (String) pipelineStageElement.getProperties().get("workflowId");
          Workflow workflow;
          if (workflowCache.containsKey(workflowId)) {
            workflow = workflowCache.get(workflowId);
          } else {
            workflow = workflowService.readWorkflow(pipeline.getAppId(), workflowId);
            notNullCheck("Workflow does not exist", workflow);
            notNullCheck("Orchestration workflow does not exist", workflow.getOrchestrationWorkflow());
            workflowCache.put(workflowId, workflow);
          }

          Map<String, String> resolvedWorkflowStepVariables =
              WorkflowServiceHelper.overrideWorkflowVariables(workflow.getOrchestrationWorkflow().getUserVariables(),
                  pipelineStageElement.getWorkflowVariables(), pipelineVariables);
          pipelineStageElement.setWorkflowVariables(resolvedWorkflowStepVariables);
          if (!BUILD.equals(workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType())) {
            resolveServices(services, serviceIds, resolvedWorkflowStepVariables, workflow);
            resolveInfraMappings(infraMappingIds, resolvedWorkflowStepVariables, workflow);
            resolveInfraDefinitions(infraDefinitionIds, resolvedWorkflowStepVariables, workflow);
            resolveEnvIds(envIds, resolvedWorkflowStepVariables, workflow);
          }
          if (!workflowIds.contains(workflowId)) {
            workflowIds.add(workflowId);
          }
        }
      }
    }
    pipeline.setServices(services);
    pipeline.setEnvIds(envIds);
    pipeline.setInfraMappingIds(infraMappingIds);
    pipeline.setInfraDefinitionIds(infraDefinitionIds);
    pipeline.setWorkflowIds(workflowIds);
    return pipeline;
  }

  private List<Service> resolveServices(
      List<Service> services, List<String> serviceIds, Map<String, String> pseWorkflowVariables, Workflow workflow) {
    List<Service> resolvedServices = workflowService.getResolvedServices(workflow, pseWorkflowVariables);
    if (resolvedServices != null) {
      for (Service resolvedService : resolvedServices) {
        if (!serviceIds.contains(resolvedService.getUuid())) {
          services.add(resolvedService);
          serviceIds.add(resolvedService.getUuid());
        }
      }
    }
    return resolvedServices;
  }

  private void resolveInfraMappings(
      List<String> infraMappingIds, Map<String, String> pseWorkflowVariables, Workflow workflow) {
    List<String> resolvedInfraMappingIds = workflowService.getResolvedInfraMappingIds(workflow, pseWorkflowVariables);
    if (resolvedInfraMappingIds != null) {
      resolvedInfraMappingIds.stream()
          .filter(resolvedInfraId -> !infraMappingIds.contains(resolvedInfraId))
          .forEach(infraMappingIds::add);
    }
  }

  private void resolveInfraDefinitions(
      List<String> infraDefinitionIds, Map<String, String> pseWorkflowVariables, Workflow workflow) {
    List<String> resolvedInfraDefinitionIds =
        workflowService.getResolvedInfraDefinitionIds(workflow, pseWorkflowVariables);
    if (resolvedInfraDefinitionIds != null) {
      resolvedInfraDefinitionIds.stream()
          .filter(resolvedInfraId -> !infraDefinitionIds.contains(resolvedInfraId))
          .forEach(infraDefinitionIds::add);
    }
  }

  @Override
  public Pipeline getPipelineByName(String appId, String pipelineName) {
    return wingsPersistence.createQuery(Pipeline.class)
        .filter("appId", appId)
        .filter(PipelineKeys.name, pipelineName)
        .get();
  }

  @Override
  public void setPipelineDetails(List<Pipeline> pipelines, boolean withFinalValuesOnly) {
    for (Pipeline pipeline : pipelines) {
      setSinglePipelineDetails(pipeline, withFinalValuesOnly);
    }
  }

  private void setSinglePipelineDetails(Pipeline pipeline, boolean withFinalValuesOnly) {
    boolean hasSshInfraMapping = false;
    boolean templatized = false;
    boolean pipelineParameterized = false;
    boolean infraRefactor = featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, pipeline.getAccountId());
    Set<String> invalidWorkflows = new HashSet<>();
    List<PipelineStage> pipelineStages = pipeline.getPipelineStages();
    List<Variable> pipelineVariables = new ArrayList<>();
    List<DeploymentType> deploymentTypes = new ArrayList<>();
    Map<String, Workflow> workflowCache = new HashMap<>();
    for (PipelineStage pipelineStage : pipelineStages) {
      for (PipelineStageElement pse : pipelineStage.getPipelineStageElements()) {
        if (!ENV_STATE.name().equals(pse.getType()) || pse.checkDisableAssertion()) {
          continue;
        }

        try {
          String workflowId = (String) pse.getProperties().get("workflowId");
          Workflow workflow;
          if (workflowCache.containsKey(workflowId)) {
            workflow = workflowCache.get(workflowId);
          } else {
            workflow = workflowService.readWorkflowWithoutServices(pipeline.getAppId(), workflowId, infraRefactor);
            notNullCheck("Workflow does not exist", workflow, USER);
            notNullCheck("Orchestration workflow does not exist", workflow.getOrchestrationWorkflow());
            workflowCache.put(workflowId, workflow);
          }

          if (!hasSshInfraMapping) {
            hasSshInfraMapping = workflowServiceHelper.workflowHasSshDeploymentPhase(
                (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow());
          }
          deploymentTypes.addAll(workflowServiceHelper.obtainDeploymentTypes(workflow.getOrchestrationWorkflow()));
          if (!templatized && isNotEmpty(pse.getWorkflowVariables())) {
            templatized = true;
          }

          if (!workflow.getOrchestrationWorkflow().isValid()) {
            invalidWorkflows.add(workflow.getName());
          }

          validateWorkflowVariables(workflow, pse, invalidWorkflows, pse.getWorkflowVariables());
          setPipelineVariables(workflow, pse, pipelineVariables, withFinalValuesOnly, infraRefactor);
          if (!pipelineParameterized) {
            pipelineParameterized = checkPipelineEntityParameterized(pse.getWorkflowVariables(), workflow);
          }
        } catch (Exception ex) {
          logger.warn(
              format("Exception occurred while reading workflow associated to the pipeline %s", pipeline.toString()),
              ex);
        }
      }
    }
    if (!invalidWorkflows.isEmpty()) {
      pipeline.setValid(false);
      pipeline.setValidationMessage(format(PIPELINE_ENV_STATE_VALIDATION_MESSAGE, invalidWorkflows.toString()));
    }

    pipeline.setPipelineVariables(reorderPipelineVariables(pipelineVariables));
    pipeline.setHasSshInfraMapping(hasSshInfraMapping);
    pipeline.setEnvParameterized(pipelineParameterized);
    pipeline.setTemplatized(templatized);
    pipeline.setDeploymentTypes(deploymentTypes.stream().distinct().collect(toList()));
  }

  private List<Variable> reorderPipelineVariables(List<Variable> pipelineVariables) {
    // Reorder pipeline variables
    List<Variable> reorderedPipelineVariables = new ArrayList<>();
    List<Variable> nonEntityVariables =
        pipelineVariables.stream().filter(variable -> variable.obtainEntityType() == null).collect(toList());
    List<Variable> entityVariables =
        pipelineVariables.stream().filter(variable -> variable.obtainEntityType() != null).collect(toList());
    reorderedPipelineVariables.addAll(entityVariables);
    reorderedPipelineVariables.addAll(nonEntityVariables);
    return reorderedPipelineVariables;
  }

  private boolean checkPipelineEntityParameterized(Map<String, String> pseWorkflowVaraibles, Workflow workflow) {
    List<Variable> workflowVariables = workflow.getOrchestrationWorkflow().getUserVariables();
    if (isEmpty(workflowVariables) || isEmpty(pseWorkflowVaraibles)) {
      return false;
    }
    boolean atleastOneEntityParameterized;
    List<Variable> entityVariables =
        workflowVariables.stream().filter(variable -> variable.obtainEntityType() != null).collect(toList());
    for (Variable variable : entityVariables) {
      String value = pseWorkflowVaraibles.get(variable.getName());
      if (value != null) {
        atleastOneEntityParameterized = matchesVariablePattern(value);
        if (atleastOneEntityParameterized) {
          return true;
        }
      }
    }
    return false;
  }

  private void setServicesAndPipelineVariables(Pipeline pipeline) {
    List<String> serviceIds = new ArrayList<>();
    List<String> envIds = new ArrayList<>();
    List<Variable> pipelineVariables = new ArrayList<>();
    boolean templatized = false;
    boolean envParameterized = false;
    boolean hasBuildWorkflow = false;
    boolean infraRefactor = featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, pipeline.getAccountId());
    List<DeploymentType> deploymentTypes = new ArrayList<>();
    Set<String> invalidWorkflows = new HashSet<>();
    Map<String, Workflow> workflowCache = new HashMap<>();
    for (PipelineStage pipelineStage : pipeline.getPipelineStages()) {
      for (PipelineStageElement pse : pipelineStage.getPipelineStageElements()) {
        if (ENV_STATE.name().equals(pse.getType())) {
          if (pse.checkDisableAssertion()) {
            continue;
          }

          String workflowId = (String) pse.getProperties().get("workflowId");
          Workflow workflow;
          if (workflowCache.containsKey(workflowId)) {
            workflow = workflowCache.get(workflowId);
          } else {
            workflow = workflowService.readWorkflowWithoutServices(pipeline.getAppId(), workflowId, infraRefactor);
            notNullCheck("Workflow does not exist", workflow, USER);
            notNullCheck("Orchestration workflow does not exist", workflow.getOrchestrationWorkflow());
            workflowCache.put(workflowId, workflow);
          }

          if (!workflow.getOrchestrationWorkflow().isValid()) {
            invalidWorkflows.add(workflow.getName());
          }

          if (!hasBuildWorkflow) {
            hasBuildWorkflow = BUILD.equals(workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType());
          }
          resolveArtifactNeededServicesOfWorkflowAndEnvIds(
              serviceIds, envIds, deploymentTypes, pse.getWorkflowVariables(), workflow);

          validateWorkflowVariables(workflow, pse, invalidWorkflows, pse.getWorkflowVariables());

          setPipelineVariables(workflow, pse, pipelineVariables, false, infraRefactor);
          if (!templatized && isNotEmpty(pse.getWorkflowVariables())) {
            templatized = true;
          }
          if (!envParameterized) {
            envParameterized = checkPipelineEntityParameterized(pse.getWorkflowVariables(), workflow);
          }
        }
      }
    }
    if (!invalidWorkflows.isEmpty()) {
      pipeline.setValid(false);
      pipeline.setValidationMessage(format(PIPELINE_ENV_STATE_VALIDATION_MESSAGE, invalidWorkflows.toString()));
    }

    pipeline.setServices(serviceResourceService.fetchServicesByUuids(pipeline.getAppId(), serviceIds));
    pipeline.setPipelineVariables(reorderPipelineVariables(pipelineVariables));
    pipeline.setEnvSummaries(environmentService.obtainEnvironmentSummaries(pipeline.getAppId(), envIds));
    pipeline.setTemplatized(templatized);
    pipeline.setEnvParameterized(envParameterized);
    pipeline.setDeploymentTypes(deploymentTypes);
    pipeline.setHasBuildWorkflow(hasBuildWorkflow);
  }

  private void setPipelineVariables(Workflow workflow, PipelineStageElement pse, List<Variable> pipelineVariables,
      boolean withFinalValuesOnly, boolean infraRefator) {
    List<Variable> workflowVariables = workflow.getOrchestrationWorkflow().getUserVariables();
    Map<String, String> pseWorkflowVariables = pse.getWorkflowVariables();

    if (isEmpty(workflowVariables)) {
      return;
    }
    List<Variable> nonEntityVariables =
        workflowVariables.stream()
            .filter(variable -> (variable.obtainEntityType() == null) && !variable.isFixed())
            .collect(toList());

    if (isEmpty(pseWorkflowVariables)) {
      if (!isEmpty(workflowVariables)) {
        nonEntityVariables.forEach(variable -> {
          if (!contains(pipelineVariables, variable.getName())) {
            pipelineVariables.add(variable.cloneInternal());
          }
        });
      }
      return;
    }
    for (Variable variable : workflowVariables) {
      notEmptyCheck("Empty variable name", variable.getName());
      String value = pseWorkflowVariables.get(variable.getName());
      if (variable.obtainEntityType() == null) {
        // Non-entity variables.
        if (isEmpty(value) && !variable.isFixed()) {
          if (!contains(pipelineVariables, variable.getName())) {
            pipelineVariables.add(variable.cloneInternal());
          }
        }
      } else {
        // Entity variables.
        if (isNotEmpty(value)) {
          String variableName = matchesVariablePattern(value) ? getName(value) : null;
          if (variableName != null) {
            // Variable is an expression - templatized pipeline.
            if (!contains(pipelineVariables, variableName)) {
              // Variable is an expression so prompt for the value at runtime.
              Variable pipelineVariable = variable.cloneInternal();
              pipelineVariable.setName(variableName);
              EntityType entityType = pipelineVariable.obtainEntityType();

              if (ENVIRONMENT.equals(entityType)) {
                setRelatedFieldEnvironment(infraRefator, workflowVariables, pseWorkflowVariables, pipelineVariable);
              } else {
                cloneRelatedFieldName(pseWorkflowVariables, pipelineVariable);
              }
              populateParentFields(
                  pipelineVariable, entityType, workflowVariables, variable.getName(), pseWorkflowVariables);
              if (withFinalValuesOnly) {
                // If only final concrete values are needed, set value as null as the used has not entered them yet.
                pipelineVariable.setValue(null);
              } else {
                // Set variable value as workflow templatized variable name.
                pipelineVariable.setValue(variable.getName());
              }
              pipelineVariables.add(pipelineVariable);
            } else {
              Variable storedVar = getContainedVariable(pipelineVariables, variableName);
              if (storedVar != null && ENVIRONMENT.equals(storedVar.obtainEntityType())) {
                updateRelatedFieldEnvironment(infraRefator, workflowVariables, pseWorkflowVariables, storedVar);
              }
            }
          }
        }
      }
    }
  }

  void populateParentFields(Variable pipelineVariable, EntityType entityType, List<Variable> workflowVariables,
      String originalVarName, Map<String, String> pseWorkflowVariables) {
    if (workflowVariables == null) {
      return;
    }
    workflowVariables = workflowVariables.stream()
                            .filter(t
                                -> t.obtainEntityType() != null && t.getMetadata() != null
                                    && t.getMetadata().get(Variable.RELATED_FIELD) != null)
                            .collect(Collectors.toList());
    Map<String, String> parentFields = new HashMap<>();
    switch (entityType) {
      case INFRASTRUCTURE_MAPPING:
      case INFRASTRUCTURE_DEFINITION:
        // instead of parent fields they have envId and service Id which is set inside the method
        handleInfraPipelineVariable(pipelineVariable, workflowVariables, originalVarName, pseWorkflowVariables);
        break;
      case APPDYNAMICS_TIERID:
        parentFields =
            handleAppDynamicsTierIdVariable(pipelineVariable, workflowVariables, originalVarName, pseWorkflowVariables);
        break;
      case APPDYNAMICS_APPID:
        parentFields =
            handleAppDynamicsAppIdVariable(pipelineVariable, workflowVariables, originalVarName, pseWorkflowVariables);
        break;
      case ELK_INDICES:
        parentFields =
            handleElkIndicesVariable(pipelineVariable, workflowVariables, originalVarName, pseWorkflowVariables);
        break;
      case NEWRELIC_APPID:
        parentFields =
            handleNewRelicAppIdVariable(pipelineVariable, workflowVariables, originalVarName, pseWorkflowVariables);
        break;
      case NEWRELIC_MARKER_APPID:
        parentFields = handleNewRelicMarkerAppIdVariable(
            pipelineVariable, workflowVariables, originalVarName, pseWorkflowVariables);
        break;
      case SPLUNK_CONFIGID:
        parentFields =
            handleSplunkConfigIdVariable(pipelineVariable, workflowVariables, originalVarName, pseWorkflowVariables);
        break;
      default:
        logger.info("no parent fields required to be set");
    }
    if (isNotEmpty(parentFields)) {
      if (pipelineVariable.getMetadata().get(Variable.PARENT_FIELDS) != null) {
        Map<String, String> existingParents =
            (Map<String, String>) pipelineVariable.getMetadata().get(Variable.PARENT_FIELDS);
        existingParents.putAll(parentFields);
      } else {
        pipelineVariable.getMetadata().put(Variable.PARENT_FIELDS, parentFields);
      }
    }
  }

  private Map<String, String> handleNewRelicMarkerAppIdVariable(Variable pipelineVariable,
      List<Variable> workflowVariables, String originalVarName, Map<String, String> pseWorkflowVariables) {
    Map<String, String> parentFields = new HashMap<>();
    for (Variable var : workflowVariables) {
      if (NEWRELIC_MARKER_CONFIGID.equals(var.obtainEntityType())) {
        if (var.getMetadata() != null && originalVarName.equals(var.getMetadata().get(Variable.RELATED_FIELD))) {
          String relatedVarValue = pseWorkflowVariables.get(var.getName());
          parentFields.put("analysisServerConfigId", relatedVarValue);
        }
      }
    }
    return parentFields;
  }

  private Map<String, String> handleNewRelicAppIdVariable(Variable pipelineVariable, List<Variable> workflowVariables,
      String originalVarName, Map<String, String> pseWorkflowVariables) {
    Map<String, String> parentFields = new HashMap<>();
    for (Variable var : workflowVariables) {
      if (NEWRELIC_CONFIGID.equals(var.obtainEntityType())) {
        if (var.getMetadata() != null && originalVarName.equals(var.getMetadata().get(Variable.RELATED_FIELD))) {
          String relatedVarValue = pseWorkflowVariables.get(var.getName());
          parentFields.put("analysisServerConfigId", relatedVarValue);
        }
      }
    }
    return parentFields;
  }

  private Map<String, String> handleElkIndicesVariable(Variable pipelineVariable, List<Variable> workflowVariables,
      String originalVarName, Map<String, String> pseWorkflowVariables) {
    Map<String, String> parentFields = new HashMap<>();
    for (Variable var : workflowVariables) {
      if (ELK_CONFIGID.equals(var.obtainEntityType())) {
        if (var.getMetadata() != null && originalVarName.equals(var.getMetadata().get(Variable.RELATED_FIELD))) {
          String relatedVarValue = pseWorkflowVariables.get(var.getName());
          parentFields.put("analysisServerConfigId", relatedVarValue);
        }
      }
    }
    return parentFields;
  }

  private Map<String, String> handleSplunkConfigIdVariable(Variable pipelineVariable, List<Variable> workflowVariables,
      String originalVarName, Map<String, String> pseWorkflowVariables) {
    Map<String, String> parentFields = new HashMap<>();
    for (Variable var : workflowVariables) {
      if (SPLUNK_CONFIGID.equals(var.obtainEntityType())) {
        if (var.getMetadata() != null && originalVarName.equals(var.getMetadata().get(Variable.RELATED_FIELD))) {
          String relatedVarValue = pseWorkflowVariables.get(var.getName());
          parentFields.put("analysisServerConfigId", relatedVarValue);
        }
      }
    }
    return parentFields;
  }

  private Map<String, String> handleAppDynamicsAppIdVariable(Variable pipelineVariable,
      List<Variable> workflowVariables, String originalVarName, Map<String, String> pseWorkflowVariables) {
    Map<String, String> parentFields = new HashMap<>();
    for (Variable var : workflowVariables) {
      if (APPDYNAMICS_CONFIGID.equals(var.obtainEntityType())) {
        if (var.getMetadata() != null && originalVarName.equals(var.getMetadata().get(Variable.RELATED_FIELD))) {
          String relatedVarValue = pseWorkflowVariables.get(var.getName());
          parentFields.put("analysisServerConfigId", relatedVarValue);
        }
      }
    }
    return parentFields;
  }

  private Map<String, String> handleAppDynamicsTierIdVariable(Variable pipelineVariable,
      List<Variable> workflowVariables, String originalVarName, Map<String, String> pseWorkflowVariables) {
    Map<String, String> parentFields = new HashMap<>();
    for (Variable var : workflowVariables) {
      if (APPDYNAMICS_APPID.equals(var.obtainEntityType())) {
        if (var.getMetadata() != null && originalVarName.equals(var.getMetadata().get(Variable.RELATED_FIELD))) {
          String relatedVarValue = pseWorkflowVariables.get(var.getName());
          parentFields.put("applicationId", relatedVarValue);
        }
      } else if (APPDYNAMICS_CONFIGID.equals(var.obtainEntityType())) {
        if (var.getMetadata() != null && originalVarName.equals(var.getMetadata().get(Variable.RELATED_FIELD))) {
          String relatedVarValue = pseWorkflowVariables.get(var.getName());
          parentFields.put("analysisServerConfigId", relatedVarValue);
        }
      }
    }
    return parentFields;
  }

  private void handleInfraPipelineVariable(Variable pipelineVariable, List<Variable> workflowVariables,
      String originalVarName, Map<String, String> pseWorkflowVariables) {
    for (Variable var : workflowVariables) {
      if (ENVIRONMENT.equals(var.obtainEntityType())) {
        if (var.getMetadata() == null || var.getMetadata().get(Variable.RELATED_FIELD) == null) {
          continue;
        }
        String relatedFields = String.valueOf(var.getMetadata().get(Variable.RELATED_FIELD));
        Optional<String> relatedField =
            Arrays.stream(relatedFields.split(",")).filter(t -> t.equals(originalVarName)).findFirst();
        if (relatedField.isPresent()) {
          String relatedVarValue = pseWorkflowVariables.get(var.getName());
          if (!matchesVariablePattern(relatedVarValue)) {
            pipelineVariable.getMetadata().put(Variable.ENV_ID, relatedVarValue);
          }
        }
      } else if (SERVICE.equals(var.obtainEntityType())) {
        if (var.getMetadata() != null && originalVarName.equals(var.getMetadata().get(Variable.RELATED_FIELD))) {
          String relatedVarValue = pseWorkflowVariables.get(var.getName());
          if (!matchesVariablePattern(relatedVarValue)) {
            pipelineVariable.getMetadata().put(Variable.SERVICE_ID, relatedVarValue);
          }
        }
      }
    }
  }

  private void cloneRelatedFieldName(Map<String, String> pseWorkflowVariables, Variable pipelineVariable) {
    if (pipelineVariable.getMetadata().get("relatedField") != null) {
      String relatedFieldOldValue = String.valueOf(pipelineVariable.getMetadata().get("relatedField"));
      if (isNotEmpty(relatedFieldOldValue) && !relatedFieldOldValue.equals("null")) {
        String relatedFieldNewValue = getName(pseWorkflowVariables.get(relatedFieldOldValue));
        pipelineVariable.getMetadata().put("relatedField", relatedFieldNewValue);
      }
    }
  }

  private void setRelatedFieldEnvironment(boolean infraRefactor, List<Variable> workflowVariables,
      Map<String, String> pseWorkflowVariables, Variable pipelineVariable) {
    if (infraRefactor) {
      pipelineVariable.getMetadata().put(
          "relatedField", join(",", getInfraDefVariables(workflowVariables, pseWorkflowVariables)));

    } else {
      pipelineVariable.getMetadata().put(
          "relatedField", join(",", getInfraVariables(workflowVariables, pseWorkflowVariables)));
    }
  }

  void updateRelatedFieldEnvironment(boolean infraRefactor, List<Variable> workflowVariables,
      Map<String, String> pseWorkflowVariables, Variable pipelineVariable) {
    if (infraRefactor) {
      String currentRelatedFields = (String) pipelineVariable.getMetadata().get("relatedField");
      pipelineVariable.getMetadata().put("relatedField",
          StringUtils.join(
              currentRelatedFields, ",", join(",", getInfraDefVariables(workflowVariables, pseWorkflowVariables))));

    } else {
      String currentRelatedFields = (String) pipelineVariable.getMetadata().get("relatedField");
      pipelineVariable.getMetadata().put("relatedField",
          StringUtils.join(
              currentRelatedFields, ",", join(",", getInfraVariables(workflowVariables, pseWorkflowVariables))));
    }
  }

  private List<String> getInfraVariables(List<Variable> workflowVariables, Map<String, String> pseWorkflowVariables) {
    List<Variable> infraMappingVariable =
        workflowVariables.stream()
            .filter(t -> t.obtainEntityType() != null && t.obtainEntityType().equals(INFRASTRUCTURE_MAPPING))
            .collect(toList());
    List<String> infraVarNames = new ArrayList<>();
    for (Variable variable : infraMappingVariable) {
      infraVarNames.add(getName(pseWorkflowVariables.get(variable.getName())));
    }
    return infraVarNames;
  }

  private List<String> getInfraDefVariables(
      List<Variable> workflowVariables, Map<String, String> pseWorkflowVariables) {
    List<Variable> infraDefVariable =
        workflowVariables.stream()
            .filter(t -> t.obtainEntityType() != null && t.obtainEntityType().equals(INFRASTRUCTURE_DEFINITION))
            .collect(toList());
    List<String> infraVarNames = new ArrayList<>();
    for (Variable variable : infraDefVariable) {
      infraVarNames.add(getName(pseWorkflowVariables.get(variable.getName())));
    }
    return infraVarNames;
  }

  private void resolveEnvIds(List<String> envIds, Map<String, String> pseWorkflowVariables, Workflow workflow) {
    String envId = workflowService.resolveEnvironmentId(workflow, pseWorkflowVariables);
    if (envId != null && !envIds.contains(envId)) {
      envIds.add(envId);
    }
  }

  private void resolveArtifactNeededServicesOfWorkflowAndEnvIds(List<String> serviceIds, List<String> envIds,
      List<DeploymentType> deploymentTypes, Map<String, String> pseWorkflowVariables, Workflow workflow) {
    DeploymentMetadata deploymentMetadata =
        workflowService.fetchDeploymentMetadata(workflow.getAppId(), workflow, pseWorkflowVariables, serviceIds, envIds,
            Include.ARTIFACT_SERVICE, Include.ENVIRONMENT, Include.DEPLOYMENT_TYPE);
    if (deploymentMetadata != null) {
      if (deploymentMetadata.getArtifactRequiredServiceIds() != null) {
        deploymentMetadata.getArtifactRequiredServiceIds()
            .stream()
            .filter(serviceId -> !serviceIds.contains(serviceId))
            .forEach(serviceIds::add);
      }
      if (deploymentMetadata.getEnvIds() != null) {
        deploymentMetadata.getEnvIds().stream().filter(envId -> !envIds.contains(envId)).forEach(envIds::add);
      }
      if (deploymentMetadata.getDeploymentTypes() != null) {
        deploymentMetadata.getDeploymentTypes()
            .stream()
            .filter(deploymentType -> !deploymentTypes.contains(deploymentType))
            .forEach(deploymentTypes::add);
      }
    }
  }

  private void validateWorkflowVariables(Workflow workflow, PipelineStageElement pse, Set<String> invalidWorkflows,
      Map<String, String> pseWorkflowVariables) {
    if (isEmpty(pseWorkflowVariables)) {
      return;
    }
    Set<String> pseWorkflowVariableNames = pseWorkflowVariables.keySet();
    Set<String> workflowVariableNames = (workflow.getOrchestrationWorkflow().getUserVariables() == null)
        ? new HashSet<>()
        : (workflow.getOrchestrationWorkflow()
                  .getUserVariables()
                  .stream()
                  .map(variable -> variable.getName())
                  .collect(toSet()));
    for (String pseWorkflowVariable : pseWorkflowVariableNames) {
      if (!workflowVariableNames.contains(pseWorkflowVariable)) {
        pse.setValid(false);
        pse.setValidationMessage("Workflow variables updated or deleted");
        invalidWorkflows.add(workflow.getName());
        break;
      }
    }
  }

  private boolean contains(List<Variable> pipelineVariables, String name) {
    return pipelineVariables.stream().anyMatch(
        variable -> variable != null && variable.getName() != null && variable.getName().equals(name));
  }

  private Variable getContainedVariable(List<Variable> pipelineVariables, String name) {
    return pipelineVariables.stream()
        .filter(variable -> variable != null && variable.getName() != null && variable.getName().equals(name))
        .findFirst()
        .orElse(null);
  }

  @Override
  @ValidationGroups(Create.class)
  public Pipeline save(Pipeline pipeline) {
    validatePipelineNameForDuplicates(pipeline);
    ensurePipelineStageUuidAndParallelIndex(pipeline);
    checkUniquePipelineStepName(pipeline);

    String accountId = appService.getAccountIdByAppId(pipeline.getAppId());
    pipeline.setAccountId(accountId);
    StaticLimitCheckerWithDecrement checker = (StaticLimitCheckerWithDecrement) limitCheckerFactory.getInstance(
        new Action(accountId, ActionType.CREATE_PIPELINE));

    return LimitEnforcementUtils.withLimitCheck(checker, () -> {
      Set<String> keywords = pipeline.generateKeywords();
      validatePipeline(pipeline, keywords);
      pipeline.setKeywords(trimmedLowercaseSet(keywords));

      wingsPersistence.save(pipeline);

      // TODO: remove this when all the needed verification is done from validatePipeline
      new StateMachine(pipeline, workflowService.stencilMap(pipeline.getAppId()));

      yamlPushService.pushYamlChangeSet(accountId, null, pipeline, Type.CREATE, pipeline.isSyncFromGit(), false);

      if (!pipeline.isSample()) {
        eventPublishHelper.publishAccountEvent(
            accountId, AccountEvent.builder().accountEventType(AccountEventType.PIPELINE_CREATED).build(), true, true);
      }

      return pipeline;
    });
  }

  private void validatePipelineNameForDuplicates(Pipeline pipeline) {
    if (wingsPersistence.createQuery(Pipeline.class)
            .filter(PipelineKeys.appId, pipeline.getAppId())
            .filter(PipelineKeys.name, pipeline.getName())
            .getKey()
        != null) {
      throw new InvalidRequestException("Duplicate name " + pipeline.getName(), USER);
    }
  }

  @Override
  public List<String> obtainPipelineNamesReferencedByTemplatedEntity(String appId, String templatedEntityId) {
    List<String> referencedPipelines = new ArrayList<>();
    try (HIterator<Pipeline> pipelineHIterator =
             new HIterator<>(wingsPersistence.createQuery(Pipeline.class).filter(PipelineKeys.appId, appId).fetch())) {
      while (pipelineHIterator.hasNext()) {
        Pipeline pipeline = pipelineHIterator.next();
        // Templatized Id in workflow variables
        if (pipeline.getPipelineStages()
                .stream()
                .flatMap(pipelineStage -> pipelineStage.getPipelineStageElements().stream())
                .anyMatch(pse
                    -> pse.getWorkflowVariables() != null
                        && pse.getWorkflowVariables().values().contains(templatedEntityId))) {
          referencedPipelines.add(pipeline.getName());
        }
      }
    }
    return referencedPipelines;
  }

  @Override
  public DeploymentMetadata fetchDeploymentMetadata(String appId, String pipelineId,
      Map<String, String> pipelineVariables, List<String> artifactNeededServiceIds, List<String> envIds,
      DeploymentMetadata.Include... includeList) {
    return fetchDeploymentMetadata(
        appId, pipelineId, pipelineVariables, artifactNeededServiceIds, envIds, false, null, includeList);
  }

  @Override
  public DeploymentMetadata fetchDeploymentMetadata(String appId, String pipelineId,
      Map<String, String> pipelineVariables, List<String> artifactNeededServiceIds, List<String> envIds,
      boolean withDefaultArtifact, WorkflowExecution workflowExecution, DeploymentMetadata.Include... includeList) {
    Map<String, Workflow> workflowCache = new HashMap<>();
    Pipeline pipeline = readPipelineWithResolvedVariables(appId, pipelineId, pipelineVariables, workflowCache);
    return fetchDeploymentMetadata(appId, pipeline, artifactNeededServiceIds, envIds, withDefaultArtifact,
        workflowExecution, workflowCache, includeList);
  }

  @Override
  public DeploymentMetadata fetchDeploymentMetadata(String appId, Pipeline pipeline,
      List<String> artifactNeededServiceIds, List<String> envIds, DeploymentMetadata.Include... includeList) {
    return fetchDeploymentMetadata(appId, pipeline, artifactNeededServiceIds, envIds, false, null, null, includeList);
  }

  private DeploymentMetadata fetchDeploymentMetadata(String appId, Pipeline pipeline,
      List<String> artifactNeededServiceIds, List<String> envIds, boolean withDefaultArtifact,
      WorkflowExecution workflowExecution, Map<String, Workflow> workflowCache,
      DeploymentMetadata.Include... includeList) {
    notNullCheck("Pipeline does not exist", pipeline, USER);
    if (artifactNeededServiceIds == null) {
      artifactNeededServiceIds = new ArrayList<>();
    }
    if (envIds == null) {
      envIds = new ArrayList<>();
    }
    DeploymentMetadata finalDeploymentMetadata = DeploymentMetadata.builder()
                                                     .artifactRequiredServiceIds(artifactNeededServiceIds)
                                                     .envIds(envIds)
                                                     .deploymentTypes(new ArrayList<>())
                                                     .artifactVariables(new ArrayList<>())
                                                     .build();

    boolean infraRefactor = featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, pipeline.getAccountId());
    boolean isBuildPipeline = false;
    if (workflowCache == null) {
      workflowCache = new HashMap<>();
    }
    for (PipelineStage pipelineStage : pipeline.getPipelineStages()) {
      for (PipelineStageElement pse : pipelineStage.getPipelineStageElements()) {
        if (!ENV_STATE.name().equals(pse.getType()) || pse.checkDisableAssertion()) {
          continue;
        }

        String workflowId = (String) pse.getProperties().get("workflowId");
        Workflow workflow;
        if (workflowCache.containsKey(workflowId)) {
          workflow = workflowCache.get(workflowId);
        } else {
          workflow = workflowService.readWorkflowWithoutServices(pipeline.getAppId(), workflowId, infraRefactor);
          notNullCheck("Workflow does not exist", workflow, USER);
          notNullCheck("Orchestration workflow does not exist", workflow.getOrchestrationWorkflow(), USER);
          workflowCache.put(workflowId, workflow);
        }

        OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
        DeploymentMetadata deploymentMetadata = workflowService.fetchDeploymentMetadata(appId, workflow,
            pse.getWorkflowVariables(), null, null, withDefaultArtifact, workflowExecution, includeList);
        if (deploymentMetadata == null) {
          continue;
        }

        if (!isBuildPipeline && BUILD.equals(orchestrationWorkflow.getOrchestrationWorkflowType())) {
          // If pipeline is a build pipeline, don't get artifact variable metadata.
          isBuildPipeline = true;

          // Remove any existing artifact variables.
          finalDeploymentMetadata.setArtifactVariables(new ArrayList<>());

          // Remove ARTIFACT_SERVICE from includeList.
          Stream<Include> includeStream =
              isEmpty(includeList) ? Arrays.stream(Include.values()) : Arrays.stream(includeList);
          includeList =
              includeStream.filter(include -> !Include.ARTIFACT_SERVICE.equals(include)).toArray(Include[] ::new);
        }

        mergeDeploymentMetadata(workflow, finalDeploymentMetadata, deploymentMetadata);
      }
    }

    return finalDeploymentMetadata;
  }

  private void mergeDeploymentMetadata(
      Workflow workflow, DeploymentMetadata finalDeploymentMetadata, DeploymentMetadata deploymentMetadata) {
    mergeLists(
        finalDeploymentMetadata.getArtifactRequiredServiceIds(), deploymentMetadata.getArtifactRequiredServiceIds());
    mergeLists(finalDeploymentMetadata.getEnvIds(), deploymentMetadata.getEnvIds());
    mergeLists(finalDeploymentMetadata.getDeploymentTypes(), deploymentMetadata.getDeploymentTypes());

    if (isEmpty(deploymentMetadata.getArtifactVariables())) {
      return;
    }

    List<ArtifactVariable> finalArtifactVariables = finalDeploymentMetadata.getArtifactVariables();
    for (ArtifactVariable artifactVariable : deploymentMetadata.getArtifactVariables()) {
      mergeArtifactVariable(finalArtifactVariables, artifactVariable, workflow.getUuid());
    }
  }

  private void mergeArtifactVariable(
      List<ArtifactVariable> finalArtifactVariables, ArtifactVariable artifactVariable, String workflowId) {
    if (artifactVariable == null) {
      return;
    }

    List<ArtifactVariable> duplicateArtifactVariables =
        finalArtifactVariables.stream().filter(av -> av.getName().equals(artifactVariable.getName())).collect(toList());
    if (isEmpty(duplicateArtifactVariables)) {
      addArtifactVariable(finalArtifactVariables, artifactVariable, workflowId);
      return;
    }

    boolean merged = false;
    for (ArtifactVariable duplicateArtifactVariable : duplicateArtifactVariables) {
      String duplicateServiceId = duplicateArtifactVariable.fetchAssociatedService();
      if (duplicateServiceId != null) {
        String serviceId = artifactVariable.fetchAssociatedService();
        if (serviceId != null && serviceId.equals(duplicateServiceId)) {
          if (!duplicateArtifactVariable.getWorkflowIds().contains(workflowId)) {
            duplicateArtifactVariable.getWorkflowIds().add(workflowId);
          }
          merged = true;
          break;
        }
      }
    }

    if (!merged) {
      addArtifactVariable(finalArtifactVariables, artifactVariable, workflowId);
    }
  }

  private void addArtifactVariable(
      List<ArtifactVariable> finalArtifactVariables, ArtifactVariable artifactVariable, String workflowId) {
    artifactVariable.setWorkflowIds(new ArrayList<>(Collections.singletonList(workflowId)));
    finalArtifactVariables.add(artifactVariable);
  }

  private static <T> void mergeLists(List<T> to, List<T> from) {
    // NOTE: to should not be null
    if (isEmpty(from)) {
      return;
    }

    for (T el : from) {
      if (!to.contains(el)) {
        to.add(el);
      }
    }
  }

  @VisibleForTesting
  void checkUniquePipelineStepName(Pipeline pipeline) {
    List<PipelineStage> pipelineStages = pipeline.getPipelineStages();
    if (pipelineStages == null) {
      return;
    }
    Set<String> pipelineStageNameSet = new HashSet<>();
    for (PipelineStage pipelineStage : pipelineStages) {
      if (pipelineStage == null) {
        continue;
      }
      PipelineStageElement stageElement = pipelineStage.getPipelineStageElements().get(0);
      if (stageElement == null) {
        continue;
      }
      if (pipelineStageNameSet.contains(stageElement.getName())) {
        throw new InvalidRequestException(String.format("Duplicate step name %s.", stageElement.getName()), USER);
      }
      pipelineStageNameSet.add(stageElement.getName());
    }
  }

  private void validatePipeline(Pipeline pipeline, Set<String> keywords) {
    List<Service> services = new ArrayList<>();
    List<String> serviceIds = new ArrayList<>();
    final List<PipelineStage> pipelineStages = pipeline.getPipelineStages();
    Set<String> parameterizedEnvIds = new HashSet<>();
    if (pipelineStages != null) {
      for (int i = 0; i < pipelineStages.size(); ++i) {
        PipelineStage pipelineStage = pipelineStages.get(i);
        for (PipelineStageElement stageElement : pipelineStage.getPipelineStageElements()) {
          if (!isValidPipelineStageName(stageElement.getName())) {
            throw new InvalidArgumentsException("Pipeline stage name can only have a-z, A-Z, 0-9, -, (, ) and _", USER);
          }
          if (APPROVAL.name().equals(stageElement.getType())) {
            ApprovalState.preValidatePropertyMap(stageElement.getProperties());
          }
          if (!isValidSkipCondition(stageElement)) {
            throw new InvalidArgumentsException("Not a valid skip condition for " + stageElement.getName(), USER);
          }

          if (!ENV_STATE.name().equals(stageElement.getType())) {
            continue;
          }
          if (isNullOrEmpty((String) stageElement.getProperties().get("workflowId"))) {
            throw new InvalidArgumentsException("Workflow can not be null for Environment state", USER);
          }
          Workflow workflow = workflowService.readWorkflow(
              pipeline.getAppId(), (String) stageElement.getProperties().get("workflowId"));
          if (workflow == null || workflow.getOrchestrationWorkflow() == null) {
            throw new InvalidArgumentsException("Workflow can not be null for Environment state", USER);
          }
          keywords.add(workflow.getName());
          keywords.add(workflow.getDescription());
          List<Service> resolvedServiceForWorkflow =
              resolveServices(services, serviceIds, stageElement.getWorkflowVariables(), workflow);
          if (workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType() != BUILD
              && isNullOrEmpty((String) stageElement.getProperties().get("envId"))) {
            logger.info("It should not happen. If happens printing the properties of appId {} are {}",
                pipeline.getAppId(), stageElement.getProperties());
            throw new InvalidArgumentsException("Environment can not be null for non-build state", USER);
          }

          if (workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType() == ROLLING) {
            for (Service service : emptyIfNull(resolvedServiceForWorkflow)) {
              if (service.getDeploymentType() == DeploymentType.KUBERNETES && !service.isK8sV2()) {
                throw new InvalidRequestException(format("Rolling Type Workflow does not suport k8s-v1 "
                        + "service [%s]",
                    service.getName()));
              }
            }
          }

          String envId = workflowService.obtainTemplatedEnvironmentId(workflow, stageElement.getWorkflowVariables());
          if (envId != null && matchesVariablePattern(envId)) {
            parameterizedEnvIds.add(envId);
          }
        }
      }
    }
    if (parameterizedEnvIds.size() > 1) {
      throw new InvalidArgumentsException(
          "A pipeline may only have one environment expression across all workflows", USER);
    }
    keywords.addAll(services.stream().map(service -> service.getName()).distinct().collect(toList()));
  }

  private boolean isValidPipelineStageName(String name) {
    if (isEmpty(name)) {
      return false;
    }
    return ALLOWED_CHARS_SET_PIPELINE_STAGE.containsAll(Sets.newHashSet(Lists.charactersOf(name)));
  }

  private boolean isValidSkipCondition(PipelineStageElement pipelineStageElement) {
    if (APPROVAL.name().equals(pipelineStageElement.getType())) {
      return pipelineStageElement.getDisableAssertion() == null
          || pipelineStageElement.getDisableAssertion().equals("true");
    } else {
      return true;
    }
  }

  private boolean prunePipeline(String appId, String pipelineId) {
    // First lets make sure that we have persisted a job that will prone the descendant objects
    pruneQueue.send(new PruneEvent(Pipeline.class, appId, pipelineId));
    return wingsPersistence.delete(Pipeline.class, appId, pipelineId);
  }
}
