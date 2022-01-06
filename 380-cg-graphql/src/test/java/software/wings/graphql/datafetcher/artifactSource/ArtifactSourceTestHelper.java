/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.artifactSource;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import static java.util.Arrays.asList;

import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.beans.artifact.SmbArtifactStream;

public class ArtifactSourceTestHelper {
  public static NexusArtifactStream getNexusArtifactStream(String settingId, String artifactStreamId) {
    NexusArtifactStream nexusArtifactStream = NexusArtifactStream.builder()
                                                  .accountId(ACCOUNT_ID)
                                                  .appId(APP_ID)
                                                  .settingId(settingId)
                                                  .jobname("${repo}")
                                                  .groupId("${groupId}")
                                                  .artifactPaths(asList("${path}"))
                                                  .autoPopulate(false)
                                                  .serviceId(SERVICE_ID)
                                                  .name("testNexus")
                                                  .uuid(artifactStreamId)
                                                  .build();
    nexusArtifactStream.setArtifactStreamParameterized(true);
    return nexusArtifactStream;
  }

  public static SmbArtifactStream getSmbArtifactStream(String settingId, String artifactStreamId) {
    return SmbArtifactStream.builder()
        .accountId(ACCOUNT_ID)
        .appId(APP_ID)
        .settingId(settingId)
        .artifactPaths(asList("tmp.reg"))
        .autoPopulate(false)
        .serviceId(SERVICE_ID)
        .name("testSMB")
        .uuid(artifactStreamId)
        .build();
  }
}
