/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.generator.artifactstream;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.SettingGenerator;

import software.wings.beans.artifact.ArtifactStream;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ArtifactStreamService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.benas.randombeans.api.EnhancedRandom;

@OwnedBy(CDP)
@Singleton
public class ArtifactStreamManager {
  @Inject ArtifactStreamService artifactStreamService;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private SettingGenerator settingGenerator;
  @Inject private ArtifactStreamGeneratorFactory streamGeneratorFactory;

  @Inject WingsPersistence wingsPersistence;

  public enum ArtifactStreams {
    HARNESS_SAMPLE_ECHO_WAR,
    ARTIFACTORY_ECHO_WAR,
    HARNESS_SAMPLE_IIS_APP_S3,
    HARNESS_SAMPLE_ECR,
    HARNESS_SAMPLE_AZURE,
    HARNESS_SAMPLE_DOCKER,
    AWS_AMI,
    SPOTINST_AMI,
    AZURE_MACHINE_IMAGE_LINUX_GALLERY,
    HARNESS_EXAMPLE_LAMBDA,
    HARNESS_SAMPLE_ECHO_WAR_AT_CONNECTOR,
    HARNESS_SAMPLE_DOCKER_AT_CONNECTOR,
    PCF,
    JENKINS_METADATA_ONLY,
    BAMBOO_METADATA_ONLY,
    NEXUS2_MAVEN_METADATA_ONLY,
    NEXUS3_MAVEN_METADATA_ONLY,
    NEXUS2_MAVEN_METADATA_ONLY_PARAMETERIZED,
    NEXUS2_NPM_METADATA_ONLY_PARAMETERIZED,
    NEXUS2_NUGET_METADATA_ONLY_PARAMETERIZED,
    NEXUS3_NPM_METADATA_ONLY,
    NEXUS3_DOCKER_METADATA_ONLY
  }

  public ArtifactStream ensurePredefined(Seed seed, Owners owners, ArtifactStreams predefined) {
    return ensurePredefined(seed, owners, predefined, false);
  }

  public ArtifactStream ensurePredefined(Seed seed, Owners owners, ArtifactStreams predefined, String serviceName) {
    ArtifactStreamsGenerator streamsGenerator = streamGeneratorFactory.getArtifactStreamGenerator(predefined);
    return streamsGenerator.ensureArtifactStream(seed, owners, serviceName, false);
  }

  public ArtifactStream ensurePredefined(Seed seed, Owners owners, ArtifactStreams predefined, boolean atConnector) {
    ArtifactStreamsGenerator streamsGenerator = streamGeneratorFactory.getArtifactStreamGenerator(predefined);
    return streamsGenerator.ensureArtifactStream(seed, owners);
  }

  public ArtifactStream ensurePredefined(
      Seed seed, Owners owners, ArtifactStreams predefined, boolean atConnector, boolean metadataOnly) {
    ArtifactStreamsGenerator streamsGenerator = streamGeneratorFactory.getArtifactStreamGenerator(predefined);
    return streamsGenerator.ensureArtifactStream(seed, owners, atConnector, metadataOnly);
  }

  public ArtifactStream ensurePredefined(
      Seed seed, Owners owners, ArtifactStreams predefined, ArtifactStream artifactStream) {
    ArtifactStreamsGenerator streamsGenerator = streamGeneratorFactory.getArtifactStreamGenerator(predefined);
    return streamsGenerator.ensureArtifactStream(seed, artifactStream, owners);
  }

  /**
   * This is when u don't care about artifactStreamType.
   * Make sure, your code is able to handle random artifactStream
   *
   * @param seed
   * @param owners
   * @return
   */
  public ArtifactStream ensureRandom(Seed seed, Owners owners) {
    EnhancedRandom random = Randomizer.instance(seed);
    ArtifactStreams predefined = random.nextObject(ArtifactStreams.class);

    ArtifactStreamsGenerator streamsGenerator = streamGeneratorFactory.getArtifactStreamGenerator(predefined);
    if (streamsGenerator != null) {
      return streamsGenerator.ensureArtifactStream(seed, owners);
    }
    return null;
  }
}
