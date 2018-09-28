package software.wings.service.impl.artifact;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
import static software.wings.beans.artifact.ArtifactStreamType.BAMBOO;
import static software.wings.beans.artifact.ArtifactStreamType.GCS;
import static software.wings.beans.artifact.ArtifactStreamType.JENKINS;
import static software.wings.common.Constants.ARTIFACT_FILE_NAME;
import static software.wings.common.Constants.ARTIFACT_FILE_SIZE;
import static software.wings.common.Constants.ARTIFACT_PATH;
import static software.wings.common.Constants.BUCKET_NAME;
import static software.wings.common.Constants.BUILD_FULL_DISPLAY_NAME;
import static software.wings.common.Constants.BUILD_NO;
import static software.wings.common.Constants.KEY;
import static software.wings.common.Constants.URL;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.Map;

public class ArtifactCollectionUtil {
  public static Artifact getArtifact(ArtifactStream artifactStream, BuildDetails buildDetails) {
    return anArtifact()
        .withAppId(artifactStream.getAppId())
        .withArtifactStreamId(artifactStream.getUuid())
        .withArtifactSourceName(artifactStream.getSourceName())
        .withDisplayName(getDisplayName(artifactStream, buildDetails))
        .withDescription(buildDetails.getDescription())
        .withMetadata(getMetadata(artifactStream.getArtifactStreamType(), buildDetails))
        .withRevision(buildDetails.getRevision())
        .build();
  }

  public static String getDisplayName(ArtifactStream artifactStream, BuildDetails buildDetails) {
    if (isNotEmpty(buildDetails.getBuildDisplayName())) {
      return buildDetails.getBuildDisplayName();
    }
    if (artifactStream.getArtifactStreamType().equals(ARTIFACTORY.name())) {
      if (buildDetails.getArtifactPath() != null) {
        return artifactStream.getArtifactDisplayName("");
      }
    } else if (artifactStream.getArtifactStreamType().equals(AMAZON_S3.name())) {
      return artifactStream.getArtifactDisplayName("");
    }

    return artifactStream.getArtifactDisplayName(buildDetails.getNumber());
  }

  public static Map<String, String> getMetadata(String artifactStreamType, BuildDetails buildDetails) {
    Map<String, String> metadata = Maps.newHashMap();
    if (artifactStreamType.equals(ARTIFACTORY.name())) {
      if (buildDetails.getArtifactPath() != null) {
        metadata.put(ARTIFACT_PATH, buildDetails.getArtifactPath());
        metadata.put(
            ARTIFACT_FILE_NAME, buildDetails.getNumber().substring(buildDetails.getNumber().lastIndexOf('/') + 1));
      }
      metadata.put(BUILD_NO, buildDetails.getNumber());
      return metadata;
    } else if (artifactStreamType.equals(AMAZON_S3.name()) || artifactStreamType.equals(GCS.name())) {
      Map<String, String> buildParameters = buildDetails.getBuildParameters();
      metadata.put(ARTIFACT_PATH, buildParameters.get(ARTIFACT_PATH));
      metadata.put(ARTIFACT_FILE_NAME, buildParameters.get(ARTIFACT_PATH));
      metadata.put(BUILD_NO, buildParameters.get(BUILD_NO));
      metadata.put(BUCKET_NAME, buildParameters.get(BUCKET_NAME));
      metadata.put(KEY, buildParameters.get(KEY));
      metadata.put(URL, buildParameters.get(URL));
      metadata.put(ARTIFACT_FILE_SIZE, buildParameters.get(ARTIFACT_FILE_SIZE));
      return metadata;
    } else if (artifactStreamType.equals(JENKINS.name()) || artifactStreamType.equals(BAMBOO.name())) {
      metadata = buildDetails.getBuildParameters();
      metadata.put(BUILD_NO, buildDetails.getNumber());
      metadata.put(BUILD_FULL_DISPLAY_NAME, buildDetails.getBuildFullDisplayName());
      metadata.put(URL, buildDetails.getBuildUrl());
      return metadata;
    }
    return ImmutableMap.of(BUILD_NO, buildDetails.getNumber());
  }
}
