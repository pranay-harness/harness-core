/**
 *
 */

package software.wings.service.impl;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.api.WorkflowElement.WorkflowElementBuilder.aWorkflowElement;
import static software.wings.beans.ElementExecutionSummary.ElementExecutionSummaryBuilder.anElementExecutionSummary;
import static software.wings.beans.EmbeddedUser.Builder.anEmbeddedUser;
import static software.wings.beans.EntityType.*;
import static software.wings.beans.EntityType.ARTIFACT;
import static software.wings.beans.EntityType.ORCHESTRATED_DEPLOYMENT;
import static software.wings.beans.EntityType.SIMPLE_DEPLOYMENT;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.sm.InfraMappingSummary.Builder.anInfraMappingSummary;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;

import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.CanaryWorkflowStandardParams;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseStepExecutionData;
import software.wings.api.ServiceElement;
import software.wings.api.ServiceTemplateElement;
import software.wings.api.SimpleWorkflowParam;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.CanaryWorkflowExecutionAdvisor;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Graph.Node;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.OrchestrationWorkflowType;
import software.wings.beans.Pipeline;
import software.wings.beans.RequiredExecutionArgs;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.TemplateExpression;
import software.wings.beans.User;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.WorkflowType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.ServiceCommand;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsDeque;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.lock.PersistentLocker;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionEventAdvisor;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.ExecutionInterruptManager;
import software.wings.sm.ExecutionInterruptType;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InfraMappingSummary;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.PhaseExecutionSummary;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.PipelineSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachine;
import software.wings.sm.StateMachineExecutionCallback;
import software.wings.sm.StateMachineExecutionSimulator;
import software.wings.sm.StateMachineExecutor;
import software.wings.sm.StateType;
import software.wings.sm.StepExecutionSummary;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.ElementStateExecutionData;
import software.wings.utils.MapperUtils;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * The Class WorkflowExecutionServiceImpl.
 *
 * @author Rishi
 */
@Singleton
@ValidateOnExecution
public class WorkflowExecutionServiceImpl implements WorkflowExecutionService {
  private static final String COMMAND_NAME_PREF = "Command: ";
  private static final String WORKFLOW_NAME_PREF = "Workflow: ";
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private MainConfiguration mainConfiguration;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private StateMachineExecutor stateMachineExecutor;
  @Inject private EnvironmentService environmentService;
  @Inject private ExecutionInterruptManager executionInterruptManager;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ServiceInstanceService serviceInstanceService;
  @Inject private ArtifactService artifactService;
  @Inject private StateMachineExecutionSimulator stateMachineExecutionSimulator;
  @Inject private GraphRenderer graphRenderer;
  @Inject private AppService appService;
  @Inject private WorkflowService workflowService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private PersistentLocker persistentLocker;

  /**
   * {@inheritDoc}
   */
  @Override
  public void trigger(String appId, String stateMachineId, String executionUuid, String executionName) {
    trigger(appId, stateMachineId, executionUuid, executionName, null);
  }

