package io.harness.cdng.artifact.utils;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.common.VerificationConstants.CONNECTOR;

import com.google.common.hash.Hashing;

import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.bean.SidecarArtifactWrapper;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
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

  public String getArtifactKey(ArtifactConfig artifactConfig) {
    return artifactConfig.isPrimaryArtifact() ? artifactConfig.getIdentifier()
                                              : SIDECAR_ARTIFACT + "." + artifactConfig.getIdentifier();
  }

  public List<ArtifactConfig> convertArtifactListIntoArtifacts(ArtifactListConfig artifactListConfig) {
    List<ArtifactConfig> artifacts = new LinkedList<>();
    if (artifactListConfig == null) {
      return artifacts;
    }
    if (artifactListConfig.getPrimary() != null) {
      artifacts.add(artifactListConfig.getPrimary().getArtifactConfig());
    }
    if (EmptyPredicate.isNotEmpty(artifactListConfig.getSidecars())) {
      artifacts.addAll(artifactListConfig.getSidecars()
                           .stream()
                           .map(SidecarArtifactWrapper::getArtifactConfig)
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
}
