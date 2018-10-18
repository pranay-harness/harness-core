package software.wings.service;

import software.wings.WingsBaseTest;
import software.wings.rules.SetupScheduler;

@SetupScheduler
public class InfrastructureProvisionerServiceTest extends WingsBaseTest {
  //  @Inject @InjectMocks private InfrastructureProvisionerService infrastructureProvisionerService;
  //  @Inject private InfrastructureMappingService infrastructureMappingService;
  //
  //  @Inject OwnerManager ownerManager;
  //  @Inject ApplicationGenerator applicationGenerator;
  //  @Inject InfrastructureMappingGenerator infrastructureMappingGenerator;
  //  @Inject InfrastructureProvisionerGenerator infrastructureProvisionerGenerator;
  //  @Inject WorkflowGenerator workflowGenerator;
  //  @Inject ServiceGenerator serviceGenerator;
  //
  //  @Test
  //  public void shouldSave() {
  //    Randomizer.Seed seed = Randomizer.seed();
  //    final InfrastructureProvisioner infrastructureProvisioner =
  //    infrastructureProvisionerGenerator.ensureRandom(seed);
  //
  //    infrastructureProvisioner.setUuid(generateUuid());
  //    final InfrastructureProvisioner provisioner = infrastructureProvisionerService.save(infrastructureProvisioner);
  //    assertThat(provisioner).isEqualTo(infrastructureProvisioner);
  //  }
  //
  //  @Test
  //  public void shouldGet() {
  //    Randomizer.Seed seed = Randomizer.seed();
  //    final InfrastructureProvisioner infrastructureProvisioner =
  //    infrastructureProvisionerGenerator.ensureRandom(seed); final InfrastructureProvisioner provisioner =
  //        infrastructureProvisionerService.get(infrastructureProvisioner.getAppId(),
  //        infrastructureProvisioner.getUuid());
  //
  //    assertThat(provisioner).isEqualTo(infrastructureProvisioner);
  //  }
  //
  //  @Test
  //  public void shouldList() {
  //    Randomizer.Seed seed = Randomizer.seed();
  //    final InfrastructureProvisioner infrastructureProvisioner =
  //    infrastructureProvisionerGenerator.ensureRandom(seed); final List<InfrastructureProvisioner> provisioners =
  //    infrastructureProvisionerService.list(aPageRequest().build()); assertThat(provisioners.size()).isEqualTo(1);
  //  }
  //
  //  @Test
  //  public void testListDetails() {
  //    Randomizer.Seed seed = Randomizer.seed();
  //
  //    final Owners owners = ownerManager.create();
  //
  //    final Application application = applicationGenerator.ensureRandom(seed, owners);
  //    owners.add(application);
  //
  //    final Service service1 = serviceGenerator.ensureService(seed, owners,
  //    Service.builder().name("Service1").build()); final Service service2 = serviceGenerator.ensureService(seed,
  //    owners, Service.builder().name("Service2").build());
  //
  //    final InfrastructureProvisioner provisioner =
  //        infrastructureProvisionerGenerator.ensureInfrastructureProvisioner(seed, owners,
  //            TerraformInfrastructureProvisioner.builder()
  //                .name("Test")
  //                .mappingBlueprints(asList(
  //                    InfrastructureMappingBlueprint.builder()
  //                        .serviceId(service1.getUuid())
  //                        .deploymentType(SSH)
  //                        .cloudProviderType(AWS)
  //                        .nodeFilteringType(AWS_INSTANCE_FILTER)
  //                        .properties(
  //                            singletonList(NameValuePair.builder().name("region").value("${terraform.region}").build()))
  //                        .build(),
  //                    InfrastructureMappingBlueprint.builder()
  //                        .serviceId(service2.getUuid())
  //                        .deploymentType(SSH)
  //                        .cloudProviderType(AWS)
  //                        .nodeFilteringType(AWS_INSTANCE_FILTER)
  //                        .properties(
  //                            singletonList(NameValuePair.builder().name("region").value("${terraform.region}").build()))
  //                        .build()))
  //                .build());
  //
  //    final PageResponse<InfrastructureProvisionerDetails> pageResponse =
  //    infrastructureProvisionerService.listDetails(
  //        aPageRequest().addFilter(InfrastructureProvisioner.APP_ID_KEY, Operator.EQ,
  //        provisioner.getAppId()).build());
  //    assertThat(pageResponse.size()).isEqualTo(1);
  //    InfrastructureProvisionerDetails details = pageResponse.get(0);
  //    assertThat(details.getName()).isEqualTo("Test");
  //    assertThat(details.getRepository()).isEqualTo("https://github.com/wings-software/terraform-test.git");
  //    assertThat(details.getInfrastructureProvisionerType()).isEqualTo("TERRAFORM");
  //    assertThat(details.getServices().size()).isEqualTo(2);
  //
  //    assertThat(details.getServices().keySet()).contains("Service1", "Service2");
  //    assertThat(details.getServices().values()).contains(service1.getUuid(), service2.getUuid());
  //  }
  //
  //  @Test
  //  public void shouldDelete() {
  //    Randomizer.Seed seed = Randomizer.seed();
  //    final InfrastructureProvisioner infrastructureProvisioner =
  //    infrastructureProvisionerGenerator.ensureRandom(seed);
  //    infrastructureProvisionerService.delete(infrastructureProvisioner.getAppId(),
  //    infrastructureProvisioner.getUuid()); final List<InfrastructureProvisioner> provisioners =
  //    infrastructureProvisionerService.list(aPageRequest().build()); assertThat(provisioners.size()).isEqualTo(0);
  //  }
  //
  //  @Test
  //  public void shouldNotDelete() {
  //    Randomizer.Seed seed = Randomizer.seed();
  //    final InfrastructureProvisioner infrastructureProvisioner =
  //    infrastructureProvisionerGenerator.ensureRandom(seed); final InfrastructureMapping infrastructureMapping =
  //        infrastructureMappingGenerator.ensureInfrastructureMapping(seed, null,
  //            anAwsInfrastructureMapping()
  //                .withInfraMappingType(AWS_SSH.name())
  //                .withProvisionerId(infrastructureProvisioner.getUuid())
  //                .build());
  //
  //    Workflow workflow = workflowGenerator.ensureWorkflow(seed, null,
  //        aWorkflow()
  //            .withName("basic")
  //            .withAppId(infrastructureMapping.getAppId())
  //            .withEnvId(infrastructureMapping.getEnvId())
  //            .withWorkflowType(WorkflowType.ORCHESTRATION)
  //            .withServiceId(infrastructureMapping.getServiceId())
  //            .withInfraMappingId(infrastructureMapping.getUuid())
  //            .withOrchestrationWorkflow(
  //                aBasicOrchestrationWorkflow()
  //                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
  //                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
  //                    .build())
  //            .build());
  //
  //    assertThatThrownBy(()
  //                           -> infrastructureProvisionerService.delete(
  //                               infrastructureProvisioner.getAppId(), infrastructureProvisioner.getUuid()));
  //  }
  //
  //  @Test
  //  public void testRegenerateInfrastructureMappings() {
  //    Randomizer.Seed seed = Randomizer.seed();
  //    final Owners owners = ownerManager.create();
  //
  //    final InfrastructureProvisioner infrastructureProvisioner =
  //        infrastructureProvisionerGenerator.ensurePredefined(seed, owners, TERRAFORM_TEST);
  //
  //    InfrastructureMapping infrastructureMapping =
  //        infrastructureMappingGenerator.ensureInfrastructureMapping(seed, owners,
  //            anAwsInfrastructureMapping()
  //                .withServiceId(infrastructureProvisioner.getMappingBlueprints().get(0).getServiceId())
  //                .withInfraMappingType(AWS_SSH.name())
  //                .withProvisionerId(infrastructureProvisioner.getUuid())
  //                .build());
  //
  //    ExecutionContext context = mock(ExecutionContext.class);
  //    when(context.getAppId()).thenReturn(infrastructureProvisioner.getAppId());
  //    when(context.asMap()).thenReturn(new HashMap<>());
  //
  //    Map<String, Object> outputs = new HashMap<>();
  //    outputs.put("region", "dummy-region");
  //
  //    assertThatThrownBy(()
  //                           -> infrastructureProvisionerService.regenerateInfrastructureMappings(
  //                               infrastructureProvisioner.getUuid(), context, outputs))
  //        .isInstanceOf(InvalidRequestException.class);
  //
  //    outputs.put("security_group", asList("dummy-securityGroups"));
  //    outputs.put("archive_tags", ImmutableMap.<String, Object>of("a", "b"));
  //    outputs.put("factory_tags", ImmutableMap.<String, Object>of("a", "b"));
  //    outputs.put("warehouse_tags", ImmutableMap.<String, Object>of("a", "b"));
  //    infrastructureProvisionerService.regenerateInfrastructureMappings(
  //        infrastructureProvisioner.getUuid(), context, outputs);
  //
  //    AwsInfrastructureMapping awsInfrastructureMapping = (AwsInfrastructureMapping) infrastructureMappingService.get(
  //        infrastructureMapping.getAppId(), infrastructureMapping.getUuid());
  //
  //    assertThat(awsInfrastructureMapping.getRegion()).isEqualTo("dummy-region");
  //  }
}
