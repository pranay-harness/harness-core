package io.harness.cdng.artifact.delegate.resource;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.artifact.bean.ArtifactAttributes;
import io.harness.cdng.artifact.bean.connector.DockerhubConnectorConfig;
import io.harness.delegate.exception.ArtifactServerException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class DockerRegistryServiceImpl implements DockerRegistryService {
  @Inject private DockerPublicRegistryProcessor dockerPublicRegistryProcessor;

  @Override
  public ArtifactAttributes getLastSuccessfulBuild(
      DockerhubConnectorConfig connectorConfig, String imageName, String tag) {
    try {
      return dockerPublicRegistryProcessor.getLastSuccessfulBuild(connectorConfig, imageName, tag);
    } catch (Exception exception) {
      throw new ArtifactServerException(ExceptionUtils.getMessage(exception), exception, WingsException.USER);
    }
  }

  @Override
  public ArtifactAttributes getLastSuccessfulBuildFromRegex(
      DockerhubConnectorConfig connectorConfig, String imageName, String tagRegex) {
    try {
      return dockerPublicRegistryProcessor.getLastSuccessfulBuildFromRegex(connectorConfig, imageName, tagRegex);
    } catch (Exception exception) {
      throw new ArtifactServerException(ExceptionUtils.getMessage(exception), exception, WingsException.USER);
    }
  }
}
