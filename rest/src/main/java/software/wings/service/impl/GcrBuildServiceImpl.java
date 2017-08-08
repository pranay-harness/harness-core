package software.wings.service.impl;

import static software.wings.utils.Validator.equalCheck;

import com.google.common.collect.Lists;

import software.wings.beans.ErrorCode;
import software.wings.beans.GcpConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.gcr.GcrService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.service.intfc.GcrBuildService;
import software.wings.utils.ArtifactType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by brett on 8/2/17
 */
@Singleton
public class GcrBuildServiceImpl implements GcrBuildService {
  @Inject private GcrService gcrService;

  @Override
  public List<BuildDetails> getBuilds(
      String appId, ArtifactStreamAttributes artifactStreamAttributes, GcpConfig gcpConfig) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.GCR.name());
    return gcrService.getBuilds(gcpConfig, artifactStreamAttributes, 50);
  }

  @Override
  public List<JobDetails> getJobs(GcpConfig gcpConfig, Optional<String> parentJobName) {
    return Lists.newArrayList();
  }

  @Override
  public List<String> getArtifactPaths(String jobName, String groupId, GcpConfig config) {
    throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Operation not supported by GCR Artifact Stream");
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(
      String appId, ArtifactStreamAttributes artifactStreamAttributes, GcpConfig gcpConfig) {
    throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Operation not supported by GCR Artifact Stream");
  }

  @Override
  public Map<String, String> getPlans(GcpConfig config) {
    return getJobs(config, Optional.empty())
        .stream()
        .collect(Collectors.toMap(o -> o.getJobName(), o -> o.getJobName()));
  }

  @Override
  public Map<String, String> getPlans(GcpConfig config, ArtifactType artifactType) {
    return getPlans(config);
  }

  @Override
  public List<String> getGroupIds(String jobName, GcpConfig config) {
    throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Operation not supported by GCR Artifact Stream");
  }

  @Override
  public boolean validateArtifactSource(GcpConfig config, ArtifactStreamAttributes artifactStreamAttributes) {
    return gcrService.verifyImageName(config, artifactStreamAttributes);
  }

  @Override
  public boolean validateArtifactServer(GcpConfig config) {
    return true;
  }
}
