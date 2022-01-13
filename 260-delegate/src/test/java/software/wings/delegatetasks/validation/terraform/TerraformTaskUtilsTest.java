/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.validation.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.exception.WingsException.USER;
import static io.harness.rule.OwnerRule.ARVIND;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.provision.TfVarScriptRepositorySource;
import io.harness.provision.TfVarSource;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;
import software.wings.api.terraform.TfVarGitSource;
import software.wings.beans.GitFileConfig;

import java.io.IOException;
import java.util.Arrays;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class TerraformTaskUtilsTest extends WingsBaseTest {
  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testGetGitExceptionMessageIfExists() {
    Exception ex;

    ex = new JGitInternalException("jgit-exception", new IOException("out of memory"));
    assertThat(TerraformTaskUtils.getGitExceptionMessageIfExists(ex)).isEqualTo("out of memory");

    ex = new JGitInternalException("jgit-exception");
    assertThat(TerraformTaskUtils.getGitExceptionMessageIfExists(ex)).isEqualTo("jgit-exception");

    ex = new InvalidRequestException("msg", USER);
    assertThat(TerraformTaskUtils.getGitExceptionMessageIfExists(ex)).isEqualTo("Invalid request: msg");
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testFetchAllTfVarFilesArgument() {
    String userDir = "/u/";
    String workDir = "/w/";
    String tfVarDir = "/t/";

    assertThatThrownBy(() -> TerraformTaskUtils.fetchAllTfVarFilesArgument(userDir, null, workDir, tfVarDir))
        .isInstanceOf(NullPointerException.class);
    TfVarSource tfVarSource;

    TfVarScriptRepositorySource source1 =
        TfVarScriptRepositorySource.builder().tfVarFilePaths(Arrays.asList("p1/f1", "p2/f2")).build();

    String output1 = " -var-file=\"/u/w/p1/f1\"  -var-file=\"/u/w/p2/f2\" ";

    tfVarSource = source1;
    assertThat(TerraformTaskUtils.fetchAllTfVarFilesArgument(userDir, tfVarSource, workDir, tfVarDir))
        .isEqualTo(output1);

    TfVarSource source2 =
        TfVarGitSource.builder()
            .gitFileConfig(GitFileConfig.builder().filePathList(Arrays.asList("p3/f3", "p4/f3")).build())
            .build();
    String output2 = " -var-file=\"/u/t/p3/f3\"  -var-file=\"/u/t/p4/f3\" ";
    tfVarSource = source2;
    assertThat(TerraformTaskUtils.fetchAllTfVarFilesArgument(userDir, tfVarSource, workDir, tfVarDir))
        .isEqualTo(output2);
  }
}
