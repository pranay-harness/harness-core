package software.wings.service.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.ErrorCode.PIPELINE_EXECUTION_IN_PROGRESS;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;
import static software.wings.exception.WingsException.HARMLESS;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import de.danielbechler.util.Collections;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.EntityType;
import software.wings.beans.FailureStrategy;
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
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.exception.WingsException.ReportTarget;
import software.wings.scheduler.PruneEntityJob;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.ownership.OwnedByPipeline;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.sm.StateMachine;
import software.wings.sm.StateTypeScope;
import software.wings.stencils.Stencil;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
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
  @Inject private ServiceResourceService serviceResourceService;
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
    if (res == null || res.getResponse() == null) {
      return res;
    }
    List<Pipeline> pipelines = res.getResponse();
    for (Pipeline pipeline : pipelines) {
      List<String> invalidWorkflows = new ArrayList<>();
      List<PipelineStage> pipelineStages = pipeline.getPipelineStages();
      for (PipelineStage pipelineStage : pipelineStages) {
        List<String> invalidStageWorkflows = new ArrayList<>();
        for (PipelineStageElement pipelineStageElement : pipelineStage.getPipelineStageElements()) {
          if (ENV_STATE.name().equals(pipelineStageElement.getType())) {
            try {
              if (isNotEmpty(pipelineStageElement.getWorkflowVariables())) {
                pipeline.setTemplatized(true);
                Workflow workflow = workflowService.readWorkflow(
                    pipeline.getAppId(), (String) pipelineStageElement.getProperties().get("workflowId"));
                validatePipelineEnvState(workflow, pipelineStageElement, invalidStageWorkflows);
              }
            } catch (Exception ex) {
              logger.warn("Exception occurred while reading workflow associated to the pipeline {}", pipeline);
            }
          }
        }
        if (!invalidStageWorkflows.isEmpty()) {
          pipelineStage.setValid(false);
          pipelineStage.setValidationMessage(
              String.format(Constants.PIPELINE_ENV_STATE_VALIDATION_MESSAGE, invalidStageWorkflows.toString()));
        }
        invalidWorkflows.addAll(invalidStageWorkflows);
      }
      if (!invalidWorkflows.isEmpty()) {
        pipeline.setValid(false);
        pipeline.setValidationMessage(
            String.format(Constants.PIPELINE_ENV_STATE_VALIDATION_MESSAGE, invalidWorkflows.toString()));
      }
    }

    if (previousExecutionsCount != null && previousExecutionsCount > 0) {
      for (Pipeline pipeline : pipelines) {
        try {
          PageRequest<WorkflowExecution> workflowExecutionPageRequest =
              aPageRequest()
                  .withLimit(previousExecutionsCount.toString())
                  .addFilter("workflowId", EQ, pipeline.getUuid())
                  .build();

          pipeline.setWorkflowExecutions(
              workflowExecutionService.listExecutions(workflowExecutionPageRequest, false, false, false, false)
                  .getResponse());
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
  public Pipeline updatePipeline(Pipeline pipeline) {
    Pipeline savedPipeline = wingsPersistence.get(Pipeline.class, pipeline.getAppId(), pipeline.getUuid());
    notNullCheck("Pipeline", savedPipeline);

    validatePipeline(pipeline);
    UpdateOperations<Pipeline> ops = wingsPersistence.createUpdateOperations(Pipeline.class);
    setUnset(ops, "description", pipeline.getDescription());
    setUnset(ops, "name", pipeline.getName());
    setUnset(ops, "pipelineStages", pipeline.getPipelineStages());
    setUnset(ops, "failureStrategies", pipeline.getFailureStrategies());

    wingsPersistence.update(wingsPersistence.createQuery(Pipeline.class)
                                .field("appId")
                                .equal(pipeline.getAppId())
                                .field(ID_KEY)
                                .equal(pipeline.getUuid()),
        ops);

    wingsPersistence.saveAndGet(StateMachine.class, new StateMachine(pipeline, workflowService.stencilMap()));

    if (!savedPipeline.getName().equals(pipeline.getName())) {
      executorService.submit(() -> triggerService.updateByApp(pipeline.getAppId()));
    }

    executorService.submit(() -> {
      String accountId = appService.getAccountIdByAppId(pipeline.getAppId());
      YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
      if (ygs != null) {
        List<GitFileChange> changeSet = new ArrayList<>();
        changeSet.add(entityUpdateService.getPipelineGitSyncFile(accountId, pipeline, ChangeType.MODIFY));

        yamlChangeSetService.saveChangeSet(ygs, changeSet);
      }
    });

    return pipeline;
  }

  @Override
  public List<FailureStrategy> updateFailureStrategies(
      String appId, String pipelineId, List<FailureStrategy> failureStrategies) {
    Pipeline savedPipeline = wingsPersistence.get(Pipeline.class, appId, pipelineId);
    notNullCheck("pipeline", savedPipeline);

    savedPipeline.setFailureStrategies(failureStrategies);
    Pipeline pipeline = updatePipeline(savedPipeline);
    return pipeline.getFailureStrategies();
  }

  // TODO: Add unit tests for this function
  private void ensurePipelineSafeToDelete(Pipeline pipeline) {
    PageRequest<PipelineExecution> pageRequest =
        aPageRequest()
            .addFilter(PipelineExecution.APP_ID_KEY, EQ, pipeline.getAppId())
            .addFilter(PipelineExecution.PIPELINE_ID_KEY, EQ, pipeline.getUuid())
            .build();
    PageResponse<PipelineExecution> pageResponse = wingsPersistence.query(PipelineExecution.class, pageRequest);
    if (pageResponse == null || isEmpty(pageResponse.getResponse())
        || pageResponse.getResponse().stream().allMatch(
               pipelineExecution -> pipelineExecution.getStatus().isFinalStatus())) {
      List<Trigger> triggers = triggerService.getTriggersHasPipelineAction(pipeline.getAppId(), pipeline.getUuid());
      if (isEmpty(triggers)) {
        return;
      }
      List<String> triggerNames = triggers.stream().map(Trigger::getName).collect(Collectors.toList());

      throw new WingsException(INVALID_REQUEST, ReportTarget.USER)
          .addParam("message",
              String.format(
                  "Pipeline associated as a trigger action to triggers [%s]", Joiner.on(", ").join(triggerNames)));
    }
    throw new WingsException(PIPELINE_EXECUTION_IN_PROGRESS, HARMLESS)
        .addParam("message", String.format("Pipeline:[%s] couldn't be deleted", pipeline.getName()));
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
      changeSet.add(entityUpdateService.getPipelineGitSyncFile(accountId, pipeline, ChangeType.DELETE));
      yamlChangeSetService.saveChangeSet(ygs, changeSet);
    }

    return prunePipeline(appId, pipelineId);
  }

  private boolean prunePipeline(String appId, String pipelineId) {
    // First lets make sure that we have persisted a job that will prone the descendant objects
    PruneEntityJob.addDefaultJob(jobScheduler, Pipeline.class, appId, pipelineId, Duration.ofSeconds(5));

    return wingsPersistence.delete(Pipeline.class, appId, pipelineId);
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
        wingsPersistence.createQuery(Pipeline.class).field(Pipeline.APP_ID_KEY).equal(appId).asKeyList();
    for (Key key : pipelineKeys) {
      prunePipeline(appId, (String) key.getId());
    }
  }

  @Override
  public Pipeline clonePipeline(String originalPipelineId, Pipeline pipeline) {
    Pipeline originalPipeline = readPipeline(pipeline.getAppId(), originalPipelineId, false);
    Pipeline clonedPipeline = originalPipeline.clone();
    clonedPipeline.setName(pipeline.getName());
    clonedPipeline.setDescription(pipeline.getDescription());
    return createPipeline(clonedPipeline);
  }

  @Override
  public List<EntityType> getRequiredEntities(String appId, String pipelineId) {
    Pipeline pipeline = wingsPersistence.get(Pipeline.class, appId, pipelineId);
    notNullCheck("pipeline", pipeline);
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
          }
          if (orchestrationWorkflow.getRequiredEntityTypes() != null) {
            entityTypes.addAll(orchestrationWorkflow.getRequiredEntityTypes());
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
    notNullCheck("pipeline", pipeline);
    if (withServices) {
      populateAssociatedWorkflowServices(pipeline);
    }
    return pipeline;
  }

  @Override
  public Pipeline getPipelineByName(String appId, String pipelineName) {
    return wingsPersistence.createQuery(Pipeline.class)
        .field("appId")
        .equal(appId)
        .field("name")
        .equal(pipelineName)
        .get();
  }

  private void populateAssociatedWorkflowServices(Pipeline pipeline) {
    List<Service> services = new ArrayList<>();
    List<String> serviceIds = new ArrayList<>();
    pipeline.getPipelineStages()
        .stream()
        .flatMap(pipelineStage -> pipelineStage.getPipelineStageElements().stream())
        .filter(pipelineStageElement -> ENV_STATE.name().equals(pipelineStageElement.getType()))
        .forEach(pse -> {
          try {
            Workflow workflow =
                workflowService.readWorkflow(pipeline.getAppId(), (String) pse.getProperties().get("workflowId"));
            if (isEmpty(pse.getWorkflowVariables())) {
              workflow.getServices().forEach(service -> {
                if (!serviceIds.contains(service.getUuid())) {
                  services.add(service);
                  serviceIds.add(service.getUuid());
                }
              });
            } else {
              List<Service> resolvedServices = resolveTemplateServices(workflow, pse.getWorkflowVariables());
              if (resolvedServices != null) {
                for (Service resolvedService : resolvedServices) {
                  if (!serviceIds.contains(resolvedService.getUuid())) {
                    services.add(resolvedService);
                    serviceIds.add(resolvedService.getUuid());
                  }
                }
              }
            }

          } catch (Exception ex) {
            logger.warn("Exception occurred while reading workflow associated to the pipeline {}", pipeline);
          }
        });
    pipeline.setServices(services);
  }

  private List<Service> resolveTemplateServices(Workflow workflow, Map<String, String> workflowVariables) {
    // Lookup service
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow == null) {
      return new ArrayList<>();
    }
    List<String> serviceIds = new ArrayList<>();
    List<Variable> userVariables = orchestrationWorkflow.getUserVariables();
    if (userVariables != null) {
      List<String> serviceNames =
          userVariables.stream()
              .filter(variable -> variable.getEntityType() != null && variable.getEntityType().equals(SERVICE))
              .map(Variable::getName)
              .distinct()
              .collect(toList());
      if (serviceNames != null) {
        for (String serviceName : serviceNames) {
          if (workflowVariables.get(serviceName) != null) {
            serviceIds.add(workflowVariables.get(serviceName));
          }
        }
      }
    }
    List<String> workflowServiceIds = orchestrationWorkflow.getServiceIds();
    List<String> templatizedServiceIds = orchestrationWorkflow.getTemplatizedServiceIds();
    if (workflowServiceIds != null) {
      for (String workflowServiceId : workflowServiceIds) {
        if (!templatizedServiceIds.contains(workflowServiceId)) {
          serviceIds.add(workflowServiceId);
        }
      }
    }
    return getServices(workflow, serviceIds.stream().distinct().collect(toList()));
  }

  private void validatePipelineEnvState(
      Workflow workflow, PipelineStageElement pipelineStageElement, List<String> invalidWorkflows) {
    Map<String, String> pseWorkflowVariables = pipelineStageElement.getWorkflowVariables();
    if (isEmpty(pseWorkflowVariables)) {
      return;
    }
    Set<String> pseWorkflowVariableNames = pseWorkflowVariables.keySet();
    List<Variable> userVariables = workflow.getOrchestrationWorkflow().getUserVariables();
    Set<String> workflowVariableNames = userVariables == null
        ? new HashSet<>()
        : userVariables.stream().map(variable -> variable.getName()).collect(toSet());
    for (String pseWorkflowVariable : pseWorkflowVariableNames) {
      if (!workflowVariableNames.contains(pseWorkflowVariable)) {
        pipelineStageElement.setValid(false);
        pipelineStageElement.setValidationMessage("Workflow variables updated or deleted");
        invalidWorkflows.add(workflow.getName());
        break;
      }
    }
  }

  private List<Service> getServices(Workflow workflow, List<String> serviceIds) {
    if (isNotEmpty(serviceIds)) {
      PageRequest<Service> pageRequest = aPageRequest()
                                             .withLimit(UNLIMITED)
                                             .addFilter("appId", EQ, workflow.getAppId())
                                             .addFilter("uuid", IN, serviceIds.toArray())
                                             .build();
      return serviceResourceService.list(pageRequest, false, false);
    }
    return new ArrayList<>();
  }

  @Override
  public Pipeline createPipeline(Pipeline pipeline) {
    validatePipeline(pipeline);
    pipeline = wingsPersistence.saveAndGet(Pipeline.class, pipeline);
    Map<StateTypeScope, List<Stencil>> stencils = workflowService.stencils(null, null, null);
    wingsPersistence.saveAndGet(StateMachine.class, new StateMachine(pipeline, workflowService.stencilMap()));

    Pipeline finalPipeline = pipeline;
    executorService.submit(() -> {
      String accountId = appService.getAccountIdByAppId(finalPipeline.getAppId());
      YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
      if (ygs != null) {
        List<GitFileChange> changeSet = new ArrayList<>();
        changeSet.add(entityUpdateService.getPipelineGitSyncFile(accountId, finalPipeline, ChangeType.ADD));
        yamlChangeSetService.saveChangeSet(ygs, changeSet);
      }
    });

    return pipeline;
  }

  private void validatePipeline(Pipeline pipeline) {
    if (Collections.isEmpty(pipeline.getPipelineStages())) {
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "At least one pipeline stage required");
    }
    for (PipelineStage pipelineStage : pipeline.getPipelineStages()) {
      if (isEmpty(pipelineStage.getPipelineStageElements())) {
        throw new WingsException(INVALID_ARGUMENT).addParam("args", "Invalid pipeline stage");
      }

      for (PipelineStageElement stageElement : pipelineStage.getPipelineStageElements()) {
        if (!ENV_STATE.name().equals(stageElement.getType())) {
          continue;
        }
        if (isNullOrEmpty((String) stageElement.getProperties().get("workflowId"))) {
          throw new WingsException(INVALID_ARGUMENT).addParam("args", "Workflow can not be null for Environment state");
        }
        Workflow workflow =
            workflowService.readWorkflow(pipeline.getAppId(), (String) stageElement.getProperties().get("workflowId"));
        if (workflow == null || workflow.getOrchestrationWorkflow() == null) {
          throw new WingsException(INVALID_ARGUMENT).addParam("args", "Workflow can not be null for Environment state");
        }
        if (workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType() != OrchestrationWorkflowType.BUILD
            && isNullOrEmpty((String) stageElement.getProperties().get("envId"))) {
          throw new WingsException(INVALID_ARGUMENT)
              .addParam("args", "Environment can not be null for non-build state");
        }
      }
    }
  }
}
