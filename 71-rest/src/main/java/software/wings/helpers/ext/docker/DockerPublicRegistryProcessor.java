package software.wings.helpers.ext.docker;

import static io.harness.exception.WingsException.USER;
import static software.wings.helpers.ext.docker.DockerRegistryServiceImpl.isSuccessful;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.network.Http;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import retrofit2.Response;
import software.wings.beans.DockerConfig;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.exception.InvalidArtifactServerException;
import software.wings.helpers.ext.docker.DockerRegistryServiceImpl.DockerRegistryToken;
import software.wings.helpers.ext.docker.client.DockerRestClientFactory;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class DockerPublicRegistryProcessor {
  @Inject private DockerRestClientFactory dockerRestClientFactory;
  @Inject private DockerRegistryUtils dockerRegistryUtils;

  private ExpiringMap<String, String> cachedBearerTokens = ExpiringMap.builder().variableExpiration().build();

  public boolean verifyImageName(
      DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails, String imageName) {
    try {
      DockerRegistryRestClient registryRestClient =
          dockerRestClientFactory.getDockerRegistryRestClient(dockerConfig, encryptionDetails);
      Response<DockerPublicImageTagResponse> response =
          registryRestClient.listPublicImageTags(imageName, null, 1).execute();
      if (!isSuccessful(response)) {
        // image not found or user doesn't have permission to list image tags
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
            .addParam("args", "Image name [" + imageName + "] does not exist in Docker registry.");
      }
    } catch (IOException e) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, USER, e).addParam("message", e.getMessage());
    }
    return true;
  }

  public List<BuildDetails> getBuilds(DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails,
      String imageName, int maxNumberOfBuilds) throws IOException {
    DockerRegistryRestClient registryRestClient =
        dockerRestClientFactory.getDockerRegistryRestClient(dockerConfig, encryptionDetails);
    Response<DockerPublicImageTagResponse> response =
        registryRestClient.listPublicImageTags(imageName, null, maxNumberOfBuilds).execute();

    if (!isSuccessful(response)) {
      throw new InvalidArtifactServerException(response.message(), USER);
    }

    return paginate(response.body(), dockerConfig, imageName, registryRestClient, maxNumberOfBuilds);
  }

  /**
   * Paginates through the results of tags and accumulates them in one list.
   *
   * @param tagsPage response page from the APO
   * @param limit maximum build to paginate upto. A repo like library/node has 1500+ builds,
   *              we might not want to show all of them on UI.
   * @throws IOException
   */

  @VisibleForTesting
  List<BuildDetails> paginate(DockerPublicImageTagResponse tagsPage, DockerConfig dockerConfig, String imageName,
      DockerRegistryRestClient registryRestClient, int limit) throws IOException {
    // process first page
    List<BuildDetails> details = processPage(tagsPage, dockerConfig, imageName);

    if (details.size() >= limit || tagsPage == null || tagsPage.getNext() == null) {
      return details.stream().limit(limit).collect(Collectors.toList());
    }

    HttpUrl nextPageUrl = HttpUrl.parse(tagsPage.getNext());
    String nextPageNum = nextPageUrl == null ? null : nextPageUrl.queryParameter("page");

    // process rest of pages
    while (EmptyPredicate.isNotEmpty(nextPageNum)) {
      Response<DockerPublicImageTagResponse> pageResponse =
          registryRestClient.listPublicImageTags(imageName, Integer.valueOf(nextPageNum), limit).execute();

      if (!isSuccessful(pageResponse)) {
        throw new InvalidArtifactServerException(pageResponse.message(), USER);
      }

      DockerPublicImageTagResponse page = pageResponse.body();
      List<BuildDetails> pageDetails = processPage(page, dockerConfig, imageName);
      details.addAll(pageDetails);

      if (details.size() >= limit || page == null || page.getNext() == null) {
        break;
      }

      nextPageUrl = HttpUrl.parse(page.getNext());
      nextPageNum = nextPageUrl == null ? null : nextPageUrl.queryParameter("page");
    }

    return details.stream().limit(limit).collect(Collectors.toList());
  }

  private List<BuildDetails> processPage(
      DockerPublicImageTagResponse publicImageTags, DockerConfig dockerConfig, String imageName) {
    String tagUrl = dockerConfig.getDockerRegistryUrl().endsWith("/")
        ? dockerConfig.getDockerRegistryUrl() + imageName + "/tags/"
        : dockerConfig.getDockerRegistryUrl() + "/" + imageName + "/tags/";

    String domainName = Http.getDomainWithPort(dockerConfig.getDockerRegistryUrl());

    if (publicImageTags != null && EmptyPredicate.isNotEmpty(publicImageTags.getResults())) {
      return publicImageTags.getResults()
          .stream()
          .map(tag -> {
            Map<String, String> metadata = new HashMap<>();
            metadata.put(ArtifactMetadataKeys.image, domainName + "/" + imageName + ":" + tag.getName());
            metadata.put(ArtifactMetadataKeys.tag, tag.getName());
            return aBuildDetails()
                .withNumber(tag.getName())
                .withBuildUrl(tagUrl + tag.getName())
                .withMetadata(metadata)
                .withUiDisplayName("Tag# " + tag.getName())
                .build();
          })
          .collect(Collectors.toList());

    } else {
      if (publicImageTags == null) {
        logger.warn("Docker public image tag response was null.");
      } else {
        logger.warn("Docker public image tag response had an empty or missing tag list.");
      }
      return Collections.emptyList();
    }
  }

  public List<Map<String, String>> getLabels(
      DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails, String imageName, List<String> buildNos) {
    DockerRegistryRestClient registryRestClient =
        dockerRestClientFactory.getDockerRegistryRestClient(dockerConfig, encryptionDetails);
    Function<Headers, String> getToken = headers -> getToken(headers, registryRestClient);
    return dockerRegistryUtils.getLabels(registryRestClient, getToken, "", imageName, buildNos);
  }

  private String getToken(Headers headers, DockerRegistryRestClient registryRestClient) {
    String authHeaderValue = headers.get("Www-Authenticate");
    if (!cachedBearerTokens.containsKey(authHeaderValue)) {
      DockerRegistryToken dockerRegistryToken = fetchPublicToken(registryRestClient, authHeaderValue);
      if (dockerRegistryToken != null) {
        if (dockerRegistryToken.getExpires_in() != null) {
          cachedBearerTokens.put(authHeaderValue, dockerRegistryToken.getToken(), ExpirationPolicy.CREATED,
              dockerRegistryToken.getExpires_in(), TimeUnit.SECONDS);
        } else {
          return dockerRegistryToken.getToken();
        }
      }
    }
    return cachedBearerTokens.get(authHeaderValue);
  }

  private DockerRegistryToken fetchPublicToken(DockerRegistryRestClient registryRestClient, String authHeaderValue) {
    try {
      Map<String, String> tokens = DockerRegistryUtils.extractAuthChallengeTokens(authHeaderValue);
      if (tokens != null) {
        DockerRegistryToken registryToken =
            registryRestClient.getPublicToken(tokens.get("realm"), tokens.get("service"), tokens.get("scope"))
                .execute()
                .body();
        if (registryToken != null) {
          tokens.putIfAbsent(authHeaderValue, registryToken.getToken());
          return registryToken;
        }
      }
    } catch (IOException e) {
      logger.warn("Exception occurred while fetching public token", e);
    }
    return null;
  }
}
