/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.utils.RepositoryFormat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jexl3.JexlException;

@OwnedBy(CDC)
@Singleton
@Slf4j
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class ArtifactStreamHelper {
  @Inject private ManagerExpressionEvaluator evaluator;

  public void resolveArtifactStreamRuntimeValues(ArtifactStream artifactStream, Map<String, Object> runtimeValues) {
    try {
      if (artifactStream instanceof NexusArtifactStream) {
        String repositoryFormat = ((NexusArtifactStream) artifactStream).getRepositoryFormat();
        if (repositoryFormat.equals(RepositoryFormat.maven.name())) {
          resolveRuntimeValuesForMaven((NexusArtifactStream) artifactStream, runtimeValues);
        } else if (repositoryFormat.equals(RepositoryFormat.npm.name())
            || repositoryFormat.equals(RepositoryFormat.nuget.name())) {
          resolveRuntimeValuesForNpmNuGet((NexusArtifactStream) artifactStream, runtimeValues);
        }
      }
    } catch (JexlException e) {
      throw new InvalidRequestException(
          "Unable to resolve variables from the given values for artifact source: " + artifactStream.getName(), e);
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
