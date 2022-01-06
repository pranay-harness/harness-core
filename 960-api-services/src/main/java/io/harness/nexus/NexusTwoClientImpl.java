/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.nexus;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER;
import static io.harness.nexus.NexusHelper.isSuccessful;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidArtifactServerException;
import io.harness.exception.NestedExceptionUtils;

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

@OwnedBy(CDC)
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
    if (response.code() == 404) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Check if the Nexus URL & Nexus version are correct. Nexus URLs are different for different Nexus versions",
          "The Nexus URL or the version for the connector is incorrect",
          new InvalidArtifactServerException("Invalid Nexus connector details", USER));
    }
    if (isSuccessful(response)) {
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
