package software.wings.service.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.ListUtils.trimList;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.ErrorCode.PIPELINE_EXECUTION_IN_PROGRESS;
import static software.wings.beans.InfrastructureMappingType.AWS_SSH;
import static software.wings.beans.InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH;
import static software.wings.beans.PipelineExecution.PIPELINE_ID_KEY;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.yaml.Change.ChangeType.ADD;
import static software.wings.beans.yaml.Change.ChangeType.DELETE;
import static software.wings.beans.yaml.Change.ChangeType.MODIFY;
import static software.wings.common.Constants.PIPELINE_ENV_STATE_VALIDATION_MESSAGE;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.exception.WingsException.USER;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import de.danielbechler.util.Collections;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.EntityType;
import software.wings.beans.FailureStrategy;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.OrchestrationWorkflowType;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineExecution;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Service;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.HIterator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.scheduler.PruneEntityJob;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.ownership.OwnedByPipeline;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateMachine;
import software.wings.utils.validation.Create;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

  @Inject private AppService appService;
  @Inject private EntityUpdateService entityUpdateService;
  @Inject private ExecutorService executorService;
  @Inject private TriggerService triggerService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private YamlChangeSetService yamlChangeSetService;
  @Inject private YamlDirectoryService yamlDirectoryService;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

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

    if (!savedPipeline.getName().equals(pipeline.getName())) {
      executorService.submit(() -> triggerService.updateByApp(pipeline.getAppId()));
    }

    sendYamlUpdate(pipeline, MODIFY);

    return pipeline;
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
          for (PipelineStage pipelineStage : pipeline.getPipelineStages()) {
            if (pipelineStage.getPipelineStageElements() != null) {
              for (PipelineStageElement pipelineStageElement : pipelineStage.getPipelineStageElements()) {
                // Env Id in pipeline
                if (pipelineStageElement.getProperties() != null
                    && pipelineStageElement.getProperties().get("envId") != null
                    && pipelineStageElement.getProperties().get("envId").equals(envId)) {
                  referencedPipelines.add(pipeline.getName());
                  continue;
                }
                // Env Id in workflow variables
                if (pipelineStageElement.getWorkflowVariables() != null
                    && pipelineStageElement.getWorkflowVariables().values().contains(envId)) {
                  referencedPipelines.add(pipeline.getName());
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
    return deletePipeline(appId, pipelineId, false);
  }

  private boolean deletePipeline(String appId, String pipelineId, boolean forceDelete) {
    Pipeline pipeline = wingsPersistence.get(Pipeline.class, appId, pipelineId);
    if (pipeline == null) {
      return true;
    }

    if (!forceDelete) {
      ensurePipelineSafeToDelete(pipeline);
    }

    // YAML is identified by name that can be reused after deletion. Pruning yaml eventual consistent
    // may result in deleting object from a new application created after the first one was deleted,
    // or preexisting being renamed to the vacated name. This is why we have to do this synchronously.
    String accountId = appService.getAccountIdByAppId(pipeline.getAppId());
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      List<GitFileChange> changeSet = new ArrayList<>();
      changeSet.add(entityUpdateService.getPipelineGitSyncFile(accountId, pipeline, DELETE));
      yamlChangeSetService.saveChangeSet(ygs, changeSet);
    }

    return prunePipeline(appId, pipelineId);
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
        if (ENV_STATE.name().equals(pipelineStageElement.getType())) {
          Workflow workflow = workflowService.readWorkflow(
              pipeline.getAppId(), (String) pipelineStageElement.getProperties().get("workflowId"));
          OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
          if (orchestrationWorkflow != null) {
            if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(OrchestrationWorkflowType.BUILD)) {
              return new ArrayList<>();
            }
            if (orchestrationWorkflow.getRequiredEntityTypes() != null) {
              entityTypes.addAll(orchestrationWorkflow.getRequiredEntityTypes());
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
    return readPipeline(appId, pipelineId, withServices, false);
  }

  @Override
  public Pipeline readPipeline(String appId, String pipelineId, boolean withServices, boolean withEnvironments) {
    Pipeline pipeline = wingsPersistence.get(Pipeline.class, appId, pipelineId);
    notNullCheck("Pipeline was deleted", pipeline, USER);
    if (withServices) {
      resolveServicesAndEnvs(pipeline, withEnvironments);
    }
    return pipeline;
  }

  @Override
  public Pipeline getPipelineByName(String appId, String pipelineName) {
    return wingsPersistence.createQuery(Pipeline.class).filter("appId", appId).filter("name", pipelineName).get();
  }

  private void setPipelineDetails(List<Pipeline> pipelines) {
    for (Pipeline pipeline : pipelines) {
      boolean hasSshInfraMapping = false;
      List<String> invalidWorkflows = new ArrayList<>();
      List<PipelineStage> pipelineStages = pipeline.getPipelineStages();
      List<Variable> pipelineVariables = new ArrayList<>();
      for (PipelineStage pipelineStage : pipelineStages) {
        List<String> invalidStageWorkflows = new ArrayList<>();
        for (PipelineStageElement pipelineStageElement : pipelineStage.getPipelineStageElements()) {
          if (ENV_STATE.name().equals(pipelineStageElement.getType())) {
            try {
              Workflow workflow = workflowService.readWorkflow(
                  pipeline.getAppId(), (String) pipelineStageElement.getProperties().get("workflowId"));
              if (!hasSshInfraMapping) {
                List<InfrastructureMapping> infrastructureMappings =
                    workflowService.getResolvedInfraMappings(workflow, pipelineStageElement.getWorkflowVariables());
                if (isNotEmpty(infrastructureMappings)) {
                  hasSshInfraMapping =
                      infrastructureMappings.stream().anyMatch((InfrastructureMapping infra)
                                                                   -> AWS_SSH.name().equals(infra.getInfraMappingType())
                              || PHYSICAL_DATA_CENTER_SSH.name().equals(infra.getInfraMappingType()));
                }
              }
              if (isNotEmpty(pipelineStageElement.getWorkflowVariables())) {
                pipeline.setTemplatized(true);
              }
              validatePipelineEnvState(workflow, pipelineStageElement, invalidStageWorkflows, pipelineVariables);
            } catch (Exception ex) {
              logger.warn("Exception occurred while reading workflow associated to the pipeline {}", pipeline, ex);
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
      pipeline.setHasSshInfraMapping(hasSshInfraMapping);
      pipeline.setPipelineVariables(pipelineVariables);
    }
  }

  private void resolveServicesAndEnvs(Pipeline pipeline, boolean resolveEnv) {
    List<Service> services = new ArrayList<>();
    List<String> serviceIds = new ArrayList<>();
    List<String> envIds = new ArrayList<>();
    pipeline.getPipelineStages()
        .stream()
        .flatMap(pipelineStage -> pipelineStage.getPipelineStageElements().stream())
        .filter(pipelineStageElement -> ENV_STATE.name().equals(pipelineStageElement.getType()))
        .forEach(pse -> {
          try {
            Workflow workflow =
                workflowService.readWorkflow(pipeline.getAppId(), (String) pse.getProperties().get("workflowId"));
            resolveServicesOfWorkflow(services, serviceIds, pse, workflow);
            if (resolveEnv) {
              resolveEnvIds(envIds, pse, workflow);
            }
          } catch (Exception ex) {
            logger.warn("Exception occurred while reading workflow associated to the pipeline {}", pipeline);
          }
        });
    pipeline.setServices(services);
    pipeline.setEnvIds(envIds);
  }

  private void resolveEnvIds(List<String> envIds, PipelineStageElement pse, Workflow workflow) {
    String envId = workflowService.resolveEnvironmentId(workflow, pse.getWorkflowVariables());
    if (envId != null) {
      envIds.add(envId);
    }
  }

  private void resolveServicesOfWorkflow(
      List<Service> services, List<String> serviceIds, PipelineStageElement pse, Workflow workflow) {
    if (isEmpty(pse.getWorkflowVariables())) {
      if (workflow.getServices() != null) {
        workflow.getServices().forEach((Service service) -> {
          if (!serviceIds.contains(service.getUuid())) {
            services.add(service);
            serviceIds.add(service.getUuid());
          }
        });
      }
    } else {
      List<Service> resolvedServices = workflowService.getResolvedServices(workflow, pse.getWorkflowVariables());
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

  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE") // TODO
  private void validatePipelineEnvState(
      Workflow workflow, PipelineStageElement pse, List<String> invalidWorkflows, List<Variable> pipelineVariables) {
    Map<String, String> pseWorkflowVariables = pse.getWorkflowVariables();
    if (workflow == null || workflow.getOrchestrationWorkflow() == null) {
      return;
    }
    List<Variable> userVariables = workflow.getOrchestrationWorkflow().getUserVariables();
    if (isEmpty(userVariables)) {
      userVariables = new ArrayList<>();
    }

    List<Variable> nonEntityVariables =
        userVariables.stream()
            .filter(variable -> (variable.getEntityType() == null) & !variable.isFixed())
            .collect(toList());

    if (isEmpty(pseWorkflowVariables)) {
      if (!isEmpty(userVariables)) {
        pipelineVariables.addAll(nonEntityVariables);
      }
      return;
    }
    Set<String> pseWorkflowVariableNames = pseWorkflowVariables.keySet();
    Set<String> workflowVariableNames = (userVariables == null)
        ? new HashSet<>()
        : userVariables.stream().map(variable -> variable.getName()).collect(toSet());
    for (String pseWorkflowVariable : pseWorkflowVariableNames) {
      if (!workflowVariableNames.contains(pseWorkflowVariable)) {
        pse.setValid(false);
        pse.setValidationMessage("Workflow variables updated or deleted");
        invalidWorkflows.add(workflow.getName());
        break;
      }
    }
    for (Variable variable : nonEntityVariables) {
      if (pseWorkflowVariables.get(variable.getName()) == null) {
        pipelineVariables.add(variable);
      }
    }
  }

  @Override
  @ValidationGroups(Create.class)
  public Pipeline save(Pipeline pipeline) {
    ensurePipelineStageUuids(pipeline);

    List<Object> keywords = pipeline.generateKeywords();
    validatePipeline(pipeline, keywords);
    pipeline.setKeywords(trimList(keywords));
    pipeline = wingsPersistence.saveAndGet(Pipeline.class, pipeline);
    wingsPersistence.saveAndGet(StateMachine.class, new StateMachine(pipeline, workflowService.stencilMap()));

    Pipeline finalPipeline = pipeline;
    sendYamlUpdate(finalPipeline, ADD);

    return pipeline;
  }

  private void validatePipeline(Pipeline pipeline, List<Object> keywords) {
    if (Collections.isEmpty(pipeline.getPipelineStages())) {
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "At least one pipeline stage required");
    }
    List<Service> services = new ArrayList<>();
    List<String> serviceIds = new ArrayList<>();
    final List<PipelineStage> pipelineStages = pipeline.getPipelineStages();
    for (int i = 0; i < pipelineStages.size(); ++i) {
      PipelineStage pipelineStage = pipelineStages.get(i);
      if (isEmpty(pipelineStage.getPipelineStageElements())) {
        throw new WingsException(INVALID_ARGUMENT, USER).addParam("args", "Invalid pipeline stage");
      }
      for (PipelineStageElement stageElement : pipelineStage.getPipelineStageElements()) {
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
        resolveServicesOfWorkflow(services, serviceIds, stageElement, workflow);
        if (workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType() != OrchestrationWorkflowType.BUILD
            && isNullOrEmpty((String) stageElement.getProperties().get("envId"))) {
          throw new WingsException(INVALID_ARGUMENT, USER)
              .addParam("args", "Environment can not be null for non-build state");
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
    keywords.addAll(services.stream().map(service -> service.getName()).distinct().collect(toList()));
  }

  private void sendYamlUpdate(Pipeline pipeline, ChangeType changeType) {
    executorService.submit(() -> {
      String accountId = appService.getAccountIdByAppId(pipeline.getAppId());
      YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
      if (ygs != null) {
        List<GitFileChange> changeSet = new ArrayList<>();
        changeSet.add(entityUpdateService.getPipelineGitSyncFile(accountId, pipeline, changeType));

        yamlChangeSetService.saveChangeSet(ygs, changeSet);
      }
    });
  }

  private boolean prunePipeline(String appId, String pipelineId) {
    // First lets make sure that we have persisted a job that will prone the descendant objects
    PruneEntityJob.addDefaultJob(jobScheduler, Pipeline.class, appId, pipelineId, Duration.ofSeconds(5));
    return wingsPersistence.delete(Pipeline.class, appId, pipelineId);
  }
}
