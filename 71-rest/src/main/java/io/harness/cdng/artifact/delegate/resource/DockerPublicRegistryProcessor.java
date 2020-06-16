package io.harness.cdng.artifact.delegate.resource;

import static io.harness.exception.WingsException.USER;
import static software.wings.helpers.ext.docker.DockerRegistryServiceImpl.isSuccessful;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import io.harness.cdng.artifact.bean.ArtifactAttributes;
import io.harness.cdng.artifact.bean.DockerArtifactAttributes;
import io.harness.cdng.artifact.bean.connector.DockerhubConnectorConfig;
import io.harness.cdng.artifact.delegate.beans.DockerPublicImageTagResponse;
import io.harness.data.structure.EmptyPredicate;
import io.harness.expression.RegexFunctor;
import io.harness.network.Http;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.exception.InvalidArtifactServerException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class DockerPublicRegistryProcessor {
  @VisibleForTesting
  DockerRegistryRestClient getDockerRegistryRestClient(DockerhubConnectorConfig connectorConfig) {
    OkHttpClient okHttpClient = Http.getUnsafeOkHttpClient(connectorConfig.getRegistryUrl());
    Retrofit retrofit = new Retrofit.Builder()
                            .client(okHttpClient)
                            .baseUrl(connectorConfig.getRegistryUrl())
                            .addConverterFactory(JacksonConverterFactory.create())
                            .build();
    return retrofit.create(DockerRegistryRestClient.class);
  }

  public ArtifactAttributes getLastSuccessfulBuild(
      DockerhubConnectorConfig connectorConfig, String imageName, String tag) throws IOException {
    DockerRegistryRestClient registryRestClient = getDockerRegistryRestClient(connectorConfig);
    Response<DockerPublicImageTagResponse.Result> response =
        registryRestClient.getPublicImageTag(imageName, tag).execute();

    if (!isSuccessful(response)) {
      throw new InvalidArtifactServerException(response.message(), USER);
    }
    DockerArtifactAttributes dockerArtifactAttributes =
        processSingleResultResponse(response.body(), imageName, connectorConfig);
    if (dockerArtifactAttributes == null) {
      throw new InvalidArtifactServerException("Didn't get last successful build", USER);
    }
    return dockerArtifactAttributes;
  }

  public ArtifactAttributes getLastSuccessfulBuildFromRegex(
      DockerhubConnectorConfig connectorConfig, String imageName, String tagRegex) throws IOException {
    DockerRegistryRestClient registryRestClient = getDockerRegistryRestClient(connectorConfig);
    Response<DockerPublicImageTagResponse> response =
        registryRestClient.listPublicImageTags(imageName, null, DockerRegistryService.MAX_NO_OF_TAGS_PER_PUBLIC_IMAGE)
            .execute();

    if (!isSuccessful(response)) {
      throw new InvalidArtifactServerException(response.message(), USER);
    }

    List<DockerArtifactAttributes> dockerArtifactAttributes =
        paginate(response.body(), connectorConfig, imageName, registryRestClient);
    List<DockerArtifactAttributes> attributesList =
        dockerArtifactAttributes.stream()
            .filter(artifact -> new RegexFunctor().match(tagRegex, artifact.getTag()))
            .sorted(DockerArtifactAttributes::compareTo)
            .collect(Collectors.toList());

    if (attributesList.isEmpty()) {
      throw new InvalidArtifactServerException("Didn't get last successful build", USER);
    }
    return attributesList.get(0);
  }

  @VisibleForTesting
  List<DockerArtifactAttributes> paginate(DockerPublicImageTagResponse tagsPage, DockerhubConnectorConfig dockerConfig,
      String imageName, DockerRegistryRestClient registryRestClient) throws IOException {
    // process first page
    List<DockerArtifactAttributes> details = processPageResponse(tagsPage, dockerConfig, imageName);

    if (details.size() >= DockerRegistryService.MAX_NO_OF_TAGS_PER_PUBLIC_IMAGE || tagsPage == null
        || tagsPage.getNext() == null) {
      return details.stream().limit(DockerRegistryService.MAX_NO_OF_TAGS_PER_PUBLIC_IMAGE).collect(Collectors.toList());
    }

    HttpUrl nextPageUrl = HttpUrl.parse(tagsPage.getNext());
    String nextPageNum = nextPageUrl == null ? null : nextPageUrl.queryParameter("page");

    // process rest of pages
    while (EmptyPredicate.isNotEmpty(nextPageNum)) {
      Response<DockerPublicImageTagResponse> pageResponse =
          registryRestClient
              .listPublicImageTags(
                  imageName, Integer.valueOf(nextPageNum), DockerRegistryService.MAX_NO_OF_TAGS_PER_PUBLIC_IMAGE)
              .execute();

      if (!isSuccessful(pageResponse)) {
        throw new InvalidArtifactServerException(pageResponse.message(), USER);
      }
      DockerPublicImageTagResponse page = pageResponse.body();
      List<DockerArtifactAttributes> pageDetails = processPageResponse(page, dockerConfig, imageName);
      details.addAll(pageDetails);
      if (details.size() >= DockerRegistryService.MAX_NO_OF_TAGS_PER_PUBLIC_IMAGE || page == null
          || page.getNext() == null) {
        break;
      }
      nextPageUrl = HttpUrl.parse(page.getNext());
      nextPageNum = nextPageUrl == null ? null : nextPageUrl.queryParameter("page");
    }

    return details.stream().limit(DockerRegistryService.MAX_NO_OF_TAGS_PER_PUBLIC_IMAGE).collect(Collectors.toList());
  }

  @VisibleForTesting
  List<DockerArtifactAttributes> processPageResponse(
      DockerPublicImageTagResponse publicImageTags, DockerhubConnectorConfig dockerConfig, String imageName) {
    if (publicImageTags != null && EmptyPredicate.isNotEmpty(publicImageTags.getResults())) {
      return publicImageTags.getResults()
          .stream()
          .map(tag -> processSingleResultResponse(tag, imageName, dockerConfig))
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

  @VisibleForTesting
  DockerArtifactAttributes processSingleResultResponse(
      DockerPublicImageTagResponse.Result publicImageTag, String imageName, DockerhubConnectorConfig connectorConfig) {
    if (publicImageTag != null) {
      return DockerArtifactAttributes.builder()
          .dockerHubConnector(connectorConfig.getIdentifier())
          .imagePath(imageName)
          .tag(publicImageTag.getName())
          .build();
    } else {
      logger.warn("Docker public image tag response was null.");
      return null;
    }
  }
}