  /**
   * Trigger.
   *
   * @param appId          the app id
   * @param stateMachineId the state machine id
   * @param executionUuid  the execution uuid
   * @param executionName  the execution name
   * @param callback       the callback
   */
  void trigger(String appId, String stateMachineId, String executionUuid, String executionName,
      StateMachineExecutionCallback callback) {
    stateMachineExecutor.execute(appId, stateMachineId, executionUuid, executionName, null, callback);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<WorkflowExecution> listExecutions(
      PageRequest<WorkflowExecution> pageRequest, boolean includeGraph) {
    return listExecutions(pageRequest, includeGraph, false, true, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<WorkflowExecution> listExecutions(PageRequest<WorkflowExecution> pageRequest,
      boolean includeGraph, boolean runningOnly, boolean withBreakdownAndSummary, boolean includeStatus) {
    PageResponse<WorkflowExecution> res = wingsPersistence.query(WorkflowExecution.class, pageRequest);
    if (res == null || res.size() == 0) {
      return res;
    }
    if (withBreakdownAndSummary) {
      res.forEach(this ::refreshBreakdown);

      res.forEach(this ::refreshSummaries);
    }

    for (WorkflowExecution workflowExecution : res) {
      if (!runningOnly || workflowExecution.isRunningStatus() || workflowExecution.isPausedStatus()) {
        // populateGraph(workflowExecution, null, null, null, false);
        populateNodeHierarchy(workflowExecution, includeGraph, includeStatus);
      }
    }
    return res;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowExecution getExecutionDetails(String appId, String workflowExecutionId) {
    return getExecutionDetails(appId, workflowExecutionId, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowExecution getExecutionDetails(
      String appId, String workflowExecutionId, List<String> expandedGroupIds) {
    WorkflowExecution workflowExecution = getExecutionDetailsWithoutGraph(appId, workflowExecutionId);

    if (expandedGroupIds == null) {
      expandedGroupIds = new ArrayList<>();
    }
    if (workflowExecution != null) {
      populateNodeHierarchyWithGraph(workflowExecution);
    }
    workflowExecution.setExpandedGroupIds(expandedGroupIds);
    return workflowExecution;
  }

  @Override
  public WorkflowExecution getExecutionDetailsWithoutGraph(String appId, String workflowExecutionId) {
    logger.debug("Retrieving workflow execution details for id {} of App Id {} ", workflowExecutionId, appId);
    WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, appId, workflowExecutionId);

    if (workflowExecution.getExecutionArgs() != null) {
      if (workflowExecution.getExecutionArgs().getServiceInstanceIdNames() != null) {
        PageRequest<ServiceInstance> pageRequest =
            aPageRequest()
                .addFilter("appId", EQ, appId)
                .addFilter("uuid", Operator.IN,
                    workflowExecution.getExecutionArgs().getServiceInstanceIdNames().keySet().toArray())
                .build();
        workflowExecution.getExecutionArgs().setServiceInstances(
            serviceInstanceService.list(pageRequest).getResponse());
      }
      if (workflowExecution.getExecutionArgs().getArtifactIdNames() != null) {
        PageRequest<Artifact> pageRequest =
            aPageRequest()
                .addFilter("appId", EQ, appId)
                .addFilter(
                    "uuid", Operator.IN, workflowExecution.getExecutionArgs().getArtifactIdNames().keySet().toArray())
                .build();
        workflowExecution.getExecutionArgs().setArtifacts(artifactService.list(pageRequest, false).getResponse());
      }
    }
    refreshBreakdown(workflowExecution);
    refreshSummaries(workflowExecution);
    return workflowExecution;
  }

  private void populateNodeHierarchy(WorkflowExecution workflowExecution, boolean includeGraph, boolean includeStatus) {
    if (includeStatus || includeGraph) {
      List<StateExecutionInstance> allInstances = queryAllInstances(workflowExecution);
      if (allInstances == null || allInstances.isEmpty()) {
        return;
      }
      Map<String, StateExecutionInstance> allInstancesIdMap =
          allInstances.stream().collect(toMap(StateExecutionInstance::getUuid, identity()));

      if (allInstances.stream().anyMatch(
              i -> i.getStatus() == ExecutionStatus.PAUSED || i.getStatus() == ExecutionStatus.PAUSING)) {
        workflowExecution.setStatus(ExecutionStatus.PAUSED);
      } else if (allInstances.stream().anyMatch(i -> i.getStatus() == ExecutionStatus.WAITING)) {
        workflowExecution.setStatus(ExecutionStatus.WAITING);
      } else {
        List<ExecutionInterrupt> executionInterrupts = executionInterruptManager.checkForExecutionInterrupt(
            workflowExecution.getAppId(), workflowExecution.getUuid());
        if (executionInterrupts != null
            && executionInterrupts.stream().anyMatch(
                   e -> e.getExecutionInterruptType() == ExecutionInterruptType.PAUSE_ALL)) {
          workflowExecution.setStatus(ExecutionStatus.PAUSING);
        }
      }
      if (includeGraph) {
        StateMachine sm = wingsPersistence.get(
            StateMachine.class, workflowExecution.getAppId(), workflowExecution.getStateMachineId());
        workflowExecution.setExecutionNode(
            graphRenderer.generateHierarchyNode(allInstancesIdMap, sm.getInitialStateName(), null, true, true));
      }
    }
  }

  private void populateNodeHierarchyWithGraph(WorkflowExecution workflowExecution) {
    populateNodeHierarchy(workflowExecution, true, false);
  }

  private List<StateExecutionInstance> queryAllInstances(WorkflowExecution workflowExecution) {
    logger.debug("Querying all state execution instance for Workflow execution {} ", workflowExecution.getUuid());
    PageRequest<StateExecutionInstance> req = aPageRequest()
                                                  .withLimit(PageRequest.UNLIMITED)
                                                  .addFilter("appId", EQ, workflowExecution.getAppId())
                                                  .addFilter("executionUuid", EQ, workflowExecution.getUuid())
                                                  .addFieldsExcluded("contextElements", "callback")
                                                  .build();

    return wingsPersistence.query(StateExecutionInstance.class, req);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowExecution triggerPipelineExecution(String appId, String pipelineId, ExecutionArgs executionArgs) {
    return triggerPipelineExecution(appId, pipelineId, executionArgs, null);
  }

  /**
   * Trigger pipeline execution workflow execution.
   *
   * @param appId                   the app id
   * @param pipelineId              the pipeline id
   * @param executionArgs           the execution args
   * @param workflowExecutionUpdate the workflow execution update  @return the workflow execution
   * @return the workflow execution
   */
  public WorkflowExecution triggerPipelineExecution(
      String appId, String pipelineId, ExecutionArgs executionArgs, WorkflowExecutionUpdate workflowExecutionUpdate) {
    Pipeline pipeline = wingsPersistence.get(Pipeline.class, appId, pipelineId);
    if (pipeline == null) {
      throw new WingsException(ErrorCode.NON_EXISTING_PIPELINE);
    }
    List<WorkflowExecution> runningWorkflowExecutions =
        getRunningWorkflowExecutions(WorkflowType.PIPELINE, appId, pipelineId);
    if (runningWorkflowExecutions != null) {
      for (WorkflowExecution workflowExecution : runningWorkflowExecutions) {
        if (workflowExecution.getStatus() == ExecutionStatus.NEW) {
          throw new WingsException(ErrorCode.PIPELINE_ALREADY_TRIGGERED, "pilelineName", pipeline.getName());
        }
        if (workflowExecution.getStatus() == ExecutionStatus.RUNNING) {
          // Analyze if pipeline is in initial stage
        }
      }
    }

    StateMachine stateMachine = workflowService.readLatestStateMachine(appId, pipelineId);
    if (stateMachine == null) {
      throw new WingsException("No stateMachine associated with " + pipelineId);
    }
    WorkflowExecution workflowExecution = new WorkflowExecution();
    workflowExecution.setAppId(appId);
    workflowExecution.setWorkflowId(pipelineId);
    workflowExecution.setWorkflowType(WorkflowType.PIPELINE);
    workflowExecution.setStateMachineId(stateMachine.getUuid());

    workflowExecution.setPipelineSummary(PipelineSummary.Builder.aPipelineSummary()
                                             .withPipelineId(pipelineId)
                                             .withPipelineName(pipeline.getName())
                                             .build());

    WorkflowStandardParams stdParams = new WorkflowStandardParams();
    stdParams.setAppId(appId);
    if (executionArgs.getArtifacts() != null && !executionArgs.getArtifacts().isEmpty()) {
      stdParams.setArtifactIds(
          executionArgs.getArtifacts().stream().map(Artifact::getUuid).collect(Collectors.toList()));
    }
    User user = UserThreadLocal.get();
    if (user != null) {
      stdParams.setCurrentUser(
          anEmbeddedUser().withUuid(user.getUuid()).withEmail(user.getEmail()).withName(user.getName()).build());
    }
    workflowExecution.setExecutionArgs(executionArgs);

    return triggerExecution(workflowExecution, stateMachine, workflowExecutionUpdate, stdParams);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowExecution triggerOrchestrationExecution(
      String appId, String envId, String workflowId, ExecutionArgs executionArgs) {
    return triggerOrchestrationWorkflowExecution(appId, envId, workflowId, executionArgs, null);
  }

  /**
   * Trigger orchestration execution workflow execution.
   *
   * @param appId                   the app id
   * @param envId                   the env id
   * @param workflowId              the orchestration id
   * @param executionArgs           the execution args
   * @param workflowExecutionUpdate the workflow execution update
   * @return the workflow execution
   */
  public WorkflowExecution triggerOrchestrationWorkflowExecution(String appId, String envId, String workflowId,
      ExecutionArgs executionArgs, WorkflowExecutionUpdate workflowExecutionUpdate) {
    boolean lockAcquired = false;
    try {
      lockAcquired = persistentLocker.acquireLock(Workflow.class, workflowId);
      if (!lockAcquired) {
        logger.error("Persistent lock could not be acquired to trigger workflow: {}", workflowId);
        return null;
      }

      List<WorkflowExecution> runningWorkflowExecutions =
          getRunningWorkflowExecutions(WorkflowType.ORCHESTRATION, appId, workflowId);
      if (runningWorkflowExecutions != null && runningWorkflowExecutions.size() > 0) {
        throw new WingsException(ErrorCode.WORKFLOW_ALREADY_TRIGGERED);
      }
      // TODO - validate list of artifact Ids if it's matching for all the services involved in this orchestration

      Workflow workflow = workflowService.readWorkflow(appId, workflowId);

      if (!workflow.getOrchestrationWorkflow().isValid()) {
        throw new WingsException(
            ErrorCode.INVALID_REQUEST, "message", "Workflow requested for execution is not valid/complete.");
      }
      StateMachine stateMachine = workflowService.readStateMachine(appId, workflowId, workflow.getDefaultVersion());
      if (stateMachine == null) {
        throw new WingsException("No stateMachine associated with " + workflowId);
      }

      WorkflowExecution workflowExecution = new WorkflowExecution();
      workflowExecution.setAppId(appId);
      workflowExecution.setEnvId(envId);
      workflowExecution.setWorkflowId(workflowId);
      workflowExecution.setName(WORKFLOW_NAME_PREF + workflow.getName());
      workflowExecution.setWorkflowType(WorkflowType.ORCHESTRATION);
      workflowExecution.setStateMachineId(stateMachine.getUuid());
      workflowExecution.setExecutionArgs(executionArgs);

      WorkflowStandardParams stdParams;
      if (workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType() == OrchestrationWorkflowType.CANARY
          || workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType() == OrchestrationWorkflowType.BASIC
          || workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType()
              == OrchestrationWorkflowType.MULTI_SERVICE) {
        stdParams = new CanaryWorkflowStandardParams();

        if (workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow) {
          CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
              (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
          if (canaryOrchestrationWorkflow.getUserVariables() != null) {
            stdParams.setWorkflowElement(
                aWorkflowElement()
                    .withVariables(getWorkflowVariables(canaryOrchestrationWorkflow, executionArgs))
                    .build());
          }
        }
      } else {
        stdParams = new WorkflowStandardParams();
      }

      stdParams.setAppId(appId);
      stdParams.setEnvId(envId);
      if (executionArgs.getArtifacts() != null && !executionArgs.getArtifacts().isEmpty()) {
        stdParams.setArtifactIds(
            executionArgs.getArtifacts().stream().map(Artifact::getUuid).collect(Collectors.toList()));
      }
      stdParams.setExecutionCredential(executionArgs.getExecutionCredential());

      return triggerExecution(
          workflowExecution, stateMachine, new CanaryWorkflowExecutionAdvisor(), workflowExecutionUpdate, stdParams);
    } finally {
      if (lockAcquired) {
        persistentLocker.releaseLock(Workflow.class, workflowId);
      }
    }
  }

  private Map<String, Object> getWorkflowVariables(
      CanaryOrchestrationWorkflow orchestrationWorkflow, ExecutionArgs executionArgs) {
    Map<String, Object> variables = new HashMap<>();
    if (orchestrationWorkflow.getUserVariables() == null) {
      return variables;
    }
    for (Variable variable : orchestrationWorkflow.getUserVariables()) {
      if (variable.isFixed()) {
        variables.put(variable.getName(), variable.getValue());
        continue;
      }

      // no input from user
      if (executionArgs == null || executionArgs.getWorkflowVariables() == null
          || executionArgs.getWorkflowVariables().isEmpty()
          || StringUtils.isBlank(executionArgs.getWorkflowVariables().get(variable.getName()))) {
        if (variable.isMandatory() && variable.getValue() == null) {
          throw new WingsException(ErrorCode.INVALID_REQUEST, "message",
              "Workflow variable " + variable.getName() + " is mandatory for execution");
        }
        variables.put(variable.getName(), variable.getValue());
        continue;
      }
      variables.put(variable.getName(), executionArgs.getWorkflowVariables().get(variable.getName()));
    }
    return variables;
  }

  private WorkflowExecution triggerExecution(WorkflowExecution workflowExecution, StateMachine stateMachine,
      WorkflowExecutionUpdate workflowExecutionUpdate, WorkflowStandardParams stdParams,
      ContextElement... contextElements) {
    return triggerExecution(workflowExecution, stateMachine, null, workflowExecutionUpdate, stdParams, contextElements);
  }

  private WorkflowExecution triggerExecution(WorkflowExecution workflowExecution, StateMachine stateMachine,
      ExecutionEventAdvisor workflowExecutionAdvisor, WorkflowExecutionUpdate workflowExecutionUpdate,
      WorkflowStandardParams stdParams, ContextElement... contextElements) {
    Application app = appService.get(workflowExecution.getAppId());
    workflowExecution.setAppName(app.getName());
    if (workflowExecution.getEnvId() != null) {
      Environment env = environmentService.get(workflowExecution.getAppId(), workflowExecution.getEnvId(), false);
      workflowExecution.setEnvName(env.getName());
      workflowExecution.setEnvType(env.getEnvironmentType());
    }
    User user = UserThreadLocal.get();
    if (user != null) {
      workflowExecution.setTriggeredBy(
          anEmbeddedUser().withUuid(user.getUuid()).withEmail(user.getEmail()).withName(user.getName()).build());
    } else if (workflowExecution.getExecutionArgs() != null
        && workflowExecution.getExecutionArgs().getTriggeredBy() != null) {
      workflowExecution.setTriggeredBy(workflowExecution.getExecutionArgs().getTriggeredBy());
    } else {
      // Triggered by Auto Trigger
      workflowExecution.setTriggeredBy(anEmbeddedUser().withName("Deployment trigger").build());
    }
    ExecutionArgs executionArgs = workflowExecution.getExecutionArgs();
    if (executionArgs != null) {
      if (executionArgs.getServiceInstances() != null) {
        List<String> serviceInstanceIds =
            executionArgs.getServiceInstances().stream().map(ServiceInstance::getUuid).collect(Collectors.toList());
        PageRequest<ServiceInstance> pageRequest = aPageRequest()
                                                       .addFilter("appId", EQ, workflowExecution.getAppId())
                                                       .addFilter("uuid", Operator.IN, serviceInstanceIds.toArray())
                                                       .build();
        List<ServiceInstance> serviceInstances = serviceInstanceService.list(pageRequest).getResponse();

        if (serviceInstances == null || serviceInstances.size() != serviceInstanceIds.size()) {
          logger.error("Service instances argument and valid service instance retrieved size not matching");
          throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Invalid service instances");
        }
        executionArgs.setServiceInstanceIdNames(
            serviceInstances.stream().collect(Collectors.toMap(ServiceInstance::getUuid,
                serviceInstance -> serviceInstance.getHostName() + ":" + serviceInstance.getServiceName())));
      }

      if (executionArgs.getArtifacts() != null && !executionArgs.getArtifacts().isEmpty()) {
        List<String> artifactIds =
            executionArgs.getArtifacts().stream().map(Artifact::getUuid).collect(Collectors.toList());
        PageRequest<Artifact> pageRequest = aPageRequest()
                                                .addFilter("appId", EQ, workflowExecution.getAppId())
                                                .addFilter("uuid", Operator.IN, artifactIds.toArray())
                                                .build();
        List<Artifact> artifacts = artifactService.list(pageRequest, false).getResponse();

        if (artifacts == null || artifacts.size() != artifactIds.size()) {
          logger.error("Artifact argument and valid artifact retrieved size not matching");
          throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Invalid artifact");
        }

        // TODO: get rid of artifactIdNames when UI moves to artifact list
        executionArgs.setArtifactIdNames(
            artifacts.stream().collect(Collectors.toMap(Artifact::getUuid, Artifact::getDisplayName)));
        artifacts.forEach(artifact -> {
          artifact.setArtifactFiles(null);
          artifact.setCreatedBy(null);
          artifact.setLastUpdatedBy(null);
        });
        executionArgs.setArtifacts(artifacts);
        List<ServiceElement> services = new ArrayList<>();
        artifacts.forEach(artifact -> {
          artifact.getServiceIds().forEach(serviceId -> {
            Service service = serviceResourceService.get(artifact.getAppId(), serviceId);
            ServiceElement se = new ServiceElement();
            MapperUtils.mapObject(service, se);
            services.add(se);
          });
        });
        stdParams.setServices(services);
      }
      workflowExecution.setErrorStrategy(executionArgs.getErrorStrategy());
    }
    if (executionArgs.isTriggeredFromPipeline()) {
      if (executionArgs.getPipelineId() != null) {
        Pipeline pipeline =
            wingsPersistence.get(Pipeline.class, workflowExecution.getAppId(), executionArgs.getPipelineId());
        workflowExecution.setPipelineSummary(PipelineSummary.Builder.aPipelineSummary()
                                                 .withPipelineId(pipeline.getUuid())
                                                 .withPipelineName(pipeline.getName())
                                                 .build());
      }
    }

    String workflowExecutionId = wingsPersistence.save(workflowExecution);
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(workflowExecution.getAppId());
    stateExecutionInstance.setExecutionName(workflowExecution.getName());
    stateExecutionInstance.setExecutionUuid(workflowExecutionId);
    stateExecutionInstance.setExecutionType(workflowExecution.getWorkflowType());

    if (workflowExecutionUpdate == null) {
      workflowExecutionUpdate = new WorkflowExecutionUpdate();
    }
    workflowExecutionUpdate.setAppId(workflowExecution.getAppId());
    workflowExecutionUpdate.setWorkflowExecutionId(workflowExecutionId);
    workflowExecutionUpdate.setNeedToNotifyPipeline(executionArgs.isTriggeredFromPipeline());

    stateExecutionInstance.setCallback(workflowExecutionUpdate);
    if (workflowExecutionAdvisor != null) {
      stateExecutionInstance.setExecutionEventAdvisors(newArrayList(workflowExecutionAdvisor));
    }

    stdParams.setErrorStrategy(workflowExecution.getErrorStrategy());
    String workflowUrl = mainConfiguration.getPortal().getUrl() + "/"
        + String.format(mainConfiguration.getPortal().getExecutionUrlPattern(), workflowExecution.getAppId(),
              workflowExecution.getEnvId(), workflowExecution.getUuid());
    if (stdParams.getWorkflowElement() == null) {
      stdParams.setWorkflowElement(aWorkflowElement()
                                       .withUuid(workflowExecutionId)
                                       .withName(workflowExecution.getName())
                                       .withUrl(workflowUrl)
                                       .build());
    } else {
      stdParams.getWorkflowElement().setName(workflowExecution.getName());
      stdParams.getWorkflowElement().setUuid(workflowExecution.getUuid());
      stdParams.getWorkflowElement().setUrl(workflowUrl);
    }

    WingsDeque<ContextElement> elements = new WingsDeque<>();
    elements.push(stdParams);
    if (contextElements != null) {
      for (ContextElement contextElement : contextElements) {
        elements.push(contextElement);
      }
    }
    stateExecutionInstance.setContextElements(elements);
    stateMachineExecutor.execute(stateMachine, stateExecutionInstance);

    // TODO: findAndModify
    Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                         .field("appId")
                                         .equal(workflowExecution.getAppId())
                                         .field(ID_KEY)
                                         .equal(workflowExecutionId)
                                         .field("status")
                                         .equal(ExecutionStatus.NEW);
    UpdateOperations<WorkflowExecution> updateOps = wingsPersistence.createUpdateOperations(WorkflowExecution.class)
                                                        .set("status", ExecutionStatus.RUNNING)
                                                        .set("startTs", System.currentTimeMillis());

    wingsPersistence.update(query, updateOps);

    workflowExecution =
        wingsPersistence.get(WorkflowExecution.class, workflowExecution.getAppId(), workflowExecutionId);
    notifyWorkflowExecution(workflowExecution);
    return workflowExecution;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowExecution triggerEnvExecution(String appId, String envId, ExecutionArgs executionArgs) {
    return triggerEnvExecution(appId, envId, executionArgs, null);
  }

  @Override
  public void incrementInProgressCount(String appId, String workflowExecutionId, int inc) {
    UpdateOperations<WorkflowExecution> ops = wingsPersistence.createUpdateOperations(WorkflowExecution.class);
    ops.inc("breakdown.inprogress", inc);
    wingsPersistence.update(wingsPersistence.createQuery(WorkflowExecution.class)
                                .field("appId")
                                .equal(appId)
                                .field(ID_KEY)
                                .equal(workflowExecutionId),
        ops);
  }

  @Override
  public void incrementSuccess(String appId, String workflowExecutionId, int inc) {
    UpdateOperations<WorkflowExecution> ops = wingsPersistence.createUpdateOperations(WorkflowExecution.class);
    ops.inc("breakdown.success", inc);
    ops.inc("breakdown.inprogress", -1 * inc);
    wingsPersistence.update(wingsPersistence.createQuery(WorkflowExecution.class)
                                .field("appId")
                                .equal(appId)
                                .field(ID_KEY)
                                .equal(workflowExecutionId),
        ops);
  }

  @Override
  public void incrementFailed(String appId, String workflowExecutionId, Integer inc) {
    UpdateOperations<WorkflowExecution> ops = wingsPersistence.createUpdateOperations(WorkflowExecution.class);
    ops.inc("breakdown.failed", inc);
    ops.inc("breakdown.inprogress", -1 * inc);
    wingsPersistence.update(wingsPersistence.createQuery(WorkflowExecution.class)
                                .field("appId")
                                .equal(appId)
                                .field(ID_KEY)
                                .equal(workflowExecutionId),
        ops);
  }

  /**
   * Trigger env execution workflow execution.
   *
   * @param appId                   the app id
   * @param envId                   the env id
   * @param executionArgs           the execution args
   * @param workflowExecutionUpdate the workflow execution update
   * @return the workflow execution
   */
  WorkflowExecution triggerEnvExecution(
      String appId, String envId, ExecutionArgs executionArgs, WorkflowExecutionUpdate workflowExecutionUpdate) {
    if (executionArgs.getWorkflowType() == WorkflowType.ORCHESTRATION) {
      logger.debug("Received an orchestrated execution request");
      if (executionArgs.getOrchestrationId() == null) {
        logger.error("workflowId is null for an orchestrated execution");
        throw new WingsException(
            ErrorCode.INVALID_REQUEST, "message", "workflowId is null for an orchestrated execution");
      }
      return triggerOrchestrationExecution(appId, envId, executionArgs.getOrchestrationId(), executionArgs);
    } else if (executionArgs.getWorkflowType() == WorkflowType.SIMPLE) {
      logger.debug("Received an simple execution request");
      if (executionArgs.getServiceId() == null) {
        logger.error("serviceId is null for a simple execution");
        throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "serviceId is null for a simple execution");
      }
      if (executionArgs.getServiceInstances() == null || executionArgs.getServiceInstances().size() == 0) {
        logger.error("serviceInstances are empty for a simple execution");
        throw new WingsException(
            ErrorCode.INVALID_REQUEST, "message", "serviceInstances are empty for a simple execution");
      }

      return triggerSimpleExecution(appId, envId, executionArgs, workflowExecutionUpdate);

    } else {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args", "workflowType");
    }
  }

  /**
   * Trigger simple execution workflow execution.
   *
   * @param appId         the app id
   * @param envId         the env id
   * @param executionArgs the execution args
   * @return the workflow execution
   */
  private WorkflowExecution triggerSimpleExecution(
      String appId, String envId, ExecutionArgs executionArgs, WorkflowExecutionUpdate workflowExecutionUpdate) {
    Workflow workflow = workflowService.readLatestSimpleWorkflow(appId, envId);
    String workflowId = workflow.getUuid();

    StateMachine stateMachine = workflowService.readLatestStateMachine(appId, workflowId);
    if (stateMachine == null) {
      throw new WingsException("No stateMachine associated with " + workflowId);
    }

    WorkflowExecution workflowExecution = new WorkflowExecution();
    workflowExecution.setAppId(appId);
    workflowExecution.setEnvId(envId);
    workflowExecution.setWorkflowType(WorkflowType.SIMPLE);
    workflowExecution.setStateMachineId(stateMachine.getUuid());
    workflowExecution.setTotal(executionArgs.getServiceInstances().size());
    Service service = serviceResourceService.get(appId, executionArgs.getServiceId());
    workflowExecution.setName(COMMAND_NAME_PREF + service.getName() + "/" + executionArgs.getCommandName());
    workflowExecution.setWorkflowId(workflow.getUuid());
    workflowExecution.setExecutionArgs(executionArgs);

    WorkflowStandardParams stdParams = new WorkflowStandardParams();
    stdParams.setAppId(appId);
    stdParams.setEnvId(envId);
    if (executionArgs.getArtifacts() != null && !executionArgs.getArtifacts().isEmpty()) {
      stdParams.setArtifactIds(
          executionArgs.getArtifacts().stream().map(Artifact::getUuid).collect(Collectors.toList()));
    }
    stdParams.setExecutionCredential(executionArgs.getExecutionCredential());

    SimpleWorkflowParam simpleOrchestrationParams = new SimpleWorkflowParam();
    simpleOrchestrationParams.setServiceId(executionArgs.getServiceId());
    if (executionArgs.getServiceInstances() != null) {
      simpleOrchestrationParams.setInstanceIds(
          executionArgs.getServiceInstances().stream().map(ServiceInstance::getUuid).collect(Collectors.toList()));
    }
    simpleOrchestrationParams.setExecutionStrategy(executionArgs.getExecutionStrategy());
    simpleOrchestrationParams.setCommandName(executionArgs.getCommandName());
    return triggerExecution(
        workflowExecution, stateMachine, workflowExecutionUpdate, stdParams, simpleOrchestrationParams);
  }

  private List<WorkflowExecution> getRunningWorkflowExecutions(
      WorkflowType workflowType, String appId, String workflowId) {
    PageRequest<WorkflowExecution> pageRequest =
        aPageRequest()
            .addFilter("appId", EQ, appId)
            .addFilter("workflowId", EQ, workflowId)
            .addFilter("workflowType", EQ, workflowType)
            .addFilter("status", Operator.IN, ExecutionStatus.NEW, ExecutionStatus.RUNNING)
            .build();

    PageResponse<WorkflowExecution> pageResponse = wingsPersistence.query(WorkflowExecution.class, pageRequest);
    if (pageResponse == null) {
      return null;
    }
    return pageResponse.getResponse();
  }

  @Override
  public ExecutionInterrupt triggerExecutionInterrupt(ExecutionInterrupt executionInterrupt) {
    String executionUuid = executionInterrupt.getExecutionUuid();
    WorkflowExecution workflowExecution =
        wingsPersistence.get(WorkflowExecution.class, executionInterrupt.getAppId(), executionUuid);
    if (workflowExecution == null) {
      throw new WingsException(
          ErrorCode.INVALID_ARGUMENT, "args", "No WorkflowExecution for executionUuid:" + executionUuid);
    }

    return executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
  }

  @Override
  public RequiredExecutionArgs getRequiredExecutionArgs(String appId, String envId, ExecutionArgs executionArgs) {
    Validator.notNullCheck("workflowType", executionArgs.getWorkflowType());

    if (executionArgs.getWorkflowType() == WorkflowType.ORCHESTRATION
        || executionArgs.getWorkflowType() == WorkflowType.ORCHESTRATION) {
      logger.debug("Received an orchestrated execution request");
      Validator.notNullCheck("orchestrationId", executionArgs.getOrchestrationId());

      Workflow workflow = workflowService.readWorkflow(appId, executionArgs.getOrchestrationId());
      if (workflow == null || workflow.getOrchestrationWorkflow() == null) {
        throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "OrchestrationWorkflow not found");
      }

      StateMachine stateMachine =
          workflowService.readStateMachine(appId, executionArgs.getOrchestrationId(), workflow.getDefaultVersion());
      if (stateMachine == null) {
        throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Associated state machine not found");
      }

      RequiredExecutionArgs requiredExecutionArgs = new RequiredExecutionArgs();
      requiredExecutionArgs.setEntityTypes(workflow.getOrchestrationWorkflow().getRequiredEntityTypes());
      return requiredExecutionArgs;

    } else if (executionArgs.getWorkflowType() == WorkflowType.SIMPLE) {
      logger.debug("Received an simple execution request");
      if (executionArgs.getServiceId() == null) {
        logger.error("serviceId is null for a simple execution");
        throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "serviceId is null for a simple execution");
      }
      if (executionArgs.getServiceInstances() == null || executionArgs.getServiceInstances().size() == 0) {
        logger.error("serviceInstances are empty for a simple execution");
        throw new WingsException(
            ErrorCode.INVALID_REQUEST, "message", "serviceInstances are empty for a simple execution");
      }
      RequiredExecutionArgs requiredExecutionArgs = new RequiredExecutionArgs();
      if (StringUtils.isNotBlank(executionArgs.getCommandName())) {
        ServiceCommand command = serviceResourceService.getCommandByName(
            appId, executionArgs.getServiceId(), envId, executionArgs.getCommandName());
        if (command.getCommand().isArtifactNeeded()) {
          requiredExecutionArgs.getEntityTypes().add(ARTIFACT);
        }
      }
      List<String> serviceInstanceIds =
          executionArgs.getServiceInstances().stream().map(ServiceInstance::getUuid).collect(Collectors.toList());
      Set<EntityType> infraReqEntityTypes =
          stateMachineExecutionSimulator.getInfrastructureRequiredEntityType(appId, serviceInstanceIds);
      if (infraReqEntityTypes != null) {
        requiredExecutionArgs.getEntityTypes().addAll(infraReqEntityTypes);
      }
      return requiredExecutionArgs;
    }

    return null;
  }

  @Override
  public boolean workflowExecutionsRunning(WorkflowType workflowType, String appId, String workflowId) {
    PageRequest<WorkflowExecution> pageRequest =
        aPageRequest()
            .addFilter("appId", EQ, appId)
            .addFilter("workflowId", EQ, workflowId)
            .addFilter("workflowType", EQ, workflowType)
            .addFilter("status", Operator.IN, ExecutionStatus.NEW, ExecutionStatus.RUNNING)
            .addFieldsIncluded("uuid")
            .build();

    PageResponse<WorkflowExecution> pageResponse = wingsPersistence.query(WorkflowExecution.class, pageRequest);
    if (pageResponse == null || pageResponse.size() == 0) {
      return false;
    }
    return true;
  }

  private void notifyWorkflowExecution(WorkflowExecution workflowExecution) {
    EntityType entityType = ORCHESTRATED_DEPLOYMENT;
    if (workflowExecution.getWorkflowType() == WorkflowType.SIMPLE) {
      entityType = SIMPLE_DEPLOYMENT;
    }
    //
    //    History history =
    //    History.Builder.aHistory().withAppId(workflowExecution.getAppId()).withEventType(EventType.CREATED).withEntityType(entityType)
    //        .withEntityId(workflowExecution.getUuid()).withEntityName(workflowExecution.getName()).withEntityNewValue(workflowExecution)
    //        .withShortDescription(workflowExecution.getName() + " started").withTitle(workflowExecution.getName() + "
    //        started").build();
    //    historyService.createAsync(history);
  }

  @Override
  public CountsByStatuses getBreakdown(String appId, String workflowExecutionId) {
    WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, appId, workflowExecutionId);
    refreshBreakdown(workflowExecution);
    return workflowExecution.getBreakdown();
  }

  @Override
  public Node getExecutionDetailsForNode(String appId, String workflowExecutionId, String stateExecutionInstanceId) {
    StateExecutionInstance stateExecutionInstance =
        wingsPersistence.get(StateExecutionInstance.class, appId, stateExecutionInstanceId);
    return graphRenderer.convertToNode(stateExecutionInstance);
  }

  @Override
  public StateExecutionInstance getStateExecutionData(String appId, String stateExecutionInstanceId) {
    return wingsPersistence.get(StateExecutionInstance.class, appId, stateExecutionInstanceId);
  }

  @Override
  public void deleteByWorkflow(String appId, String workflowId) {
    wingsPersistence.createQuery(WorkflowExecution.class)
        .field("appId")
        .equal(appId)
        .field("workflowId")
        .equal(workflowId)
        .asList()
        .forEach(workflowExecution -> {
          wingsPersistence.delete(workflowExecution);
          wingsPersistence.createQuery(StateExecutionInstance.class)
              .field("appId")
              .equal(appId)
              .field("stateMachineId")
              .equal(workflowExecution.getStateMachineId())
              .forEach(stateExecutionInstance -> {
                wingsPersistence.delete(stateExecutionInstance);
                wingsPersistence.delete(wingsPersistence.createQuery(ExecutionInterrupt.class)
                                            .field("appId")
                                            .equal(appId)
                                            .field("stateExecutionInstanceId")
                                            .equal(stateExecutionInstance.getUuid()));
              });
        });
  }

  private void refreshSummaries(WorkflowExecution workflowExecution) {
    if (workflowExecution.getServiceExecutionSummaries() != null) {
      return;
    }

    List<ElementExecutionSummary> serviceExecutionSummaries = new ArrayList<>();
    // TODO : version should also be captured as part of the WorkflowExecution
    Workflow workflow = workflowService.readWorkflow(workflowExecution.getAppId(), workflowExecution.getWorkflowId());
    if (workflow != null && workflow.getOrchestrationWorkflow() != null) {
      List<Service> services;
      if (workflow.isTemplatized()) {
        services = resolveServices(workflow, workflow.getOrchestrationWorkflow(), workflowExecution);
      } else {
        services = workflow.getServices();
      }
      if (workflow.getWorkflowType() == WorkflowType.SIMPLE) {
        services = asList(serviceResourceService.get(
            workflow.getAppId(), workflowExecution.getExecutionArgs().getServiceId(), false));
      }
      if (services != null) {
        services.forEach(service -> {
          ServiceElement serviceElement =
              ServiceElement.Builder.aServiceElement().withUuid(service.getUuid()).withName(service.getName()).build();
          ElementExecutionSummary elementSummary =
              anElementExecutionSummary().withContextElement(serviceElement).withStatus(ExecutionStatus.QUEUED).build();
          serviceExecutionSummaries.add(elementSummary);
          List<InfrastructureMapping> infraMappings =
              infrastructureMappingService
                  .list(aPageRequest()
                            .addFilter("appId", EQ, workflow.getAppId())
                            .addFilter("envId", EQ, workflowExecution.getEnvId())
                            .addFilter("serviceId", EQ, service.getUuid())
                            .build())
                  .getResponse();
          List<InfraMappingSummary> infraMappingSummaries = new ArrayList<>();
          for (InfrastructureMapping infraMapping : infraMappings) {
            infraMappingSummaries.add(anInfraMappingSummary()
                                          .withInframappingId(infraMapping.getUuid())
                                          .withInfraMappingType(infraMapping.getInfraMappingType())
                                          .withComputerProviderName(infraMapping.getComputeProviderName())
                                          .withDisplayName(infraMapping.getDisplayName())
                                          .withDeploymentType(infraMapping.getDeploymentType())
                                          .withComputerProviderType(infraMapping.getComputeProviderType())
                                          .build());
          }
          elementSummary.setInfraMappingSummary(infraMappingSummaries);
        });
      }
    }
    Map<String, ElementExecutionSummary> serviceExecutionSummaryMap = serviceExecutionSummaries.stream().collect(
        Collectors.toMap(summary -> summary.getContextElement().getUuid(), Function.identity()));

    populateServiceSummary(serviceExecutionSummaryMap, workflowExecution);

    if (!serviceExecutionSummaryMap.isEmpty()) {
      Collections.sort(serviceExecutionSummaries, ElementExecutionSummary.startTsComparator);
      workflowExecution.setServiceExecutionSummaries(serviceExecutionSummaries);

      if (workflowExecution.getStatus() == ExecutionStatus.SUCCESS
          || workflowExecution.getStatus() == ExecutionStatus.FAILED
          || workflowExecution.getStatus() == ExecutionStatus.ERROR
          || workflowExecution.getStatus() == ExecutionStatus.ABORTED) {
        wingsPersistence.updateField(WorkflowExecution.class, workflowExecution.getUuid(), "serviceExecutionSummaries",
            workflowExecution.getServiceExecutionSummaries());
      }
    }
  }

  private List<Service> resolveServices(
      Workflow workflow, OrchestrationWorkflow orchestrationWorkflow, WorkflowExecution workflowExecution) {
    // Lookup service
    List<String> workflowServiceIds = orchestrationWorkflow.getServiceIds();
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
    List<Variable> userVariables = canaryOrchestrationWorkflow.getUserVariables();
    List<String> serviceNames = new ArrayList<>();
    if (userVariables != null) {
      serviceNames =
          userVariables.stream()
              .filter(variable -> variable.getEntityType() != null && variable.getEntityType().equals(SERVICE))
              .map(Variable::getName)
              .collect(Collectors.toList());
    }
    List<String> serviceIds = new ArrayList<>();
    Map<String, String> workflowVariables = workflowExecution.getExecutionArgs() != null
        ? workflowExecution.getExecutionArgs().getWorkflowVariables()
        : null;
    if (workflowVariables != null) {
      Set<String> workflowVariableNames = workflowVariables.keySet();
      for (String variableName : workflowVariableNames) {
        if (serviceNames.contains(variableName)) {
          serviceIds.add(workflowVariables.get(variableName));
        }
      }
    }
    List<String> templatizedServiceIds = new ArrayList<>();
    List<WorkflowPhase> workflowPhases = canaryOrchestrationWorkflow.getWorkflowPhases();
    if (workflowPhases != null) {
      for (WorkflowPhase workflowPhase : workflowPhases) {
        List<TemplateExpression> templateExpressions = workflowPhase.getTemplateExpressions();
        if (templateExpressions != null) {
          if (templateExpressions.stream().anyMatch(
                  templateExpression -> templateExpression.getFieldName().equals("serviceId"))) {
            templatizedServiceIds.add(workflowPhase.getServiceId());
          }
        }
      }
    }
    if (workflowServiceIds != null) {
      for (String workflowServiceId : workflowServiceIds) {
        if (!templatizedServiceIds.contains(workflowServiceId)) {
          serviceIds.add(workflowServiceId);
        }
      }
    }

    PageRequest<Service> pageRequest = aPageRequest()
                                           .withLimit(PageRequest.UNLIMITED)
                                           .addFilter("appId", EQ, workflow.getAppId())
                                           .addFilter("uuid", IN, serviceIds.toArray())
                                           .build();
    return serviceResourceService.list(pageRequest, false, false);
  }

  private void populateServiceSummary(
      Map<String, ElementExecutionSummary> serviceSummaryMap, WorkflowExecution workflowExecution) {
    PageRequest<StateExecutionInstance> pageRequest =
        aPageRequest()
            .addFilter("appId", EQ, workflowExecution.getAppId())
            .addFilter("executionUuid", EQ, workflowExecution.getUuid())
            .addFilter("stateType", Operator.IN, StateType.REPEAT.name(), StateType.FORK.name(),
                StateType.SUB_WORKFLOW.name(), StateType.PHASE.name(), StateType.PHASE_STEP.name())
            .addFilter("parentInstanceId", Operator.NOT_EXISTS, null)
            .addOrder("createdAt", OrderType.ASC)
            .build();

    PageResponse<StateExecutionInstance> pageResponse =
        wingsPersistence.query(StateExecutionInstance.class, pageRequest);

    if (pageResponse == null || pageResponse.isEmpty()) {
      return;
    }

    for (StateExecutionInstance stateExecutionInstance : pageResponse.getResponse()) {
      if (!(stateExecutionInstance.getStateExecutionData() instanceof ElementStateExecutionData)) {
        continue;
      }
      if (stateExecutionInstance.isRollback()) {
        continue;
      }

      ElementStateExecutionData elementStateExecutionData =
          (ElementStateExecutionData) stateExecutionInstance.getStateExecutionData();
      if (elementStateExecutionData.getElementStatusSummary() == null
          || elementStateExecutionData.getElementStatusSummary().isEmpty()) {
        continue;
      }
      for (ElementExecutionSummary summary : elementStateExecutionData.getElementStatusSummary()) {
        ServiceElement serviceElement = getServiceElement(summary.getContextElement());
        if (serviceElement == null) {
          continue;
        }
        ElementExecutionSummary serviceSummary = serviceSummaryMap.get(serviceElement.getUuid());
        if (serviceSummary == null) {
          serviceSummary =
              anElementExecutionSummary().withContextElement(serviceElement).withStatus(ExecutionStatus.QUEUED).build();
          serviceSummaryMap.put(serviceElement.getUuid(), serviceSummary);
        }
        if (serviceSummary.getStartTs() == null
            || (summary.getStartTs() != null && serviceSummary.getStartTs() > summary.getStartTs())) {
          serviceSummary.setStartTs(summary.getStartTs());
        }
        if (serviceSummary.getEndTs() == null
            || (summary.getEndTs() != null && serviceSummary.getEndTs() < summary.getEndTs())) {
          serviceSummary.setEndTs(summary.getEndTs());
        }
        if (serviceSummary.getInstanceStatusSummaries() == null) {
          serviceSummary.setInstanceStatusSummaries(new ArrayList<>());
        }
        if (summary.getInstanceStatusSummaries() != null) {
          serviceSummary.getInstanceStatusSummaries().addAll(summary.getInstanceStatusSummaries());
        }
        serviceSummary.setStatus(summary.getStatus());
      }
    }
  }

  private ServiceElement getServiceElement(ContextElement contextElement) {
    if (contextElement == null) {
      return null;
    }
    switch (contextElement.getElementType()) {
      case SERVICE: {
        return (ServiceElement) contextElement;
      }
      case SERVICE_TEMPLATE: {
        return ((ServiceTemplateElement) contextElement).getServiceElement();
      }
      case INSTANCE: {
        return ((InstanceElement) contextElement).getServiceTemplateElement().getServiceElement();
      }
      case PARAM: {
        if (Constants.PHASE_PARAM.equals(contextElement.getName())) {
          return ((PhaseElement) contextElement).getServiceElement();
        }
        break;
      }
      default: {}
    }
    return null;
  }

  private void refreshBreakdown(WorkflowExecution workflowExecution) {
    if ((workflowExecution.getStatus() == ExecutionStatus.SUCCESS
            || workflowExecution.getStatus() == ExecutionStatus.FAILED
            || workflowExecution.getStatus() == ExecutionStatus.ERROR
            || workflowExecution.getStatus() == ExecutionStatus.ABORTED)
        && workflowExecution.getBreakdown() != null) {
      return;
    }

    StateMachine sm = wingsPersistence.get(StateMachine.class, workflowExecution.getStateMachineId());
    PageRequest<StateExecutionInstance> req =
        aPageRequest()
            .withLimit(PageRequest.UNLIMITED)
            .addFilter("appId", EQ, workflowExecution.getAppId())
            .addFilter("executionUuid", EQ, workflowExecution.getUuid())
            .addFieldsIncluded("uuid", "stateName", "contextElement", "parentInstanceId", "status")
            .build();
    PageResponse<StateExecutionInstance> res = wingsPersistence.query(StateExecutionInstance.class, req);
    CountsByStatuses breakdown = stateMachineExecutionSimulator.getStatusBreakdown(
        workflowExecution.getAppId(), workflowExecution.getEnvId(), sm, res.getResponse());
    int total = breakdown.getFailed() + breakdown.getSuccess() + breakdown.getInprogress() + breakdown.getQueued();

    workflowExecution.setBreakdown(breakdown);
    workflowExecution.setTotal(total);
    logger.info("Got the breakdown workflowExecution: {}, status: {}, breakdown: {}", workflowExecution.getUuid(),
        workflowExecution.getStatus(), breakdown);

    if (workflowExecution.getStatus() == ExecutionStatus.SUCCESS
        || workflowExecution.getStatus() == ExecutionStatus.FAILED
        || workflowExecution.getStatus() == ExecutionStatus.ERROR
        || workflowExecution.getStatus() == ExecutionStatus.ABORTED) {
      logger.info("Set the breakdown of the completed workflowExecution: {}, status: {}, breakdown: {}",
          workflowExecution.getUuid(), workflowExecution.getStatus(), breakdown);

      Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                           .field("appId")
                                           .equal(workflowExecution.getAppId())
                                           .field(ID_KEY)
                                           .equal(workflowExecution.getUuid());

      UpdateOperations<WorkflowExecution> updateOps = wingsPersistence.createUpdateOperations(WorkflowExecution.class);

      try {
        updateOps.set("breakdown", breakdown).set("total", total);
        UpdateResults updated = wingsPersistence.update(query, updateOps);
        logger.info("Updated : {} row", updated.getWriteResult().getN());
      } catch (java.lang.Exception e) {
        logger.error("Error in breakdown retrieval", e);
      }
    }
  }

  @Override
  public List<ElementExecutionSummary> getElementsSummary(
      String appId, String executionUuid, String parentStateExecutionInstanceId) {
    PageRequest<StateExecutionInstance> pageRequest =
        aPageRequest()
            .withLimit(PageRequest.UNLIMITED)
            .addFilter("appId", EQ, appId)
            .addFilter("executionUuid", EQ, executionUuid)
            .addFilter("parentInstanceId", Operator.IN, parentStateExecutionInstanceId)
            .addOrder("createdAt", OrderType.ASC)
            .build();

    PageResponse<StateExecutionInstance> pageResponse =
        wingsPersistence.query(StateExecutionInstance.class, pageRequest);
    if (pageResponse == null || pageResponse.isEmpty()) {
      return null;
    }

    List<StateExecutionInstance> contextTransitionInstances = pageResponse.getResponse()
                                                                  .stream()
                                                                  .filter(instance -> instance.isContextTransition())
                                                                  .collect(Collectors.toList());
    Map<String, StateExecutionInstance> prevInstanceIdMap =
        pageResponse.getResponse()
            .stream()
            .filter(instance -> instance.getPrevInstanceId() != null)
            .collect(Collectors.toMap(instance -> instance.getPrevInstanceId(), Function.identity()));

    List<ElementExecutionSummary> elementExecutionSummaries = new ArrayList<>();
    for (StateExecutionInstance stateExecutionInstance : contextTransitionInstances) {
      ContextElement contextElement = stateExecutionInstance.getContextElement();
      ElementExecutionSummary elementExecutionSummary = anElementExecutionSummary()
                                                            .withContextElement(contextElement)
                                                            .withStartTs(stateExecutionInstance.getStartTs())
                                                            .build();

      List<InstanceStatusSummary> instanceStatusSummaries = new ArrayList<>();

      StateExecutionInstance last = stateExecutionInstance;
      for (StateExecutionInstance next = stateExecutionInstance; next != null;
           next = prevInstanceIdMap.get(next.getUuid())) {
        StateType nextStateType = StateType.valueOf(next.getStateType());
        if (nextStateType == null) {
          continue;
        }
        if ((nextStateType == StateType.REPEAT || nextStateType == StateType.FORK || nextStateType == StateType.PHASE
                || nextStateType == StateType.PHASE_STEP || nextStateType == StateType.SUB_WORKFLOW)
            && next.getStateExecutionData() instanceof ElementStateExecutionData) {
          ElementStateExecutionData elementStateExecutionData =
              (ElementStateExecutionData) next.getStateExecutionData();
          instanceStatusSummaries.addAll(elementStateExecutionData.getElementStatusSummary()
                                             .stream()
                                             .filter(e -> e.getInstanceStatusSummaries() != null)
                                             .flatMap(l -> l.getInstanceStatusSummaries().stream())
                                             .collect(Collectors.toList()));
        } else if ((nextStateType == StateType.ECS_SERVICE_DEPLOY
                       || nextStateType == StateType.KUBERNETES_REPLICATION_CONTROLLER_DEPLOY
                       || nextStateType == StateType.AWS_CODEDEPLOY_STATE)
            && next.getStateExecutionData() instanceof CommandStateExecutionData) {
          CommandStateExecutionData commandStateExecutionData =
              (CommandStateExecutionData) next.getStateExecutionData();
          instanceStatusSummaries.addAll(commandStateExecutionData.getNewInstanceStatusSummaries());
        }
        last = next;
      }

      if (elementExecutionSummary.getEndTs() == null || elementExecutionSummary.getEndTs() < last.getEndTs()) {
        elementExecutionSummary.setEndTs(last.getEndTs());
      }
      if (contextElement != null && contextElement.getElementType() == ContextElementType.INSTANCE) {
        instanceStatusSummaries.add(anInstanceStatusSummary()
                                        .withInstanceElement((InstanceElement) contextElement)
                                        .withStatus(last.getStatus())
                                        .build());
      }

      elementExecutionSummary.setStatus(last.getStatus());
      elementExecutionSummary.setInstanceStatusSummaries(instanceStatusSummaries);
      elementExecutionSummaries.add(elementExecutionSummary);
    }

    return elementExecutionSummaries;
  }

  @Override
  public PhaseExecutionSummary getPhaseExecutionSummary(
      String appId, String executionUuid, String stateExecutionInstanceId) {
    PhaseExecutionSummary phaseExecutionSummary = new PhaseExecutionSummary();
    PageRequest<StateExecutionInstance> pageRequest =
        aPageRequest()
            .withLimit(PageRequest.UNLIMITED)
            .addFilter("appId", EQ, appId)
            .addFilter("executionUuid", EQ, executionUuid)
            .addFilter("parentInstanceId", Operator.IN, stateExecutionInstanceId)
            .addFilter("stateType", EQ, StateType.PHASE_STEP.name())
            .addFieldsIncluded(
                "uuid", "parentInstanceId", "contextElement", "status", "stateType", "stateName", "stateExecutionMap")
            .build();

    PageResponse<StateExecutionInstance> pageResponse =
        wingsPersistence.query(StateExecutionInstance.class, pageRequest);
    if (pageResponse == null || pageResponse.getResponse() == null || pageResponse.getResponse().isEmpty()) {
      return phaseExecutionSummary;
    }

    pageResponse.getResponse().forEach(instance -> {
      StateExecutionData stateExecutionData = instance.getStateExecutionData();
      if (stateExecutionData instanceof PhaseStepExecutionData) {
        PhaseStepExecutionData phaseStepExecutionData = (PhaseStepExecutionData) stateExecutionData;
        phaseExecutionSummary.getPhaseStepExecutionSummaryMap().put(
            instance.getStateName(), phaseStepExecutionData.getPhaseStepExecutionSummary());
      }
    });

    return phaseExecutionSummary;
  }

  @Override
  public PhaseStepExecutionSummary getPhaseStepExecutionSummary(
      String appId, String executionUuid, String stateExecutionInstanceId) {
    PhaseStepExecutionSummary phaseStepExecutionSummary = new PhaseStepExecutionSummary();
    List<StepExecutionSummary> stepExecutionSummaryList = phaseStepExecutionSummary.getStepExecutionSummaryList();

    List<String> parentInstanceIds = asList(stateExecutionInstanceId);
    while (parentInstanceIds != null && !parentInstanceIds.isEmpty()) {
      PageRequest<StateExecutionInstance> pageRequest =
          aPageRequest()
              .withLimit(PageRequest.UNLIMITED)
              .addFilter("appId", EQ, appId)
              .addFilter("executionUuid", EQ, executionUuid)
              .addFilter("parentInstanceId", Operator.IN, parentInstanceIds.toArray())
              .addFieldsIncluded(
                  "uuid", "parentInstanceId", "contextElement", "status", "stateType", "stateName", "stateExecutionMap")
              .build();

      PageResponse<StateExecutionInstance> pageResponse =
          wingsPersistence.query(StateExecutionInstance.class, pageRequest);
      if (pageResponse == null || pageResponse.getResponse() == null || pageResponse.getResponse().isEmpty()) {
        break;
      }

      pageResponse.getResponse()
          .stream()
          .filter(instance
              -> !StateType.REPEAT.name().equals(instance.getStateType())
                  && !StateType.FORK.name().equals(instance.getStateType()))
          .forEach(instance -> {
            stepExecutionSummaryList.add(instance.getStateExecutionData().getStepExecutionSummary());
          });

      parentInstanceIds = pageResponse.getResponse()
                              .stream()
                              .filter(instance
                                  -> StateType.REPEAT.name().equals(instance.getStateType())
                                      || StateType.FORK.name().equals(instance.getStateType()))
                              .map(StateExecutionInstance::getUuid)
                              .collect(Collectors.toList());
    }

    return phaseStepExecutionSummary;
  }
}
