package software.wings.generator;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.BuildWorkflow.BuildOrchestrationWorkflowBuilder.aBuildOrchestrationWorkflow;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.COLLECT_ARTIFACT;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PREPARE_STEPS;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.WRAP_UP;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.common.Constants.INFRASTRUCTURE_NODE_NAME;
import static software.wings.common.Constants.SELECT_NODE_NAME;
import static software.wings.generator.InfrastructureMappingGenerator.InfrastructureMappings.AWS_SSH_TEST;
import static software.wings.generator.SettingGenerator.Settings.HARNESS_JENKINS_CONNECTOR;
import static software.wings.sm.StateType.ARTIFACT_COLLECTION;
import static software.wings.sm.StateType.JENKINS;
import static software.wings.sm.StateType.RESOURCE_CONSTRAINT;
import static software.wings.utils.WingsIntegrationTestConstants.SEED_BUILD_WORKFLOW_NAME;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.Application;
import software.wings.beans.BasicOrchestrationWorkflow;
import software.wings.beans.Environment;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.OrchestrationWorkflowType;
import software.wings.beans.ResourceConstraint;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder;
import software.wings.beans.WorkflowType;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.common.Constants;
import software.wings.generator.ArtifactStreamGenerator.ArtifactStreams;
import software.wings.generator.OwnerManager.Owners;
import software.wings.generator.ResourceConstraintGenerator.ResourceConstraints;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.states.ArtifactCollectionState;
import software.wings.sm.states.JenkinsState;
import software.wings.sm.states.ResourceConstraintState.HoldingScope;

@Singleton
public class WorkflowGenerator {
  @Inject private WorkflowService workflowService;

  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private OrchestrationWorkflowGenerator orchestrationWorkflowGenerator;
  @Inject private InfrastructureMappingGenerator infrastructureMappingGenerator;
  @Inject private ResourceConstraintGenerator resourceConstraintGenerator;
  @Inject private ArtifactStreamGenerator artifactStreamGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private SettingGenerator settingGenerator;

  public enum Workflows { BASIC_SIMPLE, BASIC_10_NODES, PERMANENTLY_BLOCKED_RESOURCE_CONSTRAINT, BUILD }

  public Workflow ensurePredefined(Randomizer.Seed seed, Owners owners, Workflows predefined) {
    switch (predefined) {
      case BASIC_SIMPLE:
        return ensureBasicSimple(seed, owners);
      case BASIC_10_NODES:
        return ensureBasic10Nodes(seed, owners);
      case PERMANENTLY_BLOCKED_RESOURCE_CONSTRAINT:
        return ensurePermanentlyBlockedResourceConstraint(seed, owners);
      case BUILD:
        return ensureBuildWorkflow(seed, owners);
      default:
        unhandled(predefined);
    }

    return null;
  }

