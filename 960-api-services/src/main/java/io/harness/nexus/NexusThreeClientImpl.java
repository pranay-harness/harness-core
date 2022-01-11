/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.nexus;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.nexus.NexusHelper.isSuccessful;

import static java.util.Collections.emptyMap;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidArtifactServerException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.nexus.model.Nexus3Repository;

import software.wings.utils.RepositoryFormat;

import com.google.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import retrofit2.Response;
import retrofit2.converter.jackson.JacksonConverterFactory;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class NexusThreeClientImpl {
  public Map<String, String> getRepositories(NexusRequest nexusConfig, String repositoryFormat) throws IOException {
    log.info("Retrieving repositories");
    NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig);
    Response<List<Nexus3Repository>> response;
    if (nexusConfig.isHasCredentials()) {
      response =
          nexusThreeRestClient
              .listRepositories(Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())))
              .execute();
    } else {
      response = nexusThreeRestClient.listRepositories().execute();
    }

    if (isSuccessful(response)) {
      if (isNotEmpty(response.body())) {
        log.info("Retrieving {} repositories success", repositoryFormat);
        final Map<String, String> repositories;
        if (repositoryFormat == null) {
          repositories =
              response.body().stream().collect(Collectors.toMap(Nexus3Repository::getName, Nexus3Repository::getName));
        } else {
          final String filterBy = repositoryFormat.equals(RepositoryFormat.maven.name()) ? "maven2" : repositoryFormat;
          repositories = response.body()
                             .stream()
                             .filter(o -> o.getFormat().equals(filterBy))
                             .collect(Collectors.toMap(Nexus3Repository::getName, Nexus3Repository::getName));
        }
        log.info("Retrieved repositories are {}", repositories.values());
        return repositories;
      } else {
        throw NestedExceptionUtils.hintWithExplanationException(
            "Check if the connector details - URL & credentials are correct",
            "No repositories were found for the connector",
            new InvalidArtifactServerException("Failed to fetch the repositories", WingsException.USER));
      }
    }
    log.info("No repositories found returning empty map");
    return emptyMap();
  }

  public NexusThreeRestClient getNexusThreeClient(NexusRequest nexusConfig) {
    return NexusHelper.getRetrofit(nexusConfig, JacksonConverterFactory.create()).create(NexusThreeRestClient.class);
  }

  public boolean isServerValid(NexusRequest nexusConfig) throws IOException {
    log.info("Validate if nexus is running by retrieving repositories");
    NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig);
    Response<List<Nexus3Repository>> response;
    if (nexusConfig.isHasCredentials()) {
      response =
          nexusThreeRestClient
              .listRepositories(Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())))
              .execute();
    } else {
      response = nexusThreeRestClient.listRepositories().execute();
    }
    if (response == null) {
      return false;
    }

    if (response.code() == 404) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Check if the Nexus URL & Nexus version are correct. Nexus URLs are different for different Nexus versions",
          "The Nexus URL or the version for the connector is incorrect",
          new InvalidArtifactServerException("Invalid Nexus connector details"));
    }
    return NexusHelper.isSuccessful(response);
  }
}
