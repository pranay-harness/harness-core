package io.harness.ngpipeline.artifact.bean;

import io.harness.ngpipeline.pipeline.executions.beans.ArtifactSummary;
import io.harness.ngpipeline.pipeline.executions.beans.GcrArtifactSummary;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@EqualsAndHashCode(callSuper = false)
@TypeAlias("gcrArtifactOutcome")
@JsonTypeName("gcrArtifactOutcome")
// TODO : Create a shared Module b/w pipline and CD/CI where these entities can go to and eventually We need to
// deprecate that module 878-pms-coupling
// @TargetModule(878-pms-coupling)
public class GcrArtifactOutcome implements ArtifactOutcome {
  /** Docker hub registry connector. */
  String connectorRef;
  /** Images in repos need to be referenced via a path. */
  String imagePath;
  /** Tag refers to exact tag number. */
  String tag;
  /** Tag regex is used to get latest build from builds matching regex. */
  String tagRegex;
  /** RegistryHostname */
  String registryHostname;
  /** Identifier for artifact. */
  String identifier;
  /** Type to identify whether primary and sidecars artifact. */
  String artifactType;
  /** Whether this config corresponds to primary artifact.*/
  boolean primaryArtifact;
  /** registryHostName/imagePath:tag */
  String image;
  /** imagePullSecret for Gcr credentials base encoded.*/
  String imagePullSecret;

  @Override
  public ArtifactSummary getArtifactSummary() {
    return GcrArtifactSummary.builder().imagePath(imagePath).tag(tag).build();
  }

  @Override
  public String getType() {
    return "gcrArtifactOutcome";
  }
}
