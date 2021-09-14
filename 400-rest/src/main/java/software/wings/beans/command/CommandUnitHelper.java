/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.command;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;

import java.util.Map;

public class CommandUnitHelper {
  public void addArtifactFileNameToEnv(Map<String, String> envVariables, CommandExecutionContext context) {
    if (context.isMultiArtifact()) {
      if (isNotEmpty(context.getArtifactFileName())) {
        envVariables.put("ARTIFACT_FILE_NAME", context.getArtifactFileName());
      }
    } else {
      if (isNotEmpty(context.getArtifactFiles())) {
        String name = context.getArtifactFiles().get(0).getName();
        if (isNotEmpty(name)) {
          envVariables.put("ARTIFACT_FILE_NAME", name);
        }
      } else if (context.getMetadata() != null) {
        String value = context.getMetadata().get(ArtifactMetadataKeys.artifactFileName);
        if (isNotEmpty(value)) {
          envVariables.put("ARTIFACT_FILE_NAME", value);
        }
      }
    }
  }
}
