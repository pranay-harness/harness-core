/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.k8s.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.WingsException;
import io.harness.k8s.manifest.ObjectYamlUtils;
import io.harness.k8s.model.Release.Status;
import io.harness.serializer.YamlUtils;

import com.esotericsoftware.yamlbeans.YamlException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@OwnedBy(CDP)
public class ReleaseHistory {
  public static final String defaultVersion = "v1";

  private String version;
  private List<Release> releases;

  public static ReleaseHistory createNew() {
    ReleaseHistory releaseHistory = new ReleaseHistory();
    releaseHistory.setVersion(defaultVersion);
    releaseHistory.setReleases(new ArrayList<>());
    return releaseHistory;
  }

  public static ReleaseHistory createFromData(String releaseHistory) throws IOException {
    return new YamlUtils().read(releaseHistory, ReleaseHistory.class);
  }

  public Release createNewRelease(List<KubernetesResourceId> resources) {
    int releaseNumber = 1;
    if (!this.getReleases().isEmpty()) {
      releaseNumber = getLatestRelease().getNumber() + 1;
    }
    this.getReleases().add(
        0, Release.builder().number(releaseNumber).status(Status.InProgress).resources(resources).build());

    return getLatestRelease();
  }

  public Release createNewReleaseWithResourceMap(List<KubernetesResource> resources) {
    int releaseNumber = 1;
    if (!this.getReleases().isEmpty()) {
      releaseNumber = getLatestRelease().getNumber() + 1;
    }
    this.getReleases().add(0,
        Release.builder()
            .number(releaseNumber)
            .status(Status.InProgress)
            .resources(resources.stream().map(KubernetesResource::getResourceId).collect(toList()))
            .resourcesWithSpec(resources)
            .build());

    return getLatestRelease();
  }

  public Release getLatestRelease() {
    if (this.getReleases().isEmpty()) {
      throw new WingsException("No existing release found.");
    }

    return this.getReleases().get(0);
  }

  public void setReleaseStatus(Status status) {
    this.getLatestRelease().setStatus(status);
  }

  public void setReleaseNumber(int releaseNumber) {
    this.getLatestRelease().setNumber(releaseNumber);
  }

  public Release getLastSuccessfulRelease() {
    for (Release release : this.getReleases()) {
      if (release.getStatus() == Status.Succeeded) {
        return release;
      }
    }
    return null;
  }

  public Release getPreviousRollbackEligibleRelease(int currentReleaseNumber) {
    for (Release release : this.getReleases()) {
      if (release.getNumber() < currentReleaseNumber && release.getStatus() == Status.Succeeded) {
        return release;
      }
    }
    return null;
  }

  @Nullable
  public Release getRelease(int releaseNumber) {
    for (Release release : this.getReleases()) {
      if (release.getNumber() == releaseNumber) {
        return release;
      }
    }

    return null;
  }

  public String getAsYaml() throws YamlException {
    return ObjectYamlUtils.toYaml(this);
  }

  public void cleanup() {
    Release lastSuccessfulRelease = this.getLastSuccessfulRelease();
    int lastSuccessfulReleaseNumber = lastSuccessfulRelease != null ? lastSuccessfulRelease.getNumber() : 0;
    releases.removeIf(
        release -> release.getNumber() < lastSuccessfulReleaseNumber || Status.Failed == release.getStatus());
  }

  public ReleaseHistory cloneInternal() {
    return ReleaseHistory.builder().version(this.version).releases(new ArrayList<>(this.releases)).build();
  }
}
