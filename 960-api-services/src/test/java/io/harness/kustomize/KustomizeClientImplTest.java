/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.kustomize;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cli.CliHelper;
import io.harness.cli.CliResponse;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class KustomizeClientImplTest extends CategoryTest {
  @Mock private CliHelper cliHelper;
  @Mock LogCallback logCallback;
  @InjectMocks private KustomizeClientImpl kustomizeClientImpl;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testBuild() throws InterruptedException, IOException, TimeoutException {
    // tests correct parameters are passed to executeCliCommand
    CliResponse cliResponse = CliResponse.builder().build();
    doReturn(cliResponse)
        .when(cliHelper)
        .executeCliCommand("KUSTOMIZE_BINARY_PATH build KUSTOMIZE_DIR_PATH",
            io.harness.kustomize.KustomizeConstants.KUSTOMIZE_COMMAND_TIMEOUT, Collections.emptyMap(),
            "MANIFEST_FILES_DIRECTORY", logCallback);

    CliResponse actualResponse = kustomizeClientImpl.build(
        "MANIFEST_FILES_DIRECTORY", "KUSTOMIZE_DIR_PATH", "KUSTOMIZE_BINARY_PATH", logCallback);

    assertThat(actualResponse).isEqualTo(cliResponse);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testBuildWithPlugins() throws InterruptedException, IOException, TimeoutException {
    // tests correct parameters are passed to executeCliCommand
    CliResponse cliResponse = CliResponse.builder().build();
    doReturn(cliResponse)
        .when(cliHelper)
        .executeCliCommand(
            "XDG_CONFIG_HOME=PLUGIN_PATH KUSTOMIZE_BINARY_PATH build --enable_alpha_plugins KUSTOMIZE_DIR_PATH",
            KustomizeConstants.KUSTOMIZE_COMMAND_TIMEOUT, Collections.emptyMap(), "MANIFEST_FILES_DIRECTORY",
            logCallback);

    CliResponse actualResponse = kustomizeClientImpl.buildWithPlugins(
        "MANIFEST_FILES_DIRECTORY", "KUSTOMIZE_DIR_PATH", "KUSTOMIZE_BINARY_PATH", "PLUGIN_PATH", logCallback);

    assertThat(actualResponse).isEqualTo(cliResponse);
  }
}
