package software.wings.service;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.InfrastructureMappingBlueprint.CloudProviderType.AWS;
import static software.wings.beans.InfrastructureMappingType.AWS_SSH;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.generator.InfrastructureProvisionerGenerator.InfrastructureProvisioners.TERRAFORM_TEST;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.exception.InvalidRequestException;
import org.junit.Test;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingBlueprint;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.InfrastructureProvisionerDetails;
import software.wings.beans.Service;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowType;
import software.wings.common.Constants;
import software.wings.generator.ApplicationGenerator;
import software.wings.generator.InfrastructureMappingGenerator;
import software.wings.generator.InfrastructureProvisionerGenerator;
import software.wings.generator.OwnerManager;
import software.wings.generator.OwnerManager.Owners;
import software.wings.generator.Randomizer;
import software.wings.generator.ServiceGenerator;
import software.wings.generator.WorkflowGenerator;
import software.wings.rules.SetupScheduler;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.sm.ExecutionContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SetupScheduler
public class InfrastructureProvisionerServiceTest extends WingsBaseTest {
  @Inject @InjectMocks private InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject private InfrastructureMappingService infrastructureMappingService;

  @Inject OwnerManager ownerManager;
  @Inject ApplicationGenerator applicationGenerator;
  @Inject InfrastructureMappingGenerator infrastructureMappingGenerator;
  @Inject InfrastructureProvisionerGenerator infrastructureProvisionerGenerator;
  @Inject WorkflowGenerator workflowGenerator;
  @Inject ServiceGenerator serviceGenerator;

  @Test
  public void shouldSave() {
    Randomizer.Seed seed = Randomizer.seed();
    final InfrastructureProvisioner infrastructureProvisioner = infrastructureProvisionerGenerator.ensureRandom(seed);

    infrastructureProvisioner.setUuid(generateUuid());
    final InfrastructureProvisioner provisioner = infrastructureProvisionerService.save(infrastructureProvisioner);
    assertThat(provisioner).isEqualTo(infrastructureProvisioner);
  }

  @Test
  public void shouldGet() {
    Randomizer.Seed seed = Randomizer.seed();
    final InfrastructureProvisioner infrastructureProvisioner = infrastructureProvisionerGenerator.ensureRandom(seed);
    final InfrastructureProvisioner provisioner =
        infrastructureProvisionerService.get(infrastructureProvisioner.getAppId(), infrastructureProvisioner.getUuid());

    assertThat(provisioner).isEqualTo(infrastructureProvisioner);
  }

  @Test
  public void shouldList() {
    Randomizer.Seed seed = Randomizer.seed();
    final InfrastructureProvisioner infrastructureProvisioner = infrastructureProvisionerGenerator.ensureRandom(seed);
    final List<InfrastructureProvisioner> provisioners = infrastructureProvisionerService.list(aPageRequest().build());
    assertThat(provisioners.size()).isEqualTo(1);
  }

  @Test
  public void testListDetails() {
    Randomizer.Seed seed = Randomizer.seed();

    final Owners owners = ownerManager.create();

    final Application application = applicationGenerator.ensureRandom(seed, owners);
    owners.add(application);

    final Service service1 = serviceGenerator.ensureService(seed, owners, Service.builder().name("Service1").build());
    final Service service2 = serviceGenerator.ensureService(seed, owners, Service.builder().name("Service2").build());

    final InfrastructureProvisioner provisioner =
        infrastructureProvisionerGenerator.ensureInfrastructureProvisioner(seed, owners,
            TerraformInfrastructureProvisioner.builder()
                .name("Test")
                .mappingBlueprints(asList(InfrastructureMappingBlueprint.builder()
                                              .serviceId(service1.getUuid())
                                              .deploymentType(SSH)
                                              .cloudProviderType(AWS)
                                              .build(),
                    InfrastructureMappingBlueprint.builder()
                        .serviceId(service2.getUuid())
                        .deploymentType(SSH)
                        .cloudProviderType(AWS)
                        .build()))
                .build());

    final PageResponse<InfrastructureProvisionerDetails> pageResponse = infrastructureProvisionerService.listDetails(
        aPageRequest().addFilter(InfrastructureProvisioner.APP_ID_KEY, Operator.EQ, provisioner.getAppId()).build());
    assertThat(pageResponse.size()).isEqualTo(1);
    InfrastructureProvisionerDetails details = pageResponse.get(0);
    assertThat(details.getName()).isEqualTo("Test");
    assertThat(details.getRepository()).isEqualTo("https://github.com/wings-software/terraform-test.git");
    assertThat(details.getInfrastructureProvisionerType()).isEqualTo("TERRAFORM");
    assertThat(details.getServices().size()).isEqualTo(2);

    assertThat(details.getServices().keySet()).contains("Service1", "Service2");
    assertThat(details.getServices().values()).contains(service1.getUuid(), service2.getUuid());
  }

