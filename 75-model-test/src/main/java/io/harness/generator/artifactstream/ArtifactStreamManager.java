package io.harness.generator.artifactstream;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.api.EnhancedRandom;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.SettingGenerator;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ArtifactStreamService;

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
    HARNESS_SAMPLE_IIS_APP,
    HARNESS_SAMPLE_ECR,
    HARNESS_SAMPLE_DOCKER
  }

  public ArtifactStream ensurePredefined(Seed seed, Owners owners, ArtifactStreams predefined) {
    ArtifactStreamsGenerator streamsGenerator = streamGeneratorFactory.getArtifactStreamGenerator(predefined);
    if (streamsGenerator != null) {
      return streamsGenerator.ensureArtifact(seed, owners);
    }
    return null;
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
      return streamsGenerator.ensureArtifact(seed, owners);
    }
    return null;
  }
}
