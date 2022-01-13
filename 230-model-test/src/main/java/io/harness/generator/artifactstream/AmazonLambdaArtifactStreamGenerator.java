/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.generator.artifactstream;

import static java.util.Arrays.asList;

import io.harness.generator.GeneratorConstants;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.SettingGenerator;
import io.harness.generator.SettingGenerator.Settings;

import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.AmazonS3ArtifactStream;
import software.wings.beans.artifact.ArtifactStream;

import com.google.inject.Inject;

public class AmazonLambdaArtifactStreamGenerator extends AmazonS3ArtifactStreamStreamsGenerator {
  @Inject private SettingGenerator settingGenerator;

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners) {
    Service service = owners.obtainService();
    Application application = owners.obtainApplication();

    final SettingAttribute settingAttribute =
        settingGenerator.ensurePredefined(seed, owners, Settings.AWS_DEPLOYMENT_FUNCTIONAL_TESTS_CLOUD_PROVIDER);

    return ensureArtifactStream(seed,
        AmazonS3ArtifactStream.builder()
            .appId(application.getUuid())
            .serviceId(service.getUuid())
            .name("harness_example_lambda_function")
            .sourceName(settingAttribute.getName())
            .jobname(GeneratorConstants.AWS_LAMBDA_ARTIFACT_S3BUCKET)
            .artifactPaths(asList(GeneratorConstants.AWS_LAMBDA_ARTIFACT_PATH))
            .settingId(settingAttribute.getUuid())
            .build(),
        owners);
  }
}
