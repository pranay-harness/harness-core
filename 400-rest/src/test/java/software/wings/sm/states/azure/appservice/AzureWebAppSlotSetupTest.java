package software.wings.sm.states.azure.appservice;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.context.ContextElementType.AZURE_WEBAPP_SETUP;
import static io.harness.rule.OwnerRule.ANIL;

import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.sm.states.azure.appservices.AzureWebAppSlotSetup.APP_SERVICE_SLOT_SETUP;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.harness.azure.model.AzureAppServiceConfiguration;
import io.harness.beans.DecryptableEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.EncryptedData;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.azure.registry.AzureRegistryType;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.task.azure.AzureTaskExecutionResponse;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureAppDeploymentData;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSlotSetupResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.tasks.ResponseData;

import software.wings.WingsBaseTest;
import software.wings.api.InstanceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureWebAppInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionData;
import software.wings.sm.states.ManagerExecutionLogCallback;
import software.wings.sm.states.azure.AzureSweepingOutputServiceHelper;
import software.wings.sm.states.azure.AzureVMSSStateHelper;
import software.wings.sm.states.azure.appservices.AzureAppServiceSlotSetupContextElement;
import software.wings.sm.states.azure.appservices.AzureAppServiceSlotSetupExecutionData;
import software.wings.sm.states.azure.appservices.AzureAppServiceSlotSetupExecutionSummary;
import software.wings.sm.states.azure.appservices.AzureAppServiceStateData;
import software.wings.sm.states.azure.appservices.AzureWebAppSlotSetup;
import software.wings.sm.states.azure.appservices.manifest.AzureAppServiceManifestUtils;
import software.wings.sm.states.azure.artifact.ArtifactStreamMapper;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

public class AzureWebAppSlotSetupTest extends WingsBaseTest {
  @Mock private DelegateService delegateService;
  @Mock private SecretManager secretManager;
  @Mock private AzureSweepingOutputServiceHelper azureSweepingOutputServiceHelper;
  @Spy @InjectMocks private AzureAppServiceManifestUtils azureAppServiceManifestUtils;
  @Spy @InjectMocks private AzureVMSSStateHelper azureVMSSStateHelper;
  @Spy @InjectMocks AzureWebAppSlotSetup state = new AzureWebAppSlotSetup("Slot Setup state");

  private final String ACTIVITY_ID = "activityId";
  private final String INFRA_ID = "infraId";

