package io.harness.cdng.artifact.utils;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.common.VerificationConstants.CONNECTOR;

import com.google.common.hash.Hashing;

import io.harness.cdng.artifact.bean.ArtifactConfigWrapper;
import io.harness.cdng.artifact.bean.ArtifactOutcome;
import io.harness.cdng.artifact.bean.ArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.SidecarArtifactWrapper;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.connector.ConnectorConfig;
import io.harness.cdng.artifact.bean.connector.DockerhubConnectorConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.delegate.task.ArtifactTaskParameters;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class ArtifactUtils {
  public final String PRIMARY_ARTIFACT = "primary";
  public final String SIDECAR_ARTIFACT = "sidecars";

  public boolean isPrimaryArtifact(ArtifactConfigWrapper artifactConfigWrapper) {
    return artifactConfigWrapper.getArtifactType().equals(PRIMARY_ARTIFACT);
  }

  public String getArtifactKey(ArtifactConfigWrapper artifactConfigWrapper) {
    return isPrimaryArtifact(artifactConfigWrapper)
        ? artifactConfigWrapper.getIdentifier()
        : artifactConfigWrapper.getArtifactType() + "." + artifactConfigWrapper.getIdentifier();
  }

  public boolean isPrimaryArtifact(ArtifactOutcome artifactOutcome) {
    return artifactOutcome.getArtifactType().equals(PRIMARY_ARTIFACT);
  }

  public List<ArtifactConfigWrapper> convertArtifactListIntoArtifacts(ArtifactListConfig artifactListConfig) {
    List<ArtifactConfigWrapper> artifacts = new LinkedList<>();
    if (artifactListConfig == null) {
      return artifacts;
    }
    if (artifactListConfig.getPrimary() != null) {
      artifacts.add(artifactListConfig.getPrimary());
    }
    if (EmptyPredicate.isNotEmpty(artifactListConfig.getSidecars())) {
      artifacts.addAll(artifactListConfig.getSidecars()
                           .stream()
                           .map(SidecarArtifactWrapper::getArtifact)
                           .collect(Collectors.toList()));
    }
    return artifacts;
  }

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
