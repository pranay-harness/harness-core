package io.harness.generator;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.generator.InfrastructureMappingGenerator.InfrastructureMappings.AWS_SSH_TEST;
import static io.harness.generator.ResourceConstraintGenerator.ResourceConstraints.GENERIC_ASAP_TEST;
import static io.harness.generator.SettingGenerator.Settings.HARNESS_JENKINS_CONNECTOR;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.BuildWorkflow.BuildOrchestrationWorkflowBuilder.aBuildOrchestrationWorkflow;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
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
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.common.Constants.INFRASTRUCTURE_NODE_NAME;
import static software.wings.common.Constants.SELECT_NODE_NAME;
import static software.wings.sm.StateType.HTTP;
import static software.wings.sm.StateType.RESOURCE_CONSTRAINT;
import static software.wings.sm.StateType.TERRAFORM_DESTROY;
import static software.wings.sm.StateType.TERRAFORM_PROVISION;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.WorkflowType;
import io.harness.generator.EnvironmentGenerator.Environments;
import io.harness.generator.InfrastructureMappingGenerator.InfrastructureMappings;
import io.harness.generator.InfrastructureProvisionerGenerator.InfrastructureProvisioners;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.artifactstream.ArtifactStreamManager;
import io.harness.generator.artifactstream.ArtifactStreamManager.ArtifactStreams;
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
import software.wings.beans.ResourceConstraint;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.beans.template.Template;
import software.wings.beans.template.command.ShellScriptTemplate;
import software.wings.common.Constants;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;
import software.wings.sm.states.HttpState;
import software.wings.sm.states.JenkinsState;
import software.wings.sm.states.ResourceConstraintState.HoldingScope;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class WorkflowGenerator {
  @Inject private WorkflowService workflowService;
  @Inject private ServiceResourceService serviceResourceService;

  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private OrchestrationWorkflowGenerator orchestrationWorkflowGenerator;
  @Inject private InfrastructureMappingGenerator infrastructureMappingGenerator;
  @Inject private InfrastructureProvisionerGenerator infrastructureProvisionerGenerator;
  @Inject private ResourceConstraintGenerator resourceConstraintGenerator;
  @Inject private ArtifactStreamManager artifactStreamManager;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private ScmSecret scmSecret;
  @Inject private SecretGenerator secretGenerator;
  @Inject private SettingGenerator settingGenerator;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private TemplateGenerator templateGenerator;

  public enum Workflows {
    BASIC_SIMPLE,
    BASIC_10_NODES,
    TERRAFORM,
    PERMANENTLY_BLOCKED_RESOURCE_CONSTRAINT,
    BUILD_JENKINS,
    BUILD_SHELL_SCRIPT,
    BASIC_ECS
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
      case BUILD_SHELL_SCRIPT:
        return ensureBuildShellScript(seed, owners);
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
    owners.obtainEnvironment(() -> environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST));

    final InfrastructureProvisioner infrastructureProvisioner =
        infrastructureProvisionerGenerator.ensurePredefined(seed, owners, InfrastructureProvisioners.TERRAFORM_TEST);

    owners.obtainService(()
                             -> serviceResourceService.get(infrastructureProvisioner.getAppId(),
                                 infrastructureProvisioner.getMappingBlueprints().iterator().next().getServiceId()));

    infrastructureMappingGenerator.ensurePredefined(seed, owners, InfrastructureMappings.TERRAFORM_AWS_SSH_TEST);

    final SecretName awsPlaygroundAccessKeyName = SecretName.builder().value("aws_playground_access_key").build();
    final String awsPlaygroundAccessKey = scmSecret.decryptToString(awsPlaygroundAccessKeyName);
    final SecretName awsPlaygroundSecretKeyName = SecretName.builder().value("aws_playground_secret_key").build();
    final String awsPlaygroundSecretKeyId = secretGenerator.ensureStored(owners, awsPlaygroundSecretKeyName);

    // TODO: this is temporary adding second key, to workaround bug in the UI
    final SecretName terraformPasswordName = SecretName.builder().value("terraform_password").build();
    secretGenerator.ensureStored(owners, terraformPasswordName);

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
                            .addStep(GraphNode.builder()
                                         .type(TERRAFORM_PROVISION.name())
                                         .name("Provision infra")
                                         .properties(
                                             ImmutableMap.<String, Object>builder()
                                                 .put("provisionerId", infrastructureProvisioner.getUuid())
                                                 .put("variables",
                                                     asList(ImmutableMap.of("name", "access_key", "value",
                                                                awsPlaygroundAccessKey, "valueType", Type.TEXT.name()),
                                                         ImmutableMap.of("name", "secret_key", "value",
                                                             awsPlaygroundSecretKeyId, "valueType",
                                                             Type.ENCRYPTED_TEXT.name())))
                                                 .build())
                                         .build())
                            .build())
                    .withPostDeploymentSteps(
                        aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT)
                            .addStep(GraphNode.builder()
                                         .type(TERRAFORM_DESTROY.name())
                                         .name("Deprovision infra")
                                         .properties(ImmutableMap.<String, Object>builder()
                                                         .put("provisionerId", infrastructureProvisioner.getUuid())
                                                         .build())
                                         .build())
                            .build())
                    .build())
            .build());
  }

  private Workflow ensurePermanentlyBlockedResourceConstraint(Randomizer.Seed seed, Owners owners) {
    InfrastructureMapping infrastructureMapping =
        infrastructureMappingGenerator.ensurePredefined(seed, owners, AWS_SSH_TEST);

    final ResourceConstraint asapResourceConstraint =
        resourceConstraintGenerator.ensurePredefined(seed, owners, GENERIC_ASAP_TEST);

    return ensureWorkflow(seed, owners,
        aWorkflow()
            .withName("Resource constraint")
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withInfraMappingId(infrastructureMapping.getUuid())
            .withOrchestrationWorkflow(
                aBasicOrchestrationWorkflow()
                    .withPreDeploymentSteps(
                        aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT)
                            .addStep(GraphNode.builder()
                                         .type(RESOURCE_CONSTRAINT.name())
                                         .name(asapResourceConstraint.getName() + " 1")
                                         .properties(ImmutableMap.<String, Object>builder()
                                                         .put("resourceConstraintId", asapResourceConstraint.getUuid())
                                                         .put("permits", 6)
                                                         .put("holdingScope", HoldingScope.WORKFLOW.name())
                                                         .build())
                                         .build())
                            .addStep(GraphNode.builder()
                                         .type(RESOURCE_CONSTRAINT.name())
                                         .name(asapResourceConstraint.getName() + " 2")
                                         .properties(ImmutableMap.<String, Object>builder()
                                                         .put("resourceConstraintId", asapResourceConstraint.getUuid())
                                                         .put("permits", 6)
                                                         .put("holdingScope", HoldingScope.WORKFLOW.name())
                                                         .build())
                                         .build())
                            .build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build());
  }

  private Workflow ensureBuildJenkins(Seed seed, Owners owners) {
    // Ensure artifact stream

    // TODO: Change it to Docker ArtifactStream
    artifactStreamManager.ensurePredefined(seed, owners, ArtifactStreams.HARNESS_SAMPLE_ECHO_WAR);

    SettingAttribute jenkinsConfig = settingGenerator.ensurePredefined(seed, owners, HARNESS_JENKINS_CONNECTOR);

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
                    .addWorkflowPhase(
                        aWorkflowPhase()
                            .phaseSteps(asList(aPhaseStep(PREPARE_STEPS, Constants.PREPARE_STEPS).build(),
                                aPhaseStep(COLLECT_ARTIFACT, Constants.COLLECT_ARTIFACT)
                                    .addStep(
                                        GraphNode.builder()
                                            .id(generateUuid())
                                            .type(JENKINS.name())
                                            .name("Jenkins - pipeline")
                                            .properties(
                                                ImmutableMap.<String, Object>builder()
                                                    .put(JenkinsState.JENKINS_CONFIG_ID_KEY, jenkinsConfig.getUuid())
                                                    .put(JenkinsState.JOB_NAME_KEY, "build-description-setter")
                                                    .put(JenkinsState.SWEEPING_OUTPUT_NAME_KEY, "pipeline")
                                                    .put(JenkinsState.SWEEPING_OUTPUT_SCOPE_KEY, PIPELINE)
                                                    .build())
                                            .build())
                                    .addStep(
                                        GraphNode.builder()
                                            .id(generateUuid())
                                            .type(JENKINS.name())
                                            .name("Jenkins - workflow")
                                            .properties(
                                                ImmutableMap.<String, Object>builder()
                                                    .put(JenkinsState.JENKINS_CONFIG_ID_KEY, jenkinsConfig.getUuid())
                                                    .put(JenkinsState.JOB_NAME_KEY, "build-description-setter")
                                                    .put(JenkinsState.SWEEPING_OUTPUT_NAME_KEY, "workflow")
                                                    .put(JenkinsState.SWEEPING_OUTPUT_SCOPE_KEY, WORKFLOW)
                                                    .build())
                                            .build())
                                    .addStep(
                                        GraphNode.builder()
                                            .id(generateUuid())
                                            .type(JENKINS.name())
                                            .name("Jenkins - phase")
                                            .properties(
                                                ImmutableMap.<String, Object>builder()
                                                    .put(JenkinsState.JENKINS_CONFIG_ID_KEY, jenkinsConfig.getUuid())
                                                    .put(JenkinsState.JOB_NAME_KEY, "build-description-setter")
                                                    .put(JenkinsState.SWEEPING_OUTPUT_NAME_KEY, "phase")
                                                    .put(JenkinsState.SWEEPING_OUTPUT_SCOPE_KEY, PHASE)
                                                    .build())
                                            .build())
                                    .build(),
                                aPhaseStep(WRAP_UP, Constants.WRAP_UP)
                                    .addStep(getHTTPNode("pipeline"))
                                    .addStep(getHTTPNode("workflow"))
                                    .addStep(getHTTPNode("phase"))
                                    .build()))
                            .build())
                    .build())
            .build());
  }

  private GraphNode getHTTPNode(String scope) {
    return GraphNode.builder()
        .id(generateUuid())
        .type(HTTP.name())
        .name("HTTP - " + scope)
        .properties(
            ImmutableMap.<String, Object>builder()
                .put(HttpState.URL_KEY, format("http://www.google.com?h=${context.output(\"%s\").buildNumber}", scope))
                .put(HttpState.METHOD_KEY, "GET")
                .build())
        .build();
  }

  private Workflow ensureBuildShellScript(Seed seed, Owners owners) {
    Template shellScriptTemplate =
        templateGenerator.ensurePredefined(seed, owners, TemplateGenerator.Templates.SHELL_SCRIPT);
    ShellScriptTemplate templateObject = (ShellScriptTemplate) shellScriptTemplate.getTemplateObject();
    Map<String, Object> properties = new HashMap<>();
    properties.put("scriptType", templateObject.getScriptType());
    properties.put("scriptString", templateObject.getScriptString());
    properties.put("outputVars", templateObject.getOutputVars());
    properties.put("connectionType", "SSH");
    properties.put("executeOnDelegate", true);

    return ensureWorkflow(seed, owners,
        aWorkflow()
            .withName("Shell Script Build Workflow")
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withOrchestrationWorkflow(
                aBuildOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .addWorkflowPhase(aWorkflowPhase()
                                          .phaseSteps(asList(aPhaseStep(PREPARE_STEPS, Constants.PREPARE_STEPS).build(),
                                              aPhaseStep(COLLECT_ARTIFACT, Constants.COLLECT_ARTIFACT)
                                                  .addStep(GraphNode.builder()
                                                               .id(generateUuid())
                                                               .type(StateType.SHELL_SCRIPT.name())
                                                               .name(shellScriptTemplate.getName())
                                                               .properties(properties)
                                                               .templateVariables(shellScriptTemplate.getVariables())
                                                               .templateUuid(shellScriptTemplate.getUuid())
                                                               .templateVersion("latest")
                                                               .build())
                                                  .build(),
                                              aPhaseStep(WRAP_UP, Constants.WRAP_UP).build()))
                                          .build())
                    .build())
            .build());
  }

  public Workflow exists(Workflow workflow) {
    return workflowService.readWorkflowByName(workflow.getAppId(), workflow.getName());
  }

  public Workflow ensureWorkflow(Randomizer.Seed seed, OwnerManager.Owners owners, Workflow workflow) {
    WorkflowBuilder builder = aWorkflow();

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

    if (workflow.getEnvId() != null) {
      builder.withEnvId(workflow.getEnvId());
    } else {
      OrchestrationWorkflowType orchestrationWorkflowType =
          workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType();
      if (!OrchestrationWorkflowType.BUILD.equals(orchestrationWorkflowType)) {
        Environment environment = owners.obtainEnvironment();
        builder.withEnvId(environment.getUuid());
      }
    }

    if (workflow.getServiceId() != null) {
      builder.withServiceId(workflow.getServiceId());
    } else {
      OrchestrationWorkflowType orchestrationWorkflowType =
          workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType();
      if (!OrchestrationWorkflowType.BUILD.equals(orchestrationWorkflowType)) {
        Service service = owners.obtainService();
        builder.withServiceId(service.getUuid());
      }
    }

    if (workflow.getWorkflowType() != null) {
      builder.withWorkflowType(workflow.getWorkflowType());
    } else {
      throw new UnsupportedOperationException();
    }

    if (workflow.getOrchestrationWorkflow() != null) {
      builder.withOrchestrationWorkflow(workflow.getOrchestrationWorkflow());
    } else {
      throw new UnsupportedOperationException();
    }

    if (workflow.getInfraMappingId() != null) {
      builder.withInfraMappingId(workflow.getInfraMappingId());
    } else {
      if (!OrchestrationWorkflowType.BUILD.equals(workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType())) {
        throw new UnsupportedOperationException();
      }
    }
    final Workflow finalWorkflow = builder.build();

    return GeneratorUtils.suppressDuplicateException(
        () -> workflowService.createWorkflow(finalWorkflow), () -> exists(finalWorkflow));
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
