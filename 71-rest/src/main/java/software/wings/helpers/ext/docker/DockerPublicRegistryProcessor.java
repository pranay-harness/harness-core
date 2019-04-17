package software.wings.helpers.ext.docker;

import static io.harness.exception.WingsException.USER;
import static software.wings.common.Constants.IMAGE;
import static software.wings.common.Constants.TAG;
import static software.wings.helpers.ext.docker.DockerRegistryServiceImpl.isSuccessful;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.network.Http;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import retrofit2.Response;
import software.wings.beans.DockerConfig;
import software.wings.helpers.ext.docker.client.DockerRestClientFactory;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class DockerPublicRegistryProcessor {
  @Inject private DockerRestClientFactory dockerRestClientFactory;

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
  private List<BuildDetails> paginate(DockerPublicImageTagResponse tagsPage, DockerConfig dockerConfig,
      String imageName, DockerRegistryRestClient registryRestClient, int limit) throws IOException {
    // process first page
    List<BuildDetails> details = processPage(tagsPage, dockerConfig, imageName);

    if (details.size() >= limit || tagsPage.getNext() == null) {
      return details.stream().limit(limit).collect(Collectors.toList());
    }

    HttpUrl nextPageUrl = HttpUrl.parse(tagsPage.getNext());
    String nextPageNum = nextPageUrl.queryParameter("page");

    // process rest of pages
    while (EmptyPredicate.isNotEmpty(nextPageNum)) {
      DockerPublicImageTagResponse page =
          registryRestClient.listPublicImageTags(imageName, Integer.valueOf(nextPageNum), limit).execute().body();
      List<BuildDetails> pageDetails = processPage(page, dockerConfig, imageName);
      details.addAll(pageDetails);

      if (details.size() >= limit || page.getNext() == null) {
        break;
      }

      nextPageUrl = HttpUrl.parse(page.getNext());
      nextPageNum = nextPageUrl.queryParameter("page");
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
            Map<String, String> metadata = new HashMap();
            metadata.put(IMAGE, domainName + "/" + imageName + ":" + tag.getName());
            metadata.put(TAG, tag.getName());
            return aBuildDetails()
                .withNumber(tag.getName())
                .withBuildUrl(tagUrl + tag.getName())
                .withMetadata(metadata)
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
}
