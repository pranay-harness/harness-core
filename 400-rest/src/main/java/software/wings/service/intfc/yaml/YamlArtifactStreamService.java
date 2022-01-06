/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.yaml;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.rest.RestResponse;

import software.wings.beans.Base;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.yaml.YamlPayload;

/**
 * Api interface for all artifact stream/build source types
 *
 * @author rktummala on 10/09/17
 */

@OwnedBy(CDC)
public interface YamlArtifactStreamService {
  RestResponse<YamlPayload> getArtifactStreamYaml(String appId, String artifactStreamId);

  ArtifactStream.Yaml getArtifactStreamYamlObject(String artifactStreamId);

  String getArtifactStreamYamlString(ArtifactStream artifactStream);

  String getArtifactStreamYamlString(String appId, String artifactStreamId);

  String getArtifactStreamYamlString(String artifactStreamId);

  RestResponse<Base> updateArtifactStream(
      String appId, String artifactStreamId, YamlPayload yamlPayload, boolean deleteEnabled);
}
