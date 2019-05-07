package software.wings.service.impl.artifact;

import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.artifact.ArtifactStreamType.ACR;
import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;
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

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.artifact.ArtifactUtilities;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.network.Http;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.EcrConfig;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
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
import software.wings.delegatetasks.buildsource.BuildSourceParameters;
import software.wings.delegatetasks.buildsource.BuildSourceParameters.BuildSourceRequestType;
import software.wings.expression.SecretFunctor;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.helpers.ext.ecr.EcrClassicService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.aws.manager.AwsEcrHelperServiceManager;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.ArtifactType;
import software.wings.utils.RepositoryType;
import software.wings.utils.Validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                          .withAppId(artifactStream.getAppId())
                          .withArtifactStreamId(artifactStream.getUuid())
                          .withArtifactSourceName(artifactStream.getSourceName())
                          .withDisplayName(getDisplayName(artifactStream, buildDetails))
                          .withDescription(buildDetails.getDescription())
                          .withMetadata(getMetadata(artifactStream, buildDetails))
                          .withRevision(buildDetails.getRevision())
                          .withArtifactStreamType(artifactStream.getArtifactStreamType())
                          .withUiDisplayName(buildDetails.getUiDisplayName());
    if (settingId != null) {
      builder.withSettingId(settingId);
    }
    if (accountId != null) {
      builder.withAccountId(accountId);
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
        metadata.put(ArtifactMetadataKeys.ARTIFACT_PATH, buildDetails.getArtifactPath());
        metadata.put(ArtifactMetadataKeys.ARTIFACT_FILE_NAME,
            buildDetails.getNumber().substring(buildDetails.getNumber().lastIndexOf('/') + 1));
        if (buildDetails.getArtifactFileSize() != null) {
          metadata.put(ArtifactMetadataKeys.ARTIFACT_FILE_SIZE, buildDetails.getArtifactFileSize());
        }
      }
      if (isNotEmpty(buildDetails.getBuildUrl())) {
        metadata.put(ArtifactMetadataKeys.URL, buildDetails.getBuildUrl());
      }
      metadata.put(ArtifactMetadataKeys.BUILD_NO, buildDetails.getNumber());
      return metadata;
    } else if (artifactStreamType.equals(AMAZON_S3.name()) || artifactStreamType.equals(GCS.name())) {
      Map<String, String> buildParameters = buildDetails.getBuildParameters();
      metadata.put(ArtifactMetadataKeys.ARTIFACT_PATH, buildParameters.get(ArtifactMetadataKeys.ARTIFACT_PATH));
      metadata.put(ArtifactMetadataKeys.ARTIFACT_FILE_NAME, buildParameters.get(ArtifactMetadataKeys.ARTIFACT_PATH));
      metadata.put(ArtifactMetadataKeys.BUILD_NO, buildParameters.get(ArtifactMetadataKeys.BUILD_NO));
      metadata.put(ArtifactMetadataKeys.BUCKET_NAME, buildParameters.get(ArtifactMetadataKeys.BUCKET_NAME));
      metadata.put(ArtifactMetadataKeys.KEY, buildParameters.get(ArtifactMetadataKeys.KEY));
      metadata.put(ArtifactMetadataKeys.URL, buildParameters.get(ArtifactMetadataKeys.URL));
      metadata.put(
          ArtifactMetadataKeys.ARTIFACT_FILE_SIZE, buildParameters.get(ArtifactMetadataKeys.ARTIFACT_FILE_SIZE));
      return metadata;
    } else if (artifactStreamType.equals(JENKINS.name()) || artifactStreamType.equals(BAMBOO.name())) {
      metadata.putAll(buildDetails.getBuildParameters());
      metadata.put(ArtifactMetadataKeys.BUILD_NO, buildDetails.getNumber());
      metadata.put(ArtifactMetadataKeys.BUILD_FULL_DISPLAY_NAME, buildDetails.getBuildFullDisplayName());
      metadata.put(ArtifactMetadataKeys.URL, buildDetails.getBuildUrl());
      return metadata;
    } else if (artifactStreamType.equals(SMB.name()) || artifactStreamType.equals(SFTP.name())) {
      metadata.putAll(buildDetails.getBuildParameters());
      metadata.put(ArtifactMetadataKeys.BUILD_NO, buildDetails.getNumber());
      metadata.put(ArtifactMetadataKeys.ARTIFACT_PATH, metadata.get(ArtifactMetadataKeys.ARTIFACT_PATH));
      metadata.put(ArtifactMetadataKeys.BUILD_FULL_DISPLAY_NAME, buildDetails.getBuildFullDisplayName());
      metadata.put(ArtifactMetadataKeys.URL, buildDetails.getBuildUrl());
      return metadata;
    } else if (artifactStreamType.equals(NEXUS.name())) {
      metadata.put(ArtifactMetadataKeys.BUILD_NO, buildDetails.getNumber());
      if (isNotEmpty(buildDetails.getBuildUrl())) {
        metadata.put(ArtifactMetadataKeys.URL, buildDetails.getBuildUrl());
      }
      return metadata;
    }

    metadata.put(ArtifactMetadataKeys.BUILD_NO, buildDetails.getNumber());
    return metadata;
  }

  public ImageDetails fetchContainerImageDetails(Artifact artifact, String appId, String workflowExecutionId) {
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());
    if (artifactStream == null) {
      throw new InvalidRequestException("Artifact Stream [" + artifact.getArtifactSourceName() + "] was deleted");
    }

    ImageDetails imageDetails = getDockerImageDetailsInternal(artifactStream, workflowExecutionId);
    imageDetails.setTag(artifact.getBuildNo());
    return imageDetails;
  }

  public String getDockerConfig(String appId, String artifactStreamId) {
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
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

  private ImageDetails getDockerImageDetailsInternal(ArtifactStream artifactStream, String workflowExecutionId) {
    ImageDetailsBuilder imageDetailsBuilder = ImageDetails.builder();
    String appId = artifactStream.getAppId();
    String settingId = artifactStream.getSettingId();
    if (artifactStream.getArtifactStreamType().equals(DOCKER.name())) {
      DockerArtifactStream dockerArtifactStream = (DockerArtifactStream) artifactStream;
      DockerConfig dockerConfig = (DockerConfig) settingsService.get(settingId).getValue();
      managerDecryptionService.decrypt(
          dockerConfig, secretManager.getEncryptionDetails(dockerConfig, appId, workflowExecutionId));

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
      String imageUrl = getImageUrl(ecrArtifactStream, workflowExecutionId, appId);
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
            getAmazonEcrAuthToken(awsConfig, secretManager.getEncryptionDetails(awsConfig, appId, workflowExecutionId),
                imageUrl.substring(0, imageUrl.indexOf('.')), ecrArtifactStream.getRegion(), appId));
      } else {
        // There is a point when old ECR artifact streams would be using the old ECR Artifact Server
        // definition until migration happens. The deployment code handles both the cases.
        EcrConfig ecrConfig = (EcrConfig) settingsService.get(settingId).getValue();
        imageDetailsBuilder.password(awsHelperService.getAmazonEcrAuthToken(
            ecrConfig, secretManager.getEncryptionDetails(ecrConfig, appId, workflowExecutionId)));
      }
    } else if (artifactStream.getArtifactStreamType().equals(GCR.name())) {
      GcrArtifactStream gcrArtifactStream = (GcrArtifactStream) artifactStream;
      String imageName = gcrArtifactStream.getRegistryHostName() + "/" + gcrArtifactStream.getDockerImageName();
      imageDetailsBuilder.name(imageName).sourceName(imageName).registryUrl(imageName);
    } else if (artifactStream.getArtifactStreamType().equals(ACR.name())) {
      AcrArtifactStream acrArtifactStream = (AcrArtifactStream) artifactStream;
      AzureConfig azureConfig = (AzureConfig) settingsService.get(settingId).getValue();
      String loginServer = azureHelperService.getLoginServerForRegistry(azureConfig,
          secretManager.getEncryptionDetails(azureConfig, appId, workflowExecutionId),
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
          artifactoryConfig, secretManager.getEncryptionDetails(artifactoryConfig, appId, workflowExecutionId));
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
          nexusConfig, secretManager.getEncryptionDetails(nexusConfig, appId, workflowExecutionId));

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
  }

  private String getImageUrl(EcrArtifactStream ecrArtifactStream, String workflowExecutionId, String appId) {
    SettingAttribute settingAttribute = settingsService.get(ecrArtifactStream.getSettingId());
    SettingValue value = settingAttribute.getValue();
    if (SettingVariableTypes.AWS.name().equals(value.getType())) {
      AwsConfig awsConfig = (AwsConfig) value;
      return getEcrImageUrl(awsConfig,
          secretManager.getEncryptionDetails(awsConfig, ecrArtifactStream.getAppId(), workflowExecutionId),
          ecrArtifactStream.getRegion(), ecrArtifactStream, appId);
    } else {
      EcrConfig ecrConfig = (EcrConfig) value;
      return ecrClassicService.getEcrImageUrl(ecrConfig, ecrArtifactStream);
    }
  }

  private String getEcrImageUrl(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      EcrArtifactStream ecrArtifactStream, String appId) {
    return awsEcrHelperServiceManager.getEcrImageUrl(
        awsConfig, encryptionDetails, region, ecrArtifactStream.getImageName(), appId);
  }

  private String getAmazonEcrAuthToken(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String awsAccount, String region, String appId) {
    return awsEcrHelperServiceManager.getAmazonEcrAuthToken(awsConfig, encryptionDetails, awsAccount, region, appId);
  }

  public ArtifactStreamAttributes renderCustomArtifactScriptString(CustomArtifactStream customArtifactStream) {
    Application app = appService.get(customArtifactStream.getAppId());
    Validator.notNullCheck("Application does not exist", app, USER);

    Map<String, Object> context = new HashMap<>();
    context.put("secrets",
        SecretFunctor.builder()
            .managerDecryptionService(managerDecryptionService)
            .secretManager(secretManager)
            .accountId(app.getAccountId())
            .build());

    // Find the FETCH VERSION Script from artifact stream
    Script versionScript =
        customArtifactStream.getScripts()
            .stream()
            .filter(script -> script.getAction() == null || script.getAction().equals(Action.FETCH_VERSIONS))
            .findFirst()
            .orElse(null);
    if (isNotEmpty(customArtifactStream.getTemplateVariables())) {
      Map<String, Object> templateVariableMap =
          TemplateHelper.convertToVariableMap(customArtifactStream.getTemplateVariables());
      if (isNotEmpty(templateVariableMap)) {
        context.putAll(templateVariableMap);
      }
    }
    Validator.notNullCheck("Fetch Version script is missing", versionScript, USER);

    ArtifactStreamAttributes artifactStreamAttributes = customArtifactStream.fetchArtifactStreamAttributes();

    String scriptString = versionScript.getScriptString();
    Validator.notNullCheck("Script string can not be empty", scriptString, USER);
    artifactStreamAttributes.setCustomArtifactStreamScript(evaluator.substitute(scriptString, context));
    artifactStreamAttributes.setAccountId(app.getAccountId());
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
    Map<String, String> metadata = new HashMap();
    metadata.put(ArtifactMetadataKeys.IMAGE, domainName + "/" + imageName + ":" + tag);
    metadata.put(ArtifactMetadataKeys.TAG, tag);
    return aBuildDetails().withNumber(tag).withMetadata(metadata).withBuildUrl(tagUrl + tag).build();
  }

  public static ArtifactStreamAttributes getArtifactStreamAttributes(ArtifactStream artifactStream, Service service) {
    ArtifactStreamAttributes artifactStreamAttributes = artifactStream.fetchArtifactStreamAttributes();
    artifactStreamAttributes.setArtifactType(service.getArtifactType());
    return artifactStreamAttributes;
  }

  public Service getService(String appId, ArtifactStream artifactStream) {
    Service service = serviceResourceService.get(appId, artifactStream.getServiceId(), false);
    if (service == null) {
      artifactStreamService.delete(appId, artifactStream.getUuid());
      throw new WingsException(ErrorCode.GENERAL_ERROR)
          .addParam("message", format("Artifact stream %s is a zombie.", artifactStream.getUuid()));
    }
    return service;
  }

  private static boolean isArtifactoryDockerOrGenric(ArtifactStream artifactStream, ArtifactType artifactType) {
    if (ARTIFACTORY.name().equals(artifactStream.getArtifactStreamType())) {
      return ArtifactType.DOCKER.equals(artifactType)
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
        || isArtifactoryDockerOrGenric(artifactStream, artifactType)) {
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
    return ARTIFACTORY.name().equals(artifactStreamType) && BuildSourceRequestType.GET_BUILDS.equals(requestType) ? 25
                                                                                                                  : -1;
  }

  public BuildSourceParameters getBuildSourceParameters(
      String appId, ArtifactStream artifactStream, SettingAttribute settingAttribute) {
    String artifactStreamType = artifactStream.getArtifactStreamType();
    SettingValue settingValue = settingAttribute.getValue();

    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingValue, null, null);

    ArtifactStreamAttributes artifactStreamAttributes;
    BuildSourceRequestType requestType;
    if (!appId.equals(GLOBAL_APP_ID)) {
      Service service = getService(appId, artifactStream);
      artifactStreamAttributes = getArtifactStreamAttributes(artifactStream, service);
      requestType = getRequestType(artifactStream, service.getArtifactType());
    } else {
      artifactStreamAttributes = artifactStream.fetchArtifactStreamAttributes();
      requestType = getRequestType(artifactStream);
    }

    return BuildSourceParameters.builder()
        .accountId(settingAttribute.getAccountId())
        .appId(artifactStream.getAppId())
        .artifactStreamAttributes(artifactStreamAttributes)
        .artifactStreamType(artifactStreamType)
        .settingValue(settingValue)
        .encryptedDataDetails(encryptedDataDetails)
        .buildSourceRequestType(requestType)
        .limit(getLimit(artifactStream.getArtifactStreamType(), requestType))
        .build();
  }
}
