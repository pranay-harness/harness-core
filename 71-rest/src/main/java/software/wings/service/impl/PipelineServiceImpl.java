package software.wings.service.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.ListUtils.trimList;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.eraro.ErrorCode.PIPELINE_EXECUTION_IN_PROGRESS;
import static io.harness.exception.WingsException.USER;
import static io.harness.mongo.MongoUtils.setUnset;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.beans.PipelineExecution.PIPELINE_ID_KEY;
import static software.wings.common.Constants.PIPELINE_ENV_STATE_VALIDATION_MESSAGE;
import static software.wings.expression.ManagerExpressionEvaluator.getName;
import static software.wings.expression.ManagerExpressionEvaluator.matchesVariablePattern;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import de.danielbechler.util.Collections;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import io.harness.scheduler.PersistentScheduler;
import io.harness.validation.Create;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.EntityType;
import software.wings.beans.Event.Type;
import software.wings.beans.FailureStrategy;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineExecution;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Service;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.trigger.Trigger;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.PruneEntityJob;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.ownership.OwnedByPipeline;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateMachine;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 10/26/16.
 */
@Singleton
@ValidateOnExecution
public class PipelineServiceImpl implements PipelineService {
  private static final Logger logger = LoggerFactory.getLogger(PipelineServiceImpl.class);
  private static final Set<Character> ALLOWED_CHARS_SET_PIPELINE_STAGE =
      Sets.newHashSet(Lists.charactersOf("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_ ()"));

