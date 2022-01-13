/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask.polling.artifact;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.artifacts.response.ArtifactDelegateResponse;
import io.harness.polling.artifact.ArtifactCollectionUtilsNg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@OwnedBy(HarnessTeam.CDC)
public class ArtifactsCollectionCache {
  Set<String> publishedArtifactKeys;
  Set<String> unpublishedArtifactKeys;
  Set<String> toBeDeletedArtifactKeys;
  List<ArtifactDelegateResponse> unpublishedArtifacts; // This holds manifests in the same order as collected.
  // This tells manager that this is first time we are collecting on this delegate.
  // If this is true, then unpublishedManifests comprise of all versions present in repository.
  @NonFinal @Setter boolean firstCollectionOnDelegate;

  public ArtifactsCollectionCache() {
    this.publishedArtifactKeys = new HashSet<>();
    this.unpublishedArtifactKeys = new HashSet<>();
    this.toBeDeletedArtifactKeys = new HashSet<>();
    this.unpublishedArtifacts = new ArrayList<>();
    this.firstCollectionOnDelegate = true;
  }

  public boolean needsToPublish() {
    return !unpublishedArtifacts.isEmpty() || !toBeDeletedArtifactKeys.isEmpty();
  }

  public void populateCache(List<ArtifactDelegateResponse> artifactDelegateResponses) {
    if (isEmpty(artifactDelegateResponses)) {
      return;
    }

    Set<String> newKeys = new HashSet<>();
    for (ArtifactDelegateResponse response : artifactDelegateResponses) {
      String buildKey = ArtifactCollectionUtilsNg.getArtifactKey(response);
      newKeys.add(buildKey);
      if (!publishedArtifactKeys.contains(buildKey)) {
        unpublishedArtifacts.add(response);
        unpublishedArtifactKeys.add(buildKey);
      }
    }

    for (String key : publishedArtifactKeys) {
      if (!newKeys.contains(key)) {
        toBeDeletedArtifactKeys.add(key);
      }
    }
  }

  public void clearUnpublishedArtifacts(Collection<ArtifactDelegateResponse> builds) {
    if (isEmpty(builds)) {
      return;
    }

    Set<String> artifactKeys =
        builds.stream().map(ArtifactCollectionUtilsNg::getArtifactKey).collect(Collectors.toSet());
    this.publishedArtifactKeys.addAll(artifactKeys);
    this.unpublishedArtifactKeys.removeAll(artifactKeys);
    this.unpublishedArtifacts.removeAll(builds);
  }

  public void removeDeletedArtifactKeys(Collection<String> deletedKeys) {
    if (isEmpty(deletedKeys)) {
      return;
    }

    publishedArtifactKeys.removeAll(deletedKeys);
    toBeDeletedArtifactKeys.removeAll(deletedKeys);
  }
}
