package io.harness.cdng.artifact.utils;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.common.VerificationConstants.CONNECTOR;

import com.google.common.hash.Hashing;

import io.harness.cdng.artifact.bean.ArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.connector.ConnectorConfig;
import io.harness.cdng.artifact.bean.connector.DockerhubConnectorConfig;
import io.harness.cdng.artifact.delegate.task.ArtifactTaskParameters;
import io.harness.exception.InvalidRequestException;
import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

@UtilityClass
public class ArtifactUtils {
  public final String PRIMARY_ARTIFACT = "primary";
  public final String SIDECAR_ARTIFACT = "sidecar";

  public void appendIfNecessary(StringBuilder keyBuilder, String value) {
    if (keyBuilder == null) {
      throw new InvalidRequestException("Key string builder cannot be null");
    }
    if (isNotEmpty(value)) {
      keyBuilder.append(CONNECTOR).append(value);
    }
  }

  // TODO(archit): Check whether string should be case sensitive or not.
  public String generateUniqueHashFromStringList(List<String> valuesList) {
    valuesList.sort(Comparator.nullsLast(String::compareTo));
    StringBuilder keyBuilder = new StringBuilder();
    valuesList.forEach(s -> appendIfNecessary(keyBuilder, s));
    return Hashing.sha256().hashString(keyBuilder.toString(), StandardCharsets.UTF_8).toString();
  }

  public ArtifactTaskParameters getArtifactTaskParameters(String accountId, ArtifactSourceAttributes sourceAttributes) {
    return ArtifactTaskParameters.builder()
        .accountId(accountId)
        .attributes(sourceAttributes)
        .connectorConfig(getConnectorConfig(sourceAttributes))
        .build();
  }

  // TODO(archit): will call connector corresponding to connector identifier, accountID, projectId.
  ConnectorConfig getConnectorConfig(ArtifactSourceAttributes artifactSourceAttributes) {
    DockerArtifactSourceAttributes sourceAttributes = (DockerArtifactSourceAttributes) artifactSourceAttributes;
    return DockerhubConnectorConfig.builder()
        .registryUrl(sourceAttributes.getDockerhubConnector())
        .identifier(sourceAttributes.getDockerhubConnector())
        .build();
  }
}
