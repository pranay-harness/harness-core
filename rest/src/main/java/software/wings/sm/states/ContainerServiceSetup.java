package software.wings.sm.states;

import static software.wings.beans.FeatureName.ECS_CREATE_CLUSTER;
import static software.wings.beans.FeatureName.KUBERNETES_CREATE_CLUSTER;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ClusterElement;
import software.wings.api.ContainerServiceElement;
import software.wings.api.PhaseElement;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.DockerConfig;
import software.wings.beans.EcrConfig;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.ErrorCode;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.beans.artifact.GcrArtifactStream;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.container.ContainerTask;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.ecr.EcrClassicService;
import software.wings.helpers.ext.ecr.EcrService;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateExecutionData;
import software.wings.sm.WorkflowStandardParams;

import java.util.Optional;

/**
 * Created by brett on 9/29/17
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ContainerServiceSetup extends State {
  static final int KEEP_N_REVISIONS = 3;

  @Transient private static final Logger logger = LoggerFactory.getLogger(ContainerServiceSetup.class);

  private int maxInstances;
  private ResizeStrategy resizeStrategy;
  @Inject @Transient private transient EcrService ecrService;
  @Inject @Transient private transient EcrClassicService ecrClassicService;
  @Inject @Transient private transient AwsHelperService awsHelperService;
  @Inject @Transient protected transient SettingsService settingsService;
  @Inject @Transient protected transient ServiceResourceService serviceResourceService;
  @Inject @Transient protected transient InfrastructureMappingService infrastructureMappingService;
  @Inject @Transient protected transient ArtifactStreamService artifactStreamService;
  @Inject @Transient protected transient FeatureFlagService featureFlagService;
  @Inject @Transient protected transient SecretManager secretManager;
  @Inject @Transient protected transient EncryptionService encryptionService;

  ContainerServiceSetup(String name, String type) {
    super(name, type);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
      String serviceId = phaseElement.getServiceElement().getUuid();

      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      Artifact artifact = workflowStandardParams.getArtifactForService(serviceId);
      ImageDetails imageDetails = fetchArtifactDetails(artifact, context);

      Application app = workflowStandardParams.getApp();
      String envName = workflowStandardParams.getEnv().getName();

      String serviceName = serviceResourceService.get(app.getUuid(), serviceId).getName();
      ContainerTask containerTask =
          serviceResourceService.getContainerTaskByDeploymentType(app.getAppId(), serviceId, getDeploymentType());

      InfrastructureMapping infrastructureMapping =
          infrastructureMappingService.get(app.getUuid(), phaseElement.getInfraMappingId());
      if (infrastructureMapping == null || !(infrastructureMapping instanceof ContainerInfrastructureMapping)
          || !isValidInfraMapping(infrastructureMapping)) {
        throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Invalid infrastructure type");
      }

      ContainerInfrastructureMapping containerInfrastructureMapping =
          (ContainerInfrastructureMapping) infrastructureMapping;

      String clusterName = containerInfrastructureMapping.getClusterName();
      if (!(infrastructureMapping instanceof DirectKubernetesInfrastructureMapping)
          && Constants.RUNTIME.equals(clusterName)) {
        if ((infrastructureMapping instanceof GcpKubernetesInfrastructureMapping
                && featureFlagService.isEnabled(KUBERNETES_CREATE_CLUSTER, app.getAccountId()))
            || (infrastructureMapping instanceof EcsInfrastructureMapping
                   && featureFlagService.isEnabled(ECS_CREATE_CLUSTER, app.getAccountId()))) {
          clusterName = getClusterNameFromContextElement(context);
        } else {
          throw new WingsException(
              ErrorCode.INVALID_REQUEST, "message", "Runtime creation of clusters is not yet supported.");
        }
      }

      StateExecutionData executionData = createService(context, serviceName, imageDetails, app.getName(), envName,
          clusterName, containerInfrastructureMapping, containerTask);

      ContainerServiceElement containerServiceElement =
          buildContainerServiceElement(phaseElement, serviceId, context.getAppId(), context.getWorkflowExecutionId(),
              containerInfrastructureMapping, getContainerServiceNameFromExecutionData(executionData));

      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.SUCCESS)
          .addContextElement(containerServiceElement)
          .addNotifyElement(containerServiceElement)
          .withStateExecutionData(executionData)
          .build();
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", e.getMessage(), e);
    }
  }

  public int getMaxInstances() {
    return maxInstances;
  }

  public void setMaxInstances(int maxInstances) {
    this.maxInstances = maxInstances;
  }

  public ResizeStrategy getResizeStrategy() {
    return resizeStrategy;
  }

  public void setResizeStrategy(ResizeStrategy resizeStrategy) {
    this.resizeStrategy = resizeStrategy;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  protected class ImageDetails {
    String name;
    String tag;
    String sourceName;
    String registryUrl;
    String username;
    String password;
  }

  /**
   * Fetches artifact image details
   */
  private ImageDetails fetchArtifactDetails(Artifact artifact, ExecutionContext context) {
    ImageDetails imageDetails = new ImageDetails();
    imageDetails.tag = artifact.getBuildNo();
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());
    String settingId = artifactStream.getSettingId();
    if (artifactStream.getArtifactStreamType().equals(ArtifactStreamType.DOCKER.name())) {
      DockerArtifactStream dockerArtifactStream = (DockerArtifactStream) artifactStream;
      imageDetails.name = dockerArtifactStream.getImageName();
      imageDetails.sourceName = dockerArtifactStream.getSourceName();
      DockerConfig dockerConfig = (DockerConfig) settingsService.get(settingId).getValue();
      encryptionService.decrypt(dockerConfig,
          secretManager.getEncryptionDetails(dockerConfig, context.getAppId(), context.getWorkflowExecutionId()));
      imageDetails.registryUrl = dockerConfig.getDockerRegistryUrl();
      imageDetails.username = dockerConfig.getUsername();
      imageDetails.password = new String(dockerConfig.getPassword());
    } else if (artifactStream.getArtifactStreamType().equals(ArtifactStreamType.ECR.name())) {
      EcrArtifactStream ecrArtifactStream = (EcrArtifactStream) artifactStream;
      // name should be 830767422336.dkr.ecr.us-east-1.amazonaws.com/todolist
      String imageUrl = getImageUrl(ecrArtifactStream, context);
      imageDetails.name = imageUrl;
      // sourceName should be todolist
      imageDetails.sourceName = ecrArtifactStream.getSourceName();
      // registryUrl should be https://830767422336.dkr.ecr.us-east-1.amazonaws.com/
      imageDetails.registryUrl = "https://" + imageUrl + (imageUrl.endsWith("/") ? "" : "/");
      imageDetails.username = "AWS";

      SettingValue settingValue = settingsService.get(settingId).getValue();

      // All the new ECR artifact streams use cloud provider AWS settings for accesskey and secret
      if (SettingVariableTypes.AWS.name().equals(settingValue.getType())) {
        AwsConfig awsConfig = (AwsConfig) settingsService.get(settingId).getValue();
        encryptionService.decrypt(awsConfig,
            secretManager.getEncryptionDetails(awsConfig, context.getAppId(), context.getWorkflowExecutionId()));
        imageDetails.password = awsHelperService.getAmazonEcrAuthToken(imageUrl.substring(0, imageUrl.indexOf('.')),
            ecrArtifactStream.getRegion(), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      } else {
        // There is a point when old ECR artifact streams would be using the old ECR Artifact Server definition until
        // migration happens. The deployment code handles both the cases.
        EcrConfig ecrConfig = (EcrConfig) settingsService.get(settingId).getValue();
        imageDetails.password = awsHelperService.getAmazonEcrAuthToken(ecrConfig,
            secretManager.getEncryptionDetails(ecrConfig, context.getAppId(), context.getWorkflowExecutionId()));
      }
    } else if (artifactStream.getArtifactStreamType().equals(ArtifactStreamType.GCR.name())) {
      GcrArtifactStream gcrArtifactStream = (GcrArtifactStream) artifactStream;
      String imageName = gcrArtifactStream.getRegistryHostName() + "/" + gcrArtifactStream.getDockerImageName();
      imageDetails.name = imageName;
      imageDetails.sourceName = imageName;
      imageDetails.registryUrl = imageName;
    } else if (artifactStream.getArtifactStreamType().equals(ArtifactStreamType.ARTIFACTORY.name())) {
      ArtifactoryArtifactStream artifactoryArtifactStream = (ArtifactoryArtifactStream) artifactStream;
      imageDetails.name = artifactoryArtifactStream.getImageName();
      imageDetails.sourceName = artifactoryArtifactStream.getSourceName();
      ArtifactoryConfig artifactoryConfig = (ArtifactoryConfig) settingsService.get(settingId).getValue();
      encryptionService.decrypt(artifactoryConfig,
          secretManager.getEncryptionDetails(artifactoryConfig, context.getAppId(), context.getWorkflowExecutionId()));
      imageDetails.registryUrl = artifactoryConfig.getArtifactoryUrl();
      imageDetails.username = artifactoryConfig.getUsername();
      imageDetails.password = new String(artifactoryConfig.getPassword());
    } else {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message",
          artifactStream.getArtifactStreamType() + " artifact source can't be used for containers");
    }
    return imageDetails;
  }

  private String getImageUrl(EcrArtifactStream ecrArtifactStream, ExecutionContext context) {
    SettingAttribute settingAttribute = settingsService.get(ecrArtifactStream.getSettingId());
    SettingValue value = settingAttribute.getValue();
    if (SettingVariableTypes.AWS.name().equals(value.getType())) {
      AwsConfig awsConfig = (AwsConfig) value;
      return ecrService.getEcrImageUrl(awsConfig,
          secretManager.getEncryptionDetails(awsConfig, context.getAppId(), context.getWorkflowExecutionId()),
          ecrArtifactStream.getRegion(), ecrArtifactStream);
    } else {
      EcrConfig ecrConfig = (EcrConfig) value;
      return ecrClassicService.getEcrImageUrl(ecrConfig, ecrArtifactStream);
    }
  }

  protected String getClusterNameFromContextElement(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);

    Optional<ClusterElement> contextElement =
        context.<ClusterElement>getContextElementList(ContextElementType.CLUSTER)
            .stream()
            .filter(clusterElement -> phaseElement.getInfraMappingId().equals(clusterElement.getInfraMappingId()))
            .findFirst();

    return contextElement.isPresent() ? contextElement.get().getName() : "";
  }

  protected abstract String getContainerServiceNameFromExecutionData(StateExecutionData executionData);

  protected abstract String getDeploymentType();

  protected abstract StateExecutionData createService(ExecutionContext context, String serviceName,
      ImageDetails imageDetails, String appName, String envName, String clusterName,
      ContainerInfrastructureMapping infrastructureMapping, ContainerTask containerTask);

  protected abstract boolean isValidInfraMapping(InfrastructureMapping infrastructureMapping);

  protected abstract ContainerServiceElement buildContainerServiceElement(PhaseElement phaseElement, String serviceId,
      String appId, String workflowExecutionId, ContainerInfrastructureMapping infrastructureMapping,
      String containerServiceName);
}
