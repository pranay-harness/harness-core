package software.wings.delegatetasks.buildsource;

public class ArtifactStreamNotFound extends RuntimeException {
  public ArtifactStreamNotFound(String artifactStreamId) {
    super(String.format("ArtifactServer %s could not be found", artifactStreamId));
  }
}
