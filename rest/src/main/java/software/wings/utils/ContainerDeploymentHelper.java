package software.wings.utils;

import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.beans.artifact.ArtifactStreamType.ACR;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.HostElement;
import software.wings.api.InstanceElement;
import software.wings.api.ServiceTemplateElement;
import software.wings.beans.AwsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.EcrConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.AcrArtifactStream;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.beans.artifact.GcrArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.beans.container.ImageDetails;
import software.wings.beans.container.ImageDetails.ImageDetailsBuilder;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.ContainerInfo.Status;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.helpers.ext.ecr.EcrClassicService;
import software.wings.helpers.ext.ecr.EcrService;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 4/6/18.
 */
@Singleton
public class ContainerDeploymentHelper {
  @Inject private SettingsService settingsService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private EncryptionService encryptionService;
  @Inject private SecretManager secretManager;
  @Inject private AwsHelperService awsHelperService;
  @Inject private AzureHelperService azureHelperService;
  @Inject private EcrService ecrService;
  @Inject private EcrClassicService ecrClassicService;
  private static final Logger logger = LoggerFactory.getLogger(ContainerDeploymentHelper.class);

  public List<InstanceStatusSummary> getInstanceStatusSummaryFromContainerInfoList(
      List<ContainerInfo> containerInfos, ServiceTemplateElement serviceTemplateElement) {
    List<InstanceStatusSummary> instanceStatusSummaries = new ArrayList<>();
    if (EmptyPredicate.isNotEmpty(containerInfos)) {
      for (ContainerInfo containerInfo : containerInfos) {
        HostElement hostElement = aHostElement()
                                      .withHostName(containerInfo.getHostName())
                                      .withEc2Instance(containerInfo.getEc2Instance())
                                      .build();
        InstanceElement instanceElement = anInstanceElement()
                                              .withUuid(containerInfo.getContainerId())
                                              .withDockerId(containerInfo.getContainerId())
                                              .withHostName(containerInfo.getHostName())
                                              .withHost(hostElement)
                                              .withServiceTemplateElement(serviceTemplateElement)
                                              .withDisplayName(containerInfo.getContainerId())
                                              .build();
        ExecutionStatus status =
            containerInfo.getStatus() == Status.SUCCESS ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED;
        instanceStatusSummaries.add(
            anInstanceStatusSummary().withStatus(status).withInstanceElement(instanceElement).build());
      }
    }
    return instanceStatusSummaries;
  }

