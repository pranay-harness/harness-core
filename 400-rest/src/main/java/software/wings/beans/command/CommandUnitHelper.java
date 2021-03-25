package software.wings.beans.command;

import static io.harness.data.structure.HasPredicate.hasSome;

import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;

import java.util.Map;

public class CommandUnitHelper {
  public void addArtifactFileNameToEnv(Map<String, String> envVariables, CommandExecutionContext context) {
    if (context.isMultiArtifact()) {
      if (hasSome(context.getArtifactFileName())) {
        envVariables.put("ARTIFACT_FILE_NAME", context.getArtifactFileName());
      }
    } else {
      if (hasSome(context.getArtifactFiles())) {
        String name = context.getArtifactFiles().get(0).getName();
        if (hasSome(name)) {
          envVariables.put("ARTIFACT_FILE_NAME", name);
        }
      } else if (context.getMetadata() != null) {
        String value = context.getMetadata().get(ArtifactMetadataKeys.artifactFileName);
        if (hasSome(value)) {
          envVariables.put("ARTIFACT_FILE_NAME", value);
        }
      }
    }
  }
}
