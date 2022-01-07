/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.artifactory;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.ARTIFACT_SERVER_ERROR;
import static io.harness.exception.WingsException.ReportTarget;
import static io.harness.exception.WingsException.USER;
import static io.harness.network.Http.connectableHttpUrl;

import static org.jfrog.artifactory.client.ArtifactoryRequest.ContentType.JSON;
import static org.jfrog.artifactory.client.ArtifactoryRequest.Method.GET;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ArtifactoryServerException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.network.Http;

import java.net.SocketTimeoutException;
import java.util.EnumSet;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpResponseException;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.jfrog.artifactory.client.ArtifactoryRequest;
import org.jfrog.artifactory.client.ArtifactoryResponse;
import org.jfrog.artifactory.client.ProxyConfig;
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl;

@Slf4j
@OwnedBy(CDC)
public class ArtifactoryClientImpl {
  public boolean validateArtifactServer(ArtifactoryConfigRequest config) {
    if (!connectableHttpUrl(getBaseUrl(config))) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Check if the Artifactory URL is reachable from your delegate(s)",
          "The given artifactory URL is not reachable",
          new ArtifactoryServerException("Could not reach Artifactory Server at : " + config.getArtifactoryUrl(),
              ErrorCode.INVALID_ARTIFACT_SERVER, USER));
    }
    return isRunning(config);
  }

  public boolean isRunning(ArtifactoryConfigRequest artifactoryConfig) {
    log.info("Validating artifactory server");
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig);
    ArtifactoryRequest repositoryRequest =
        new ArtifactoryRequestImpl().apiUrl("api/repositories/").method(GET).responseType(JSON);
    try {
      ArtifactoryResponse artifactoryResponse = artifactory.restCall(repositoryRequest);
      handleErrorResponse(artifactoryResponse);
      log.info("Validating artifactory server success");
    } catch (RuntimeException e) {
      log.error("Runtime exception occurred while validating artifactory", e);
      handleAndRethrow(e, USER);
    } catch (SocketTimeoutException e) {
      log.error("Exception occurred while validating artifactory", e);
      return true;
    } catch (Exception e) {
      log.error("Exception occurred while validating artifactory", e);
      handleAndRethrow(e, USER);
    }
    return true;
  }

  public static void handleErrorResponse(ArtifactoryResponse artifactoryResponse) throws java.io.IOException {
    if (!artifactoryResponse.isSuccessResponse()) {
      if (artifactoryResponse.getStatusLine().getStatusCode() == 407) {
        throw NestedExceptionUtils.hintWithExplanationException(
            "The proxy settings may be not be configured correctly for Artifactory or the Delegate",
            "The Artifactory Server responded with status code 407",
            new InvalidRequestException(artifactoryResponse.getStatusLine().getReasonPhrase()));
      }
      if (artifactoryResponse.getStatusLine().getStatusCode() == 404) {
        throw NestedExceptionUtils.hintWithExplanationException(
            "Check if the URL is correct. Consider appending `/artifactory` to the connector endpoint if you have not already.",
            "Artifactory connector URL may be incorrect or the server is down or the server is not reachable from the delegate",
            new ArtifactoryServerException(
                "Artifactory Server responded with Not Found.", ErrorCode.INVALID_ARTIFACT_SERVER, USER));
      }
      ArtifactoryErrorResponse errorResponse = artifactoryResponse.parseBody(ArtifactoryErrorResponse.class);
      String errorMessage =
          "Request to server failed with status code: " + artifactoryResponse.getStatusLine().getStatusCode();
      if (isNotEmpty(errorResponse.getErrors())) {
        errorMessage +=
            " with message - " + errorResponse.getErrors().stream().map(ArtifactoryError::getMessage).findFirst().get();
      }
      throw NestedExceptionUtils.hintWithExplanationException(
          "The server could have failed authenticate. Please check your credentials", errorMessage,
          new ArtifactoryServerException(errorMessage, ErrorCode.INVALID_ARTIFACT_SERVER, USER));
    }
  }

  public static void handleAndRethrow(Exception e, EnumSet<ReportTarget> reportTargets) {
    if (e instanceof HttpResponseException) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "The server could have failed authenticate. Please check your credentials", e.getMessage(),
          new ArtifactoryServerException(e.getMessage(), ErrorCode.INVALID_ARTIFACT_SERVER, reportTargets));
    }
    if (e instanceof SocketTimeoutException) {
      String serverMayNotBeRunningMessaage = e.getMessage() + "."
          + "SocketTimeout: Artifactory server may not be running";
      throw NestedExceptionUtils.hintWithExplanationException(
          "Check if the URL is correct. Consider appending `/artifactory` to the endpoint if you have not already.",
          "Artifactory connector URL may be incorrect or the server may be down or the server may not be reachable from the delegate",
          new ArtifactoryServerException(
              serverMayNotBeRunningMessaage, ErrorCode.INVALID_ARTIFACT_SERVER, reportTargets));
    }
    if (e instanceof WingsException) {
      throw(WingsException) e;
    }
    throw NestedExceptionUtils.hintWithExplanationException(
        "Check if the URL is correct. Consider appending `/artifactory` to the endpoint if you have not already.",
        "Artifactory connector URL may be incorrect or the server may be down or the server may not be reachable from the delegate",
        new ArtifactoryServerException(ExceptionUtils.getMessage(e), ARTIFACT_SERVER_ERROR, reportTargets, e));
  }

  public static Artifactory getArtifactoryClient(ArtifactoryConfigRequest artifactoryConfig) {
    ArtifactoryClientBuilder builder = ArtifactoryClientBuilder.create();
    try {
      builder.setUrl(getBaseUrl(artifactoryConfig));
      if (artifactoryConfig.isHasCredentials()) {
        if (isEmpty(artifactoryConfig.getPassword())) {
          throw NestedExceptionUtils.hintWithExplanationException("Provide a password with username",
              "Password is blank. It is a required field",
              new ArtifactoryServerException(
                  "Password is a required field along with Username", ErrorCode.INVALID_ARTIFACT_SERVER, USER));
        }
        builder.setUsername(artifactoryConfig.getUsername());
        builder.setPassword(new String(artifactoryConfig.getPassword()));
      } else {
        log.info("Username is not set for artifactory config {} . Will use anonymous access.",
            artifactoryConfig.getArtifactoryUrl());
      }

      HttpHost httpProxyHost = Http.getHttpProxyHost(artifactoryConfig.getArtifactoryUrl());
      if (httpProxyHost != null) {
        builder.setProxy(new ProxyConfig(httpProxyHost.getHostName(), httpProxyHost.getPort(), Http.getProxyScheme(),
            Http.getProxyUserName(), Http.getProxyPassword()));
      }
      builder.setSocketTimeout(30000);
      builder.setConnectionTimeout(30000);
    } catch (Exception ex) {
      handleAndRethrow(ex, USER);
    }
    return builder.build();
  }

  public static String getBaseUrl(ArtifactoryConfigRequest artifactoryConfig) {
    return artifactoryConfig.getArtifactoryUrl().endsWith("/") ? artifactoryConfig.getArtifactoryUrl()
                                                               : artifactoryConfig.getArtifactoryUrl() + "/";
  }

  @Data
  public static class ArtifactoryErrorResponse {
    List<ArtifactoryError> errors;
  }

  @Data
  public static class ArtifactoryError {
    String message;
    int status;
  }
}