  public ImageDetails fetchArtifactDetails(Artifact artifact, String appId, String workflowExecutionId) {
    ImageDetailsBuilder imageDetails = ImageDetails.builder().tag(artifact.getBuildNo());
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());
    String settingId = artifactStream.getSettingId();
    if (artifactStream.getArtifactStreamType().equals(DOCKER.name())) {
      DockerArtifactStream dockerArtifactStream = (DockerArtifactStream) artifactStream;
      DockerConfig dockerConfig = (DockerConfig) settingsService.get(settingId).getValue();
      encryptionService.decrypt(
          dockerConfig, secretManager.getEncryptionDetails(dockerConfig, appId, workflowExecutionId));
      imageDetails.name(dockerArtifactStream.getImageName())
          .sourceName(dockerArtifactStream.getSourceName())
          .registryUrl(dockerConfig.getDockerRegistryUrl())
          .username(dockerConfig.getUsername())
          .password(new String(dockerConfig.getPassword()));
    } else if (artifactStream.getArtifactStreamType().equals(ECR.name())) {
      EcrArtifactStream ecrArtifactStream = (EcrArtifactStream) artifactStream;
      String imageUrl = getImageUrl(ecrArtifactStream, workflowExecutionId);
      // name should be 830767422336.dkr.ecr.us-east-1.amazonaws.com/todolist
      // sourceName should be todolist
      // registryUrl should be https://830767422336.dkr.ecr.us-east-1.amazonaws.com/
      imageDetails.name(imageUrl)
          .sourceName(ecrArtifactStream.getSourceName())
          .registryUrl("https://" + imageUrl + (imageUrl.endsWith("/") ? "" : "/"))
          .username("AWS");
      SettingValue settingValue = settingsService.get(settingId).getValue();

      // All the new ECR artifact streams use cloud provider AWS settings for accesskey and secret
      if (SettingVariableTypes.AWS.name().equals(settingValue.getType())) {
        AwsConfig awsConfig = (AwsConfig) settingsService.get(settingId).getValue();
        encryptionService.decrypt(awsConfig, secretManager.getEncryptionDetails(awsConfig, appId, workflowExecutionId));
        imageDetails.password(awsHelperService.getAmazonEcrAuthToken(imageUrl.substring(0, imageUrl.indexOf('.')),
            ecrArtifactStream.getRegion(), awsConfig.getAccessKey(), awsConfig.getSecretKey()));
      } else {
        // There is a point when old ECR artifact streams would be using the old ECR Artifact Server definition until
        // migration happens. The deployment code handles both the cases.
        EcrConfig ecrConfig = (EcrConfig) settingsService.get(settingId).getValue();
        imageDetails.password(awsHelperService.getAmazonEcrAuthToken(
            ecrConfig, secretManager.getEncryptionDetails(ecrConfig, appId, workflowExecutionId)));
      }
    } else if (artifactStream.getArtifactStreamType().equals(GCR.name())) {
      GcrArtifactStream gcrArtifactStream = (GcrArtifactStream) artifactStream;
      String imageName = gcrArtifactStream.getRegistryHostName() + "/" + gcrArtifactStream.getDockerImageName();
      imageDetails.name(imageName).sourceName(imageName).registryUrl(imageName);
    } else if (artifactStream.getArtifactStreamType().equals(ACR.name())) {
      AcrArtifactStream acrArtifactStream = (AcrArtifactStream) artifactStream;
      AzureConfig azureConfig = (AzureConfig) settingsService.get(settingId).getValue();
      String loginServer = azureHelperService.getLoginServerForRegistry(azureConfig,
          secretManager.getEncryptionDetails(azureConfig, appId, workflowExecutionId),
          acrArtifactStream.getSubscriptionId(), acrArtifactStream.getRegistryName());

      imageDetails.registryUrl(azureHelperService.getUrl(loginServer))
          .sourceName(acrArtifactStream.getRepositoryName())
          .name(loginServer + "/" + acrArtifactStream.getRepositoryName())
          .username(azureConfig.getClientId())
          .password(new String(azureConfig.getKey()));
    } else if (artifactStream.getArtifactStreamType().equals(ARTIFACTORY.name())) {
      ArtifactoryArtifactStream artifactoryArtifactStream = (ArtifactoryArtifactStream) artifactStream;
      ArtifactoryConfig artifactoryConfig = (ArtifactoryConfig) settingsService.get(settingId).getValue();
      encryptionService.decrypt(
          artifactoryConfig, secretManager.getEncryptionDetails(artifactoryConfig, appId, workflowExecutionId));
      String url = artifactoryConfig.getArtifactoryUrl();
      if (artifactoryArtifactStream.getDockerRepositoryServer() != null) {
        String registryUrl = String.format(
            "http%s://%s", url.startsWith("https") ? "s" : "", artifactoryArtifactStream.getDockerRepositoryServer());

        imageDetails
            .name(
                artifactoryArtifactStream.getDockerRepositoryServer() + "/" + artifactoryArtifactStream.getImageName())
            .sourceName(artifactoryArtifactStream.getSourceName())
            .registryUrl(registryUrl)
            .username(artifactoryConfig.getUsername())
            .password(new String(artifactoryConfig.getPassword()));
      } else {
        int firstDotIndex = url.indexOf('.');
        int slashAfterDomain = url.indexOf('/', firstDotIndex);
        String registryUrl = url.substring(0, firstDotIndex) + "-" + artifactoryArtifactStream.getJobname()
            + url.substring(firstDotIndex, slashAfterDomain > 0 ? slashAfterDomain : url.length());
        String namePrefix = registryUrl.substring(registryUrl.indexOf("://") + 3);
        imageDetails.name(namePrefix + "/" + artifactoryArtifactStream.getImageName())
            .sourceName(artifactoryArtifactStream.getSourceName())
            .registryUrl(registryUrl)
            .username(artifactoryConfig.getUsername())
            .password(new String(artifactoryConfig.getPassword()));
      }
    } else if (artifactStream.getArtifactStreamType().equals(NEXUS.name())) {
      NexusArtifactStream nexusArtifactStream = (NexusArtifactStream) artifactStream;
      NexusConfig nexusConfig = (NexusConfig) settingsService.get(settingId).getValue();
      encryptionService.decrypt(
          nexusConfig, secretManager.getEncryptionDetails(nexusConfig, appId, workflowExecutionId));

      String url = nexusConfig.getNexusUrl();
      int firstDotIndex = url.indexOf('.');
      int colonIndex = url.indexOf(':', firstDotIndex);
      int endIndex = colonIndex > 0 ? colonIndex : url.length();
      String registryUrl = url.substring(0, endIndex) + ":"
          + (nexusArtifactStream.getDockerPort() != null ? nexusArtifactStream.getDockerPort() : "5000");
      String namePrefix = registryUrl.substring(registryUrl.indexOf("://") + 3);
      logger.info("Nexus Registry url: " + registryUrl);
      imageDetails.name(namePrefix + "/" + nexusArtifactStream.getImageName())
          .sourceName(nexusArtifactStream.getSourceName())
          .registryUrl(registryUrl)
          .username(nexusConfig.getUsername())
          .password(new String(nexusConfig.getPassword()));
    } else {
      throw new WingsException(ErrorCode.INVALID_REQUEST)
          .addParam(
              "message", artifactStream.getArtifactStreamType() + " artifact source can't be used for containers");
    }
    return imageDetails.build();
  }

  private String getImageUrl(EcrArtifactStream ecrArtifactStream, String workflowExecutionId) {
    SettingAttribute settingAttribute = settingsService.get(ecrArtifactStream.getSettingId());
    SettingValue value = settingAttribute.getValue();
    if (SettingVariableTypes.AWS.name().equals(value.getType())) {
      AwsConfig awsConfig = (AwsConfig) value;
      return ecrService.getEcrImageUrl(awsConfig,
          secretManager.getEncryptionDetails(awsConfig, ecrArtifactStream.getAppId(), workflowExecutionId),
          ecrArtifactStream.getRegion(), ecrArtifactStream);
    } else {
      EcrConfig ecrConfig = (EcrConfig) value;
      return ecrClassicService.getEcrImageUrl(ecrConfig, ecrArtifactStream);
    }
  }
}
