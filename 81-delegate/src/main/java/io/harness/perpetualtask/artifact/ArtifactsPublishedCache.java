package io.harness.perpetualtask.artifact;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import org.apache.commons.lang3.tuple.ImmutablePair;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
public class ArtifactsPublishedCache {
  // Max artifacts published to the manager in one call.
  public static final int ARTIFACT_ONE_TIME_PUBLISH_LIMIT = 500;

  private Set<String> publishedArtifactKeys;
  private List<BuildDetails> unpublishedBuildDetails;
  private Set<String> unpublishedArtifactKeys;
  private Set<String> toBeDeletedArtifactKeys;
  private Function<BuildDetails, String> buildDetailsKeyFunction;
  private boolean enableCleanup;

  public ArtifactsPublishedCache(Collection<String> publishedArtifactKeys,
      Function<BuildDetails, String> buildDetailsKeyFunction, boolean enableCleanup) {
    this.publishedArtifactKeys =
        new HashSet<>(publishedArtifactKeys == null ? Collections.emptySet() : publishedArtifactKeys);
    this.toBeDeletedArtifactKeys = new HashSet<>();
    this.unpublishedBuildDetails = new ArrayList<>();
    this.unpublishedArtifactKeys = new HashSet<>();
    this.buildDetailsKeyFunction = buildDetailsKeyFunction;
    this.enableCleanup = enableCleanup;
  }

  /*
   * Add all the build details to unpublished build details. This method ignores artifacts already in the published or
   * unpublished set. It also find out artifacts that need to cleaned up.
   */
  public void addArtifactCollectionResult(List<BuildDetails> builds) {
    if (isEmpty(builds)) {
      return;
    }

    Set<String> newKeys = new HashSet<>();
    for (BuildDetails build : builds) {
      String key = buildDetailsKeyFunction.apply(build);
      newKeys.add(key);
      if (!publishedArtifactKeys.contains(key) && !unpublishedArtifactKeys.contains(key)) {
        unpublishedBuildDetails.add(build);
        unpublishedArtifactKeys.add(key);
      }
    }

    if (enableCleanup) {
      for (String key : publishedArtifactKeys) {
        if (!newKeys.contains(key)) {
          toBeDeletedArtifactKeys.add(key);
        }
      }
    }
  }

  public void removeDeletedArtifactKeys(Collection<String> deletedKeys) {
    if (!enableCleanup || isEmpty(deletedKeys)) {
      return;
    }

    publishedArtifactKeys.removeAll(deletedKeys);
    toBeDeletedArtifactKeys.removeAll(deletedKeys);
  }

  public void addPublishedBuildDetails(Collection<BuildDetails> builds) {
    if (isEmpty(builds)) {
      return;
    }

    Set<String> artifactKeys =
        builds.stream().map(build -> buildDetailsKeyFunction.apply(build)).collect(Collectors.toSet());
    publishedArtifactKeys.addAll(artifactKeys);
    unpublishedArtifactKeys.removeAll(artifactKeys);
    unpublishedBuildDetails =
        unpublishedBuildDetails.stream()
            .filter(build -> unpublishedArtifactKeys.contains(buildDetailsKeyFunction.apply(build)))
            .collect(Collectors.toList());
  }

  public boolean needsToPublish() {
    return hasToBeDeletedArtifactKeys() || hasUnpublishedBuildDetails();
  }

  public boolean hasToBeDeletedArtifactKeys() {
    return enableCleanup && !toBeDeletedArtifactKeys.isEmpty();
  }

  public Set<String> getToBeDeletedArtifactKeys() {
    return !enableCleanup || toBeDeletedArtifactKeys.isEmpty() ? new HashSet<>()
                                                               : new HashSet<>(toBeDeletedArtifactKeys);
  }

  public boolean hasUnpublishedBuildDetails() {
    return !unpublishedBuildDetails.isEmpty();
  }

  /*
   * Return unpublished build details (not more than ARTIFACT_ONE_TIME_PUBLISH_LIMIT). The right value of the pair is
   * true if there are more unpublished build details left.
   */
  public ImmutablePair<List<BuildDetails>, Boolean> getLimitedUnpublishedBuildDetails() {
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
}
