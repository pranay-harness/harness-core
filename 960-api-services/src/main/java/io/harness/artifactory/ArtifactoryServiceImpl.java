package io.harness.artifactory;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.ARTIFACT_SERVER_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.network.Http.connectableHttpUrl;

import static org.jfrog.artifactory.client.ArtifactoryRequest.ContentType.JSON;
import static org.jfrog.artifactory.client.ArtifactoryRequest.Method.GET;

import io.harness.eraro.ErrorCode;
import io.harness.exception.ArtifactoryServerException;
import io.harness.exception.WingsException;
import io.harness.network.Http;

import java.net.SocketTimeoutException;
import java.util.EnumSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpResponseException;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.jfrog.artifactory.client.ArtifactoryRequest;
import org.jfrog.artifactory.client.ProxyConfig;
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl;

@Slf4j
public class ArtifactoryServiceImpl {
  public boolean validateArtifactServer(ArtifactoryConfigRequest config) {
    if (!connectableHttpUrl(getBaseUrl(config))) {
      throw new ArtifactoryServerException("Could not reach Artifactory Server at : " + config.getArtifactoryUrl(),
          ErrorCode.INVALID_ARTIFACT_SERVER, USER);
    }
    return isRunning(config);
  }

  public boolean isRunning(ArtifactoryConfigRequest artifactoryConfig) {
    log.info("Validating artifactory server");
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig);
    ArtifactoryRequest repositoryRequest =
        new ArtifactoryRequestImpl().apiUrl("api/repositories/").method(GET).responseType(JSON);
    try {
      artifactory.restCall(repositoryRequest);
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
  private void handleAndRethrow(Exception e, EnumSet<WingsException.ReportTarget> reportTargets) {
    if (e instanceof HttpResponseException) {
      throw new ArtifactoryServerException(e.getMessage(), ErrorCode.INVALID_ARTIFACT_SERVER, reportTargets);
    }
    if (e instanceof SocketTimeoutException) {
      String serverMayNotBeRunningMessaage = e.getMessage() + "."
          + "SocketTimeout: Artifactory server may not be running";
      throw new ArtifactoryServerException(
          serverMayNotBeRunningMessaage, ErrorCode.INVALID_ARTIFACT_SERVER, reportTargets);
    }
    if (e instanceof WingsException) {
      throw(WingsException) e;
    }
    throw new ArtifactoryServerException(ExceptionUtils.getMessage(e), ARTIFACT_SERVER_ERROR, reportTargets, e);
  }

  private Artifactory getArtifactoryClient(ArtifactoryConfigRequest artifactoryConfig) {
    ArtifactoryClientBuilder builder = ArtifactoryClientBuilder.create();
    try {
      builder.setUrl(getBaseUrl(artifactoryConfig));
      if (artifactoryConfig.isHasCredentials()) {
        if (isEmpty(artifactoryConfig.getPassword())) {
          throw new ArtifactoryServerException(
              "Password is a required field along with Username", ErrorCode.INVALID_ARTIFACT_SERVER, USER);
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

  private String getBaseUrl(ArtifactoryConfigRequest artifactoryConfig) {
    return artifactoryConfig.getArtifactoryUrl().endsWith("/") ? artifactoryConfig.getArtifactoryUrl()
                                                               : artifactoryConfig.getArtifactoryUrl() + "/";
  }
}