  private Workflow ensureBasicSimple(Randomizer.Seed seed, Owners owners) {
    InfrastructureMapping infrastructureMapping =
        infrastructureMappingGenerator.ensurePredefined(seed, owners, AWS_SSH_TEST);

    return ensureWorkflow(seed, owners,
        aWorkflow()
            .withName("Basic - simple")
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withInfraMappingId(infrastructureMapping.getUuid())
            .withOrchestrationWorkflow(
                aBasicOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build());
  }

  private Workflow ensureBasic10Nodes(Randomizer.Seed seed, Owners owners) {
    InfrastructureMapping infrastructureMapping =
        infrastructureMappingGenerator.ensurePredefined(seed, owners, AWS_SSH_TEST);

    Workflow workflow = ensureWorkflow(seed, owners,
        aWorkflow()
            .withName("Basic - 10 nodes")
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withInfraMappingId(infrastructureMapping.getUuid())
            .withOrchestrationWorkflow(
                aBasicOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build());

    workflow = postProcess(workflow, PostProcessInfo.builder().selectNodeCount(10).build());
    return workflow;
  }

  private Workflow ensurePermanentlyBlockedResourceConstraint(Randomizer.Seed seed, Owners owners) {
    InfrastructureMapping infrastructureMapping =
        infrastructureMappingGenerator.ensurePredefined(seed, owners, AWS_SSH_TEST);

    final ResourceConstraint asapResourceConstraint =
        resourceConstraintGenerator.ensurePredefined(seed, owners, ResourceConstraints.GENERIC_ASAP_TEST);

    return ensureWorkflow(seed, owners,
        aWorkflow()
            .withName("Resource constraint")
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withInfraMappingId(infrastructureMapping.getUuid())
            .withOrchestrationWorkflow(
                aBasicOrchestrationWorkflow()
                    .withPreDeploymentSteps(
                        aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT)
                            .addStep(aGraphNode()
                                         .withType(RESOURCE_CONSTRAINT.name())
                                         .withName(asapResourceConstraint.getName() + " 1")
                                         .addProperty("resourceConstraintId", asapResourceConstraint.getUuid())
                                         .addProperty("permits", 6)
                                         .addProperty("holdingScope", HoldingScope.WORKFLOW.name())
                                         .build())
                            .addStep(aGraphNode()
                                         .withType(RESOURCE_CONSTRAINT.name())
                                         .withName(asapResourceConstraint.getName() + " 2")
                                         .addProperty("resourceConstraintId", asapResourceConstraint.getUuid())
                                         .addProperty("permits", 6)
                                         .addProperty("holdingScope", HoldingScope.WORKFLOW.name())
                                         .build())
                            .build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build());
  }

  private Workflow ensureBuildWorkflow(Randomizer.Seed seed, Owners owners) {
    // Ensure artifact stream

    WorkflowPhaseBuilder workflowPhaseBuilder = WorkflowPhaseBuilder.aWorkflowPhase();
    workflowPhaseBuilder.addPhaseStep(aPhaseStep(PREPARE_STEPS, Constants.PREPARE_STEPS).build());

    // TODO: Change it to Docker ArtifactStream
    JenkinsArtifactStream jenkinsArtifactStream = (JenkinsArtifactStream) artifactStreamGenerator.ensurePredefined(
        seed, owners, ArtifactStreams.HARNESS_SAMPLE_ECHO_WAR);

    SettingAttribute jenkinsConfig = settingGenerator.ensurePredefined(seed, owners, HARNESS_JENKINS_CONNECTOR);

    workflowPhaseBuilder.addPhaseStep(
        aPhaseStep(COLLECT_ARTIFACT, Constants.COLLECT_ARTIFACT)
            .addStep(aGraphNode()
                         .withId(generateUuid())
                         .withType(JENKINS.name())
                         .withName("Jenkins")
                         .addProperty(JenkinsState.JENKINS_CONFIG_ID_KEY, jenkinsConfig.getUuid())
                         .addProperty(JenkinsState.JOB_NAME_KEY, "build-description-setter")
                         .addProperty(JenkinsState.SWEEPING_OUTPUT_NAME_KEY, "jenkins")
                         .build())
            .addStep(aGraphNode()
                         .withId(generateUuid())
                         .withType(ARTIFACT_COLLECTION.name())
                         .addProperty(ArtifactCollectionState.ARTIFACT_STREAM_ID_KEY, jenkinsArtifactStream.getUuid())
                         .addProperty(ArtifactCollectionState.BUILD_NO_KEY,
                             "${regex.replace('tag: ([\\w-]+)', '$1', ${Jenkins.description}}")
                         .withName(Constants.ARTIFACT_COLLECTION)
                         .build())
            .build());
    workflowPhaseBuilder.addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());

    return ensureWorkflow(seed, owners,
        aWorkflow()
            .withName(SEED_BUILD_WORKFLOW_NAME)
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withOrchestrationWorkflow(
                aBuildOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .addWorkflowPhase(workflowPhaseBuilder.build())
                    .build())
            .build());
  }

  public Workflow ensureWorkflow(Randomizer.Seed seed, OwnerManager.Owners owners, Workflow workflow) {
    EnhancedRandom random = Randomizer.instance(seed);

    WorkflowBuilder builder = aWorkflow();

    OrchestrationWorkflowType orchestrationWorkflowType =
        workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType();
    if (workflow != null && workflow.getAppId() != null) {
      builder.withAppId(workflow.getAppId());
    } else {
      final Application application = owners.obtainApplication();
      builder.withAppId(application.getUuid());
    }

    if (workflow != null && workflow.getEnvId() != null) {
      builder.withEnvId(workflow.getEnvId());
    } else {
      Environment environment = owners.obtainEnvironment();
      builder.withEnvId(environment.getUuid());
    }

    if (workflow != null && workflow.getServiceId() != null) {
      builder.withServiceId(workflow.getServiceId());
    } else {
      if (!OrchestrationWorkflowType.BUILD.equals(orchestrationWorkflowType)) {
        Service service = owners.obtainService();
        builder.withServiceId(service.getUuid());
      }
    }

    if (workflow != null && workflow.getName() != null) {
      builder.withName(workflow.getName());
    } else {
      throw new UnsupportedOperationException();
    }

    if (workflow != null && workflow.getWorkflowType() != null) {
      builder.withWorkflowType(workflow.getWorkflowType());
    } else {
      throw new UnsupportedOperationException();
    }

    if (workflow != null && workflow.getOrchestrationWorkflow() != null) {
      builder.withOrchestrationWorkflow(workflow.getOrchestrationWorkflow());
    } else {
      throw new UnsupportedOperationException();
    }

    if (workflow != null && workflow.getInfraMappingId() != null) {
      builder.withInfraMappingId(workflow.getInfraMappingId());
    } else {
      if (!OrchestrationWorkflowType.BUILD.equals(orchestrationWorkflowType)) {
        throw new UnsupportedOperationException();
      }
    }

    return workflowService.createWorkflow(builder.build());
  }

  @Value
  @Builder
  public static class PostProcessInfo {
    private Integer selectNodeCount;
  }

  public Workflow postProcess(Workflow workflow, PostProcessInfo params) {
    if (params.getSelectNodeCount() != null) {
      if (workflow.getOrchestrationWorkflow() instanceof BasicOrchestrationWorkflow) {
        final GraphNode selectNodes =
            ((BasicOrchestrationWorkflow) workflow.getOrchestrationWorkflow())
                .getGraph()
                .getSubworkflows()
                .entrySet()
                .stream()
                .filter(entry -> INFRASTRUCTURE_NODE_NAME.equals(entry.getValue().getGraphName()))
                .findFirst()
                .get()
                .getValue()
                .getNodes()
                .stream()
                .filter(entry -> SELECT_NODE_NAME.equals(entry.getName()))
                .findFirst()
                .get();

        selectNodes.getProperties().put("instanceCount", params.getSelectNodeCount());
      }
    }

    return workflowService.updateWorkflow(workflow);
  }
}
