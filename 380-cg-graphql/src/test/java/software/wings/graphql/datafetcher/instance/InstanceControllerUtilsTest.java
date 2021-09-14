/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher.instance;

import static io.harness.rule.OwnerRule.POOJA;

import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.graphql.datafetcher.instance.InstanceControllerUtils.DUMMY_ARTIFACT_SOURCE_ID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.category.element.UnitTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.schema.type.artifact.QLArtifact;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class InstanceControllerUtilsTest extends AbstractDataFetcherTestBase {
  @Inject @InjectMocks private InstanceControllerUtils utils;

  @Mock HPersistence persistence;

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testGetQlArtifactNull() {
    when(persistence.get(Artifact.class, "ARTIFACT_ID")).thenReturn(null);
    QLArtifact qlArtifact = utils.getQlArtifact(Instance.builder().lastArtifactId("ARTIFACT_ID").build());
    assertThat(qlArtifact.getArtifactSourceId()).isNotEmpty();
    assertThat(qlArtifact.getArtifactSourceId()).isEqualTo(DUMMY_ARTIFACT_SOURCE_ID);

    when(persistence.get(Artifact.class, "ARTIFACT_ID")).thenReturn(null);
    qlArtifact =
        utils.getQlArtifact(Instance.builder().lastArtifactId("ARTIFACT_ID").lastArtifactStreamId("AS_ID").build());
    assertThat(qlArtifact.getArtifactSourceId()).isNotEmpty();
    assertThat(qlArtifact.getArtifactSourceId()).isEqualTo("AS_ID");
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testGetQlArtifactNonNull() {
    when(persistence.get(Artifact.class, "ARTIFACT_ID")).thenReturn(anArtifact().withArtifactStreamId("AS_ID").build());
    QLArtifact qlArtifact =
        utils.getQlArtifact(Instance.builder().lastArtifactId("ARTIFACT_ID").lastArtifactStreamId("AS_ID").build());
    assertThat(qlArtifact.getArtifactSourceId()).isNotEmpty();
    assertThat(qlArtifact.getArtifactSourceId()).isEqualTo("AS_ID");
  }
}