  @Inject private AppService appService;
  @Inject private ExecutorService executorService;
  @Inject private TriggerService triggerService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private YamlPushService yamlPushService;
  @Inject private WorkflowServiceHelper workflowServiceHelper;

  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

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
  public PageResponse<Pipeline> listPipelines(
      PageRequest<Pipeline> pageRequest, boolean withDetails, Integer previousExecutionsCount) {
    PageResponse<Pipeline> res = wingsPersistence.query(Pipeline.class, pageRequest);
    List<Pipeline> pipelines = res.getResponse();
    if (withDetails) {
      setPipelineDetails(pipelines);
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
          logger.error(format("Failed to fetch recent executions for pipeline %s", pipeline), e);
        }
      }
    }
    return res;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Pipeline update(Pipeline pipeline) {
    Pipeline savedPipeline = wingsPersistence.get(Pipeline.class, pipeline.getAppId(), pipeline.getUuid());
    notNullCheck("Pipeline not saved", savedPipeline, USER);

    List<Object> keywords = pipeline.generateKeywords();
    ensurePipelineStageUuids(pipeline);

    validatePipeline(pipeline, keywords);
    UpdateOperations<Pipeline> ops = wingsPersistence.createUpdateOperations(Pipeline.class);
    setUnset(ops, "description", pipeline.getDescription());
    setUnset(ops, "name", pipeline.getName());
    setUnset(ops, "pipelineStages", pipeline.getPipelineStages());
    setUnset(ops, "failureStrategies", pipeline.getFailureStrategies());
    setUnset(ops, "keywords", trimList(keywords));

    wingsPersistence.update(wingsPersistence.createQuery(Pipeline.class)
                                .filter("appId", pipeline.getAppId())
                                .filter(ID_KEY, pipeline.getUuid()),
        ops);

    wingsPersistence.saveAndGet(StateMachine.class, new StateMachine(pipeline, workflowService.stencilMap()));

    Pipeline updatedPipeline = wingsPersistence.get(Pipeline.class, pipeline.getAppId(), pipeline.getUuid());
    String accountId = appService.getAccountIdByAppId(pipeline.getAppId());
    boolean isRename = !savedPipeline.getName().equals(pipeline.getName());

    if (isRename) {
      executorService.submit(() -> triggerService.updateByApp(pipeline.getAppId()));
    }

    yamlPushService.pushYamlChangeSet(
        accountId, savedPipeline, updatedPipeline, Type.UPDATE, pipeline.isSyncFromGit(), isRename);

    return updatedPipeline;
  }

  private void ensurePipelineStageUuids(Pipeline pipeline) {
    // The UI or other agents my try to update the pipeline with new stages without uuids. This makes sure that
    // they all will have one.
    pipeline.getPipelineStages()
        .stream()
        .flatMap(stage -> stage.getPipelineStageElements().stream())
        .forEach(element -> {
          if (element.getUuid() == null) {
            element.setUuid(generateUuid());
          }
        });
  }

  @Override
  public List<FailureStrategy> updateFailureStrategies(
      String appId, String pipelineId, List<FailureStrategy> failureStrategies) {
    Pipeline savedPipeline = wingsPersistence.get(Pipeline.class, appId, pipelineId);
    notNullCheck("pipeline", savedPipeline);

    savedPipeline.setFailureStrategies(failureStrategies);
    Pipeline pipeline = update(savedPipeline);
    return pipeline.getFailureStrategies();
  }

  @Override
  public List<String> isEnvironmentReferenced(String appId, String envId) {
    List<String> referencedPipelines = new ArrayList<>();
    try (HIterator<Pipeline> pipelineHIterator =
             new HIterator<>(wingsPersistence.createQuery(Pipeline.class).filter(APP_ID_KEY, appId).fetch())) {
      while (pipelineHIterator.hasNext()) {
        Pipeline pipeline = pipelineHIterator.next();
        if (pipeline.getPipelineStages() != null) {
        PIPELINE_STAGE_LOOP:
          for (PipelineStage pipelineStage : pipeline.getPipelineStages()) {
            if (pipelineStage.getPipelineStageElements() != null) {
              for (PipelineStageElement pipelineStageElement : pipelineStage.getPipelineStageElements()) {
                // Env Id in pipeline
                if (pipelineStageElement.getProperties() != null
                    && pipelineStageElement.getProperties().get("envId") != null
                    && pipelineStageElement.getProperties().get("envId").equals(envId)) {
                  referencedPipelines.add(pipeline.getName());
                  break PIPELINE_STAGE_LOOP;
                }
                // Env Id in workflow variables
                if (pipelineStageElement.getWorkflowVariables() != null
                    && pipelineStageElement.getWorkflowVariables().values().contains(envId)) {
                  referencedPipelines.add(pipeline.getName());
                  break PIPELINE_STAGE_LOOP;
                }
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
                                                     .addFilter(APP_ID_KEY, EQ, pipeline.getAppId())
                                                     .addFilter(PIPELINE_ID_KEY, EQ, pipeline.getUuid())
                                                     .build();
    PageResponse<PipelineExecution> pageResponse = wingsPersistence.query(PipelineExecution.class, pageRequest);
    if (pageResponse == null || isEmpty(pageResponse.getResponse())
        || pageResponse.getResponse().stream().allMatch(
               pipelineExecution -> ExecutionStatus.isFinalStatus(pipelineExecution.getStatus()))) {
      List<Trigger> triggers = triggerService.getTriggersHasPipelineAction(pipeline.getAppId(), pipeline.getUuid());
      if (isEmpty(triggers)) {
        return;
      }
      List<String> triggerNames = triggers.stream().map(Trigger::getName).collect(toList());
      throw new InvalidRequestException(
          format("Pipeline associated as a trigger action to triggers [%s]", Joiner.on(", ").join(triggerNames)), USER);
    }
    throw new WingsException(PIPELINE_EXECUTION_IN_PROGRESS, USER)
        .addParam("message", format("Pipeline:[%s] couldn't be deleted", pipeline.getName()));
  }

  @Override
  public boolean deletePipeline(String appId, String pipelineId) {
    return deletePipeline(appId, pipelineId, false, false);
  }

  private boolean deletePipeline(String appId, String pipelineId, boolean forceDelete, boolean syncFromGit) {
    Pipeline pipeline = wingsPersistence.get(Pipeline.class, appId, pipelineId);
    if (pipeline == null) {
      return true;
    }

    if (!forceDelete) {
      ensurePipelineSafeToDelete(pipeline);
    }

    String accountId = appService.getAccountIdByAppId(pipeline.getAppId());
    yamlPushService.pushYamlChangeSet(accountId, pipeline, null, Type.DELETE, syncFromGit, false);

    return prunePipeline(appId, pipelineId);
  }

  @Override
  public void deleteByYamlGit(String appId, String pipelineId, boolean syncFromGit) {
    deletePipeline(appId, pipelineId, false, syncFromGit);
  }

  @Override
  public void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String pipelineId) {
    List<OwnedByPipeline> services =
        ServiceClassLocator.descendingServices(this, PipelineServiceImpl.class, OwnedByPipeline.class);
    PruneEntityJob.pruneDescendingEntities(services, descending -> descending.pruneByPipeline(appId, pipelineId));
  }

  @Override
  public void pruneByApplication(String appId) {
    List<Key<Pipeline>> pipelineKeys =
        wingsPersistence.createQuery(Pipeline.class).filter(APP_ID_KEY, appId).asKeyList();
    for (Key key : pipelineKeys) {
      prunePipeline(appId, (String) key.getId());
    }
  }

  @Override
  public Pipeline clonePipeline(String originalPipelineId, Pipeline pipeline) {
    Pipeline originalPipeline = readPipeline(pipeline.getAppId(), originalPipelineId, false);
    Pipeline clonedPipeline = originalPipeline.cloneInternal();
    clonedPipeline.setName(pipeline.getName());
    clonedPipeline.setDescription(pipeline.getDescription());
    return save(clonedPipeline);
  }

  @Override
  public List<EntityType> getRequiredEntities(String appId, String pipelineId) {
    Pipeline pipeline = wingsPersistence.get(Pipeline.class, appId, pipelineId);
    notNullCheck("pipeline", pipeline, USER);
    List<EntityType> entityTypes = new ArrayList<>();
    for (PipelineStage pipelineStage : pipeline.getPipelineStages()) {
      for (PipelineStageElement pipelineStageElement : pipelineStage.getPipelineStageElements()) {
        if (pipelineStageElement.isDisable()) {
          continue;
        }
        if (ENV_STATE.name().equals(pipelineStageElement.getType())) {
          Workflow workflow = workflowService.readWorkflow(
              pipeline.getAppId(), (String) pipelineStageElement.getProperties().get("workflowId"));
          OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
          if (orchestrationWorkflow != null) {
            if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(BUILD)) {
              return new ArrayList<>();
            }
            Set<EntityType> requiredEntityTypes =
                workflowService.fetchRequiredEntityTypes(appId, orchestrationWorkflow);
            if (requiredEntityTypes != null) {
              entityTypes.addAll(requiredEntityTypes);
            }
          }
        }
      }
    }
    return entityTypes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Pipeline readPipeline(String appId, String pipelineId, boolean withServices) {
    Pipeline pipeline = wingsPersistence.get(Pipeline.class, appId, pipelineId);
    notNullCheck("Pipeline does not exist", pipeline, USER);
    if (withServices) {
      setServicesAndPipelineVariables(pipeline);
    }
    return pipeline;
  }

  @Override
  public Pipeline readPipelineWithResolvedVariables(
      String appId, String pipelineId, Map<String, String> inputPipelineVariables) {
    Pipeline pipeline = wingsPersistence.get(Pipeline.class, appId, pipelineId);
    notNullCheck("Pipeline does not exist", pipeline, USER);
    List<Service> services = new ArrayList<>();
    List<String> serviceIds = new ArrayList<>();
    List<String> envIds = new ArrayList<>();
    Map<String, String> resolvedPipelineVariables = new LinkedHashMap<>();
    Map<String, String> reducedPipelineVariables =
        isEmpty(inputPipelineVariables) ? new LinkedHashMap<>() : new LinkedHashMap<>(inputPipelineVariables);
    for (PipelineStage pipelineStage : pipeline.getPipelineStages()) {
      for (PipelineStageElement pipelineStageElement : pipelineStage.getPipelineStageElements()) {
        if (ENV_STATE.name().equals(pipelineStageElement.getType())) {
          try {
            Workflow workflow = workflowService.readWorkflow(
                pipeline.getAppId(), (String) pipelineStageElement.getProperties().get("workflowId"));
            notNullCheck("Workflow does not exist", workflow);
            Map<String, String> workflowVariables = pipelineStageElement.getWorkflowVariables();
            Map<String, String> resolvedWorkflowVariables = new LinkedHashMap<>();
            if (isNotEmpty(workflowVariables)) {
              for (Entry<String, String> variableEntry : workflowVariables.entrySet()) {
                String variableName = variableEntry.getKey();
                String variableValue = getName(workflowVariables.get(variableName));
                if (isNotEmpty(inputPipelineVariables)) {
                  if (inputPipelineVariables.containsKey(variableValue)) {
                    resolvedPipelineVariables.put(variableName, inputPipelineVariables.get(variableValue));
                    resolvedWorkflowVariables.put(variableName, inputPipelineVariables.get(variableValue));
                    reducedPipelineVariables.remove(variableValue);
                  } else if (inputPipelineVariables.containsKey(variableName)) {
                    resolvedPipelineVariables.put(variableName, inputPipelineVariables.get(variableName));
                    resolvedWorkflowVariables.put(variableName, inputPipelineVariables.get(variableName));
                    reducedPipelineVariables.remove(variableName);
                  } else {
                    resolvedWorkflowVariables.put(variableName, variableEntry.getValue());
                  }
                } else {
                  if (!matchesVariablePattern(variableEntry.getValue())) {
                    resolvedWorkflowVariables.put(variableName, variableValue);
                  }
                }
              }
            }
            if (!BUILD.equals(workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType())) {
              resolveServicesOfWorkflow(services, serviceIds, resolvedWorkflowVariables, workflow);
              resolveEnvIds(envIds, resolvedWorkflowVariables, workflow);
            }
          } catch (Exception ex) {
            logger.warn("Exception occurred while reading workflow associated to the pipeline + [" + pipelineId
                    + "]. Reason: {}",
                ex.getMessage());
          }
        }
      }
    }
    pipeline.setServices(services);
    // Add all input variables to the resolved pipeline variables
    for (Entry<String, String> entry : reducedPipelineVariables.entrySet()) {
      if (!resolvedPipelineVariables.containsKey(entry.getKey())) {
        resolvedPipelineVariables.put(entry.getKey(), entry.getValue());
      }
    }
    pipeline.setResolvedPipelineVariables(resolvedPipelineVariables);
    pipeline.setEnvIds(envIds);
    return pipeline;
  }

  @Override
  public Pipeline getPipelineByName(String appId, String pipelineName) {
    return wingsPersistence.createQuery(Pipeline.class).filter("appId", appId).filter("name", pipelineName).get();
  }

  private void setPipelineDetails(List<Pipeline> pipelines) {
    for (Pipeline pipeline : pipelines) {
      boolean hasSshInfraMapping = false;
      boolean templatized = false;
      boolean pipelineParameterized = false;
      List<String> invalidWorkflows = new ArrayList<>();
      List<PipelineStage> pipelineStages = pipeline.getPipelineStages();
      List<Variable> pipelineVariables = new ArrayList<>();
      for (PipelineStage pipelineStage : pipelineStages) {
        List<String> invalidStageWorkflows = new ArrayList<>();
        for (PipelineStageElement pse : pipelineStage.getPipelineStageElements()) {
          if (pse.isDisable()) {
            continue;
          }
          if (ENV_STATE.name().equals(pse.getType())) {
            try {
              Workflow workflow =
                  workflowService.readWorkflow(pipeline.getAppId(), (String) pse.getProperties().get("workflowId"));
              Validator.notNullCheck("Workflow does not exist", workflow, USER);
              Validator.notNullCheck("Orchestration workflow does not exist", workflow.getOrchestrationWorkflow());
              if (!hasSshInfraMapping) {
                hasSshInfraMapping = workflowServiceHelper.workflowHasSshDeploymentPhase(
                    (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow());
              }
              if (!templatized && isNotEmpty(pse.getWorkflowVariables())) {
                templatized = true;
              }
              validateWorkflowVariables(workflow, pse, invalidWorkflows, pse.getWorkflowVariables());
              setPipelineVariables(workflow.getOrchestrationWorkflow().getUserVariables(), pse.getWorkflowVariables(),
                  pipelineVariables);
              if (!pipelineParameterized) {
                pipelineParameterized = checkPipelineEntityParameterized(pse.getWorkflowVariables(), workflow);
              }
            } catch (Exception ex) {
              logger.warn(format("Exception occurred while reading workflow associated to the pipeline %s",
                              pipeline.toString()),
                  ex);
            }
          }
        }
        if (!invalidStageWorkflows.isEmpty()) {
          pipelineStage.setValid(false);
          pipelineStage.setValidationMessage(
              format(PIPELINE_ENV_STATE_VALIDATION_MESSAGE, invalidStageWorkflows.toString()));
        }
        invalidWorkflows.addAll(invalidStageWorkflows);
      }
      if (!invalidWorkflows.isEmpty()) {
        pipeline.setValid(false);
        pipeline.setValidationMessage(format(PIPELINE_ENV_STATE_VALIDATION_MESSAGE, invalidWorkflows.toString()));
      }

      pipeline.setPipelineVariables(reorderPipelineVariables(pipelineVariables));
      pipeline.setHasSshInfraMapping(hasSshInfraMapping);
      pipeline.setEnvParameterized(pipelineParameterized);
      pipeline.setTemplatized(templatized);
    }
  }

  private List<Variable> reorderPipelineVariables(List<Variable> pipelineVariables) {
    // Reorder pipeline variables
    List<Variable> reorderedPipelineVariables = new ArrayList<>();
    List<Variable> nonEntityVariables =
        pipelineVariables.stream().filter(variable -> variable.getEntityType() == null).collect(toList());
    List<Variable> entityVariables =
        pipelineVariables.stream().filter(variable -> variable.getEntityType() != null).collect(toList());
    reorderedPipelineVariables.addAll(entityVariables);
    reorderedPipelineVariables.addAll(nonEntityVariables);
    return reorderedPipelineVariables;
  }

  private boolean checkPipelineEntityParameterized(Map<String, String> pseWorkflowVaraibles, Workflow workflow) {
    List<Variable> workflowVariables = workflow.getOrchestrationWorkflow().getUserVariables();
    if (isEmpty(workflowVariables) || isEmpty(pseWorkflowVaraibles)) {
      return false;
    }
    List<Variable> entityVariables =
        workflowVariables.stream().filter(variable -> variable.getEntityType() != null).collect(toList());
    for (Variable variable : entityVariables) {
      String value = pseWorkflowVaraibles.get(variable.getName());
      if (value != null) {
        return matchesVariablePattern(value);
      }
    }
    return false;
  }

  private void setServicesAndPipelineVariables(Pipeline pipeline) {
    List<Service> services = new ArrayList<>();
    List<String> serviceIds = new ArrayList<>();
    List<String> envIds = new ArrayList<>();
    List<Variable> pipelineVariables = new ArrayList<>();
    boolean templatized = false;
    boolean envParameterized = false;
    for (PipelineStage pipelineStage : pipeline.getPipelineStages()) {
      for (PipelineStageElement pse : pipelineStage.getPipelineStageElements()) {
        if (ENV_STATE.name().equals(pse.getType())) {
          if (pse.isDisable()) {
            continue;
          }
          Workflow workflow =
              workflowService.readWorkflow(pipeline.getAppId(), (String) pse.getProperties().get("workflowId"));
          Validator.notNullCheck("Workflow does not exist", workflow, USER);
          Validator.notNullCheck("Orchestration workflow does not exist", workflow.getOrchestrationWorkflow(), USER);
          resolveServicesOfWorkflow(services, serviceIds, pse.getWorkflowVariables(), workflow);
          setPipelineVariables(
              workflow.getOrchestrationWorkflow().getUserVariables(), pse.getWorkflowVariables(), pipelineVariables);
          if (!templatized && isNotEmpty(pse.getWorkflowVariables())) {
            templatized = true;
          }
          if (!envParameterized) {
            envParameterized = checkPipelineEntityParameterized(pse.getWorkflowVariables(), workflow);
          }
        }
      }
    }

    pipeline.setServices(services);
    pipeline.setPipelineVariables(reorderPipelineVariables(pipelineVariables));
    pipeline.setEnvIds(envIds);
    pipeline.setTemplatized(templatized);
    pipeline.setEnvParameterized(envParameterized);
  }

  private void setPipelineVariables(
      List<Variable> workflowVariables, Map<String, String> pseWorkflowVariables, List<Variable> pipelineVariables) {
    if (isEmpty(workflowVariables)) {
      return;
    }
    List<Variable> nonEntityVariables =
        workflowVariables.stream()
            .filter(variable -> (variable.getEntityType() == null) & !variable.isFixed())
            .collect(toList());

    if (isEmpty(pseWorkflowVariables)) {
      if (!isEmpty(workflowVariables)) {
        nonEntityVariables.forEach(variable -> pipelineVariables.add(variable.cloneInternal()));
      }
      return;
    }
    for (Variable variable : workflowVariables) {
      // Entity Variables
      String value = pseWorkflowVariables.get(variable.getName());
      if (variable.getEntityType() == null) {
        if (isEmpty(value) && !variable.isFixed()) {
          if (!contains(pipelineVariables, variable.getName())) {
            pipelineVariables.add(variable.cloneInternal());
          }
        }
      } else {
        if (isNotEmpty(value)) {
          String variableName = matchesVariablePattern(value) ? getName(value) : null;
          if (variableName != null) {
            if (!contains(pipelineVariables, variableName)) {
              // Variable has expression so prompt for the value at runtime
              Variable pipelineVariable = variable.cloneInternal();
              pipelineVariable.setValue(variable.getName());
              pipelineVariable.setName(variableName);
              pipelineVariables.add(pipelineVariable);
            }
          }
        }
      }
    }
  }

  private void resolveEnvIds(List<String> envIds, Map<String, String> pseWorkflowVariables, Workflow workflow) {
    String envId = workflowService.resolveEnvironmentId(workflow, pseWorkflowVariables);
    if (envId != null && !envIds.contains(envId)) {
      envIds.add(envId);
    }
  }

  private void resolveServicesOfWorkflow(
      List<Service> services, List<String> serviceIds, Map<String, String> pseWorkflowVariables, Workflow workflow) {
    if (isEmpty(pseWorkflowVariables)) {
      if (workflow.getServices() != null) {
        workflow.getServices().forEach((Service service) -> {
          if (!serviceIds.contains(service.getUuid())) {
            services.add(service);
            serviceIds.add(service.getUuid());
          }
        });
      }
    } else {
      List<Service> resolvedServices = workflowService.getResolvedServices(workflow, pseWorkflowVariables);
      if (resolvedServices != null) {
        for (Service resolvedService : resolvedServices) {
          if (!serviceIds.contains(resolvedService.getUuid())) {
            services.add(resolvedService);
            serviceIds.add(resolvedService.getUuid());
          }
        }
      }
    }
  }

  private void validateWorkflowVariables(Workflow workflow, PipelineStageElement pse, List<String> invalidWorkflows,
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

  @Override
  @ValidationGroups(Create.class)
  public Pipeline save(Pipeline pipeline) {
    ensurePipelineStageUuids(pipeline);

    List<Object> keywords = pipeline.generateKeywords();
    validatePipeline(pipeline, keywords);
    pipeline.setKeywords(trimList(keywords));
    Pipeline finalPipeline = wingsPersistence.saveAndGet(Pipeline.class, pipeline);
    wingsPersistence.saveAndGet(StateMachine.class, new StateMachine(finalPipeline, workflowService.stencilMap()));

    String accountId = appService.getAccountIdByAppId(pipeline.getAppId());
    yamlPushService.pushYamlChangeSet(accountId, null, finalPipeline, Type.CREATE, pipeline.isSyncFromGit(), false);

    return finalPipeline;
  }

  private void validatePipeline(Pipeline pipeline, List<Object> keywords) {
    if (Collections.isEmpty(pipeline.getPipelineStages())) {
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "At least one pipeline stage required");
    }
    List<Service> services = new ArrayList<>();
    List<String> serviceIds = new ArrayList<>();
    final List<PipelineStage> pipelineStages = pipeline.getPipelineStages();
    Set<String> parameterizedEnvIds = new HashSet<>();
    for (int i = 0; i < pipelineStages.size(); ++i) {
      PipelineStage pipelineStage = pipelineStages.get(i);
      if (isEmpty(pipelineStage.getPipelineStageElements())) {
        throw new WingsException(INVALID_ARGUMENT, USER).addParam("args", "Invalid pipeline stage");
      }
      for (PipelineStageElement stageElement : pipelineStage.getPipelineStageElements()) {
        if (!isValidPipelineStageName(stageElement.getName())) {
          throw new WingsException(INVALID_ARGUMENT, USER)
              .addParam("args", "Pipeline stage name can only have a-z, A-Z, 0-9, -, (, ) and _");
        }
        if (!ENV_STATE.name().equals(stageElement.getType())) {
          continue;
        }
        if (isNullOrEmpty((String) stageElement.getProperties().get("workflowId"))) {
          throw new WingsException(INVALID_ARGUMENT, USER)
              .addParam("args", "Workflow can not be null for Environment state");
        }
        Workflow workflow =
            workflowService.readWorkflow(pipeline.getAppId(), (String) stageElement.getProperties().get("workflowId"));
        if (workflow == null || workflow.getOrchestrationWorkflow() == null) {
          throw new WingsException(INVALID_ARGUMENT, USER)
              .addParam("args", "Workflow can not be null for Environment state");
        }
        keywords.add(workflow.getName());
        keywords.add(workflow.getDescription());
        resolveServicesOfWorkflow(services, serviceIds, stageElement.getWorkflowVariables(), workflow);
        if (workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType() != BUILD
            && isNullOrEmpty((String) stageElement.getProperties().get("envId"))) {
          throw new WingsException(INVALID_ARGUMENT, USER)
              .addParam("args", "Environment can not be null for non-build state");
        }

        String envId = workflowService.obtainTemplatedEnvironmentId(workflow, stageElement.getWorkflowVariables());
        if (envId != null && matchesVariablePattern(envId)) {
          parameterizedEnvIds.add(envId);
        }

        //        if (workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType() ==
        //        OrchestrationWorkflowType.BUILD
        //            && i > 0) {
        //          throw new WingsException(INVALID_ARGUMENT)
        //              .addParam("args",
        //                  "A pipeline can have only one build workflow and it has to be at the beginning of the
        //                  pipeline. "
        //                      + "If the pipeline needs more than one artifact use multiple steps in the build
        //                      workflow
        //                      "
        //                      + "to build and collect all required artifacts.");
        //        }
      }
    }
    if (parameterizedEnvIds.size() > 1) {
      throw new WingsException(INVALID_ARGUMENT, USER)
          .addParam("args", "A pipeline may only have one environment expression across all workflows");
    }
    keywords.addAll(services.stream().map(service -> service.getName()).distinct().collect(toList()));
  }

  private boolean isValidPipelineStageName(String name) {
    if (isEmpty(name)) {
      return false;
    }
    return ALLOWED_CHARS_SET_PIPELINE_STAGE.containsAll(Sets.newHashSet(Lists.charactersOf(name)));
  }

  private boolean prunePipeline(String appId, String pipelineId) {
    // First lets make sure that we have persisted a job that will prone the descendant objects
    PruneEntityJob.addDefaultJob(jobScheduler, Pipeline.class, appId, pipelineId, ofSeconds(5), ofSeconds(15));
    return wingsPersistence.delete(Pipeline.class, appId, pipelineId);
  }
}
