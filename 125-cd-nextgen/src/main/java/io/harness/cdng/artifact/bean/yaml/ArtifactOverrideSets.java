/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.artifact.bean.yaml;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.yaml.PrimaryArtifact.PrimaryArtifactStepParameters;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifact.SidecarArtifactStepParameters;
import io.harness.cdng.visitor.helpers.artifact.ArtifactOverridesVisitorHelper;
import io.harness.data.validator.EntityIdentifier;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("overrideSet")
@SimpleVisitorHelper(helperClass = ArtifactOverridesVisitorHelper.class)
@TypeAlias("artifactOverrideSets")
public class ArtifactOverrideSets implements Visitable {
  @EntityIdentifier String identifier;
  ArtifactListConfig artifacts;

  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add("artifacts", artifacts);
    return children;
  }

  @Value
  public static class ArtifactOverrideSetsStepParametersWrapper {
    ArtifactOverrideSetsStepParameters artifacts;

    public static ArtifactOverrideSetsStepParametersWrapper fromArtifactOverrideSets(
        ArtifactOverrideSets artifactOverrideSets) {
      return new ArtifactOverrideSetsStepParametersWrapper(
          ArtifactOverrideSetsStepParameters.fromArtifactOverrideSets(artifactOverrideSets));
    }
  }

  @Value
  public static class ArtifactOverrideSetsStepParameters {
    PrimaryArtifactStepParameters primary;
    Map<String, SidecarArtifactStepParameters> sidecars;

    public static ArtifactOverrideSetsStepParameters fromArtifactOverrideSets(
        ArtifactOverrideSets artifactOverrideSets) {
      if (artifactOverrideSets == null || artifactOverrideSets.getArtifacts() == null) {
        return null;
      }
      return new ArtifactOverrideSetsStepParameters(
          PrimaryArtifactStepParameters.fromPrimaryArtifact(artifactOverrideSets.getArtifacts().getPrimary()),
          artifactOverrideSets.getArtifacts().getSidecars() == null
              ? null
              : artifactOverrideSets.getArtifacts()
                    .getSidecars()
                    .stream()
                    .map(SidecarArtifactWrapper::getSidecar)
                    .collect(Collectors.toMap(
                        SidecarArtifact::getIdentifier, SidecarArtifactStepParameters::fromPrimaryArtifact)));
    }
  }
}
