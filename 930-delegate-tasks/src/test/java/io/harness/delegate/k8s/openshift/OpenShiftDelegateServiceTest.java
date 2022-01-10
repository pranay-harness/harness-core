/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.k8s.openshift;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.beans.FileData;
import io.harness.category.element.UnitTests;
import io.harness.cli.CliResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.openshift.OpenShiftClient;
import io.harness.rule.Owner;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class OpenShiftDelegateServiceTest extends CategoryTest {
  @Mock private OpenShiftClient openShiftClient;
  @Mock private LogCallback logCallback;
  @InjectMocks private OpenShiftDelegateService openShiftDelegateService;
  private static final String BASE_PATH = "openshift";

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testProcessTemplatization() throws IOException {
    shouldGiveK8SReadyYaml();
    shouldThrowExceptionWhenOcProcessFails();
    shouldThrowExceptionWhenProcessResultEmpty();
    shouldThrowExceptionWhenItemsEmpty();
  }

  private void shouldThrowExceptionWhenItemsEmpty() throws IOException {
    String OC_BINARY_PATH = "OC_BINARY_PATH";
    String TEMPLATE_FILE_PATH = "TEMPLATE_FILE_PATH";
    String MANIFEST_DIRECTORY_PATH = ".";
    List<String> paramFileContent = Arrays.asList("a:b", "c:d");
    List<String> paramFilePaths = Arrays.asList("params-0", "params-1");

    String ocResultYamlFile = "oc_empty_items.yaml";
    CliResponse cliResponse = CliResponse.builder()
                                  .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                  .output(readFileAsString(ocResultYamlFile))
                                  .build();

    Mockito.doReturn(cliResponse)
        .when(openShiftClient)
        .process(OC_BINARY_PATH, TEMPLATE_FILE_PATH, paramFilePaths, MANIFEST_DIRECTORY_PATH, logCallback);

    assertThatThrownBy(()
                           -> openShiftDelegateService.processTemplatization(MANIFEST_DIRECTORY_PATH, OC_BINARY_PATH,
                               TEMPLATE_FILE_PATH, logCallback, paramFileContent))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Items list can't be empty");
  }

  private void shouldThrowExceptionWhenProcessResultEmpty() {
    String OC_BINARY_PATH = "OC_BINARY_PATH";
    String TEMPLATE_FILE_PATH = "TEMPLATE_FILE_PATH";
    String MANIFEST_DIRECTORY_PATH = ".";
    List<String> paramFileContent = Arrays.asList("a:b", "c:d");
    List<String> paramFilePaths = Arrays.asList("params-0", "params-1");

    CliResponse cliResponse =
        CliResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).output("").build();

    Mockito.doReturn(cliResponse)
        .when(openShiftClient)
        .process(OC_BINARY_PATH, TEMPLATE_FILE_PATH, paramFilePaths, MANIFEST_DIRECTORY_PATH, logCallback);

    assertThatThrownBy(()
                           -> openShiftDelegateService.processTemplatization(MANIFEST_DIRECTORY_PATH, OC_BINARY_PATH,
                               TEMPLATE_FILE_PATH, logCallback, paramFileContent))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Oc process result can't be empty");
  }

  private void shouldThrowExceptionWhenOcProcessFails() throws IOException {
    String OC_BINARY_PATH = "OC_BINARY_PATH";
    String TEMPLATE_FILE_PATH = "TEMPLATE_FILE_PATH";
    String MANIFEST_DIRECTORY_PATH = ".";
    List<String> paramFileContent = Arrays.asList("a:b", "c:d");
    List<String> paramFilePaths = Arrays.asList("params-0", "params-1");

    CliResponse cliResponse = CliResponse.builder()
                                  .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                                  .output("Invalid parameter")
                                  .build();

    Mockito.doReturn(cliResponse)
        .when(openShiftClient)
        .process(OC_BINARY_PATH, TEMPLATE_FILE_PATH, paramFilePaths, MANIFEST_DIRECTORY_PATH, logCallback);

    assertThatThrownBy(()
                           -> openShiftDelegateService.processTemplatization(MANIFEST_DIRECTORY_PATH, OC_BINARY_PATH,
                               TEMPLATE_FILE_PATH, logCallback, paramFileContent))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Oc process command failed. Invalid parameter");
  }

  private void shouldGiveK8SReadyYaml() throws IOException {
    String OC_BINARY_PATH = "OC_BINARY_PATH";
    String TEMPLATE_FILE_PATH = "TEMPLATE_FILE_PATH";
    String MANIFEST_DIRECTORY_PATH = ".";
    List<String> paramFileContent = Arrays.asList("a:b", "c:d");
    List<String> paramFilePaths = Arrays.asList("params-0", "params-1");

    String ocResultYamlFile = "oc_process_result.yaml";
    String expectedYamlFile = "expected_parsed_result.yaml";

    CliResponse cliResponse = CliResponse.builder()
                                  .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                  .output(readFileAsString(ocResultYamlFile))
                                  .build();

    Mockito.doReturn(cliResponse)
        .when(openShiftClient)
        .process(OC_BINARY_PATH, TEMPLATE_FILE_PATH, paramFilePaths, MANIFEST_DIRECTORY_PATH, logCallback);

    List<FileData> manifestFiles = openShiftDelegateService.processTemplatization(
        MANIFEST_DIRECTORY_PATH, OC_BINARY_PATH, TEMPLATE_FILE_PATH, logCallback, paramFileContent);

    assertThat(manifestFiles).hasSize(1);
    assertThat(manifestFiles.get(0).getFileContent()).isEqualTo(readFileAsString(expectedYamlFile));
  }

  private String readFileAsString(String resourceFilePath) throws IOException {
    ClassLoader classLoader = OpenShiftDelegateServiceTest.class.getClassLoader();
    return Resources.toString(
        Objects.requireNonNull(classLoader.getResource(BASE_PATH + "/" + resourceFilePath)), StandardCharsets.UTF_8);
  }
}
