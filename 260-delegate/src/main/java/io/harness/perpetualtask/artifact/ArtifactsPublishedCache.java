/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask.artifact;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * ArtifactsPublishedCache is a delegate side in-memory cache for a single artifact stream. It keeps track of artifacts
 * published to the manager, unpublished artifacts which the delegate knows about but not the manager and artifact keys
 * that need to deleted by the manager.
 *
 * Before starting a new artifact collection we first ensure 2 things:
 * - There are no artifact keys that need to be deleted\
 * - There are no unpublished build details
 *
 * Example:
 *   Before starting the perpetual task we had 500 artifacts (build number: 1-500)
 *   State - published: 1-500, unpublished: nothing, toBeDeleted: nothing
 *
 *   Now the customer adds 300 new artifacts to their third-party artifact repository (ex. GCR) and also deletes the
 *   build number 200. So finally they have 799 artifacts with build numbers 1-800 except build number 200
 *
 *   In the next artifact collection, we get 799 artifacts out of which 300 are new and build no 200 was deleted
 *   State - published: 1-500 except 200, unpublished: 501-800, toBeDeleted: 200
 *
 *   In the next run, we publish all the artifact keys that need to be deleted
 *   State - published: 1-500 except 200, unpublished: 501-800, toBeDeleted: nothing
 *
 *   In the next run, we publish 250 (ARTIFACT_ONE_TIME_PUBLISH_LIMIT) out of the 300 unpublished artifacts
 *   State - published: 1-750 except 200, unpublished: 751-800, toBeDeleted: nothing
 *
 *   Finally, we publish the remaining 50 unpublished artifacts
 *   State - published: 1-800 except 200, unpublished: nothing, toBeDeleted: nothing
 *
 *   Now, we can do artifact collection
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public class ArtifactsPublishedCache<P> {
  // Max artifacts published to the manager in one call.
  public static final int ARTIFACT_ONE_TIME_PUBLISH_LIMIT = 500;

  @Getter Set<String> publishedArtifactKeys;
  Set<String> unpublishedArtifactKeys;
  List<P> unpublishedBuildDetails;
  Set<String> toBeDeletedArtifactKeys;
  Function<P, String> buildDetailsKeyFunction;
  boolean enableCleanup;
  @NonFinal @Getter @Setter boolean fetchFromCache;

  public ArtifactsPublishedCache(
      Collection<String> publishedArtifactKeys, Function<P, String> buildDetailsKeyFunction, boolean enableCleanup) {
    this.publishedArtifactKeys =
        new HashSet<>(publishedArtifactKeys == null ? Collections.emptySet() : publishedArtifactKeys);
    this.toBeDeletedArtifactKeys = new HashSet<>();
    this.unpublishedArtifactKeys = new HashSet<>();
    this.unpublishedBuildDetails = new ArrayList<>();
    this.buildDetailsKeyFunction = buildDetailsKeyFunction;
    this.enableCleanup = enableCleanup;
    this.fetchFromCache = false;
  }

  /**
   * Add all the build details to unpublished build details. This method ignores artifacts already in the published or
   * unpublished set. It also find out artifacts that need to cleaned up.
   *
   * @param builds the new build details returned by third party-repo
   */
  public void addCollectionResult(List<P> builds) {
    if (isEmpty(builds)) {
      return;
    }

    Set<String> newKeys = new HashSet<>();
    for (P build : builds) {
      String key = buildDetailsKeyFunction.apply(build);
      newKeys.add(key);
      if (!publishedArtifactKeys.contains(key) && !unpublishedArtifactKeys.contains(key)) {
        // Add any new key we find as unpublished.
        unpublishedBuildDetails.add(build);
        unpublishedArtifactKeys.add(key);
      }
    }

    if (enableCleanup) {
      // If some published keys are no longer in the new result, add them in the to be deleted set.
      for (String key : publishedArtifactKeys) {
        if (!newKeys.contains(key)) {
          toBeDeletedArtifactKeys.add(key);
        }
      }

      // If some unpublished keys are no longer in the new result, remove them from unpublished set and list.
      Set<String> unpublishedKeysToRemove = new HashSet<>();
      boolean changed = false;
      for (String key : unpublishedArtifactKeys) {
        if (!newKeys.contains(key)) {
          unpublishedKeysToRemove.add(key);
          changed = true;
        }
      }

      if (changed) {
        unpublishedArtifactKeys.removeAll(unpublishedKeysToRemove);
        updateUnpublishedBuildDetails();
      }
    }
  }

  public void removeDeletedArtifactKeys(Collection<String> deletedKeys) {
    if (!enableCleanup || isEmpty(deletedKeys)) {
      return;
    }

    publishedArtifactKeys.removeAll(deletedKeys);
    toBeDeletedArtifactKeys.removeAll(deletedKeys);
    updateUnpublishedBuildDetails();
  }

  public void addPublishedBuildDetails(Collection<P> builds) {
    if (isEmpty(builds)) {
      return;
    }

    Set<String> artifactKeys = builds.stream().map(buildDetailsKeyFunction).collect(Collectors.toSet());
    publishedArtifactKeys.addAll(artifactKeys);
    unpublishedArtifactKeys.removeAll(artifactKeys);
    updateUnpublishedBuildDetails();
  }

  public boolean needsToPublish() {
    return hasToBeDeletedArtifactKeys() || hasUnpublishedBuildDetails();
  }

  public boolean hasToBeDeletedArtifactKeys() {
    return enableCleanup && !toBeDeletedArtifactKeys.isEmpty();
  }

  public Set<String> getToBeDeletedArtifactKeys() {
    return !hasToBeDeletedArtifactKeys() ? new HashSet<>() : new HashSet<>(toBeDeletedArtifactKeys);
  }

  public boolean hasUnpublishedBuildDetails() {
    return !unpublishedBuildDetails.isEmpty();
  }

  /**
   * Return unpublished build details (not more than ARTIFACT_ONE_TIME_PUBLISH_LIMIT). The right value of the pair is
   * true if there are more unpublished build details left.
   *
   * @return the next limited sublist of unpublished build details and boolean indicating if any more are left
   */
  public ImmutablePair<List<P>, Boolean> getLimitedUnpublishedBuildDetails() {
    if (isEmpty(unpublishedBuildDetails)) {
      return ImmutablePair.of(new ArrayList<>(), Boolean.FALSE);
    }

    if (unpublishedBuildDetails.size() <= ARTIFACT_ONE_TIME_PUBLISH_LIMIT) {
      return ImmutablePair.of(new ArrayList<>(unpublishedBuildDetails), Boolean.FALSE);
    } else {
      return ImmutablePair.of(
          new ArrayList<>(unpublishedBuildDetails.subList(0, ARTIFACT_ONE_TIME_PUBLISH_LIMIT)), Boolean.TRUE);
    }
  }

  /**
   * Update unpublishedBuildDetails so that it contains only those build details that are also in
   * unpublishedArtifactKeys.
   */
  private void updateUnpublishedBuildDetails() {
    unpublishedBuildDetails.removeIf(build -> !unpublishedArtifactKeys.contains(buildDetailsKeyFunction.apply(build)));
  }
}