  private final String SUBSCRIPTION_ID = "subscriptionId";
  private final String RESOURCE_GROUP = "resourceGroup";
  private final String APP_NAME = "testWebApp";
  private final String DEPLOYMENT_SLOT = "deploymentSlot";
  private final String DEPLOYMENT_SLOT_ID = "testWebApp/deploymentSlot";
  private final String APP_SERVICE_PLAN_ID = "appService-plan-id";
  private final String HOST_NAME = "hostname";

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSlotSetupExecuteSuccess() {
    String appId = "appId";
    String serviceId = "serviceId";
    String envId = "envId";
    String activityId = "activityId";
    String delegateResult = "Done";
    String appServiceName = "app-service";
    String slotName = "stage";

    Application app = Application.Builder.anApplication().uuid(appId).build();
    Environment env = Environment.Builder.anEnvironment().uuid(envId).build();
    Service service = Service.builder().uuid(serviceId).build();
    Activity activity = Activity.builder().uuid(ACTIVITY_ID).build();

    AzureWebAppInfrastructureMapping azureWebAppInfrastructureMapping = getAzureWebAppInfraMapping();

    AzureConfig azureConfig = AzureConfig.builder().build();
    Artifact artifact = Artifact.Builder.anArtifact().build();
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();

    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    ManagerExecutionLogCallback managerExecutionLogCallback = mock(ManagerExecutionLogCallback.class);

    doReturn(appServiceName).when(context).renderExpression("${webapp}");
    doReturn(slotName).when(context).renderExpression("${slot}");

    AzureAppServiceStateData appServiceStateData = AzureAppServiceStateData.builder()
                                                       .application(app)
                                                       .environment(env)
                                                       .service(service)
                                                       .infrastructureMapping(azureWebAppInfrastructureMapping)
                                                       .resourceGroup("rg")
                                                       .subscriptionId("subId")
                                                       .azureConfig(azureConfig)
                                                       .artifact(artifact)
                                                       .azureEncryptedDataDetails(encryptedDataDetails)
                                                       .artifact(Artifact.Builder.anArtifact().build())
                                                       .currentUser(EmbeddedUser.builder().build())
                                                       .serviceId("serviceId")
                                                       .build();

    doReturn(appServiceStateData).when(azureVMSSStateHelper).populateAzureAppServiceData(context);

    doReturn(activity)
        .when(azureVMSSStateHelper)
        .createAndSaveActivity(any(), any(), anyString(), anyString(), any(), anyListOf(CommandUnit.class));
    doReturn(managerExecutionLogCallback).when(azureVMSSStateHelper).getExecutionLogCallback(activity);
    mockArtifactStreamMapper();
    doReturn(delegateResult).when(delegateService).queueTask(any());

    state.setSlotSteadyStateTimeout("10");
    AzureAppServiceConfiguration azureAppServiceConfiguration = new AzureAppServiceConfiguration();
    azureAppServiceConfiguration.setAppSettingsJSON(getAppSettingsJSON());
    azureAppServiceConfiguration.setConnStringsJSON(getConnStringJSON());
    doReturn("service-var-value").when(context).renderExpression("${serviceVariable.your_var_name}");
    doReturn("property-value").when(context).renderExpression("property-value");
    doReturn("https://harness.jfrog-ui.io").when(context).renderExpression("https://harness.jfrog-ui.io");
    doReturn("10").when(context).renderExpression("10");
    doReturn("var_name").when(context).renderExpression("${secrets.getValue(\\\"var_name\\\")");
    doReturn("Value2").when(context).renderExpression("Value2");
    doReturn("false").when(context).renderExpression("false");
    doReturn("jdbc:mysql://localhost/test").when(context).renderExpression("jdbc:mysql://localhost/test");
    doReturn("jdbc:sqlxxxxxxxx//INNOWAVE-99\\SQLEXPRESS01;databaseName=EDS")
        .when(context)
        .renderExpression("jdbc:sqlxxxxxxxx//INNOWAVE-99\\SQLEXPRESS01;databaseName=EDS");

    doReturn(azureAppServiceConfiguration).when(azureAppServiceManifestUtils).getAzureAppServiceConfiguration(any());
    doReturn(Collections.singletonList(EncryptedDataDetail.builder().build()))
        .when(secretManager)
        .getEncryptionDetails(any(), anyString(), anyString());
    doReturn(EncryptedData.builder().uuid("encrypted-data-uuid").build())
        .when(secretManager)
        .getSecretMappedToAppByName(anyString(), anyString(), anyString(), anyString());

    mockArtifactoryData(artifact, context);

    state.setAppService("${webapp}");
    state.setDeploymentSlot("${slot}");

    ExecutionResponse result = state.execute(context);

    assertThat(result).isNotNull();
    assertThat(result.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(result.getErrorMessage()).isNull();
    assertThat(result.getStateExecutionData()).isNotNull();
    assertThat(result.getStateExecutionData()).isInstanceOf(AzureAppServiceSlotSetupExecutionData.class);

    AzureAppServiceSlotSetupExecutionData stateExecutionData =
        (AzureAppServiceSlotSetupExecutionData) result.getStateExecutionData();
    assertThat(stateExecutionData.equals(new AzureAppServiceSlotSetupExecutionData())).isFalse();
    assertThat(stateExecutionData.getActivityId()).isEqualTo(activityId);
    assertThat(stateExecutionData.getAppServiceName()).isEqualTo(appServiceName);
    assertThat(stateExecutionData.getDeploySlotName()).isEqualTo(slotName);
    assertThat(stateExecutionData.getAppServiceSlotSetupTimeOut()).isNotNull();
    assertThat(stateExecutionData.getInfrastructureMappingId()).isEqualTo("infraId");

    AzureAppServiceSlotSetupExecutionSummary stepExecutionSummary = stateExecutionData.getStepExecutionSummary();
    assertThat(stepExecutionSummary.equals(AzureAppServiceSlotSetupExecutionSummary.builder().build())).isFalse();
    assertThat(stateExecutionData.getStepExecutionSummary().toString()).isNotNull();
    assertThat(stateExecutionData.getAppServiceName()).isEqualTo(appServiceName);
    assertThat(stateExecutionData.getDeploySlotName()).isEqualTo(slotName);

    assertThat(stateExecutionData.getExecutionDetails()).isNotEmpty();
    assertThat(stateExecutionData.getExecutionSummary()).isNotEmpty();
    assertThat(stateExecutionData.getStepExecutionSummary()).isNotNull();
  }

  private void mockArtifactoryData(Artifact artifact, ExecutionContextImpl context) {
    String accountId = "accountId";
    ArtifactoryConfig artifactoryConfig = ArtifactoryConfig.builder()
                                              .encryptedPassword("secret")
                                              .artifactoryUrl("artifactory-url")
                                              .username("username")
                                              .accountId(accountId)
                                              .build();
    SettingAttribute serverSetting = SettingAttribute.Builder.aSettingAttribute().withValue(artifactoryConfig).build();

    ArtifactStreamAttributes artifactStreamAttributes = ArtifactStreamAttributes.builder().build();
    artifactStreamAttributes.setArtifactStreamType(ArtifactStreamType.ARTIFACTORY.name());
    artifactStreamAttributes.setServerSetting(serverSetting);

    ArtifactStreamMapper artifactStreamMapper =
        ArtifactStreamMapper.getArtifactStreamMapper(artifact, artifactStreamAttributes);
    doReturn(artifactStreamMapper).when(azureVMSSStateHelper).getConnectorMapper(artifact);

    doReturn(Collections.singletonList(EncryptedDataDetail.builder().build()))
        .when(azureVMSSStateHelper)
        .getEncryptedDataDetails(context, artifactoryConfig);
  }

  private String getAppSettingsJSON() {
    return "[\n  {\n    \"name\": \"DOCKER_REGISTRY_SERVER_URL\",\n    \"value\": \"https://harness.jfrog-ui.io\",\n    \"slotSetting\": false\n  },\n  {\n    \"name\": \"DOCKER_REGISTRY_SERVER_USERNAME\",\n    \"value\": \"${serviceVariable.your_var_name}\",\n    \"slotSetting\": false\n  },\n  {\n    \"name\": \"Key1\",\n    \"value\": \"${secrets.getValue(\\\"var_name\\\")}\",\n    \"slotSetting\": true\n  },\n  {\n    \"name\": \"Key2\",\n    \"value\": \"Value2\",\n    \"slotSetting\": false\n  },\n  {\n    \"name\": \"WEBSITES_ENABLE_APP_SERVICE_STORAGE\",\n    \"value\": \"false\",\n    \"slotSetting\": false\n  }\n]";
  }

  private String getConnStringJSON() {
    return "[\n  {\n    \"name\": \"CONN_STRING_WITH_SECRET\",\n    \"value\": \"${secrets.getValue(\\\"var_name\\\")}\",\n    \"type\": \"Custom\",\n    \"slotSetting\": false\n  },\n  {\n    \"name\": \"MY_SQL_CONN_STRING\",\n    \"value\": \"jdbc:mysql://localhost/test\",\n    \"type\": \"MySql\",\n    \"slotSetting\": true\n  },\n  {\n    \"name\": \"SQL_SERVER_CONN_STRING\",\n    \"value\": \"jdbc:sqlxxxxxxxx//INNOWAVE-99\\\\SQLEXPRESS01;databaseName=EDS\",\n    \"type\": \"SQLServer\",\n    \"slotSetting\": true\n  }\n]";
  }

  private AzureWebAppInfrastructureMapping getAzureWebAppInfraMapping() {
    AzureWebAppInfrastructureMapping azureWebAppInfrastructureMapping = AzureWebAppInfrastructureMapping.builder()
                                                                            .resourceGroup(RESOURCE_GROUP)
                                                                            .subscriptionId(SUBSCRIPTION_ID)
                                                                            .build();
    azureWebAppInfrastructureMapping.setUuid(INFRA_ID);
    return azureWebAppInfrastructureMapping;
  }

  private void mockArtifactStreamMapper() {
    ArtifactStreamMapper mockArtifactStreamMapper = mockGetArtifactStreamMapper();
    mockGetAzureRegistryType(mockArtifactStreamMapper);
    ConnectorConfigDTO mockConnectorConfigDTO = mockGetConnectorConfigDTO(mockArtifactStreamMapper);
    mockGetConnectorDTOAuthCredentials(mockArtifactStreamMapper, mockConnectorConfigDTO);
    mockGetConnectorAuthEncryptedDataDetails();
  }

  private ArtifactStreamMapper mockGetArtifactStreamMapper() {
    ArtifactStreamMapper mockArtifactStreamMapper = mock(ArtifactStreamMapper.class);
    doReturn(mockArtifactStreamMapper).when(azureVMSSStateHelper).getConnectorMapper(any());
    return mockArtifactStreamMapper;
  }

  private void mockGetAzureRegistryType(ArtifactStreamMapper mockArtifactStreamMapper) {
    AzureRegistryType azureRegistryType = AzureRegistryType.DOCKER_HUB_PUBLIC;
    doReturn(azureRegistryType).when(mockArtifactStreamMapper).getAzureRegistryType();
  }

  private ConnectorConfigDTO mockGetConnectorConfigDTO(ArtifactStreamMapper mockArtifactStreamMapper) {
    ConnectorConfigDTO mockConnectorConfigDTO = mock(ConnectorConfigDTO.class);
    doReturn(mockConnectorConfigDTO).when(mockArtifactStreamMapper).getConnectorDTO();
    return mockConnectorConfigDTO;
  }

  private void mockGetConnectorDTOAuthCredentials(
      ArtifactStreamMapper mockArtifactStreamMapper, ConnectorConfigDTO mockConnectorConfigDTO) {
    DecryptableEntity mockDecryptableEntity = mock(DecryptableEntity.class);
    doReturn(Optional.of(mockDecryptableEntity))
        .when(mockArtifactStreamMapper)
        .getConnectorDTOAuthCredentials(mockConnectorConfigDTO);
  }

  private void mockGetConnectorAuthEncryptedDataDetails() {
    List<EncryptedDataDetail> encryptedDataDetailList = new ArrayList<>();
    EncryptedDataDetail mockEncryptedDataDetail = mock(EncryptedDataDetail.class);
    encryptedDataDetailList.add(mockEncryptedDataDetail);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSlotSetupExecuteFailure() {
    Activity activity = Activity.builder().uuid(ACTIVITY_ID).build();
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    ManagerExecutionLogCallback managerExecutionLogCallback = mock(ManagerExecutionLogCallback.class);

    doReturn(activity)
        .when(azureVMSSStateHelper)
        .createAndSaveActivity(any(), any(), anyString(), anyString(), any(), anyListOf(CommandUnit.class));
    doReturn(managerExecutionLogCallback).when(azureVMSSStateHelper).getExecutionLogCallback(activity);
    doThrow(Exception.class).when(azureVMSSStateHelper).populateAzureAppServiceData(eq(context));

    assertThatThrownBy(() -> state.execute(context)).isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSlotSetupHandleAsyncResponseSuccess() {
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    doNothing().when(azureVMSSStateHelper).updateActivityStatus(anyString(), anyString(), any());
    doReturn(SUCCESS).when(azureVMSSStateHelper).getAppServiceExecutionStatus(any());

    List<AzureAppDeploymentData> azureAppDeploymentData = getAzureAppDeploymentData();
    AzureWebAppInfrastructureMapping azureWebAppInfrastructureMapping = getAzureWebAppInfraMapping();
    Map<String, ResponseData> responseMap = getResponseDataMap(azureAppDeploymentData);
    AzureAppServiceSlotSetupExecutionData stateExecutionData = getStateExecutionData();
    List<InstanceElement> instanceElements = Collections.singletonList(
        anInstanceElement().uuid("uuid").hostName(HOST_NAME).displayName(HOST_NAME).newInstance(true).build());

    doReturn(stateExecutionData).when(context).getStateExecutionData();
    doReturn(azureWebAppInfrastructureMapping)
        .when(azureVMSSStateHelper)
        .getAzureWebAppInfrastructureMapping(any(), any());

    doReturn(instanceElements)
        .when(azureSweepingOutputServiceHelper)
        .generateAzureAppInstanceElements(any(), any(), any());

    ArgumentCaptor<AzureAppServiceSlotSetupContextElement> contextElementArgumentCaptor =
        ArgumentCaptor.forClass(AzureAppServiceSlotSetupContextElement.class);

    ExecutionResponse executionResponse = state.handleAsyncResponse(context, responseMap);

    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(SUCCESS);
    verifySetupContextElement(context, contextElementArgumentCaptor);
    verifyContextAndNotifyElements(executionResponse);
    verifyPostStateExecutionData(executionResponse);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSlotSetupHandleAsyncResponseFailure() {
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    doNothing().when(azureVMSSStateHelper).updateActivityStatus(anyString(), anyString(), any());
    doReturn(FAILED).when(azureVMSSStateHelper).getAppServiceExecutionStatus(any());
    doReturn(getStateExecutionData()).when(context).getStateExecutionData();
    ArgumentCaptor<AzureAppServiceSlotSetupContextElement> contextElementArgumentCaptor =
        ArgumentCaptor.forClass(AzureAppServiceSlotSetupContextElement.class);

    ExecutionResponse executionResponse =
        state.handleAsyncResponse(context, getResponseDataMap(getAzureAppDeploymentData()));

    assertThat(executionResponse.getExecutionStatus()).isEqualTo(FAILED);
    verifySetupContextElement(context, contextElementArgumentCaptor);
  }

  @NotNull
  private List<AzureAppDeploymentData> getAzureAppDeploymentData() {
    return Collections.singletonList(AzureAppDeploymentData.builder()
                                         .subscriptionId(SUBSCRIPTION_ID)
                                         .resourceGroup(RESOURCE_GROUP)
                                         .appName(APP_NAME)
                                         .deploySlot(DEPLOYMENT_SLOT)
                                         .deploySlotId(DEPLOYMENT_SLOT_ID)
                                         .appServicePlanId(APP_SERVICE_PLAN_ID)
                                         .hostName(HOST_NAME)
                                         .build());
  }

  @NotNull
  private ImmutableMap<String, ResponseData> getResponseDataMap(List<AzureAppDeploymentData> azureAppDeploymentData) {
    return ImmutableMap.of(ACTIVITY_ID,
        AzureTaskExecutionResponse.builder()
            .azureTaskResponse(AzureWebAppSlotSetupResponse.builder()
                                   .azureAppDeploymentData(azureAppDeploymentData)
                                   .preDeploymentData(AzureAppServicePreDeploymentData.builder()
                                                          .appName(APP_NAME)
                                                          .appSettingsToAdd(Collections.emptyMap())
                                                          .connStringsToAdd(Collections.emptyMap())
                                                          .slotName(DEPLOYMENT_SLOT)
                                                          .build())
                                   .build())
            .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
            .build());
  }

  private AzureAppServiceSlotSetupExecutionData getStateExecutionData() {
    return AzureAppServiceSlotSetupExecutionData.builder()
        .infrastructureMappingId(INFRA_ID)
        .deploySlotName(DEPLOYMENT_SLOT)
        .appServiceName(APP_NAME)
        .activityId(ACTIVITY_ID)
        .build();
  }

  private void verifySetupContextElement(ExecutionContextImpl context,
      ArgumentCaptor<AzureAppServiceSlotSetupContextElement> contextElementArgumentCaptor) {
    verify(azureSweepingOutputServiceHelper).saveToSweepingOutPut(contextElementArgumentCaptor.capture(), any(), any());
    AzureAppServiceSlotSetupContextElement setupContextElement = contextElementArgumentCaptor.getValue();
    assertThat(setupContextElement).isNotNull();
    assertThat(setupContextElement.equals(new AzureAppServiceSlotSetupContextElement())).isFalse();
    assertThat(setupContextElement.getCommandName()).isEqualTo(APP_SERVICE_SLOT_SETUP);
    assertThat(setupContextElement.getWebApp()).isEqualTo(APP_NAME);
    assertThat(setupContextElement.getDeploymentSlot()).isEqualTo(DEPLOYMENT_SLOT);
    assertThat(setupContextElement.getInfraMappingId()).isEqualTo(INFRA_ID);
    assertThat(setupContextElement.getPreDeploymentData()).isNotNull();

    assertThat(setupContextElement.getName()).isNull();
    assertThat(setupContextElement.getUuid()).isNull();
    assertThat(setupContextElement.cloneMin()).isNull();
    assertThat(setupContextElement.toString()).isNotNull();
    assertThat(setupContextElement.getElementType()).isEqualTo(AZURE_WEBAPP_SETUP);
    assertThat(setupContextElement.paramMap(context)).isNotEmpty();
  }

  private void verifyContextAndNotifyElements(ExecutionResponse executionResponse) {
    List<ContextElement> notifyElements = executionResponse.getNotifyElements();
    assertThat(notifyElements).isNotNull();
    assertThat(notifyElements.size()).isEqualTo(1);

    ContextElement contextElement = notifyElements.get(0);
    assertThat(contextElement).isNotNull();
    assertThat(contextElement instanceof InstanceElementListParam).isTrue();

    ContextElement instanceElementParam = executionResponse.getContextElements().get(0);
    assertThat(instanceElementParam instanceof InstanceElementListParam).isTrue();
  }

  private void verifyPostStateExecutionData(ExecutionResponse executionResponse) {
    StateExecutionData stateExecutionData = executionResponse.getStateExecutionData();
    assertThat(stateExecutionData instanceof AzureAppServiceSlotSetupExecutionData).isTrue();
    AzureAppServiceSlotSetupExecutionData executionData = (AzureAppServiceSlotSetupExecutionData) stateExecutionData;
    assertThat(executionData.getNewInstanceStatusSummaries().size()).isEqualTo(1);
  }
}
