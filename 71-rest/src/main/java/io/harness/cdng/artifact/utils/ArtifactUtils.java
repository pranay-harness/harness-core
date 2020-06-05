package io.harness.cdng.artifact.utils;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.common.VerificationConstants.CONNECTOR;

import com.google.common.hash.Hashing;

import io.harness.cdng.artifact.bean.artifactsource.ArtifactSource;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.connector.ConnectorConfig;
import io.harness.cdng.artifact.bean.connector.DockerhubConnectorConfig;
import io.harness.cdng.artifact.delegate.task.ArtifactTaskParameters;
import io.harness.exception.InvalidRequestException;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

public interface ArtifactUtils {
  static void appendIfNecessary(StringBuilder keyBuilder, String value) {
    if (keyBuilder == null) {
      throw new InvalidRequestException("Key string builder cannot be null");
    }
    if (isNotEmpty(value)) {
      keyBuilder.append(CONNECTOR).append(value);
    }
  }

  // TODO(archit): Check whether string should be case sensitive or not.
  static String generateUniqueHashFromStringList(List<String> valuesList) {
    valuesList.sort(Comparator.nullsLast(String::compareTo));
    StringBuilder keyBuilder = new StringBuilder();
    valuesList.forEach(s -> appendIfNecessary(keyBuilder, s));
    return Hashing.sha256().hashString(keyBuilder.toString(), StandardCharsets.UTF_8).toString();
  }

  static ArtifactTaskParameters getArtifactTaskParameters(ArtifactSource artifactSource) {
    return ArtifactTaskParameters.builder()
        .accountId(artifactSource.getAccountId())
        .attributes(artifactSource.getSourceAttributes())
        .connectorConfig(getConnectorConfig(artifactSource))
        .build();
  }

  // TODO(archit): will call connector corresponding to connector identifier, accountID, projectId.
  static ConnectorConfig getConnectorConfig(ArtifactSource artifactSource) {
    DockerArtifactSourceAttributes sourceAttributes =
        (DockerArtifactSourceAttributes) artifactSource.getSourceAttributes();
    return DockerhubConnectorConfig.builder().registryUrl(sourceAttributes.getDockerhubConnector()).build();
  }
}
