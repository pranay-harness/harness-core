/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.terraform.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ROHITKARELIA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cli.CliResponse;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.terraform.TFTaskType;
import io.harness.delegate.task.terraform.TerraformBaseHelper;
import io.harness.delegate.task.terraform.TerraformTaskNGParameters;
import io.harness.delegate.task.terraform.TerraformTaskNGResponse;
import io.harness.encryption.SecretRefData;
import io.harness.git.GitClientHelper;
import io.harness.git.model.GitBaseRequest;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

@OwnedBy(CDP)
public class TerraformDestroyTaskHandlerTest extends CategoryTest {
  @Inject @Spy @InjectMocks TerraformDestroyTaskHandler terraformDestroyTaskHandler;
  @Mock LogCallback logCallback;
  @Mock SecretDecryptionService secretDecryptionService;
  @Mock TerraformBaseHelper terraformBaseHelper;
  @Mock GitClientHelper gitClientHelper;

  private final EncryptedRecordData encryptedPlanContent =
      EncryptedRecordData.builder().name("planName").encryptedValue("encryptedPlan".toCharArray()).build();
  private static final String gitUsername = "username";
  private static final String gitPasswordRefId = "git_password";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testDestroy() throws IOException, TimeoutException, InterruptedException {
    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    when(terraformBaseHelper.getGitBaseRequestForConfigFile(
             anyString(), any(GitStoreDelegateConfig.class), any(GitConfigDTO.class)))
        .thenReturn(any(GitBaseRequest.class));
    when(terraformBaseHelper.fetchConfigFileAndPrepareScriptDir(
             any(), anyString(), anyString(), anyString(), any(), logCallback, anyString(), anyString()))
        .thenReturn("sourceDir");
    doNothing().when(terraformBaseHelper).downloadTfStateFile(null, "accountId", null, "scriptDir");
    when(gitClientHelper.getRepoDirectory(any())).thenReturn("sourceDir");
    File outputFile = new File("sourceDir/terraform-output.tfvars");
    FileUtils.touch(outputFile);
    when(terraformBaseHelper.executeTerraformDestroyStep(any()))
        .thenReturn(CliResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build());
    TerraformTaskNGResponse response = terraformDestroyTaskHandler.executeTaskInternal(
        getTerraformTaskParameters(), "delegateId", "taskId", logCallback);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    Files.deleteIfExists(Paths.get(outputFile.getPath()));
    Files.deleteIfExists(Paths.get("sourceDir"));
  }

  private TerraformTaskNGParameters getTerraformTaskParameters() {
    return TerraformTaskNGParameters.builder()
        .accountId("accountId")
        .taskType(TFTaskType.APPLY)
        .entityId("provisionerIdentifier")
        .encryptedTfPlan(encryptedPlanContent)
        .configFile(
            GitFetchFilesConfig.builder()
                .gitStoreDelegateConfig(
                    GitStoreDelegateConfig.builder()
                        .branch("main")
                        .path("main.tf")
                        .gitConfigDTO(
                            GitConfigDTO.builder()
                                .gitAuthType(GitAuthType.HTTP)
                                .gitAuth(GitHTTPAuthenticationDTO.builder()
                                             .username(gitUsername)
                                             .passwordRef(SecretRefData.builder().identifier(gitPasswordRefId).build())
                                             .build())
                                .build())
                        .build())
                .build())
        .planName("planName")
        .build();
  }
}
