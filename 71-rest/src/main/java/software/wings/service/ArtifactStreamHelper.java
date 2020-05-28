package software.wings.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.utils.RepositoryFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class ArtifactStreamHelper {
  @Inject private ManagerExpressionEvaluator evaluator;

  public void resolveArtifactStreamRuntimeValues(ArtifactStream artifactStream, Map<String, Object> runtimeValues) {
    if (artifactStream instanceof NexusArtifactStream) {
      String repositoryFormat = ((NexusArtifactStream) artifactStream).getRepositoryFormat();
      if (repositoryFormat.equals(RepositoryFormat.maven.name())) {
        resolveRuntimeValuesForMaven((NexusArtifactStream) artifactStream, runtimeValues);
      } else if (repositoryFormat.equals(RepositoryFormat.npm.name())
          || repositoryFormat.equals(RepositoryFormat.nuget.name())) {
        resolveRuntimeValuesForNpmNuGet((NexusArtifactStream) artifactStream, runtimeValues);
      }
    }
  }

  private void resolveRuntimeValuesForNpmNuGet(NexusArtifactStream artifactStream, Map<String, Object> runtimeValues) {
    if (isNotEmpty(artifactStream.getJobname()) && artifactStream.getJobname().startsWith("${")) {
      artifactStream.setJobname((String) evaluator.evaluate(artifactStream.getJobname(), runtimeValues));
    }
    if (isNotEmpty(artifactStream.getPackageName()) && artifactStream.getPackageName().startsWith("${")) {
      artifactStream.setPackageName((String) evaluator.evaluate(artifactStream.getPackageName(), runtimeValues));
    }
  }

  private void resolveRuntimeValuesForMaven(NexusArtifactStream artifactStream, Map<String, Object> runtimeValues) {
    if (isNotEmpty(artifactStream.getJobname()) && artifactStream.getJobname().startsWith("${")) {
      artifactStream.setJobname((String) evaluator.evaluate(artifactStream.getJobname(), runtimeValues));
    }
    if (isNotEmpty(artifactStream.getGroupId()) && artifactStream.getGroupId().startsWith("${")) {
      artifactStream.setGroupId((String) evaluator.evaluate(artifactStream.getGroupId(), runtimeValues));
    }
    if (isNotEmpty(artifactStream.getExtension()) && artifactStream.getExtension().startsWith("${")) {
      artifactStream.setExtension((String) evaluator.evaluate(artifactStream.getExtension(), runtimeValues));
    }
    if (isNotEmpty(artifactStream.getClassifier()) && artifactStream.getClassifier().startsWith("${")) {
      artifactStream.setClassifier((String) evaluator.evaluate(artifactStream.getClassifier(), runtimeValues));
    }
    if (isNotEmpty(artifactStream.getArtifactPaths())) {
      List<String> artifactPaths = artifactStream.getArtifactPaths();
      List<String> resolvedPaths = new ArrayList<>();
      for (String artifactPath : artifactPaths) {
        if (artifactPath.startsWith("${")) {
          resolvedPaths.add((String) evaluator.evaluate(artifactPath, runtimeValues));
        } else {
          resolvedPaths.add(artifactPath);
        }
      }
      artifactStream.setArtifactPaths(resolvedPaths);
    }
  }
}
