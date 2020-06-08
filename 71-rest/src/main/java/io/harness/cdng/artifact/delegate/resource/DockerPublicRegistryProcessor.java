package io.harness.cdng.artifact.delegate.resource;

import static io.harness.exception.WingsException.USER;
import static software.wings.helpers.ext.docker.DockerRegistryServiceImpl.isSuccessful;

import com.google.inject.Singleton;

import io.harness.cdng.artifact.bean.ArtifactAttributes;
import io.harness.cdng.artifact.bean.DockerArtifactAttributes;
import io.harness.cdng.artifact.bean.connector.DockerhubConnectorConfig;
import io.harness.cdng.artifact.delegate.beans.DockerPublicImageTagResponse;
import io.harness.network.Http;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.exception.InvalidArtifactServerException;

import java.io.IOException;

@Singleton
@Slf4j
public class DockerPublicRegistryProcessor {
  private DockerRegistryRestClient getDockerRegistryRestClient(DockerhubConnectorConfig connectorConfig) {
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
        processPageResponse(response.body(), imageName, connectorConfig);
    if (dockerArtifactAttributes == null) {
      throw new InvalidArtifactServerException("Didn't get last successful build", USER);
    }
    return dockerArtifactAttributes;
  }

  private DockerArtifactAttributes processPageResponse(
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
