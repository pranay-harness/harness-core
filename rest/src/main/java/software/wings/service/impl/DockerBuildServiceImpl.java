package software.wings.service.impl;

import static software.wings.utils.Validator.equalCheck;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.DockerBuildService;

import java.util.Arrays;
import java.util.List;
import javax.inject.Singleton;

/**
 * Created by anubhaw on 1/6/17.
 */
@Singleton
public class DockerBuildServiceImpl implements DockerBuildService {
  @Override
  public List<BuildDetails> getBuilds(String appId, ArtifactStream artifactStream, DockerConfig dockerConfig) {
    equalCheck(artifactStream.getArtifactStreamType(), ArtifactStreamType.DOCKER);

    DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                                    .withRegistryUrl(dockerConfig.getDockerRegistryUrl())
                                    .withRegistryUsername(dockerConfig.getUsername())
                                    .withRegistryPassword(dockerConfig.getPassword())
                                    .build();
    DockerClient docker = DockerClientBuilder.getInstance(config).build();
    ListImagesCmd listImagesCmd = docker.listImagesCmd();
    //    listImagesCmd.withImageNameFilter("").
    return Arrays.asList();
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, ArtifactStream artifactStream, DockerConfig dockerConfig) {
    return null;
  }
}
