package software.wings.generator;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.BuildWorkflow.BuildOrchestrationWorkflowBuilder.aBuildOrchestrationWorkflow;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.COLLECT_ARTIFACT;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PREPARE_STEPS;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.WRAP_UP;
import static software.wings.beans.SweepingOutput.Scope.PHASE;
import static software.wings.beans.SweepingOutput.Scope.PIPELINE;
import static software.wings.beans.SweepingOutput.Scope.WORKFLOW;
import static software.wings.beans.TaskType.JENKINS;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.common.Constants.INFRASTRUCTURE_NODE_NAME;
import static software.wings.common.Constants.SELECT_NODE_NAME;
import static software.wings.generator.InfrastructureMappingGenerator.InfrastructureMappings.AWS_SSH_TEST;
import static software.wings.generator.SettingGenerator.Settings.HARNESS_JENKINS_CONNECTOR;
import static software.wings.sm.StateType.HTTP;
import static software.wings.sm.StateType.RESOURCE_CONSTRAINT;
import static software.wings.sm.StateType.TERRAFORM_DESTROY;
import static software.wings.sm.StateType.TERRAFORM_PROVISION;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.api.EnhancedRandom;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.Application;
import software.wings.beans.BasicOrchestrationWorkflow;
import software.wings.beans.Environment;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.OrchestrationWorkflowType;
import software.wings.beans.ResourceConstraint;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder;
import software.wings.beans.WorkflowType;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.common.Constants;
import software.wings.dl.WingsPersistence;
import software.wings.generator.ArtifactStreamGenerator.ArtifactStreams;
import software.wings.generator.EnvironmentGenerator.Environments;
import software.wings.generator.InfrastructureMappingGenerator.InfrastructureMappings;
import software.wings.generator.InfrastructureProvisionerGenerator.InfrastructureProvisioners;
import software.wings.generator.OwnerManager.Owners;
import software.wings.generator.ResourceConstraintGenerator.ResourceConstraints;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.states.HttpState;
import software.wings.sm.states.JenkinsState;
import software.wings.sm.states.ResourceConstraintState.HoldingScope;

@Singleton
public class WorkflowGenerator {
  @Inject private WorkflowService workflowService;
  @Inject private ServiceResourceService serviceResourceService;

  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private OrchestrationWorkflowGenerator orchestrationWorkflowGenerator;
  @Inject private InfrastructureMappingGenerator infrastructureMappingGenerator;
  @Inject private InfrastructureProvisionerGenerator infrastructureProvisionerGenerator;
  @Inject private ResourceConstraintGenerator resourceConstraintGenerator;
  @Inject private ArtifactStreamGenerator artifactStreamGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private ScmSecret scmSecret;
  @Inject private SecretGenerator secretGenerator;
  @Inject private SettingGenerator settingGenerator;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private WingsPersistence wingsPersistence;

  public enum Workflows {
    BASIC_SIMPLE,
    BASIC_10_NODES,
    TERRAFORM,
    PERMANENTLY_BLOCKED_RESOURCE_CONSTRAINT,
    BUILD_JENKINS
  }

