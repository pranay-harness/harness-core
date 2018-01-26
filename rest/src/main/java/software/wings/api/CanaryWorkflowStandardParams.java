package software.wings.api;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.Arrays.asList;
import static software.wings.api.InfraNodeRequest.InfraNodeRequestBuilder.anInfraNodeRequest;
import static software.wings.beans.PhaseStepType.CONTAINER_DEPLOY;
import static software.wings.beans.PhaseStepType.PROVISION_NODE;
import static software.wings.sm.StateType.AWS_NODE_SELECT;
import static software.wings.sm.StateType.DC_NODE_SELECT;
import static software.wings.sm.StateType.ECS_SERVICE_DEPLOY;
import static software.wings.sm.StateType.KUBERNETES_DEPLOY;
import static software.wings.utils.Switch.unhandled;

import com.google.inject.Inject;

import org.mongodb.morphia.annotations.Transient;
import software.wings.api.InfraNodeRequest.InfraNodeRequestBuilder;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.PhaseStepType;
import software.wings.common.Constants;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.State;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachine;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.AwsNodeSelectState;
import software.wings.sm.states.DcNodeSelectState;
import software.wings.sm.states.EcsServiceDeploy;
import software.wings.sm.states.KubernetesDeploy;
import software.wings.sm.states.PhaseStepSubWorkflow;
import software.wings.sm.states.PhaseSubWorkflow;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Created by rishi on 5/25/17.
 */
public class CanaryWorkflowStandardParams extends WorkflowStandardParams {
  @Inject @Transient private transient WorkflowService workflowService;

  @Inject @Transient private transient WorkflowExecutionService workflowExecutionService;

  @Inject @Transient private transient InfrastructureMappingService infrastructureMappingService;

  public List<InfraNodeRequest> getInfraNodeRequests() {
    return getInfraNodeRequestsByPhase(null);
  }

  public List<InfraNodeRequest> getInfraNodeRequestsByPhase(String phaseName) {
    ExecutionContext context = getContext();
    ExecutionContextImpl executionContext = (ExecutionContextImpl) context;
    StateExecutionInstance stateExecutionInstance = executionContext.getStateExecutionInstance();
    StateMachine rootStateMachine =
        workflowService.readStateMachine(context.getAppId(), stateExecutionInstance.getStateMachineId());
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);

    List<InfraNodeRequest> infraNodeRequests = new ArrayList<>();
    Stream<State> stateStream = rootStateMachine.getStates().stream().filter(
        state -> state.getStateType().equals(StateType.PHASE.name()) && !state.isRollback());
    if (phaseName != null) {
      stateStream.filter(state -> state.getName().equals(phaseName));
    }
    stateStream.forEach(state -> {

      PhaseSubWorkflow phaseState = (PhaseSubWorkflow) state;
      StateMachine phaseStateMachine = rootStateMachine.getChildStateMachines().get(phaseState.getId());
      InfraNodeRequest infraNodeRequestForPhase = getInfraNodeRequestForPhase(
          context.getAppId(), phaseElement, phaseState, phaseStateMachine, rootStateMachine);
      if (infraNodeRequestForPhase != null) {
        infraNodeRequests.add(infraNodeRequestForPhase);
      }
    });

