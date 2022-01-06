/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.artifact.ArtifactStream;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
public interface CustomBuildSourceService {
  List<BuildDetails> getBuilds(@NotEmpty String artifactStreamId);

  boolean validateArtifactSource(ArtifactStream artifactStream);
}
