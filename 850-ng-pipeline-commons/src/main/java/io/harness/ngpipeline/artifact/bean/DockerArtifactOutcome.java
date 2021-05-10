package io.harness.ngpipeline.artifact.bean;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngpipeline.pipeline.executions.beans.ArtifactSummary;
import io.harness.ngpipeline.pipeline.executions.beans.DockerArtifactSummary;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@EqualsAndHashCode(callSuper = false)
@TypeAlias("dockerArtifactOutcome")
@JsonTypeName("dockerArtifactOutcome")
@OwnedBy(CDC)
// TODO : Create a shared Module b/w pipline and CD/CI where these entities can go to and eventually We need to
// deprecate that module 878-pms-coupling
// @TargetModule(878-pms-coupling)
public class DockerArtifactOutcome implements ArtifactOutcome {
  /** Docker hub registry connector. */
  String connectorRef;
  /** Images in repos need to be referenced via a path. */
  String imagePath;
  /** Tag refers to exact tag number. */
  String tag;
  /** Tag regex is used to get latest build from builds matching regex. */
  String tagRegex;
  /** Identifier for artifact. */
  String identifier;
  /** Artifact type. */
  String type;
  /** Whether this config corresponds to primary artifact.*/
  boolean primaryArtifact;
  /** domainName/imagePath:tag */
  String image;
  /** imagePullSecret for docker credentials base encoded.*/
  String imagePullSecret;

  @Override
  public ArtifactSummary getArtifactSummary() {
    return DockerArtifactSummary.builder().imagePath(getImagePath()).tag(getTag()).build();
  }

  @Override
  public String getArtifactType() {
    return type;
  }
}
