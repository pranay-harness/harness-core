package software.wings.helpers.ext.nexus;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;
import static software.wings.helpers.ext.nexus.NexusServiceImpl.getBaseUrl;
import static software.wings.helpers.ext.nexus.NexusServiceImpl.getRetrofit;
import static software.wings.helpers.ext.nexus.NexusServiceImpl.isSuccessful;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import okhttp3.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;
import retrofit2.converter.jackson.JacksonConverterFactory;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by sgurubelli on 11/16/17.
 */
@Singleton
public class NexusThreeServiceImpl {
  private static final Logger logger = LoggerFactory.getLogger(NexusTwoServiceImpl.class);

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
    Response<RepositoryResponse> response =
        nexusThreeRestClient
            .getRepositories(
                Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())), repositoryRequest)
            .execute();

    if (isSuccessful(response)) {
      if (response.body().getResult().isSuccess()) {
        logger.info("Retrieving docker repositories success");
        return response.body().getResult().getData().stream().collect(
            Collectors.toMap(o -> o.getId(), o -> o.getName()));
      } else {
        logger.warn("Failed to fetch the repositories as request is not success");
      }
    }
    logger.info("No docker repositories found. Returning empty datas");
    return emptyMap();
  }

  public List<String> getDockerImages(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repository) throws IOException {
    logger.info("Retrieving docker images for repository {} from url {}", repository, nexusConfig.getNexusUrl());
    NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig, encryptionDetails);
    Response<DockerImageResponse> response =
        nexusThreeRestClient
            .getDockerImages(
                Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())), repository)
            .execute();
    if (isSuccessful(response)) {
      if (response.body() != null && response.body().getRepositories() != null) {
        logger.info(
            "Retrieving docker images for repository {} from url {} success", repository, nexusConfig.getNexusUrl());
        return response.body().getRepositories().stream().collect(toList());
      }
    } else {
      logger.warn("Failed to fetch the docker images as request is not success");
    }
    logger.info("No images found for repository {}", repository);
    return new ArrayList<>();
  }

  public List<BuildDetails> getDockerTags(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoKey, String imageName) throws IOException {
    logger.info("Retrieving docker tags for repository {} imageName {} ", repoKey, imageName);
    List<BuildDetails> buildDetails = new ArrayList<>();
    NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig, encryptionDetails);
    Response<DockerImageTagResponse> response =
        nexusThreeRestClient
            .getDockerTags(
                Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())), repoKey, imageName)
            .execute();
    if (isSuccessful(response)) {
      if (response.body() != null && response.body().getTags() != null) {
        return response.body().getTags().stream().map(tag -> aBuildDetails().withNumber(tag).build()).collect(toList());
      }
    } else {
      logger.warn("Failed to fetch the repositories as request is not success");
    }
    logger.info("No tags found for image name {}", imageName);
    return buildDetails;
  }

  private NexusThreeRestClient getNexusThreeClient(
      final NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails) {
    encryptionService.decrypt(nexusConfig, encryptionDetails);
    return getRetrofit(getBaseUrl(nexusConfig), JacksonConverterFactory.create()).create(NexusThreeRestClient.class);
  }
}
