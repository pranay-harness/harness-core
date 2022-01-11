/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.artifactSource.batch;

import static io.harness.rule.OwnerRule.AADITI;

import static software.wings.graphql.datafetcher.artifactSource.ArtifactSourceTestHelper.getNexusArtifactStream;
import static software.wings.graphql.datafetcher.artifactSource.ArtifactSourceTestHelper.getSmbArtifactStream;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.beans.artifact.SmbArtifactStream;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.schema.type.artifactSource.QLArtifactSource;
import software.wings.graphql.schema.type.artifactSource.QLNexusArtifactSource;
import software.wings.service.intfc.ArtifactStreamService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class ArtifactSourceBatchDataLoaderTest extends AbstractDataFetcherTestBase {
  @Mock private ArtifactStreamService artifactStreamService;
  @Inject @InjectMocks private ArtifactSourceBatchDataLoader artifactSourceBatchDataLoader;

  @Before
  public void setUp() {
    on(artifactSourceBatchDataLoader).set("artifactStreamService", artifactStreamService);
  }
  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldReturnParameterizedNexus2ArtifactSource() {
    SmbArtifactStream smbArtifactStream = getSmbArtifactStream(SETTING_ID_1, ARTIFACT_STREAM_ID_1);
    NexusArtifactStream nexusArtifactStream = getNexusArtifactStream(SETTING_ID_2, ARTIFACT_STREAM_ID_2);

    Set<String> set = new HashSet<>();
    set.add(ARTIFACT_STREAM_ID_1);
    set.add(ARTIFACT_STREAM_ID_2);
    when(artifactStreamService.listByIds(set)).thenReturn(asList(smbArtifactStream, nexusArtifactStream));
    List<String> params = new ArrayList<>();
    params.addAll(asList("repo", "groupId", "path"));
    when(artifactStreamService.getArtifactStreamParameters(ARTIFACT_STREAM_ID_2)).thenReturn(params);
    Map<String, QLArtifactSource> idToArtifactSourceMap = artifactSourceBatchDataLoader.getArtifactSourceMap(set);
    assertThat(idToArtifactSourceMap).isNotEmpty();
    assertThat(idToArtifactSourceMap.get(ARTIFACT_STREAM_ID_1).getName()).isEqualTo("testSMB");
    assertThat(idToArtifactSourceMap.get(ARTIFACT_STREAM_ID_2).getName()).isEqualTo("testNexus");
    QLNexusArtifactSource qlNexusArtifactSource =
        (QLNexusArtifactSource) idToArtifactSourceMap.get(ARTIFACT_STREAM_ID_2);
    assertThat(qlNexusArtifactSource.getParameters()).containsAll(asList("repo", "groupId", "path"));
  }
}
