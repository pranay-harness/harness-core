package io.harness.functional.helm;

import static io.harness.k8s.model.HelmVersion.V2;
import static io.harness.k8s.model.HelmVersion.V3;
import static io.harness.rule.OwnerRule.ABOSII;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.CDFunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.utils.HelmHelper;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.InfrastructureDefinitionGenerator;
import io.harness.generator.InfrastructureDefinitionGenerator.InfrastructureDefinitions;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.SettingGenerator;
import io.harness.generator.SettingGenerator.Settings;
import io.harness.k8s.model.HelmVersion;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.HelmChartConfig;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.ApplicationManifestService;

@Slf4j
public class HelmFunctionalTest extends AbstractFunctionalTest {
  private static final String WORKFLOW_CLOUD_STORAGE_NAME = "Helm%s %s Deployment";
  private static final String SERVICE_CLOUD_STORAGE_NAME = "Helm%s %s Service";
  private static final String CHART_NAME = "harness-todolist";
  private static final String HELM_V2_BASE_PATH = "helmv2/charts";
  private static final String HELM_V3_BASE_PATH = "helmv3/charts";

  @Inject private OwnerManager ownerManager;
  @Inject private SettingGenerator settingGenerator;
  @Inject private InfrastructureDefinitionGenerator infrastructureDefinitionGenerator;
  @Inject private ApplicationManifestService applicationManifestService;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private HelmHelper helmHelper;

  private Owners owners;
  private InfrastructureDefinition infrastructureDefinition;
  private Workflow workflow;

  private final Seed seed = new Seed(0);

  @Before
  public void setUp() {
    owners = ownerManager.create();
    owners.obtainApplication(
        () -> applicationGenerator.ensurePredefined(seed, owners, ApplicationGenerator.Applications.GENERIC_TEST));
    infrastructureDefinition =
        infrastructureDefinitionGenerator.ensurePredefined(seed, owners, InfrastructureDefinitions.GCP_HELM);
    log.info("Ensured Infra def");
    resetCache(owners.obtainAccount().getUuid());
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(CDFunctionalTests.class)
  public void testHelmV2S3WorkflowExecution() {
    String serviceName = format(SERVICE_CLOUD_STORAGE_NAME, V2, "S3");
    String workflowName = format(WORKFLOW_CLOUD_STORAGE_NAME, V2, "S3");
    Service service = createHelmCloudStorageService(serviceName, V2, Settings.HELM_S3_CONNECTOR);
    testHelmWorkflowExecution(service, workflowName);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(CDFunctionalTests.class)
  public void testHelmV3S3WorkflowExecution() {
    String serviceName = format(SERVICE_CLOUD_STORAGE_NAME, V3, "S3");
    String workflowName = format(WORKFLOW_CLOUD_STORAGE_NAME, V3, "S3");
    Service service = createHelmCloudStorageService(serviceName, V3, Settings.HELM_S3_CONNECTOR);
    testHelmWorkflowExecution(service, workflowName);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(CDFunctionalTests.class)
  public void testHelmV2GCSWorkflowExecution() {
    String serviceName = format(SERVICE_CLOUD_STORAGE_NAME, V2, "GCS");
    String workflowName = format(WORKFLOW_CLOUD_STORAGE_NAME, V2, "GCS");
    Service service = createHelmCloudStorageService(serviceName, V2, Settings.HELM_GCS_CONNECTOR);
    testHelmWorkflowExecution(service, workflowName);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(CDFunctionalTests.class)
  public void testHelmV3GCSWorkflowExecution() {
    String serviceName = format(SERVICE_CLOUD_STORAGE_NAME, V3, "GCS");
    String workflowName = format(WORKFLOW_CLOUD_STORAGE_NAME, V3, "GCS");
    Service service = createHelmCloudStorageService(serviceName, V3, Settings.HELM_GCS_CONNECTOR);
    testHelmWorkflowExecution(service, workflowName);
  }

  private void testHelmWorkflowExecution(Service service, String workflowName) {
    addValuesYamlToService(service);
    log.info("Added values.yaml to service");
    workflow = helmHelper.createHelmWorkflow(seed, owners, workflowName, service, infrastructureDefinition);
    log.info("Workflow created");

    resetCache(owners.obtainAccount().getUuid());
    ExecutionArgs executionArgs = getExecutionArgs("functional-tests");

    WorkflowExecution workflowExecution =
        runWorkflow(bearerToken, service.getAppId(), infrastructureDefinition.getEnvId(), executionArgs);

    logStateExecutionInstanceErrors(workflowExecution);
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  private Service createHelmCloudStorageService(String serviceName, HelmVersion helmVersion, Settings connector) {
    SettingAttribute helmGCSConnector = settingGenerator.ensurePredefined(seed, owners, connector);

    HelmChartConfig helmChartConfig = HelmChartConfig.builder()
                                          .connectorId(helmGCSConnector.getUuid())
                                          .chartName(CHART_NAME)
                                          .basePath(V3 == helmVersion ? HELM_V3_BASE_PATH : HELM_V2_BASE_PATH)
                                          .build();

    return helmHelper.createHelmService(seed, owners, serviceName, helmVersion, helmChartConfig, null);
  }

  private void addValuesYamlToService(Service helmS3Service) {
    ApplicationManifest existingAppManifest = applicationManifestService.getAppManifest(
        helmS3Service.getAppId(), null, helmS3Service.getUuid(), AppManifestKind.VALUES);
    ApplicationManifest applicationManifest = ApplicationManifest.builder()
                                                  .kind(AppManifestKind.VALUES)
                                                  .serviceId(helmS3Service.getUuid())
                                                  .storeType(StoreType.Local)
                                                  .build();
    applicationManifest.setAppId(helmS3Service.getAppId());
    if (existingAppManifest == null) {
      applicationManifest = applicationManifestService.create(applicationManifest);
    } else {
      applicationManifest.setUuid(existingAppManifest.getUuid());
      applicationManifest = applicationManifestService.update(applicationManifest);
    }
    ManifestFile manifestFile = ManifestFile.builder()
                                    .fileContent("serviceName: ${workflow.variables.serviceName}")
                                    .applicationManifestId(applicationManifest.getUuid())
                                    .fileName("values.yaml")
                                    .build();
    manifestFile.setAppId(helmS3Service.getAppId());
    applicationManifestService.deleteAllManifestFilesByAppManifestId(
        applicationManifest.getAppId(), applicationManifest.getUuid());
    applicationManifestService.upsertApplicationManifestFile(manifestFile, applicationManifest, true);
  }

  @NotNull
  private ExecutionArgs getExecutionArgs(String serviceName) {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setOrchestrationId(workflow.getUuid());
    executionArgs.setWorkflowType(workflow.getWorkflowType());
    executionArgs.setWorkflowVariables(ImmutableMap.of("serviceName", serviceName));
    return executionArgs;
  }
}
