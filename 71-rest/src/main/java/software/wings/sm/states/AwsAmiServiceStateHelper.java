package software.wings.sm.states;

import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.context.ContextElementType;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.PhaseElement;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.WorkflowStandardParams;

import java.util.List;

@Slf4j
@Singleton
public class AwsAmiServiceStateHelper {
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private SettingsService settingsService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private SecretManager secretManager;

  public AwsAmiTrafficShiftAlbData populateAlbTrafficShiftSetupData(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    notNullCheck("workflowStandardParams", workflowStandardParams, USER);
    notNullCheck("currentUser", workflowStandardParams.getCurrentUser(), USER);

    Artifact artifact = ((DeploymentExecutionContext) context).getDefaultArtifactForService(serviceId);
    notNullCheck(format("Unable to find artifact for service id: %s", serviceId), artifact);

    Application app = workflowStandardParams.getApp();
    notNullCheck("Application cannot be null", app);
    Environment env = workflowStandardParams.getEnv();
    Service service = serviceResourceService.getWithDetails(app.getUuid(), serviceId);

    AwsAmiInfrastructureMapping infrastructureMapping =
        (AwsAmiInfrastructureMapping) infrastructureMappingService.get(app.getUuid(), context.fetchInfraMappingId());

    SettingAttribute cloudProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(
        (EncryptableSetting) cloudProviderSetting.getValue(), context.getAppId(), context.getWorkflowExecutionId());
    String region = infrastructureMapping.getRegion();
    AwsConfig awsConfig = (AwsConfig) cloudProviderSetting.getValue();

    return AwsAmiTrafficShiftAlbData.builder()
        .artifact(artifact)
        .app(app)
        .service(service)
        .env(env)
        .awsConfig(awsConfig)
        .infrastructureMapping(infrastructureMapping)
        .awsEncryptedDataDetails(encryptionDetails)
        .region(region)
        .serviceId(serviceId)
        .currentUser(workflowStandardParams.getCurrentUser())
        .build();
  }
}
