/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.artifact.ArtifactStream;
import software.wings.delegatetasks.buildsource.BuildSourceCleanupHelper;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.ArtifactCleanupService;
import software.wings.service.intfc.BuildSourceService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class ArtifactCleanupServiceSyncImpl implements ArtifactCleanupService {
  @Inject private BuildSourceService buildSourceService;
  @Inject private BuildSourceCleanupHelper buildSourceCleanupHelper;

  public static final Duration timeout = Duration.ofMinutes(10);

  @Override
  public void cleanupArtifacts(ArtifactStream artifactStream, String accountId) {
    log.info("Cleaning build details for artifact stream type {} and source name {} ",
        artifactStream.getArtifactStreamType(), artifactStream.getSourceName());
    List<BuildDetails> builds = buildSourceService.getBuilds(
        artifactStream.getAppId(), artifactStream.getUuid(), artifactStream.getSettingId());

    buildSourceCleanupHelper.cleanupArtifacts(accountId, artifactStream, builds);
  }
}
