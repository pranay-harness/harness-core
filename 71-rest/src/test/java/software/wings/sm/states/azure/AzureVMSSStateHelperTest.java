package software.wings.sm.states.azure;

import static io.harness.beans.OrchestrationWorkflowType.BLUE_GREEN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.beans.DecryptableEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.WingsBaseTest;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureVMSSInfrastructureMapping;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails;
import software.wings.beans.command.ServiceCommand;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.NGSecretService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.WorkflowStandardParams;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;

public class AzureVMSSStateHelperTest extends WingsBaseTest {
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ActivityService activityService;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private SettingsService settingsService;
  @Mock private SecretManager secretManager;
  @Mock private LogService logService;
  @Mock private NGSecretService ngSecretService;

  @Spy @Inject @InjectMocks AzureVMSSStateHelper azureVMSSStateHelper;

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testIsBlueGreenWorkflow() {
    ExecutionContext context = Mockito.mock(ExecutionContextImpl.class);

    when(context.getOrchestrationWorkflowType()).thenReturn(BLUE_GREEN);

    boolean isBlueGreen = azureVMSSStateHelper.isBlueGreenWorkflow(context);

    assertThat(isBlueGreen).isEqualTo(true);
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testGetArtifact() {
    String serviceId = "serviceId";
    DeploymentExecutionContext context = Mockito.mock(DeploymentExecutionContext.class);
    Artifact artifact = Mockito.mock(Artifact.class);

    when(context.getDefaultArtifactForService(serviceId)).thenReturn(artifact);

    Artifact result = azureVMSSStateHelper.getArtifact(context, serviceId);

    assertThat(result).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testGetArtifactWithException() {
    String serviceId = "serviceId";
    DeploymentExecutionContext context = Mockito.mock(DeploymentExecutionContext.class);

    when(context.getDefaultArtifactForService(serviceId)).thenReturn(null);

    assertThatThrownBy(() -> azureVMSSStateHelper.getArtifact(context, serviceId))
        .hasMessage("Unable to find artifact for service id: serviceId")
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testGetServiceByAppId() {
    String serviceId = "serviceId";
    String appId = "applicationId";
    DeploymentExecutionContext context = Mockito.mock(DeploymentExecutionContext.class);
    PhaseElement phaseElement = Mockito.mock(PhaseElement.class);
    Service service = Service.builder().uuid(serviceId).build();

    when(phaseElement.getServiceElement()).thenReturn(ServiceElement.builder().uuid(serviceId).build());
    when(context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM)).thenReturn(phaseElement);
    doReturn(service).when(serviceResourceService).getWithDetails(appId, serviceId);

    ArgumentCaptor<String> appIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> serviceIdCaptor = ArgumentCaptor.forClass(String.class);

    Service result = azureVMSSStateHelper.getServiceByAppId(context, appId);

    verify(serviceResourceService).getWithDetails(appIdCaptor.capture(), serviceIdCaptor.capture());
    assertThat(appId).isEqualTo(appIdCaptor.getValue());
    assertThat(serviceId).isEqualTo(serviceIdCaptor.getValue());
    assertThat(result).isNotNull();
    assertThat(result.getUuid()).isEqualTo(serviceId);
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testGetWorkflowStandardParams() {
    ExecutionContext context = Mockito.mock(ExecutionContextImpl.class);
    WorkflowStandardParams workflowStandardParams = Mockito.mock(WorkflowStandardParams.class);

    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);

    WorkflowStandardParams result = azureVMSSStateHelper.getWorkflowStandardParams(context);

    assertThat(result).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testGetWorkflowStandardParamsWithException() {
    ExecutionContext context = Mockito.mock(ExecutionContextImpl.class);

    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(null);

    assertThatThrownBy(() -> azureVMSSStateHelper.getWorkflowStandardParams(context))
        .hasMessage("WorkflowStandardParams can't be null or empty, accountId: null")
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testGetApplication() {
    String appId = "applicationId";
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    WorkflowStandardParams workflowStandardParams = Mockito.mock(WorkflowStandardParams.class);

    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);
    when(workflowStandardParams.getApp()).thenReturn(Application.Builder.anApplication().uuid(appId).build());

    Application result = azureVMSSStateHelper.getApplication(context);

    assertThat(result).isNotNull();
    assertThat(result.getUuid()).isEqualTo(appId);
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testGetApplicationWithException() {
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);

    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(null);

    assertThatThrownBy(() -> azureVMSSStateHelper.getApplication(context))
        .hasMessage("WorkflowStandardParams can't be null or empty, accountId: null")
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testGetApplicationWithExceptionWhenAppIsNull() {
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    WorkflowStandardParams workflowStandardParams = Mockito.mock(WorkflowStandardParams.class);

    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);
    when(workflowStandardParams.getApp()).thenReturn(null);

    assertThatThrownBy(() -> azureVMSSStateHelper.getApplication(context))
        .hasMessage("Application can't be null or empty, accountId: null")
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testGetEnvironment() {
    String envId = "environmentId";
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    WorkflowStandardParams workflowStandardParams = Mockito.mock(WorkflowStandardParams.class);

    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);
    when(workflowStandardParams.getEnv()).thenReturn(Environment.Builder.anEnvironment().uuid(envId).build());

    Environment result = azureVMSSStateHelper.getEnvironment(context);

    assertThat(result).isNotNull();
    assertThat(result.getUuid()).isEqualTo(envId);
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testGetEnvironmentWithException() {
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);

    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(null);

    assertThatThrownBy(() -> azureVMSSStateHelper.getEnvironment(context))
        .hasMessage("WorkflowStandardParams can't be null or empty, accountId: null")
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testGetEnvironmentWithExceptionWhenAppIsNull() {
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    WorkflowStandardParams workflowStandardParams = Mockito.mock(WorkflowStandardParams.class);

    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);
    when(workflowStandardParams.getApp()).thenReturn(null);

    assertThatThrownBy(() -> azureVMSSStateHelper.getEnvironment(context))
        .hasMessage("Env can't be null or empty, accountId: null")
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testGetCommand() {
    String appId = "appId";
    String serviceId = "serviceId";
    String envId = "envId";
    String commandName = "commandName";
    Command command = Command.Builder.aCommand().withName(commandName).build();
    ServiceCommand serviceCommand = ServiceCommand.Builder.aServiceCommand().withCommand(command).build();

    when(serviceResourceService.getCommandByName(appId, serviceId, envId, commandName)).thenReturn(serviceCommand);

    Command result = azureVMSSStateHelper.getCommand(appId, serviceId, envId, commandName);

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo(commandName);
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testGetCommandUnit() {
    String appId = "appId";
    String serviceId = "serviceId";
    String envId = "envId";
    String commandName = "commandName";
    Command command = Command.Builder.aCommand().withName(commandName).build();
    List<CommandUnit> commandUnitList = new ArrayList<>();
    commandUnitList.add(command);

    when(serviceResourceService.getFlattenCommandUnitList(appId, serviceId, envId, commandName))
        .thenReturn(commandUnitList);

    List<CommandUnit> result = azureVMSSStateHelper.getCommandUnitList(appId, serviceId, envId, commandName);

    assertThat(result).isNotNull();
    assertThat(result.get(0).getName()).isEqualTo(commandName);
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testRenderExpressionOrGetDefault() {
    String expr = "2";
    int defaultValue = 1;
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);

    when(context.renderExpression(expr)).thenReturn(expr);

    int result = azureVMSSStateHelper.renderExpressionOrGetDefault(expr, context, defaultValue);

    assertThat(result).isEqualTo(2);
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testRenderExpressionOrGetDefaultWithDefaultValue() {
    String expr = "two";
    int defaultValue = 1;
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);

    when(context.renderExpression(expr)).thenReturn(expr);

    int result = azureVMSSStateHelper.renderExpressionOrGetDefault(expr, context, defaultValue);

    assertThat(result).isEqualTo(defaultValue);
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testRenderTimeoutExpressionOrGetDefault() {
    String expr = "-2";
    int defaultValue = 1;
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);

    when(context.renderExpression(expr)).thenReturn(expr);

    int result = azureVMSSStateHelper.renderTimeoutExpressionOrGetDefault(expr, context, defaultValue);

    assertThat(result).isEqualTo(defaultValue);
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testFixNamePrefix() {
    String name = "name";
    String appName = "appName";
    String serviceName = "serviceName";
    String envName = "envName";
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);

    when(context.renderExpression(name)).thenReturn(name);

    String result = azureVMSSStateHelper.fixNamePrefix(context, name, appName, serviceName, envName);

    assertThat(result).isEqualTo(name);
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testFixNamePrefixWithNameNull() {
    String appName = "appName";
    String serviceName = "serviceName";
    String envName = "envName";
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);

    String result = azureVMSSStateHelper.fixNamePrefix(context, null, appName, serviceName, envName);

    assertThat(result).isEqualTo("appName__serviceName__envName");
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testGetAzureVMSSInfrastructureMapping() {
    String infraMappingId = "infraMappingId";
    String appId = "appId";
    InfrastructureMapping infrastructureMapping = AzureVMSSInfrastructureMapping.builder().build();

    when(infrastructureMappingService.get(appId, infraMappingId)).thenReturn(infrastructureMapping);

    AzureVMSSInfrastructureMapping result =
        azureVMSSStateHelper.getAzureVMSSInfrastructureMapping(infraMappingId, appId);

    assertThat(result).isNotNull();
    assertThat(result).isInstanceOf(AzureVMSSInfrastructureMapping.class);
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testGetAzureConfig() {
    String infraMappingId = "infraMappingId";
    String appId = "appId";
    String computeProviderSettingId = "computeProviderSettingId";
    String accountId = "accountId";
    SettingAttribute settingAttribute = mock(SettingAttribute.class);
    InfrastructureMapping infrastructureMapping = mock(InfrastructureMapping.class);

    when(infrastructureMappingService.get(appId, infraMappingId)).thenReturn(infrastructureMapping);
    when(infrastructureMapping.getComputeProviderSettingId()).thenReturn(computeProviderSettingId);
    when(settingsService.get(computeProviderSettingId)).thenReturn(settingAttribute);
    when(settingAttribute.getValue()).thenReturn(AzureConfig.builder().accountId(accountId).build());

    AzureConfig result = azureVMSSStateHelper.getAzureConfig(infrastructureMapping.getComputeProviderSettingId());

    assertThat(result).isNotNull();
    assertThat(result).isInstanceOf(AzureConfig.class);
    assertThat(result.getAccountId()).isEqualTo(accountId);
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testGetAzureVMSSStateTimeoutFromContext() {
    int autoScalingSteadyStateVMSSTimeout = 2;
    AzureVMSSSetupContextElement azureVMSSSetupContextElement =
        AzureVMSSSetupContextElement.builder()
            .autoScalingSteadyStateVMSSTimeout(autoScalingSteadyStateVMSSTimeout)
            .build();
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);

    when(context.getContextElement(ContextElementType.AZURE_VMSS_SETUP)).thenReturn(azureVMSSSetupContextElement);

    Integer result = azureVMSSStateHelper.getAzureVMSSStateTimeoutFromContext(context);

    assertThat(result).isNotNull();
    assertThat(result.intValue()).isEqualTo(2 * 60 * 1000);
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testGetAzureVMSSStateTimeoutFromContextWithNegativeTimeout() {
    int autoScalingSteadyStateVMSSTimeout = -1;
    AzureVMSSSetupContextElement azureVMSSSetupContextElement =
        AzureVMSSSetupContextElement.builder()
            .autoScalingSteadyStateVMSSTimeout(autoScalingSteadyStateVMSSTimeout)
            .build();
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);

    when(context.getContextElement(ContextElementType.AZURE_VMSS_SETUP)).thenReturn(azureVMSSSetupContextElement);

    Integer result = azureVMSSStateHelper.getAzureVMSSStateTimeoutFromContext(context);

    assertThat(result).isNull();
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testBuildActivity() {
    String commandName = "commandName";
    String appId = "appId";
    String appName = "appName";
    String serviceId = "serviceId";
    String userName = "userName";
    String userEmail = "test@email.com";
    String envId = "envId";
    String commandType = "commandType";
    String activityId = "activityId";
    CommandUnitDetails.CommandUnitType commandUnitType = CommandUnitDetails.CommandUnitType.AZURE_VMSS_SETUP;
    Application app = Application.Builder.anApplication().uuid(appId).name(appName).build();
    Environment env = Environment.Builder.anEnvironment().uuid(envId).build();
    Service service = Service.builder().uuid(serviceId).build();
    Activity activity = Activity.builder().uuid(activityId).build();
    EmbeddedUser embeddedUser = EmbeddedUser.builder().email(userEmail).name(userName).build();
    Command command = Command.Builder.aCommand().withName(commandName).build();
    List<CommandUnit> commandUnitList = new ArrayList<>();
    commandUnitList.add(command);

    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    PhaseElement phaseElement = Mockito.mock(PhaseElement.class);
    WorkflowStandardParams workflowStandardParams = Mockito.mock(WorkflowStandardParams.class);
    doReturn(workflowStandardParams).when(context).getContextElement(ContextElementType.STANDARD);
    doReturn(app).when(workflowStandardParams).getApp();
    doReturn(env).when(workflowStandardParams).getEnv();
    when(phaseElement.getServiceElement()).thenReturn(ServiceElement.builder().uuid(serviceId).build());
    doReturn(phaseElement).when(context).getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    doReturn(service).when(serviceResourceService).getWithDetails(appId, serviceId);
    doReturn(activity).when(activityService).save(any());
    doReturn(embeddedUser).when(workflowStandardParams).getCurrentUser();

    Activity result = azureVMSSStateHelper.createAndSaveActivity(
        context, null, commandName, commandType, commandUnitType, commandUnitList);

    assertThat(result).isNotNull();
    assertThat(result.getUuid()).isEqualTo(activityId);
    assertThat(result.getStatus()).isEqualTo(ExecutionStatus.RUNNING);
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testGetConnectorAuthEncryptedDataDetails() {
    DecryptableEntity mockDecryptableEntity = mock(DecryptableEntity.class);
    doReturn(Collections.emptyList()).when(ngSecretService).getEncryptionDetails(any(), any());

    List<EncryptedDataDetail> connectorAuthEncryptedDataDetails =
        azureVMSSStateHelper.getConnectorAuthEncryptedDataDetails("accountId", mockDecryptableEntity);

    assertThat(connectorAuthEncryptedDataDetails).isNotNull();
  }
}
