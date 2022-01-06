/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask.polling.manifest;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.polling.FirstCollectionOnDelegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Value;

@Value
@OwnedBy(HarnessTeam.CDC)
public class ManifestsCollectionCache {
  Set<String> publishedManifestKeys;
  Set<String> unpublishedManifestKeys;
  Set<String> toBeDeletedManifestKeys;
  List<String> unpublishedManifests; // This holds manifests in the same order as collected.
  // This tells manager that this is first time we are collecting on this delegate.
  // If this is true, then unpublishedManifests comprise of all versions present in repository.
  FirstCollectionOnDelegate firstCollectionOnDelegate;

  public ManifestsCollectionCache() {
    this.publishedManifestKeys = new HashSet<>();
    this.unpublishedManifestKeys = new HashSet<>();
    this.toBeDeletedManifestKeys = new HashSet<>();
    this.unpublishedManifests = new ArrayList<>();
    this.firstCollectionOnDelegate = new FirstCollectionOnDelegate(true);
  }

  public boolean needsToPublish() {
    return !unpublishedManifests.isEmpty() || !toBeDeletedManifestKeys.isEmpty();
  }

  public void populateCache(List<String> chartVersions) {
    if (isEmpty(chartVersions)) {
      return;
    }

    Set<String> newKeys = new HashSet<>();
    for (String chartVersion : chartVersions) {
      newKeys.add(chartVersion);
      if (!publishedManifestKeys.contains(chartVersion)) {
        unpublishedManifests.add(chartVersion);
        unpublishedManifestKeys.add(chartVersion);
      }
    }

    for (String chartVersion : publishedManifestKeys) {
      if (!newKeys.contains(chartVersion)) {
        toBeDeletedManifestKeys.add(chartVersion);
      }
    }
  }

  public void clearUnpublishedVersions(Collection<String> versions) {
    if (isEmpty(versions)) {
      return;
    }

    this.publishedManifestKeys.addAll(versions);
    this.unpublishedManifestKeys.removeAll(versions);
    this.unpublishedManifests.removeAll(versions);
  }

  public void removeDeletedArtifactKeys(Collection<String> deletedKeys) {
    if (isEmpty(deletedKeys)) {
      return;
    }

    publishedManifestKeys.removeAll(deletedKeys);
    toBeDeletedManifestKeys.removeAll(deletedKeys);
  }

  public void setFirstCollectionOnDelegateFalse() {
    this.firstCollectionOnDelegate.setFirstCollectionOnDelegate(false);
  }
}
