package io.harness.perpetualtask.artifact;

import static io.harness.rule.OwnerRule.GARVIT;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RunWith(MockitoJUnitRunner.class)
public class ArtifactsPublishedCacheTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testAddArtifactCollectionResult() {
    ArtifactsPublishedCache cache = prepareCache(asList("0.1.0", "0.2.0", "0.3.0"), false);
    cache.addArtifactCollectionResult(prepareBuildDetails(asList("0.1.0", "0.4.0", "0.5.0")));
    assertThat(cache.needsToPublish()).isTrue();
    assertThat(cache.hasToBeDeletedArtifactKeys()).isFalse();

    cache = prepareCache(asList("0.1.0", "0.2.0", "0.3.0"), true);
    cache.addArtifactCollectionResult(null);
    assertThat(cache.needsToPublish()).isFalse();

    cache.addArtifactCollectionResult(prepareBuildDetails(asList("0.1.0", "0.4.0", "0.5.0")));
    assertThat(cache.needsToPublish()).isTrue();
    assertThat(cache.hasToBeDeletedArtifactKeys()).isTrue();
    assertThat(cache.getToBeDeletedArtifactKeys()).containsExactly("0.2.0", "0.3.0");

    ImmutablePair<List<BuildDetails>, Boolean> res = cache.getLimitedUnpublishedBuildDetails();
    assertThat(res).isNotNull();
    assertThat(res.getLeft()).isNotNull();
    assertThat(res.getLeft().stream().map(BuildDetails::getNumber).collect(Collectors.toList()))
        .containsExactly("0.4.0", "0.5.0");
    assertThat(res.getRight()).isFalse();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testRemoveDeletedArtifactKeys() {
    ArtifactsPublishedCache cache = prepareCache(asList("0.1.0", "0.2.0", "0.3.0"), true);
    cache.addArtifactCollectionResult(prepareBuildDetails(asList("0.1.0", "0.4.0", "0.5.0")));
    cache.removeDeletedArtifactKeys(null);
    assertThat(cache.needsToPublish()).isTrue();
    assertThat(cache.hasToBeDeletedArtifactKeys()).isTrue();
    assertThat(cache.getToBeDeletedArtifactKeys()).containsExactly("0.2.0", "0.3.0");

    cache.removeDeletedArtifactKeys(Collections.singletonList("0.2.0"));
    assertThat(cache.needsToPublish()).isTrue();
    assertThat(cache.hasToBeDeletedArtifactKeys()).isTrue();
    assertThat(cache.getToBeDeletedArtifactKeys()).containsExactly("0.3.0");

    cache.removeDeletedArtifactKeys(Collections.singletonList("0.3.0"));
    assertThat(cache.needsToPublish()).isTrue();
    assertThat(cache.hasToBeDeletedArtifactKeys()).isFalse();
    assertThat(cache.getToBeDeletedArtifactKeys()).isEmpty();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testAddPublishedBuildDetails() {
    ArtifactsPublishedCache cache = prepareCache(asList("0.1.0", "0.2.0", "0.3.0"), true);
    cache.addArtifactCollectionResult(prepareBuildDetails(asList("0.1.0", "0.4.0", "0.5.0")));
    cache.removeDeletedArtifactKeys(asList("0.2.0", "0.3.0"));
    assertThat(cache.needsToPublish()).isTrue();
    assertThat(cache.hasToBeDeletedArtifactKeys()).isFalse();

    cache.addPublishedBuildDetails(null);
    ImmutablePair<List<BuildDetails>, Boolean> res = cache.getLimitedUnpublishedBuildDetails();
    assertThat(res).isNotNull();
    assertThat(res.getLeft()).isNotNull();
    assertThat(res.getLeft().stream().map(BuildDetails::getNumber).collect(Collectors.toList()))
        .containsExactly("0.4.0", "0.5.0");
    assertThat(res.getRight()).isFalse();

    cache.addPublishedBuildDetails(prepareBuildDetails(Collections.singletonList("0.4.0")));
    assertThat(cache.needsToPublish()).isTrue();
    assertThat(cache.hasUnpublishedBuildDetails()).isTrue();

    res = cache.getLimitedUnpublishedBuildDetails();
    assertThat(res).isNotNull();
    assertThat(res.getLeft()).isNotNull();
    assertThat(res.getLeft().stream().map(BuildDetails::getNumber).collect(Collectors.toList()))
        .containsExactly("0.5.0");
    assertThat(res.getRight()).isFalse();

    cache.addPublishedBuildDetails(prepareBuildDetails(Collections.singletonList("0.5.0")));
    assertThat(cache.needsToPublish()).isFalse();
    assertThat(cache.hasUnpublishedBuildDetails()).isFalse();

    res = cache.getLimitedUnpublishedBuildDetails();
    assertThat(res).isNotNull();
    assertThat(res.getLeft()).isNotNull();
    assertThat(res.getLeft()).isEmpty();
    assertThat(res.getRight()).isFalse();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testEmptyInitialPublishedArtifactKeys() {
    ArtifactsPublishedCache cache = prepareCache(null, true);
    cache.addArtifactCollectionResult(prepareBuildDetails(asList("0.1.0", "0.4.0", "0.5.0")));
    assertThat(cache.hasToBeDeletedArtifactKeys()).isFalse();

    ImmutablePair<List<BuildDetails>, Boolean> res = cache.getLimitedUnpublishedBuildDetails();
    assertThat(res).isNotNull();
    assertThat(res.getLeft()).isNotNull();
    assertThat(res.getLeft().stream().map(BuildDetails::getNumber).collect(Collectors.toList()))
        .containsExactly("0.1.0", "0.4.0", "0.5.0");
    assertThat(res.getRight()).isFalse();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testBatchingOfUnpublishedArtifacts() {
    ArtifactsPublishedCache cache = prepareCache(null, true);
    List<String> buildNos = IntStream.rangeClosed(1, ArtifactsPublishedCache.ARTIFACT_ONE_TIME_PUBLISH_LIMIT + 10)
                                .boxed()
                                .map(String::valueOf)
                                .collect(Collectors.toList());
    cache.addArtifactCollectionResult(prepareBuildDetails(buildNos));
    assertThat(cache.hasToBeDeletedArtifactKeys()).isFalse();

    ImmutablePair<List<BuildDetails>, Boolean> res = cache.getLimitedUnpublishedBuildDetails();
    assertThat(res).isNotNull();
    assertThat(res.getLeft()).isNotNull();
    assertThat(res.getLeft().size()).isEqualTo(ArtifactsPublishedCache.ARTIFACT_ONE_TIME_PUBLISH_LIMIT);
    assertThat(res.getRight()).isTrue();

    cache.addPublishedBuildDetails(res.getLeft());
    res = cache.getLimitedUnpublishedBuildDetails();
    assertThat(res).isNotNull();
    assertThat(res.getLeft()).isNotNull();
    assertThat(res.getLeft().size()).isEqualTo(10);
    assertThat(res.getRight()).isFalse();
  }

  private ArtifactsPublishedCache prepareCache(Collection<String> publishedArtifactKeys, boolean enableCleanup) {
    return new ArtifactsPublishedCache(publishedArtifactKeys, BuildDetails::getNumber, enableCleanup);
  }

  private List<BuildDetails> prepareBuildDetails(List<String> buildNos) {
    return buildNos == null
        ? null
        : buildNos.stream().map(buildNo -> aBuildDetails().withNumber(buildNo).build()).collect(Collectors.toList());
  }
}