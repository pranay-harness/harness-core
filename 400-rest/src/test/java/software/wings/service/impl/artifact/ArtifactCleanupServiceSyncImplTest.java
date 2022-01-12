/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.ABHINAV_MITTAL;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.delegatetasks.buildsource.BuildSourceCleanupHelper;
import software.wings.service.intfc.BuildSourceService;

import com.google.inject.Inject;
import java.util.Collections;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(CDC)
public class ArtifactCleanupServiceSyncImplTest extends WingsBaseTest {
  @Mock private BuildSourceService buildSourceService;
  @Mock private BuildSourceCleanupHelper buildSourceCleanupHelper;

  @Inject @InjectMocks private ArtifactCleanupServiceSyncImpl artifactCleanupServiceSync;

  private static final String ARTIFACT_STREAM_ID_5 = "ARTIFACT_STREAM_ID_5";

  ArtifactStream artifactStream = DockerArtifactStream.builder()
                                      .appId("appId")
                                      .autoPopulate(true)
                                      .imageName("artifactImage")
                                      .uuid("artifactStreamId")
                                      .serviceId("serviceId")
                                      .metadataOnly(true)
                                      .build();

  @Test
  @Owner(developers = ABHINAV_MITTAL)
  @Category(UnitTests.class)
  public void verifyArtifactCleanup() {
    artifactCleanupServiceSync.cleanupArtifacts(artifactStream, "accountId");
    verify(buildSourceService, times(1))
        .getBuilds(artifactStream.getAppId(), artifactStream.getUuid(), artifactStream.getSettingId());
    verify(buildSourceCleanupHelper, times(1)).cleanupArtifacts("accountId", artifactStream, Collections.emptyList());
  }
}
