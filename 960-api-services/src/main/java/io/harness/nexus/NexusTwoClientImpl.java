package io.harness.nexus;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

import software.wings.utils.RepositoryFormat;

import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import org.sonatype.nexus.rest.model.RepositoryListResource;
import org.sonatype.nexus.rest.model.RepositoryListResourceResponse;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

@Slf4j
public class NexusTwoClientImpl {
  public Map<String, String> getRepositories(NexusRequest nexusConfig, String repositoryFormat) throws IOException {
    log.info("Retrieving repositories");
    final Call<RepositoryListResourceResponse> request;
    if (nexusConfig.isHasCredentials()) {
      request =
          getRestClient(nexusConfig)
              .getAllRepositories(Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())));
    } else {
      request = getRestClient(nexusConfig).getAllRepositories();
    }

    final Response<RepositoryListResourceResponse> response = request.execute();
    if (NexusHelper.isSuccessful(response)) {
      log.info("Retrieving repositories success");
      if (RepositoryFormat.maven.name().equals(repositoryFormat)) {
        return response.body()
            .getData()
            .stream()
            .filter(repositoryListResource -> "maven2".equals(repositoryListResource.getFormat()))
            .collect(toMap(RepositoryListResource::getId, RepositoryListResource::getName));
      } else if (RepositoryFormat.nuget.name().equals(repositoryFormat)
          || RepositoryFormat.npm.name().equals(repositoryFormat)) {
        return response.body()
            .getData()
            .stream()
            .filter(repositoryListResource -> repositoryFormat.equals(repositoryListResource.getFormat()))
            .collect(toMap(RepositoryListResource::getId, RepositoryListResource::getName));
      }
      return response.body().getData().stream().collect(
          toMap(RepositoryListResource::getId, RepositoryListResource::getName));
    }
    log.info("No repositories found returning empty map");
    return emptyMap();
  }

  private NexusRestClient getRestClient(NexusRequest nexusConfig) {
    return NexusHelper.getRetrofit(nexusConfig, SimpleXmlConverterFactory.createNonStrict())
        .create(NexusRestClient.class);
  }
}
