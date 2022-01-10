/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.artifactSource;

import static io.harness.rule.OwnerRule.AADITI;

import static software.wings.graphql.datafetcher.artifactSource.ArtifactSourceTestHelper.getNexusArtifactStream;
import static software.wings.graphql.datafetcher.artifactSource.ArtifactSourceTestHelper.getSmbArtifactStream;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.beans.artifact.SmbArtifactStream;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.schema.query.QLArtifactSourceQueryParam;
import software.wings.graphql.schema.type.artifactSource.QLArtifactSource;
import software.wings.service.intfc.ArtifactStreamService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class ServiceArtifactSourceConnectionDataFetcherTest extends AbstractDataFetcherTestBase {
  @Mock private ArtifactStreamService artifactStreamService;
  @Inject @InjectMocks private ServiceArtifactSourceConnectionDataFetcher serviceArtifactSourceConnectionDataFetcher;

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldFetchArtifactSourcesForService() {
    SmbArtifactStream smbArtifactStream = getSmbArtifactStream(SETTING_ID_1, ARTIFACT_STREAM_ID_1);
    NexusArtifactStream nexusArtifactStream = getNexusArtifactStream(SETTING_ID_2, ARTIFACT_STREAM_ID_2);

    QLArtifactSourceQueryParam qlArtifactSourceQueryParam =
        QLArtifactSourceQueryParam.builder().applicationId(APP_ID).serviceId(SERVICE_ID).build();
    when(artifactStreamService.getArtifactStreamsForService(APP_ID, SERVICE_ID))
        .thenReturn(asList(smbArtifactStream, nexusArtifactStream));
    List<String> params = new ArrayList<>();
    params.addAll(asList("repo", "groupId", "path"));
    when(artifactStreamService.getArtifactStreamParameters(ARTIFACT_STREAM_ID_2)).thenReturn(params);
    List<QLArtifactSource> artifactSources =
        serviceArtifactSourceConnectionDataFetcher.fetch(qlArtifactSourceQueryParam, ACCOUNT_ID);
    assertThat(artifactSources).isNotEmpty();
    assertThat(artifactSources.size()).isEqualTo(2);
  }
}
