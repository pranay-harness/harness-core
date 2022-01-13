/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.harnesscli;

import static io.harness.rule.OwnerRule.SHUBHANSHU;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.CliFunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.PipelineRestUtils;

import software.wings.beans.Application;
import software.wings.beans.Pipeline;

import com.google.inject.Inject;
import java.util.Iterator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class GetPipelinesFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;

  private Application application;
  private String defaultOutput = "No Pipelines were found in the Application provided";
  private Pipeline testPipeline;

  private final Seed seed = new Seed(0);
  private Owners owners;

  @Before
  public void setUp() {
    owners = ownerManager.create();
    application = applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST);
    assertThat(application).isNotNull();
    harnesscliHelper.loginToCLI();
  }

  @Inject HarnesscliHelper harnesscliHelper;
  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(CliFunctionalTests.class)
  @Ignore("This test is skipping through maven command. Skipping for bazel")
  public void getPipelinesTest() {
    // Running harness get pipelines before creating a new pipeline
    String appId = application.getUuid();
    String command = String.format("harness get pipelines -a %s", appId);
    log.info("Running harness get pipelines");
    List<String> cliOutput = null;
    try {
      cliOutput = harnesscliHelper.executeCLICommand(command);
    } catch (Exception IOException) {
      log.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }
    assertThat(cliOutput).isNotNull();
    int outputSize = cliOutput.size();

    // Creating a new pipeline
    String pipelineName = "Test pipeline harnessCli - " + System.currentTimeMillis();
    log.info("Generated unique pipeline name : " + pipelineName);
    Pipeline pipeline = new Pipeline();
    pipeline.setName(pipelineName);
    log.info("Creating the pipeline");
    testPipeline = PipelineRestUtils.createPipeline(appId, pipeline, getAccount().getUuid(), bearerToken);
    assertThat(testPipeline).isNotNull();

    // Running harness get pipelines after creating a new pipeline
    log.info("Running harness get pipelines after creating a new pipeline");
    cliOutput = null;
    try {
      cliOutput = harnesscliHelper.executeCLICommand(command);
    } catch (Exception IOException) {
      log.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }
    assertThat(cliOutput).isNotNull();
    int newOutputSize = cliOutput.size();
    assertThat(newOutputSize).isGreaterThanOrEqualTo(outputSize + 1);

    boolean newPipelineListed = false;
    Iterator<String> iterator = cliOutput.iterator();
    while (iterator.hasNext()) {
      if (iterator.next().contains(testPipeline.getUuid())) {
        newPipelineListed = true;
        break;
      }
    }
    assertThat(newPipelineListed).isTrue();
    PipelineRestUtils.deletePipeline(appId, testPipeline.getUuid(), bearerToken);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(CliFunctionalTests.class)
  @Ignore("This test is skipping through maven command. Skipping for bazel")
  public void getPipelinesWithInvalidArgumentsTest() {
    // Running harness get pipelines with invalid appId
    String command = String.format("harness get pipelines -a %s", "INVALID_ID");
    log.info("Running harness get pipelines with invalid app ID");
    List<String> cliOutput = null;
    try {
      cliOutput = harnesscliHelper.executeCLICommand(command);
    } catch (Exception IOException) {
      log.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }
    assertThat(cliOutput).isNotNull();
    assertThat(cliOutput.size()).isEqualTo(1);
    assertThat(cliOutput.get(0)).isEqualTo(defaultOutput);
  }
}
