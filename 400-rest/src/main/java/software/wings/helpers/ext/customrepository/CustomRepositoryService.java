/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.helpers.ext.customrepository;

import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

public interface CustomRepositoryService {
  List<BuildDetails> getBuilds(ArtifactStreamAttributes artifactStreamAttributes);

  boolean validateArtifactSource(ArtifactStreamAttributes artifactStreamAttributes);
}
