package software.wings.service.impl.artifact;

import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.artifact.ArtifactStreamType.ACR;
import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;
import static software.wings.beans.artifact.ArtifactStreamType.AMI;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
import static software.wings.beans.artifact.ArtifactStreamType.BAMBOO;
import static software.wings.beans.artifact.ArtifactStreamType.CUSTOM;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;
import static software.wings.beans.artifact.ArtifactStreamType.GCS;
import static software.wings.beans.artifact.ArtifactStreamType.JENKINS;
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;
import static software.wings.beans.artifact.ArtifactStreamType.SFTP;
import static software.wings.beans.artifact.ArtifactStreamType.SMB;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;
import static software.wings.service.impl.artifact.ArtifactServiceImpl.ARTIFACT_RETENTION_SIZE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.artifact.ArtifactUtilities;
import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskBuilder;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.TaskData.TaskDataBuilder;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.network.Http;
import io.harness.persistence.HIterator;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.AwsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.EcrConfig;
import software.wings.beans.FeatureName;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.AcrArtifactStream;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.beans.artifact.Artifact.Builder;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.CustomArtifactStream;
import software.wings.beans.artifact.CustomArtifactStream.Action;
import software.wings.beans.artifact.CustomArtifactStream.Script;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.beans.artifact.GcrArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.beans.container.ImageDetails;
import software.wings.beans.container.ImageDetails.ImageDetailsBuilder;
import software.wings.beans.template.TemplateHelper;
import software.wings.beans.template.artifactsource.CustomRepositoryMapping.AttributeMapping;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.delegatetasks.buildsource.BuildSourceParameters;
import software.wings.delegatetasks.buildsource.BuildSourceParameters.BuildSourceParametersBuilder;
import software.wings.delegatetasks.buildsource.BuildSourceParameters.BuildSourceRequestType;
import software.wings.expression.SecretFunctor;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.helpers.ext.ecr.EcrClassicService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.PermitServiceImpl;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.aws.manager.AwsEcrHelperServiceManager;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.ArtifactType;
import software.wings.utils.RepositoryType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class ArtifactCollectionUtils {
  @Inject private SettingsService settingsService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ManagerDecryptionService managerDecryptionService;
  @Inject private SecretManager secretManager;
  @Inject private AwsHelperService awsHelperService;
  @Inject private AzureHelperService azureHelperService;
  @Inject private EcrClassicService ecrClassicService;
  @Inject private AwsEcrHelperServiceManager awsEcrHelperServiceManager;
  @Inject private AppService appService;
  @Inject private ExpressionEvaluator evaluator;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @Inject private ArtifactService artifactService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private AwsCommandHelper awsCommandHelper;

  @Transient
  private static final String DOCKER_REGISTRY_CREDENTIAL_TEMPLATE =
      "{\"%s\":{\"username\":\"%s\",\"password\":\"%s\"}}";

  public Artifact getArtifact(ArtifactStream artifactStream, BuildDetails buildDetails) {
    String accountId = null;
    String settingId = artifactStream.getSettingId();
    if (settingId != null) {
      SettingAttribute settingAttribute = settingsService.get(settingId);
      if (settingAttribute == null) {
        throw new InvalidRequestException(
            format("Setting attribute not found for artifact stream %s", artifactStream.getName()), USER);
      }
      accountId = settingAttribute.getAccountId();
    }

    Builder builder = anArtifact()
                          .withAppId(artifactStream.fetchAppId())
                          .withArtifactStreamId(artifactStream.getUuid())
                          .withArtifactSourceName(artifactStream.getSourceName())
                          .withDisplayName(getDisplayName(artifactStream, buildDetails))
                          .withDescription(buildDetails.getDescription())
                          .withMetadata(getMetadata(artifactStream, buildDetails))
                          .withRevision(buildDetails.getRevision())
                          .withArtifactStreamType(artifactStream.getArtifactStreamType())
                          .withUiDisplayName(buildDetails.getUiDisplayName())
                          .withArtifactDownloadMetadata(buildDetails.getArtifactFileMetadataList());
    if (settingId != null) {
      builder.withSettingId(settingId);
    }
    if (accountId != null) {
      builder.withAccountId(accountId);
    }
    if (isNotEmpty(buildDetails.getLabels())) {
      builder.withLabels(buildDetails.getLabels());
    }
    return builder.build();
  }

  private String getDisplayName(ArtifactStream artifactStream, BuildDetails buildDetails) {
    if (isNotEmpty(buildDetails.getBuildDisplayName())) {
      return buildDetails.getBuildDisplayName();
    }
    if (artifactStream.getArtifactStreamType().equals(ARTIFACTORY.name())) {
      if (buildDetails.getArtifactPath() != null) {
        return artifactStream.fetchArtifactDisplayName("");
      }
    } else if (artifactStream.getArtifactStreamType().equals(AMAZON_S3.name())) {
      return artifactStream.fetchArtifactDisplayName("");
    }

    return artifactStream.fetchArtifactDisplayName(buildDetails.getNumber());
  }

  private Map<String, String> getMetadata(ArtifactStream artifactStream, BuildDetails buildDetails) {
    String artifactStreamType = artifactStream.getArtifactStreamType();

    Map<String, String> metadata = buildDetails.getMetadata() == null ? new HashMap<>() : buildDetails.getMetadata();
    if (artifactStreamType.equals(ARTIFACTORY.name())) {
      if (buildDetails.getArtifactPath() != null) {
        metadata.put(ArtifactMetadataKeys.artifactPath, buildDetails.getArtifactPath());
        metadata.put(ArtifactMetadataKeys.artifactFileName,
            buildDetails.getNumber().substring(buildDetails.getNumber().lastIndexOf('/') + 1));
        if (buildDetails.getArtifactFileSize() != null) {
          metadata.put(ArtifactMetadataKeys.artifactFileSize, buildDetails.getArtifactFileSize());
        }
      }
      if (isNotEmpty(buildDetails.getBuildUrl())) {
        metadata.put(ArtifactMetadataKeys.url, buildDetails.getBuildUrl());
      }
      metadata.put(ArtifactMetadataKeys.buildNo, buildDetails.getNumber());
      return metadata;
    } else if (artifactStreamType.equals(AMAZON_S3.name()) || artifactStreamType.equals(GCS.name())) {
      Map<String, String> buildParameters = buildDetails.getBuildParameters();
      metadata.put(ArtifactMetadataKeys.artifactPath, buildParameters.get(ArtifactMetadataKeys.artifactPath));
      metadata.put(ArtifactMetadataKeys.artifactFileName, buildParameters.get(ArtifactMetadataKeys.artifactPath));
      metadata.put(ArtifactMetadataKeys.buildNo, buildParameters.get(ArtifactMetadataKeys.buildNo));
      metadata.put(ArtifactMetadataKeys.bucketName, buildParameters.get(ArtifactMetadataKeys.bucketName));
      metadata.put(ArtifactMetadataKeys.key, buildParameters.get(ArtifactMetadataKeys.key));
      metadata.put(ArtifactMetadataKeys.url, buildParameters.get(ArtifactMetadataKeys.url));
      metadata.put(ArtifactMetadataKeys.artifactFileSize, buildParameters.get(ArtifactMetadataKeys.artifactFileSize));
      return metadata;
    } else if (artifactStreamType.equals(JENKINS.name()) || artifactStreamType.equals(BAMBOO.name())) {
      metadata.putAll(buildDetails.getBuildParameters());
      metadata.put(ArtifactMetadataKeys.buildNo, buildDetails.getNumber());
      metadata.put(ArtifactMetadataKeys.buildFullDisplayName, buildDetails.getBuildFullDisplayName());
      metadata.put(ArtifactMetadataKeys.url, buildDetails.getBuildUrl());
      return metadata;
    } else if (artifactStreamType.equals(SMB.name()) || artifactStreamType.equals(SFTP.name())) {
      metadata.putAll(buildDetails.getBuildParameters());
      metadata.put(ArtifactMetadataKeys.buildNo, buildDetails.getNumber());
      metadata.put(ArtifactMetadataKeys.artifactPath, metadata.get(ArtifactMetadataKeys.artifactPath));
      metadata.put(ArtifactMetadataKeys.buildFullDisplayName, buildDetails.getBuildFullDisplayName());
      metadata.put(ArtifactMetadataKeys.url, buildDetails.getBuildUrl());
      return metadata;
    } else if (artifactStreamType.equals(NEXUS.name())) {
      metadata.put(ArtifactMetadataKeys.buildNo, buildDetails.getNumber());
      if (isNotEmpty(buildDetails.getBuildUrl())) {
        metadata.put(ArtifactMetadataKeys.url, buildDetails.getBuildUrl());
      }
      if (isNotEmpty(buildDetails.getArtifactFileMetadataList())) {
        metadata.put(
            ArtifactMetadataKeys.artifactFileName, buildDetails.getArtifactFileMetadataList().get(0).getFileName());
      }
      return metadata;
    }

    metadata.put(ArtifactMetadataKeys.buildNo, buildDetails.getNumber());
    return metadata;
  }

  public ImageDetails fetchContainerImageDetails(Artifact artifact, String workflowExecutionId) {
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getArtifactStreamId());
    if (artifactStream == null) {
      throw new InvalidRequestException("Artifact Stream [" + artifact.getArtifactSourceName() + "] was deleted");
    }

    ImageDetails imageDetails = getDockerImageDetailsInternal(artifactStream, workflowExecutionId);
    imageDetails.setTag(artifact.getBuildNo());
    return imageDetails;
  }

  public String getDockerConfig(String artifactStreamId) {
    return getDockerConfig(artifactStreamService.get(artifactStreamId));
  }

  private String getDockerConfig(ArtifactStream artifactStream) {
    if (artifactStream == null) {
      return "";
    }

    ImageDetails imageDetails = getDockerImageDetailsInternal(artifactStream, null);
    if (isNotBlank(imageDetails.getRegistryUrl()) && isNotBlank(imageDetails.getUsername())
        && isNotBlank(imageDetails.getPassword())) {
      return encodeBase64(format(DOCKER_REGISTRY_CREDENTIAL_TEMPLATE, imageDetails.getRegistryUrl(),
          imageDetails.getUsername(), imageDetails.getPassword()));
    }
    return "";
  }

  public DelegateTaskBuilder fetchCustomDelegateTask(String waitId, ArtifactStream artifactStream,
      ArtifactStreamAttributes artifactStreamAttributes, boolean isCollection) {
    DelegateTaskBuilder delegateTaskBuilder = DelegateTask.builder().async(true).appId(GLOBAL_APP_ID).waitId(waitId);
    final TaskDataBuilder dataBuilder = TaskData.builder().taskType(TaskType.BUILD_SOURCE_TASK.name());

    BuildSourceRequestType requestType = BuildSourceRequestType.GET_BUILDS;

    BuildSourceParametersBuilder buildSourceParametersBuilder =
        BuildSourceParameters.builder()
            .accountId(artifactStreamAttributes.getAccountId())
            .appId(artifactStream.fetchAppId())
            .artifactStreamAttributes(artifactStreamAttributes)
            .artifactStreamType(artifactStream.getArtifactStreamType())
            .buildSourceRequestType(requestType)
            .limit(ArtifactCollectionUtils.getLimit(artifactStream.getArtifactStreamType(), requestType))
            .isCollection(isCollection);

    if (isCollection) {
      buildSourceParametersBuilder.savedBuildDetailsKeys(getArtifactsKeys(artifactStream, artifactStreamAttributes));
    }
    List<String> tags = ((CustomArtifactStream) artifactStream).getTags();
    if (isNotEmpty(tags)) {
      // To remove if any empty tags in case saved for custom artifact stream
      tags = tags.stream().filter(EmptyPredicate::isNotEmpty).distinct().collect(toList());
    }

    String accountId = artifactStreamAttributes.getAccountId();

    // Set timeout. Labels are not fetched for CUSTOM artifact streams.
    long timeout = isEmpty(artifactStreamAttributes.getCustomScriptTimeout())
        ? Long.parseLong(CustomArtifactStream.DEFAULT_SCRIPT_TIME_OUT)
        : Long.parseLong(artifactStreamAttributes.getCustomScriptTimeout());
    dataBuilder.parameters(new Object[] {buildSourceParametersBuilder.build()})
        .timeout(Duration.ofSeconds(timeout).toMillis());
    delegateTaskBuilder.tags(tags);
    delegateTaskBuilder.accountId(accountId);
    delegateTaskBuilder.data(dataBuilder.build());

    return delegateTaskBuilder;
  }

  private ImageDetails getDockerImageDetailsInternal(ArtifactStream artifactStream, String workflowExecutionId) {
    logger.info("Entering getDockerImageDetailsInternal: artifactStreamName {}", artifactStream.getName());

    try {
      ImageDetailsBuilder imageDetailsBuilder = ImageDetails.builder();
      String settingId = artifactStream.getSettingId();
      if (artifactStream.getArtifactStreamType().equals(DOCKER.name())) {
        DockerArtifactStream dockerArtifactStream = (DockerArtifactStream) artifactStream;
        DockerConfig dockerConfig = (DockerConfig) settingsService.get(settingId).getValue();
        managerDecryptionService.decrypt(
            dockerConfig, secretManager.getEncryptionDetails(dockerConfig, null, workflowExecutionId));

        String domainName = Http.getDomainWithPort(dockerConfig.getDockerRegistryUrl());
        String imageName = dockerArtifactStream.getImageName();

        if (dockerConfig.hasCredentials()) {
          imageDetailsBuilder.name(imageName)
              .sourceName(dockerArtifactStream.getSourceName())
              .registryUrl(dockerConfig.getDockerRegistryUrl())
              .username(dockerConfig.getUsername())
              .password(new String(dockerConfig.getPassword()))
              .domainName(domainName);
        } else {
          imageDetailsBuilder.name(imageName)
              .sourceName(dockerArtifactStream.getSourceName())
              .registryUrl(dockerConfig.getDockerRegistryUrl())
              .domainName(domainName);
        }
      } else if (artifactStream.getArtifactStreamType().equals(ECR.name())) {
        EcrArtifactStream ecrArtifactStream = (EcrArtifactStream) artifactStream;
        String imageUrl = getImageUrl(ecrArtifactStream, workflowExecutionId);
        // name should be 830767422336.dkr.ecr.us-east-1.amazonaws.com/todolist
        // sourceName should be todolist
        // registryUrl should be https://830767422336.dkr.ecr.us-east-1.amazonaws.com/
        imageDetailsBuilder.name(imageUrl)
            .sourceName(ecrArtifactStream.getSourceName())
            .registryUrl("https://" + imageUrl + (imageUrl.endsWith("/") ? "" : "/"))
            .username("AWS");
        SettingValue settingValue = settingsService.get(settingId).getValue();

        // All the new ECR artifact streams use cloud provider AWS settings for accesskey and secret
        if (SettingVariableTypes.AWS.name().equals(settingValue.getType())) {
          AwsConfig awsConfig = (AwsConfig) settingsService.get(settingId).getValue();
          imageDetailsBuilder.password(
              getAmazonEcrAuthToken(awsConfig, secretManager.getEncryptionDetails(awsConfig, null, workflowExecutionId),
                  imageUrl.substring(0, imageUrl.indexOf('.')), ecrArtifactStream.getRegion()));
        } else {
          // There is a point when old ECR artifact streams would be using the old ECR Artifact Server
          // definition until migration happens. The deployment code handles both the cases.
          EcrConfig ecrConfig = (EcrConfig) settingsService.get(settingId).getValue();
          imageDetailsBuilder.password(awsHelperService.getAmazonEcrAuthToken(
              ecrConfig, secretManager.getEncryptionDetails(ecrConfig, null, workflowExecutionId)));
        }
      } else if (artifactStream.getArtifactStreamType().equals(GCR.name())) {
        GcrArtifactStream gcrArtifactStream = (GcrArtifactStream) artifactStream;
        String imageName = gcrArtifactStream.getRegistryHostName() + "/" + gcrArtifactStream.getDockerImageName();
        imageDetailsBuilder.name(imageName).sourceName(imageName).registryUrl(imageName);
      } else if (artifactStream.getArtifactStreamType().equals(ACR.name())) {
        AcrArtifactStream acrArtifactStream = (AcrArtifactStream) artifactStream;
        AzureConfig azureConfig = (AzureConfig) settingsService.get(settingId).getValue();
        managerDecryptionService.decrypt(
            azureConfig, secretManager.getEncryptionDetails(azureConfig, null, workflowExecutionId));
        String loginServer = isNotEmpty(acrArtifactStream.getRegistryHostName())
            ? acrArtifactStream.getRegistryHostName()
            : azureHelperService.getLoginServerForRegistry(azureConfig,
                  secretManager.getEncryptionDetails(azureConfig, null, workflowExecutionId),
                  acrArtifactStream.getSubscriptionId(), acrArtifactStream.getRegistryName());

        imageDetailsBuilder.registryUrl(azureHelperService.getUrl(loginServer))
            .sourceName(acrArtifactStream.getRepositoryName())
            .name(loginServer + "/" + acrArtifactStream.getRepositoryName())
            .username(azureConfig.getClientId())
            .password(new String(azureConfig.getKey()));
      } else if (artifactStream.getArtifactStreamType().equals(ARTIFACTORY.name())) {
        ArtifactoryArtifactStream artifactoryArtifactStream = (ArtifactoryArtifactStream) artifactStream;
        ArtifactoryConfig artifactoryConfig = (ArtifactoryConfig) settingsService.get(settingId).getValue();
        managerDecryptionService.decrypt(
            artifactoryConfig, secretManager.getEncryptionDetails(artifactoryConfig, null, workflowExecutionId));
        String registryUrl = ArtifactUtilities.getArtifactoryRegistryUrl(artifactoryConfig.getArtifactoryUrl(),
            artifactoryArtifactStream.getDockerRepositoryServer(), artifactoryArtifactStream.getJobname());
        String repositoryName = ArtifactUtilities.getArtifactoryRepositoryName(artifactoryConfig.getArtifactoryUrl(),
            artifactoryArtifactStream.getDockerRepositoryServer(), artifactoryArtifactStream.getJobname(),
            artifactoryArtifactStream.getImageName());

        if (artifactoryConfig.hasCredentials()) {
          imageDetailsBuilder.name(repositoryName)
              .sourceName(artifactoryArtifactStream.getSourceName())
              .registryUrl(registryUrl)
              .username(artifactoryConfig.getUsername())
              .password(new String(artifactoryConfig.getPassword()));
        } else {
          imageDetailsBuilder.name(repositoryName)
              .sourceName(artifactoryArtifactStream.getSourceName())
              .registryUrl(registryUrl);
        }
      } else if (artifactStream.getArtifactStreamType().equals(NEXUS.name())) {
        NexusArtifactStream nexusArtifactStream = (NexusArtifactStream) artifactStream;
        NexusConfig nexusConfig = (NexusConfig) settingsService.get(settingId).getValue();
        managerDecryptionService.decrypt(
            nexusConfig, secretManager.getEncryptionDetails(nexusConfig, null, workflowExecutionId));

        String registryUrl = ArtifactUtilities.getNexusRegistryUrl(
            nexusConfig.getNexusUrl(), nexusArtifactStream.getDockerPort(), nexusArtifactStream.getDockerRegistryUrl());
        String repositoryName =
            ArtifactUtilities.getNexusRepositoryName(nexusConfig.getNexusUrl(), nexusArtifactStream.getDockerPort(),
                nexusArtifactStream.getDockerRegistryUrl(), nexusArtifactStream.getImageName());
        logger.info("Nexus Registry url: " + registryUrl);
        if (nexusConfig.hasCredentials()) {
          imageDetailsBuilder.name(repositoryName)
              .sourceName(nexusArtifactStream.getSourceName())
              .registryUrl(registryUrl)
              .username(nexusConfig.getUsername())
              .password(new String(nexusConfig.getPassword()));
        } else {
          imageDetailsBuilder.name(repositoryName)
              .sourceName(nexusArtifactStream.getSourceName())
              .registryUrl(registryUrl);
        }
      } else if (artifactStream.getArtifactStreamType().equals(CUSTOM.name())) {
        imageDetailsBuilder.sourceName(artifactStream.getSourceName());
      } else {
        throw new InvalidRequestException(
            artifactStream.getArtifactStreamType() + " artifact source can't be used for containers");
      }
      return imageDetailsBuilder.build();
    } finally {
      logger.info("Exiting getDockerImageDetailsInternal: artifactStreamName {}", artifactStream.getName());
    }
  }

  private String getImageUrl(EcrArtifactStream ecrArtifactStream, String workflowExecutionId) {
    SettingAttribute settingAttribute = settingsService.get(ecrArtifactStream.getSettingId());
    SettingValue value = settingAttribute.getValue();
    if (SettingVariableTypes.AWS.name().equals(value.getType())) {
      AwsConfig awsConfig = (AwsConfig) value;
      return getEcrImageUrl(awsConfig, secretManager.getEncryptionDetails(awsConfig, null, workflowExecutionId),
          ecrArtifactStream.getRegion(), ecrArtifactStream);
    } else {
      EcrConfig ecrConfig = (EcrConfig) value;
      return ecrClassicService.getEcrImageUrl(ecrConfig, ecrArtifactStream);
    }
  }

  private String getEcrImageUrl(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      EcrArtifactStream ecrArtifactStream) {
    return awsEcrHelperServiceManager.getEcrImageUrl(
        awsConfig, encryptionDetails, region, ecrArtifactStream.getImageName(), null);
  }

  private String getAmazonEcrAuthToken(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String awsAccount, String region) {
    return awsEcrHelperServiceManager.getAmazonEcrAuthToken(awsConfig, encryptionDetails, awsAccount, region, null);
  }

  public ArtifactStreamAttributes renderCustomArtifactScriptString(CustomArtifactStream customArtifactStream) {
    notNullCheck("Account does not exist", customArtifactStream.getAccountId(), USER);

    Map<String, Object> context = new HashMap<>();
    context.put("secrets",
        SecretFunctor.builder()
            .managerDecryptionService(managerDecryptionService)
            .secretManager(secretManager)
            .accountId(customArtifactStream.getAccountId())
            .build());

    // Find the FETCH VERSION Script from artifact stream
    Script versionScript =
        customArtifactStream.getScripts()
            .stream()
            .filter(script -> script.getAction() == null || script.getAction() == Action.FETCH_VERSIONS)
            .findFirst()
            .orElse(null);
    if (isNotEmpty(customArtifactStream.getTemplateVariables())) {
      Map<String, Object> templateVariableMap =
          TemplateHelper.convertToVariableMap(customArtifactStream.getTemplateVariables());
      if (isNotEmpty(templateVariableMap)) {
        context.putAll(templateVariableMap);
      }
    }
    notNullCheck("Fetch Version script is missing", versionScript, USER);

    ArtifactStreamAttributes artifactStreamAttributes = customArtifactStream.fetchArtifactStreamAttributes();

    String scriptString = versionScript.getScriptString();
    notNullCheck("Script string can not be empty", scriptString, USER);
    artifactStreamAttributes.setCustomArtifactStreamScript(evaluator.substitute(scriptString, context));
    artifactStreamAttributes.setAccountId(customArtifactStream.getAccountId());
    artifactStreamAttributes.setCustomScriptTimeout(evaluator.substitute(versionScript.getTimeout(), context));
    if (versionScript.getCustomRepositoryMapping() != null) {
      validateAttributeMapping(versionScript.getCustomRepositoryMapping().getArtifactRoot(),
          versionScript.getCustomRepositoryMapping().getBuildNoPath());
      artifactStreamAttributes.setCustomAttributeMappingNeeded(true);
      artifactStreamAttributes.setArtifactRoot(
          evaluator.substitute(versionScript.getCustomRepositoryMapping().getArtifactRoot(), context));
      artifactStreamAttributes.setBuildNoPath(
          evaluator.substitute(versionScript.getCustomRepositoryMapping().getBuildNoPath(), context));
      Map<String, String> map = new HashMap<>();
      if (EmptyPredicate.isNotEmpty(versionScript.getCustomRepositoryMapping().getArtifactAttributes())) {
        for (AttributeMapping attributeMapping : versionScript.getCustomRepositoryMapping().getArtifactAttributes()) {
          String relativePath = null;
          if (isNotEmpty(attributeMapping.getRelativePath())) {
            relativePath = evaluator.substitute(attributeMapping.getRelativePath(), context);
          }
          String mappedAttribute = null;
          if (isNotEmpty(attributeMapping.getMappedAttribute())) {
            mappedAttribute = evaluator.substitute(attributeMapping.getMappedAttribute(), context);
          }
          map.put(relativePath, mappedAttribute);
        }
      }

      artifactStreamAttributes.setArtifactAttributes(map);
    }
    return artifactStreamAttributes;
  }

  private void validateAttributeMapping(String artifactRoot, String buildNoPath) {
    if (isEmpty(artifactRoot)) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, WingsException.USER)
          .addParam("message", "Artifacts Array Path cannot be empty. Please refer the documentation.");
    }
    if (isEmpty(buildNoPath)) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, WingsException.USER)
          .addParam("message", "BuildNo. Path cannot be empty. Please refer the documentation.");
    }
  }

  public static BuildDetails prepareDockerBuildDetails(DockerConfig dockerConfig, String imageName, String tag) {
    String tagUrl = dockerConfig.getDockerRegistryUrl().endsWith("/")
        ? dockerConfig.getDockerRegistryUrl() + imageName + "/tags/"
        : dockerConfig.getDockerRegistryUrl() + "/" + imageName + "/tags/";

    String domainName = Http.getDomainWithPort(dockerConfig.getDockerRegistryUrl());
    Map<String, String> metadata = new HashMap<>();
    metadata.put(ArtifactMetadataKeys.image, domainName + "/" + imageName + ":" + tag);
    metadata.put(ArtifactMetadataKeys.tag, tag);
    return aBuildDetails().withNumber(tag).withMetadata(metadata).withBuildUrl(tagUrl + tag).build();
  }

  public ArtifactStreamAttributes getArtifactStreamAttributes(ArtifactStream artifactStream, boolean isMultiArtifact) {
    if (isMultiArtifact) {
      return artifactStream.fetchArtifactStreamAttributes();
    } else {
      Service service =
          artifactStreamServiceBindingService.getService(artifactStream.fetchAppId(), artifactStream.getUuid(), true);
      return getArtifactStreamAttributes(artifactStream, service);
    }
  }

  private static ArtifactStreamAttributes getArtifactStreamAttributes(ArtifactStream artifactStream, Service service) {
    ArtifactStreamAttributes artifactStreamAttributes = artifactStream.fetchArtifactStreamAttributes();
    artifactStreamAttributes.setArtifactType(service.getArtifactType());
    return artifactStreamAttributes;
  }

  private static boolean isArtifactoryDockerOrGeneric(ArtifactStream artifactStream, ArtifactType artifactType) {
    if (ARTIFACTORY.name().equals(artifactStream.getArtifactStreamType())) {
      return ArtifactType.DOCKER == artifactType
          || !"maven".equals(artifactStream.fetchArtifactStreamAttributes().getRepositoryType());
    }
    return false;
  }

  private static boolean isArtifactoryDockerOrGeneric(ArtifactStream artifactStream) {
    return ARTIFACTORY.name().equals(artifactStream.getArtifactStreamType())
        && (artifactStream.fetchArtifactStreamAttributes().getRepositoryType().equals(RepositoryType.docker.name())
               || !"maven".equals(artifactStream.fetchArtifactStreamAttributes().getRepositoryType()));
  }

  public static BuildSourceRequestType getRequestType(ArtifactStream artifactStream, ArtifactType artifactType) {
    String artifactStreamType = artifactStream.getArtifactStreamType();

    if (ArtifactCollectionServiceAsyncImpl.metadataOnlyStreams.contains(artifactStreamType)
        || isArtifactoryDockerOrGeneric(artifactStream, artifactType)) {
      return BuildSourceRequestType.GET_BUILDS;
    } else {
      return BuildSourceRequestType.GET_LAST_SUCCESSFUL_BUILD;
    }
  }

  private static BuildSourceRequestType getRequestType(ArtifactStream artifactStream) {
    String artifactStreamType = artifactStream.getArtifactStreamType();

    if (ArtifactCollectionServiceAsyncImpl.metadataOnlyStreams.contains(artifactStreamType)
        || isArtifactoryDockerOrGeneric(artifactStream) || artifactStreamType.equals(JENKINS.name())
        || artifactStreamType.equals(BAMBOO.name())) {
      return BuildSourceRequestType.GET_BUILDS;
    } else {
      return BuildSourceRequestType.GET_LAST_SUCCESSFUL_BUILD;
    }
  }

  public static int getLimit(String artifactStreamType, BuildSourceRequestType requestType) {
    return ARTIFACTORY.name().equals(artifactStreamType) && BuildSourceRequestType.GET_BUILDS == requestType
        ? ARTIFACT_RETENTION_SIZE
        : -1;
  }

  public BuildSourceParameters getBuildSourceParameters(ArtifactStream artifactStream,
      SettingAttribute settingAttribute, boolean isCollection, boolean withSavedBuildDetailsKeys) {
    String artifactStreamType = artifactStream.getArtifactStreamType();
    SettingValue settingValue = settingAttribute.getValue();
    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingValue, null, null);

    String appId = artifactStream.fetchAppId();
    boolean multiArtifact = multiArtifactEnabled(settingAttribute.getAccountId());
    ArtifactStreamAttributes artifactStreamAttributes = getArtifactStreamAttributes(artifactStream, multiArtifact);
    BuildSourceRequestType requestType =
        getBuildSourceRequestType(artifactStream, multiArtifact, artifactStreamAttributes);

    BuildSourceParametersBuilder buildSourceParametersBuilder =
        BuildSourceParameters.builder()
            .accountId(settingAttribute.getAccountId())
            .appId(appId)
            .artifactStreamAttributes(artifactStreamAttributes)
            .artifactStreamType(artifactStreamType)
            .artifactStreamId(artifactStream.getUuid())
            .settingValue(settingValue)
            .encryptedDataDetails(encryptedDataDetails)
            .buildSourceRequestType(requestType)
            .limit(getLimit(artifactStream.getArtifactStreamType(), requestType))
            .isCollection(isCollection);
    if (withSavedBuildDetailsKeys) {
      buildSourceParametersBuilder.savedBuildDetailsKeys(getArtifactsKeys(artifactStream, artifactStreamAttributes));
    }
    return buildSourceParametersBuilder.build();
  }

  private boolean multiArtifactEnabled(String accountId) {
    return featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId);
  }

  private BuildSourceRequestType getBuildSourceRequestType(
      ArtifactStream artifactStream, boolean multiArtifact, ArtifactStreamAttributes artifactStreamAttributes) {
    BuildSourceRequestType requestType;
    if (multiArtifact) {
      requestType = getRequestType(artifactStream);
    } else {
      requestType = getRequestType(artifactStream, artifactStreamAttributes.getArtifactType());
    }
    return requestType;
  }

  public DelegateTask prepareValidateTask(String artifactStreamId) {
    ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
    if (artifactStream == null) {
      throw new InvalidRequestException("Artifact stream does not exist");
    }
    String accountId = artifactStream.getAccountId();
    BuildSourceParametersBuilder parametersBuilder = BuildSourceParameters.builder()
                                                         .appId(artifactStream.getAppId())
                                                         .artifactStreamType(artifactStream.getArtifactStreamType());
    SettingValue settingValue;
    List<String> tags;
    if (CUSTOM.name().equals(artifactStream.getArtifactStreamType())) {
      parametersBuilder.accountId(artifactStream.getAccountId())
          .buildSourceRequestType(BuildSourceRequestType.GET_BUILDS)
          .artifactStreamAttributes(artifactStream.fetchArtifactStreamAttributes());
      tags = ((CustomArtifactStream) artifactStream).getTags();
      if (isNotEmpty(tags)) {
        // To remove if any empty tags in case saved for custom artifact stream
        tags = tags.stream().filter(EmptyPredicate::isNotEmpty).distinct().collect(toList());
      }

    } else {
      SettingAttribute settingAttribute = settingsService.get(artifactStream.getSettingId());
      if (settingAttribute == null) {
        throw new InvalidRequestException("Connector / Cloud Provider associated to Artifact Stream was deleted");
      }
      settingValue = settingAttribute.getValue();
      List<EncryptedDataDetail> encryptedDataDetails =
          secretManager.getEncryptionDetails((EncryptableSetting) settingValue, null, null);
      tags = awsCommandHelper.getAwsConfigTagsFromSettingAttribute(settingAttribute);
      accountId = settingAttribute.getAccountId();
      boolean multiArtifactEnabled = multiArtifactEnabled(accountId);

      ArtifactStreamAttributes artifactStreamAttributes =
          getArtifactStreamAttributes(artifactStream, multiArtifactEnabled);

      BuildSourceRequestType requestType =
          getBuildSourceRequestType(artifactStream, multiArtifactEnabled, artifactStreamAttributes);

      parametersBuilder = BuildSourceParameters.builder()
                              .accountId(settingAttribute.getAccountId())
                              .appId(artifactStream.getAppId())
                              .artifactStreamAttributes(artifactStreamAttributes)
                              .artifactStreamType(artifactStream.getArtifactStreamType())
                              .settingValue(settingValue)
                              .encryptedDataDetails(encryptedDataDetails)
                              .buildSourceRequestType(requestType);
    }

    return DelegateTask.builder()
        .async(false)
        .accountId(accountId)
        .data(TaskData.builder()
                  .taskType(TaskType.BUILD_SOURCE_TASK.name())
                  .parameters(new Object[] {parametersBuilder.build()})
                  .timeout(TimeUnit.MINUTES.toMillis(1))
                  .build())
        .tags(tags)
        .build();
  }

  /**
   * getArtifactsKeys returns a set of Artifact keys so that they can be compared against incoming BuildDetails objects.
   *
   * @param artifactStream           the artifact stream
   * @param artifactStreamAttributes the artifact stream attributes - used only for ARTIFACTORY
   * @return the set of artifact keys
   */
  Set<String> getArtifactsKeys(ArtifactStream artifactStream, ArtifactStreamAttributes artifactStreamAttributes) {
    if (artifactStream == null || artifactStream.getArtifactStreamType() == null) {
      return Collections.emptySet();
    }

    String artifactStreamType = artifactStream.getArtifactStreamType();
    Function<Artifact, String> keyFn = getArtifactKeyFn(artifactStreamType, artifactStreamAttributes);
    Set<String> artifactKeys = new HashSet<>();
    try (HIterator<Artifact> artifacts =
             new HIterator<>(artifactService.prepareArtifactWithMetadataQuery(artifactStream).fetch())) {
      for (Artifact artifact : artifacts) {
        String key = keyFn.apply(artifact);
        if (key != null) {
          artifactKeys.add(key);
        }
      }
    }
    return artifactKeys;
  }

  /**
   * getArtifactKeyFn returns a function that can extract a unique key for an Artifact object so that it can be compared
   * with a BuildDetails object.
   *
   * @param artifactStreamType       the artifact stream type
   * @param artifactStreamAttributes the artifact stream attributes - used only for ARTIFACTORY
   * @return the function that can used to get the key for an Artifact
   */
  public static Function<Artifact, String> getArtifactKeyFn(
      String artifactStreamType, ArtifactStreamAttributes artifactStreamAttributes) {
    if (AMI.name().equals(artifactStreamType)) {
      return Artifact::getRevision;
    } else if (isGenericArtifactStream(artifactStreamType, artifactStreamAttributes)) {
      return Artifact::getArtifactPath;
    } else {
      return Artifact::getBuildNo;
    }
  }

  /**
   * getNewBuildDetails returns new BuildDetails after removing Artifact already present in DB.
   *
   * @param savedBuildDetailsKeys    the artifact keys for artifacts already stored in DB
   * @param buildDetails             the new build details fetched from third-party repo
   * @param artifactStreamType       the artifact stream type
   * @param artifactStreamAttributes the artifact stream attributes - used only for ARTIFACTORY
   * @return the new build details
   */
  public static List<BuildDetails> getNewBuildDetails(Set<String> savedBuildDetailsKeys,
      List<BuildDetails> buildDetails, String artifactStreamType, ArtifactStreamAttributes artifactStreamAttributes) {
    if (isEmpty(buildDetails)) {
      return Collections.emptyList();
    }
    if (isEmpty(savedBuildDetailsKeys)) {
      return buildDetails;
    }

    Function<BuildDetails, String> keyFn = getBuildDetailsKeyFn(artifactStreamType, artifactStreamAttributes);
    return buildDetails.stream()
        .filter(singleBuildDetails -> !savedBuildDetailsKeys.contains(keyFn.apply(singleBuildDetails)))
        .collect(Collectors.toList());
  }

  /**
   * getBuildDetailsKeyFn returns a function that can extract a unique key for a BuildDetails object so that it can be
   * compared with an Artifact object.
   *
   * @param artifactStreamType       the artifact stream type
   * @param artifactStreamAttributes the artifact stream attributes - used only for ARTIFACTORY
   * @return the function that can used to get the key for a BuildDetails
   */
  public static Function<BuildDetails, String> getBuildDetailsKeyFn(
      String artifactStreamType, ArtifactStreamAttributes artifactStreamAttributes) {
    if (AMI.name().equals(artifactStreamType)) {
      return BuildDetails::getRevision;
    } else if (isGenericArtifactStream(artifactStreamType, artifactStreamAttributes)) {
      return BuildDetails::getArtifactPath;
    } else {
      return BuildDetails::getNumber;
    }
  }

  /**
   * isGenericArtifactStream returns true if we need to compare artifact paths to check if two artifacts - one stored in
   * our DB and one from an artifact repo - are different.
   *
   * @param artifactStreamType       the artifact stream type
   * @param artifactStreamAttributes the artifact stream attributes - used only for ARTIFACTORY
   * @return true, if generic artifact stream - uses artifact path as key
   */
  static boolean isGenericArtifactStream(String artifactStreamType, ArtifactStreamAttributes artifactStreamAttributes) {
    if (AMAZON_S3.name().equals(artifactStreamType)) {
      return true;
    }
    if (ARTIFACTORY.name().equals(artifactStreamType)) {
      if (artifactStreamAttributes.getArtifactType() != null
          && artifactStreamAttributes.getArtifactType() == ArtifactType.DOCKER) {
        return false;
      }
      return artifactStreamAttributes.getRepositoryType() == null
          || !artifactStreamAttributes.getRepositoryType().equals(RepositoryType.docker.name());
    }
    return false;
  }

  public boolean skipArtifactStreamIteration(ArtifactStream artifactStream, boolean isCollection) {
    String prefix = isCollection ? "ASYNC_ARTIFACT_CRON" : "ASYNC_ARTIFACT_CLEANUP_CRON";
    String action = isCollection ? "collection" : "cleanup";
    if (artifactStream.getFailedCronAttempts() > PermitServiceImpl.MAX_FAILED_ATTEMPTS) {
      logger.warn(
          "{}: Artifact {} disabled for artifactStream due to too many failures, type: {}, id: {}, failed count: {}",
          prefix, action, artifactStream.getArtifactStreamType(), artifactStream.getUuid(),
          artifactStream.getFailedCronAttempts());
      return true;
    }

    if (!CUSTOM.name().equals(artifactStream.getArtifactStreamType())) {
      SettingAttribute settingAttribute = settingsService.getOnlyConnectivityError(artifactStream.getSettingId());
      if (settingAttribute == null) {
        throw new InvalidRequestException(
            format("%s: Invalid artifact stream setting: %s", prefix, artifactStream.getSettingId()));
      }
      if (isNotBlank(settingAttribute.getConnectivityError())) {
        logger.info(
            "{}: Skipping {} for artifact stream: {}, because of connectivity error in setting: {} and error: {}",
            prefix, action, artifactStream.getUuid(), artifactStream.getSettingId(),
            settingAttribute.getConnectivityError());
        return true;
      }
    }
    if (isCollection && isNotEmpty(artifactStream.getPerpetualTaskIds())) {
      logger.info(
          "Perpetual task enabled for the artifactStream {}, Skipping the artifact collection through iterator.",
          artifactStream.getUuid());
      return true;
    }
    return false;
  }

  public BuildSourceParameters prepareBuildSourceParameters(String artifactStreamId) {
    ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
    if (artifactStream == null) {
      throw new InvalidRequestException("Artifact stream was deleted");
    }
    return prepareBuildSourceParameters(artifactStream);
  }

  private BuildSourceParameters prepareBuildSourceParameters(ArtifactStream artifactStream) {
    String artifactStreamType = artifactStream.getArtifactStreamType();
    if (CUSTOM.name().equals(artifactStreamType)) {
      return prepareCustomBuildSourceParameters(artifactStream);
    } else {
      SettingAttribute settingAttribute = settingsService.get(artifactStream.getSettingId());
      if (settingAttribute == null) {
        throw new InvalidRequestException("Connector / Cloud Provider associated to Artifact Stream was deleted");
      }
      return getBuildSourceParameters(artifactStream, settingAttribute, true, true);
    }
  }

  private BuildSourceParameters prepareCustomBuildSourceParameters(ArtifactStream artifactStream) {
    ArtifactStreamAttributes artifactStreamAttributes =
        renderCustomArtifactScriptString((CustomArtifactStream) artifactStream);
    BuildSourceRequestType requestType = BuildSourceRequestType.GET_BUILDS;

    BuildSourceParametersBuilder buildSourceParametersBuilder =
        BuildSourceParameters.builder()
            .accountId(artifactStreamAttributes.getAccountId())
            .appId(artifactStream.fetchAppId())
            .artifactStreamId(artifactStream.getUuid())
            .artifactStreamAttributes(artifactStreamAttributes)
            .artifactStreamType(artifactStream.getArtifactStreamType())
            .buildSourceRequestType(requestType)
            .limit(ArtifactCollectionUtils.getLimit(artifactStream.getArtifactStreamType(), requestType))
            .isCollection(true);

    buildSourceParametersBuilder.savedBuildDetailsKeys(getArtifactsKeys(artifactStream, artifactStreamAttributes));

    return buildSourceParametersBuilder.build();
  }

  public List<Artifact> processBuilds(ArtifactStream artifactStream, List<BuildDetails> builds) {
    if (isEmpty(builds)) {
      return new ArrayList<>();
    }
    if (artifactStream == null) {
      logger.info("Artifact stream does not exist. Returning");
      return new ArrayList<>();
    }

    // New build are filtered at the delegate. So all the builds coming in the BuildSourceExecutionResponse are the ones
    // not present in the DB.
    return builds.stream()
        .map(buildDetails -> artifactService.create(getArtifact(artifactStream, buildDetails)))
        .collect(Collectors.toList());
  }

  public static boolean supportsCleanup(String artifactStreamType) {
    return DOCKER.name().equals(artifactStreamType) || AMI.name().equals(artifactStreamType)
        || ARTIFACTORY.name().equals(artifactStreamType) || GCR.name().equals(artifactStreamType)
        || ECR.name().equals(artifactStreamType) || ACR.name().equals(artifactStreamType)
        || NEXUS.name().equals(artifactStreamType);
  }
}