  @Test
  public void shouldDelete() {
    Randomizer.Seed seed = Randomizer.seed();
    final InfrastructureProvisioner infrastructureProvisioner = infrastructureProvisionerGenerator.ensureRandom(seed);
    infrastructureProvisionerService.delete(infrastructureProvisioner.getAppId(), infrastructureProvisioner.getUuid());
    final List<InfrastructureProvisioner> provisioners = infrastructureProvisionerService.list(aPageRequest().build());
    assertThat(provisioners.size()).isEqualTo(0);
  }

  @Test
  public void shouldNotDelete() {
    Randomizer.Seed seed = Randomizer.seed();
    final InfrastructureProvisioner infrastructureProvisioner = infrastructureProvisionerGenerator.ensureRandom(seed);
    final InfrastructureMapping infrastructureMapping =
        infrastructureMappingGenerator.ensureInfrastructureMapping(seed, null,
            anAwsInfrastructureMapping()
                .withInfraMappingType(AWS_SSH.name())
                .withProvisionerId(infrastructureProvisioner.getUuid())
                .build());

    Workflow workflow = workflowGenerator.ensureWorkflow(seed, null,
        aWorkflow()
            .withName("basic")
            .withAppId(infrastructureMapping.getAppId())
            .withEnvId(infrastructureMapping.getEnvId())
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withServiceId(infrastructureMapping.getServiceId())
            .withInfraMappingId(infrastructureMapping.getUuid())
            .withOrchestrationWorkflow(
                aBasicOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build());

    assertThatThrownBy(()
                           -> infrastructureProvisionerService.delete(
                               infrastructureProvisioner.getAppId(), infrastructureProvisioner.getUuid()));
  }

  @Test
  public void testRegenerateInfrastructureMappings() {
    Randomizer.Seed seed = Randomizer.seed();
    final Owners owners = ownerManager.create();

    final InfrastructureProvisioner infrastructureProvisioner =
        infrastructureProvisionerGenerator.ensurePredefined(seed, owners, TERRAFORM_TEST);

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingGenerator.ensureInfrastructureMapping(seed, owners,
            anAwsInfrastructureMapping()
                .withServiceId(infrastructureProvisioner.getMappingBlueprints().get(0).getServiceId())
                .withInfraMappingType(AWS_SSH.name())
                .withProvisionerId(infrastructureProvisioner.getUuid())
                .build());

    ExecutionContext context = mock(ExecutionContext.class);
    when(context.getAppId()).thenReturn(infrastructureProvisioner.getAppId());
    when(context.asMap()).thenReturn(new HashMap<>());

    Map<String, Object> outputs = new HashMap<>();
    outputs.put("region", "dummy-region");

    assertThatThrownBy(()
                           -> infrastructureProvisionerService.regenerateInfrastructureMappings(
                               infrastructureProvisioner.getUuid(), context, outputs))
        .isInstanceOf(InvalidRequestException.class);

    outputs.put("security_group", asList("dummy-securityGroups"));
    outputs.put("archive_tags", ImmutableMap.<String, Object>of("a", "b"));
    infrastructureProvisionerService.regenerateInfrastructureMappings(
        infrastructureProvisioner.getUuid(), context, outputs);

    AwsInfrastructureMapping awsInfrastructureMapping = (AwsInfrastructureMapping) infrastructureMappingService.get(
        infrastructureMapping.getAppId(), infrastructureMapping.getUuid());

    assertThat(awsInfrastructureMapping.getRegion()).isEqualTo("dummy-region");
  }
}
