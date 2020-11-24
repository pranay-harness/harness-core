package software.wings.sm.states.azure.appservices;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.command.CommandUnitDetails.CommandUnitType.AZURE_APP_SERVICE_SLOT_SETUP;
import static software.wings.sm.StateType.AZURE_WEBAPP_SLOT_SETUP;

import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.azure.model.AzureConstants;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.azure.registry.AzureRegistryType;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.task.azure.AzureTaskExecutionResponse;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppSlotSetupParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSlotSetupResponse;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.Activity;
import software.wings.beans.command.AzureVMSSDummyCommandUnit;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.service.impl.azure.manager.AzureTaskExecutionRequest;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateExecutionData;
import software.wings.sm.states.azure.artifact.ArtifactStreamMapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class AzureWebAppSlotSetup extends AbstractAzureAppServiceState {
  @Getter @Setter private String slotSteadyStateTimeout;
  @Getter @Setter private List<AzureAppServiceApplicationSetting> applicationSettings;
  @Getter @Setter private List<AzureAppServiceConnectionString> appServiceConnectionStrings;
  @Getter @Setter private String appServiceSlotSetupTimeOut;
  public static final String APP_SERVICE_SLOT_SETUP = "App Service Slot Setup";

  public AzureWebAppSlotSetup(String name) {
    super(name, AZURE_WEBAPP_SLOT_SETUP);
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    // Do nothing on abort
  }

  @Override
  @SchemaIgnore
  public Integer getTimeoutMillis(ExecutionContext context) {
    return getTimeOut(context);
  }

  @NotNull
  private Integer getTimeOut(ExecutionContext context) {
    return azureVMSSStateHelper.renderExpressionOrGetDefault(
        slotSteadyStateTimeout, context, AzureConstants.DEFAULT_AZURE_VMSS_TIMEOUT_MIN);
  }

  @Override
  protected boolean shouldExecute(ExecutionContext context) {
    // setup is first step and hence should always be run
    return true;
  }

  @Override
  protected AzureTaskExecutionRequest buildTaskExecutionRequest(
      ExecutionContext context, AzureAppServiceStateData azureAppServiceStateData, Activity activity) {
    AzureWebAppSlotSetupParameters slotSetupParameters =
        buildSlotSetupParams(context, azureAppServiceStateData, activity);

    return AzureTaskExecutionRequest.builder()
        .azureConfigDTO(azureVMSSStateHelper.createAzureConfigDTO(azureAppServiceStateData.getAzureConfig()))
        .azureConfigEncryptionDetails(azureAppServiceStateData.getAzureEncryptedDataDetails())
        .azureTaskParameters(slotSetupParameters)
        .build();
  }

  @Override
  protected StateExecutionData buildPreStateExecutionData(
      Activity activity, ExecutionContext context, AzureAppServiceStateData azureAppServiceStateData) {
    return AzureAppServiceSlotSetupExecutionData.builder()
        .activityId(activity.getUuid())
        .appServiceName(azureAppServiceStateData.getAppService())
        .deploySlotName(azureAppServiceStateData.getDeploymentSlot())
        .infrastructureMappingId(azureAppServiceStateData.getInfrastructureMapping().getUuid())
        .build();
  }

  @Override
  protected StateExecutionData buildPostStateExecutionData(
      ExecutionContext context, AzureTaskExecutionResponse executionResponse, ExecutionStatus executionStatus) {
    AzureWebAppSlotSetupResponse slotSetupTaskResponse =
        (AzureWebAppSlotSetupResponse) executionResponse.getAzureTaskResponse();

    AzureAppServiceSlotSetupExecutionData stateExecutionData = context.getStateExecutionData();
    stateExecutionData.setStatus(executionStatus);
    stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());
    stateExecutionData.setDelegateMetaInfo(executionResponse.getDelegateMetaInfo());
    stateExecutionData.setAppServiceName(slotSetupTaskResponse.getPreDeploymentData().getAppName());
    stateExecutionData.setDeploySlotName(slotSetupTaskResponse.getPreDeploymentData().getSlotName());
    return stateExecutionData;
  }

  @Override
  protected ContextElement buildContextElement(ExecutionContext context, AzureTaskExecutionResponse executionResponse) {
    AzureWebAppSlotSetupResponse slotSetupTaskResponse =
        (AzureWebAppSlotSetupResponse) executionResponse.getAzureTaskResponse();
    AzureAppServiceSlotSetupExecutionData stateExecutionData = context.getStateExecutionData();
    AzureAppServicePreDeploymentData preDeploymentData = slotSetupTaskResponse.getPreDeploymentData();

    return AzureAppServiceSlotSetupContextElement.builder()
        .infraMappingId(stateExecutionData.getInfrastructureMappingId())
        .appServiceSlotSetupTimeOut(getTimeOut(context))
        .commandName(APP_SERVICE_SLOT_SETUP)
        .webApp(preDeploymentData.getAppName())
        .deploymentSlot(preDeploymentData.getSlotName())
        .preDeploymentData(preDeploymentData)
        .build();
  }

  @Override
  protected String commandType() {
    return APP_SERVICE_SLOT_SETUP;
  }

  @NotNull
  @Override
  protected CommandUnitType commandUnitType() {
    return AZURE_APP_SERVICE_SLOT_SETUP;
  }

  @Override
  protected List<CommandUnit> commandUnits() {
    return ImmutableList.of(new AzureVMSSDummyCommandUnit(AzureConstants.STOP_DEPLOYMENT_SLOT),
        new AzureVMSSDummyCommandUnit(AzureConstants.UPDATE_DEPLOYMENT_SLOT_CONFIGURATION_SETTINGS),
        new AzureVMSSDummyCommandUnit(AzureConstants.UPDATE_DEPLOYMENT_SLOT_CONTAINER_SETTINGS),
        new AzureVMSSDummyCommandUnit(AzureConstants.START_DEPLOYMENT_SLOT));
  }

  @NotNull
  @Override
  protected String errorMessageTag() {
    return "Azure App Service slot setup failed";
  }

  @Override
  protected String skipMessage() {
    return "No Azure App service setup context element found. Skipping slot setup";
  }

  private AzureWebAppSlotSetupParameters buildSlotSetupParams(
      ExecutionContext context, AzureAppServiceStateData azureAppServiceStateData, Activity activity) {
    Map<String, AzureAppServiceApplicationSetting> applicationSettingMap = new HashMap<>();
    Map<String, AzureAppServiceConnectionString> connectionStringMap = new HashMap<>();

    if (isNotEmpty(applicationSettings)) {
      applicationSettingMap = applicationSettings.stream().collect(
          Collectors.toMap(AzureAppServiceApplicationSetting::getName, setting -> setting));
    }
    if (isNotEmpty(appServiceConnectionStrings)) {
      connectionStringMap = appServiceConnectionStrings.stream().collect(
          Collectors.toMap(AzureAppServiceConnectionString::getName, setting -> setting));
    }

    ArtifactStreamMapper artifactStreamMapper =
        azureVMSSStateHelper.getConnectorMapper(azureAppServiceStateData.getArtifact());
    AzureRegistryType azureRegistryType = artifactStreamMapper.getAzureRegistryType();
    ConnectorConfigDTO connectorConfigDTO = artifactStreamMapper.getConnectorDTO();

    List<EncryptedDataDetail> encryptedDataDetails =
        artifactStreamMapper.getConnectorDTOAuthCredentials(connectorConfigDTO)
            .map(connectorAuthCredentials
                -> azureVMSSStateHelper.getConnectorAuthEncryptedDataDetails(
                    context.getAccountId(), connectorAuthCredentials))
            .orElse(Collections.emptyList());

    return AzureWebAppSlotSetupParameters.builder()
        .accountId(azureAppServiceStateData.getApplication().getAccountId())
        .appId(azureAppServiceStateData.getApplication().getAppId())
        .commandName(APP_SERVICE_SLOT_SETUP)
        .activityId(activity.getUuid())
        .subscriptionId(azureAppServiceStateData.getSubscriptionId())
        .resourceGroupName(azureAppServiceStateData.getResourceGroup())
        .appSettings(applicationSettingMap)
        .connSettings(connectionStringMap)
        .slotName(azureAppServiceStateData.getDeploymentSlot())
        .webAppName(azureAppServiceStateData.getAppService())
        .connectorConfigDTO(connectorConfigDTO)
        .encryptedDataDetails(encryptedDataDetails)
        .azureRegistryType(azureRegistryType)
        .timeoutIntervalInMin(getTimeOut(context))
        .build();
  }
}