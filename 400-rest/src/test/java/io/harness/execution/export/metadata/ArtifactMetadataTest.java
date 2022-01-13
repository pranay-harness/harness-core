/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.metadata;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ArtifactMetadataTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFromArtifacts() {
    List<ArtifactMetadata> artifactMetadataList =
        ArtifactMetadata.fromArtifacts(Collections.singletonList(MetadataTestUtils.prepareArtifact(1)));
    assertThat(artifactMetadataList).isNotNull();
    assertThat(artifactMetadataList.size()).isEqualTo(1);
    assertThat(artifactMetadataList.get(0).getArtifactSource()).isEqualTo("asn1");
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFromBuildExecutionSummaries() {
    List<ArtifactMetadata> artifactMetadataList = ArtifactMetadata.fromBuildExecutionSummaries(
        Collections.singletonList(MetadataTestUtils.prepareBuildExecutionSummary(1)));
    assertThat(artifactMetadataList).isNotNull();
    assertThat(artifactMetadataList.size()).isEqualTo(1);
    assertThat(artifactMetadataList.get(0).getArtifactSource()).isEqualTo("asn1");
  }
}
