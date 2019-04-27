package software.wings.helpers.ext.nexus;

import static io.harness.eraro.ErrorCode.INVALID_ARTIFACT_SERVER;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static software.wings.common.Constants.IMAGE;
import static software.wings.common.Constants.TAG;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;
import static software.wings.helpers.ext.nexus.NexusServiceImpl.getBaseUrl;
import static software.wings.helpers.ext.nexus.NexusServiceImpl.getRetrofit;
import static software.wings.helpers.ext.nexus.NexusServiceImpl.isSuccessful;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.artifact.ArtifactUtilities;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import retrofit2.Response;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.config.NexusConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.nexus.model.DockerImageResponse;
import software.wings.helpers.ext.nexus.model.DockerImageTagResponse;
import software.wings.helpers.ext.nexus.model.RepositoryRequest;
import software.wings.helpers.ext.nexus.model.RepositoryResponse;
import software.wings.helpers.ext.nexus.model.RequestData;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class NexusThreeServiceImpl {
  @Inject EncryptionService encryptionService;

  public Map<String, String> getRepositories(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails)
      throws IOException {
    return getDockerRepositories(nexusConfig, encryptionDetails);
  }

  private Map<String, String> getDockerRepositories(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails) throws IOException {
    logger.info("Retrieving docker repositories");
    RepositoryRequest repositoryRequest =
        RepositoryRequest.builder()
            .action("coreui_Repository")
            .method("readReferences")
            .type("rpc")
            .tid(15)
            .data(singletonList(
                RequestData.builder()
                    .filter(singletonList(RequestData.Filter.builder().property("format").value("docker").build()))
                    .build()))
            .build();

    NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig, encryptionDetails);
    Response<RepositoryResponse> response;
    if (nexusConfig.hasCredentials()) {
      response =
          nexusThreeRestClient
              .getRepositories(Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())),
                  repositoryRequest)
              .execute();
    } else {
      response = nexusThreeRestClient.getRepositories(repositoryRequest).execute();
    }

    if (isSuccessful(response)) {
      if (response.body().getResult().isSuccess()) {
        logger.info("Retrieving docker repositories success");
        final Map<String, String> repositories =
            response.body().getResult().getData().stream().collect(Collectors.toMap(o -> o.getId(), o -> o.getName()));
        logger.info("Retrieved repositories are {}", repositories.values());
        return repositories;
      } else {
        throw new WingsException(INVALID_ARTIFACT_SERVER, WingsException.USER)
            .addParam("message", "Failed to fetch the repositories");
      }
    }
    logger.info("No docker repositories found. Returning empty results");
    return emptyMap();
  }

  public List<String> getDockerImages(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repository, List<String> images) throws IOException {
    logger.info("Retrieving docker images for repository {} from url {}", repository, nexusConfig.getNexusUrl());
    NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig, encryptionDetails);
    Response<DockerImageResponse> response;
    if (nexusConfig.hasCredentials()) {
      response =
          nexusThreeRestClient
              .getDockerImages(
                  Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())), repository)
              .execute();
    } else {
      response = nexusThreeRestClient.getDockerImages(repository).execute();
    }
    if (isSuccessful(response)) {
      if (response.body() != null && response.body().getRepositories() != null) {
        images.addAll(response.body().getRepositories().stream().collect(toList()));
        logger.info("Retrieving docker images for repository {} from url {} success. Images are {}", repository,
            nexusConfig.getNexusUrl(), images);
      }
    } else {
      logger.warn("Failed to fetch the docker images as request is not success");
      throw new WingsException(INVALID_ARTIFACT_SERVER, WingsException.USER)
          .addParam("message", "Failed to fetch the docker images");
    }
    logger.info("No images found for repository {}", repository);
    return images;
  }

  public List<BuildDetails> getDockerTags(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes) throws IOException {
    String repoKey = artifactStreamAttributes.getJobName();
    String imageName = artifactStreamAttributes.getImageName();
    String repoName = ArtifactUtilities.getNexusRepositoryName(nexusConfig.getNexusUrl(),
        artifactStreamAttributes.getNexusDockerPort(), artifactStreamAttributes.getNexusDockerRegistryUrl(),
        artifactStreamAttributes.getImageName());
    logger.info("Retrieving docker tags for repository {} imageName {} ", repoKey, imageName);
    List<BuildDetails> buildDetails = new ArrayList<>();
    NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig, encryptionDetails);
    Response<DockerImageTagResponse> response;

    if (nexusConfig.hasCredentials()) {
      response = nexusThreeRestClient
                     .getDockerTags(Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())),
                         repoKey, imageName)
                     .execute();

    } else {
      response = nexusThreeRestClient.getDockerTags(repoKey, imageName).execute();
    }

    if (isSuccessful(response)) {
      if (response.body() != null && response.body().getTags() != null) {
        return response.body()
            .getTags()
            .stream()
            .map(tag -> {
              Map<String, String> metadata = new HashMap();
              metadata.put(IMAGE, repoName + ":" + tag);
              metadata.put(TAG, tag);
              return aBuildDetails().withNumber(tag).withMetadata(metadata).withUiDisplayName("Tag# " + tag).build();
            })
            .collect(toList());
      }
    } else {
      throw new WingsException(INVALID_ARTIFACT_SERVER, WingsException.USER)
          .addParam("message", "Failed to fetch the docker tags of image [" + imageName + "]");
    }
    logger.info("No tags found for image name {}", imageName);
    return buildDetails;
  }

  private NexusThreeRestClient getNexusThreeClient(
      final NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails) {
    if (nexusConfig.hasCredentials()) {
      encryptionService.decrypt(nexusConfig, encryptionDetails);
    }
    return getRetrofit(getBaseUrl(nexusConfig), JacksonConverterFactory.create()).create(NexusThreeRestClient.class);
  }
}
