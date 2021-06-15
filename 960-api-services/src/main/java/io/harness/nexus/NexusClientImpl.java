package io.harness.nexus;

import static io.harness.eraro.ErrorCode.INVALID_ARTIFACT_SERVER;
import static io.harness.exception.WingsException.USER;

import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import io.harness.concurrent.HTimeLimiter;
import io.harness.exception.ArtifactServerException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArtifactServerException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import software.wings.utils.RepositoryFormat;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.Map;
import javax.net.ssl.SSLHandshakeException;
import javax.xml.stream.XMLStreamException;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class NexusClientImpl {
  @Inject TimeLimiter timeLimiter;
  @Inject NexusThreeClientImpl nexusThreeService;
  @Inject NexusTwoClientImpl nexusTwoService;

  public Map<String, String> getRepositories(NexusRequest nexusConfig, String repositoryFormat) {
    try {
      boolean isNexusTwo = nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x");
      return HTimeLimiter.callInterruptible(timeLimiter, Duration.ofSeconds(20), () -> {
        if (isNexusTwo) {
          if (RepositoryFormat.docker.name().equals(repositoryFormat)) {
            throw new WingsException(INVALID_ARTIFACT_SERVER, USER)
                .addParam("message", "Nexus 2.x does not support Docker artifact type");
          }
          return nexusTwoService.getRepositories(nexusConfig, repositoryFormat);
        } else {
          if (repositoryFormat == null) {
            throw new InvalidRequestException("Not supported for nexus 3.x", USER);
          }
          return nexusThreeService.getRepositories(nexusConfig, repositoryFormat);
        }
      });
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error occurred while retrieving Repositories from Nexus server " + nexusConfig.getNexusUrl(), e);
      if (e.getCause() != null && e.getCause() instanceof XMLStreamException) {
        throw new WingsException(INVALID_ARTIFACT_SERVER, USER).addParam("message", "Nexus may not be running");
      }
      checkSSLHandshakeException(e);
      return emptyMap();
    }
  }

  public boolean isRunning(NexusRequest nexusConfig) {
    if (nexusConfig.getVersion() == null || nexusConfig.getVersion().equalsIgnoreCase("2.x")) {
      return getRepositories(nexusConfig, null) != null;
    } else {
      try {
        return nexusThreeService.isServerValid(nexusConfig);
      } catch (InvalidArtifactServerException e) {
        throw e;
      } catch (WingsException e) {
        if (ExceptionUtils.getMessage(e).contains("Invalid Nexus credentials")) {
          throw e;
        }
        return true;
      } catch (Exception e) {
        log.warn("Failed to retrieve repositories. Ignoring validation for Nexus 3 for now. User can give custom path");
        checkSSLHandshakeException(e);
        return true;
      }
    }
  }

  private void checkSSLHandshakeException(Exception e) {
    if (e.getCause() instanceof SSLHandshakeException
        || ExceptionUtils.getMessage(e).contains("unable to find valid certification path")) {
      throw new ArtifactServerException("Certificate validation failed:" + getRootCauseMessage(e), e);
    }
  }
}