  public Workflow ensurePredefined(Randomizer.Seed seed, Owners owners, Workflows predefined) {
    switch (predefined) {
      case BASIC_SIMPLE:
        return ensureBasicSimple(seed, owners);
      case BASIC_10_NODES:
        return ensureBasic10Nodes(seed, owners);
      case TERRAFORM:
        return ensureTerraform(seed, owners);
      case PERMANENTLY_BLOCKED_RESOURCE_CONSTRAINT:
        return ensurePermanentlyBlockedResourceConstraint(seed, owners);
      case BUILD_JENKINS:
        return ensureBuildJenkins(seed, owners);
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

  private Workflow ensureTerraform(Randomizer.Seed seed, Owners owners) {
    Environment environment =
        owners.obtainEnvironment(() -> environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST));

    final InfrastructureProvisioner infrastructureProvisioner =
        infrastructureProvisionerGenerator.ensurePredefined(seed, owners, InfrastructureProvisioners.TERRAFORM_TEST);

    Service service = owners.obtainService(
        ()
            -> serviceResourceService.get(infrastructureProvisioner.getAppId(),
                infrastructureProvisioner.getMappingBlueprints().iterator().next().getServiceId()));

    final InfrastructureMapping terraformInfrastructureMapping =
        infrastructureMappingGenerator.ensurePredefined(seed, owners, InfrastructureMappings.TERRAFORM_AWS_SSH_TEST);

    final SecretName awsPlaygroundAccessKeyName = SecretName.builder().value("aws_playground_access_key").build();
    final String awsPlaygroundAccessKey = scmSecret.decryptToString(awsPlaygroundAccessKeyName);
    final SecretName awsPlaygroundSecretKeyName = SecretName.builder().value("aws_playground_secret_key").build();
    final String awsPlaygroundSecretKeyId = secretGenerator.ensureStored(owners, awsPlaygroundSecretKeyName);

    // TODO: this is temporary adding second key, to workaround bug in the UI
    final SecretName terraformPasswordName = SecretName.builder().value("terraform_password").build();
    final String terraformPasswordId = secretGenerator.ensureStored(owners, terraformPasswordName);

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingGenerator.ensurePredefined(seed, owners, AWS_SSH_TEST);

    return workflowGenerator.ensureWorkflow(seed, owners,
        aWorkflow()
            .withName("Terraform provision")
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withInfraMappingId(infrastructureMapping.getUuid())
            .withOrchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(
                        aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT)
                            .addStep(
                                aGraphNode()
                                    .withType(TERRAFORM_PROVISION.name())
                                    .withName("Provision infra")
                                    .addProperty("provisionerId", infrastructureProvisioner.getUuid())
                                    .addProperty("variables",
                                        asList(ImmutableMap.of("name", "access_key", "value", awsPlaygroundAccessKey,
                                                   "valueType", Type.TEXT.name()),
                                            ImmutableMap.of("name", "secret_key", "value", awsPlaygroundSecretKeyId,
                                                "valueType", Type.ENCRYPTED_TEXT.name())))
                                    .build())
                            .build())
                    .withPostDeploymentSteps(
                        aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT)
                            .addStep(aGraphNode()
                                         .withType(TERRAFORM_DESTROY.name())
                                         .withName("Deprovision infra")
                                         .addProperty("provisionerId", infrastructureProvisioner.getUuid())
                                         .build())
                            .build())
                    .build())
            .build());
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

  private Workflow ensureBuildJenkins(Randomizer.Seed seed, Owners owners) {
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
                         .withName("Jenkins - pipeline")
                         .addProperty(JenkinsState.JENKINS_CONFIG_ID_KEY, jenkinsConfig.getUuid())
                         .addProperty(JenkinsState.JOB_NAME_KEY, "build-description-setter")
                         .addProperty(JenkinsState.SWEEPING_OUTPUT_NAME_KEY, "pipeline")
                         .addProperty(JenkinsState.SWEEPING_OUTPUT_SCOPE_KEY, PIPELINE)
                         .build())
            .addStep(aGraphNode()
                         .withId(generateUuid())
                         .withType(JENKINS.name())
                         .withName("Jenkins - workflow")
                         .addProperty(JenkinsState.JENKINS_CONFIG_ID_KEY, jenkinsConfig.getUuid())
                         .addProperty(JenkinsState.JOB_NAME_KEY, "build-description-setter")
                         .addProperty(JenkinsState.SWEEPING_OUTPUT_NAME_KEY, "workflow")
                         .addProperty(JenkinsState.SWEEPING_OUTPUT_SCOPE_KEY, WORKFLOW)
                         .build())
            .addStep(aGraphNode()
                         .withId(generateUuid())
                         .withType(JENKINS.name())
                         .withName("Jenkins - phase")
                         .addProperty(JenkinsState.JENKINS_CONFIG_ID_KEY, jenkinsConfig.getUuid())
                         .addProperty(JenkinsState.JOB_NAME_KEY, "build-description-setter")
                         .addProperty(JenkinsState.SWEEPING_OUTPUT_NAME_KEY, "phase")
                         .addProperty(JenkinsState.SWEEPING_OUTPUT_SCOPE_KEY, PHASE)
                         .build())
            .build());
    workflowPhaseBuilder.addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP)
                                          .addStep(getHTTPNode("pipeline"))
                                          .addStep(getHTTPNode("workflow"))
                                          .addStep(getHTTPNode("phase"))
                                          .build());

    return ensureWorkflow(seed, owners,
        aWorkflow()
            .withName("Build Jenkins")
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withOrchestrationWorkflow(
                aBuildOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT)
                                                 .addStep(getHTTPNode("pipeline"))
                                                 .addStep(getHTTPNode("workflow"))
                                                 .addStep(getHTTPNode("phase"))
                                                 .build())
                    .addWorkflowPhase(workflowPhaseBuilder.build())
                    .build())
            .build());
  }

  private GraphNode getHTTPNode(String scope) {
    return aGraphNode()
        .withId(generateUuid())
        .withType(HTTP.name())
        .withName("HTTP - " + scope)
        .addProperty(HttpState.URL_KEY, format("http://www.google.com?h=${context.output(\"%s\").buildNumber}", scope))
        .addProperty(HttpState.METHOD_KEY, "GET")
        .build();
  }

  public Workflow exists(Workflow workflow) {
    return wingsPersistence.createQuery(Workflow.class)
        .filter(Workflow.APP_ID_KEY, workflow.getAppId())
        .filter(Workflow.NAME_KEY, workflow.getName())
        .get();
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

    if (workflow != null && workflow.getName() != null) {
      builder.withName(workflow.getName());
    } else {
      throw new UnsupportedOperationException();
    }

    Workflow existing = exists(builder.build());
    if (existing != null) {
      return existing;
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