    return infraNodeRequests;
  }

  private InfraNodeRequest getInfraNodeRequestForPhase(String appId, PhaseElement phaseElement,
      PhaseSubWorkflow phaseState, StateMachine phaseStateMachine, StateMachine rootStateMachine) {
    if (phaseStateMachine == null || isEmpty(phaseStateMachine.getStates())) {
      return null;
    }

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(appId, phaseState.getInfraMappingId());
    Validator.notNullCheck("InfrastructureMapping", infrastructureMapping);

    DeploymentType deploymentType = DeploymentType.valueOf(infrastructureMapping.getDeploymentType());

    switch (deploymentType) {
      case ECS: {
        State infraState =
            getInfraState(rootStateMachine, phaseStateMachine, CONTAINER_DEPLOY, ECS_SERVICE_DEPLOY.name());
        if (infraState != null) {
          EcsServiceDeploy ecsServiceDeploy = (EcsServiceDeploy) infraState;
          return anInfraNodeRequest()
              .withDeploymentType(DeploymentType.ECS)
              .withInstanceCount(ecsServiceDeploy.getInstanceCount())
              .withInstanceUnitType(ecsServiceDeploy.getInstanceUnitType())
              .withPhaseElement(phaseElement)
              .build();
        }
        break;
      }

      case KUBERNETES: {
        State infraState =
            getInfraState(rootStateMachine, phaseStateMachine, CONTAINER_DEPLOY, KUBERNETES_DEPLOY.name());
        if (infraState != null) {
          KubernetesDeploy replicationControllerDeploy = (KubernetesDeploy) infraState;
          return anInfraNodeRequest()
              .withDeploymentType(DeploymentType.KUBERNETES)
              .withInstanceCount(replicationControllerDeploy.getInstanceCount())
              .withInstanceUnitType(replicationControllerDeploy.getInstanceUnitType())
              .withPhaseElement(phaseElement)
              .build();
        }
        break;
      }

      case SSH: {
        State infraState = getInfraState(
            rootStateMachine, phaseStateMachine, PROVISION_NODE, DC_NODE_SELECT.name(), AWS_NODE_SELECT.name());
        if (infraState != null) {
          if (infraState instanceof DcNodeSelectState) {
            DcNodeSelectState dcNodeSelectState = (DcNodeSelectState) infraState;
            InfraNodeRequestBuilder infraNodeRequestBuilder =
                anInfraNodeRequest().withDeploymentType(DeploymentType.SSH).withPhaseElement(phaseElement);
            if (dcNodeSelectState.isSpecificHosts()) {
              infraNodeRequestBuilder.withNodeNames(dcNodeSelectState.getHostNames())
                  .withInstanceCount(dcNodeSelectState.getHostNames().size())
                  .withInstanceUnitType(InstanceUnitType.COUNT);
            } else {
              infraNodeRequestBuilder.withInstanceCount(dcNodeSelectState.getInstanceCount())
                  .withInstanceUnitType(dcNodeSelectState.getInstanceUnitType());
            }
            return infraNodeRequestBuilder.build();
          } else if (infraState instanceof AwsNodeSelectState) {
            AwsNodeSelectState awsNodeSelectState = (AwsNodeSelectState) infraState;
            InfraNodeRequestBuilder infraNodeRequestBuilder =
                anInfraNodeRequest().withDeploymentType(DeploymentType.SSH).withPhaseElement(phaseElement);
            if (awsNodeSelectState.isSpecificHosts()) {
              infraNodeRequestBuilder.withNodeNames(awsNodeSelectState.getHostNames())
                  .withInstanceCount(awsNodeSelectState.getHostNames().size())
                  .withInstanceUnitType(InstanceUnitType.COUNT);
            } else {
              infraNodeRequestBuilder.withInstanceCount(awsNodeSelectState.getInstanceCount())
                  .withInstanceUnitType(awsNodeSelectState.getInstanceUnitType());
            }
            return infraNodeRequestBuilder.build();
          }
        }
        break;
      }

        //      case AWS_AWS_CODEDEPLOY: {
        //        State infraState = getInfraState(rootStateMachine, phaseStateMachine, DEPLOY_AWSCODEDEPLOY,
        //        AWS_CODEDEPLOY_STATE.name()); if (infraState != null) {
        //          AwsCodeDeployState awsCodeDeployDeploy = (AwsCodeDeployState) infraState;
        //          return
        //          anInfraNodeRequest().withDeploymentType(DeploymentType.AWS_AWS_CODEDEPLOY).withProvisionNodes(true).withPhaseElement(phaseElement).build();
        //        }
        //        break;
        //      }

      default:
        unhandled(deploymentType);
    }

    return null;
  }

  private State getInfraState(StateMachine rootStateMachine, StateMachine phaseStateMachine,
      PhaseStepType phaseStepType, String... infraStateTypes) {
    Optional<State> infraPhaseStep =
        phaseStateMachine.getStates()
            .stream()
            .filter(state
                -> state instanceof PhaseStepSubWorkflow
                    && ((PhaseStepSubWorkflow) state).getPhaseStepType().equals(phaseStepType))
            .findFirst();
    if (!infraPhaseStep.isPresent()) {
      return null;
    }
    PhaseStepSubWorkflow phaseStepState = (PhaseStepSubWorkflow) infraPhaseStep.get();
    StateMachine phaseStepStateMachine = rootStateMachine.getChildStateMachines().get(phaseStepState.getId());
    if (phaseStepStateMachine == null || isEmpty(phaseStepStateMachine.getStates())) {
      return null;
    }

    return phaseStepStateMachine.getStates()
        .stream()
        .filter(state -> asList(infraStateTypes).contains(state.getStateType()))
        .findFirst()
        .get();
  }
}
