package software.wings.helpers.ext.azure;

import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.eraro.ErrorCode.INVALID_ARTIFACT_SERVER;
import static io.harness.exception.WingsException.USER;
import static java.util.stream.Collectors.toList;
import static software.wings.common.Constants.IMAGE;
import static software.wings.common.Constants.TAG;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AzureConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class AcrServiceImpl implements AcrService {
  private AzureHelperService azureHelperService;

  private static final Logger logger = LoggerFactory.getLogger(AcrServiceImpl.class);

  @Inject
  public AcrServiceImpl(AzureHelperService azureHelperService) {
    this.azureHelperService = azureHelperService;
  }

  @Override
  public List<String> listRegistries(
      AzureConfig config, List<EncryptedDataDetail> encryptionDetails, String subscriptionId) {
    try {
      return azureHelperService.listContainerRegistries(config, encryptionDetails, subscriptionId);
    } catch (Exception e) {
      throw new WingsException(INVALID_ARTIFACT_SERVER, USER).addParam("message", ExceptionUtils.getMessage(e));
    }
  }

  @Override
  public List<BuildDetails> getBuilds(AzureConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes, int maxNumberOfBuilds) {
    try {
      String loginServer = azureHelperService.getLoginServerForRegistry(config, encryptionDetails,
          artifactStreamAttributes.getSubscriptionId(), artifactStreamAttributes.getRegistryName());

      String repository = loginServer + "/" + artifactStreamAttributes.getRepositoryName();

      return azureHelperService
          .listRepositoryTags(config, encryptionDetails, artifactStreamAttributes.getSubscriptionId(),
              artifactStreamAttributes.getRegistryName(), artifactStreamAttributes.getRepositoryName())
          .stream()
          .map(tag -> {
            Map<String, String> metadata = new HashMap();
            metadata.put(IMAGE, repository + ":" + tag);
            metadata.put(TAG, tag);
            return aBuildDetails().withNumber(tag).withMetadata(metadata).build();
          })
          .collect(toList());
    } catch (Exception e) {
      throw new WingsException(INVALID_ARTIFACT_SERVER, USER).addParam("message", ExceptionUtils.getMessage(e));
    }
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(
      AzureConfig config, List<EncryptedDataDetail> encryptionDetails, String imageName) {
    return null;
  }

  @Override
  public boolean verifyImageName(AzureConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes) {
    if (!azureHelperService.isValidSubscription(
            config, encryptionDetails, artifactStreamAttributes.getSubscriptionId())) {
      logger.info(
          "SubscriptionId [" + artifactStreamAttributes.getSubscriptionId() + "] does not exist in Azure account.");
      throw new WingsException(INVALID_ARGUMENT, USER)
          .addParam("args",
              "SubscriptionId [" + artifactStreamAttributes.getSubscriptionId() + "] does not exist in Azure account.");
    }

    if (!azureHelperService.isValidContainerRegistry(config, encryptionDetails,
            artifactStreamAttributes.getSubscriptionId(), artifactStreamAttributes.getRegistryName())) {
      logger.info(
          "Registry [" + artifactStreamAttributes.getRegistryName() + "] does not exist in Azure subscription.");
      throw new WingsException(INVALID_ARGUMENT, USER)
          .addParam("args",
              "Registry [" + artifactStreamAttributes.getRegistryName() + "] does not exist in Azure subscription.");
    }

    if (!azureHelperService
             .listRepositories(config, encryptionDetails, artifactStreamAttributes.getSubscriptionId(),
                 artifactStreamAttributes.getRegistryName())
             .contains(artifactStreamAttributes.getRepositoryName())) {
      logger.info(
          "Repository [" + artifactStreamAttributes.getRepositoryName() + "] does not exist in Azure Registry.");
      throw new WingsException(INVALID_ARGUMENT, USER)
          .addParam("args",
              "Repository [" + artifactStreamAttributes.getRepositoryName() + "] does not exist in Azure Registry.");
    }

    return true;
  }

  @Override
  public boolean validateCredentials(AzureConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes) {
    azureHelperService.listRepositoryTags(config, encryptionDetails, artifactStreamAttributes.getSubscriptionId(),
        artifactStreamAttributes.getRegistryName(), artifactStreamAttributes.getRepositoryName());
    return true;
  }
}
