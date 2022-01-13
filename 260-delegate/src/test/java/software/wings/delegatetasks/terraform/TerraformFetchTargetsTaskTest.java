/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.delegatetasks.TerraformFetchTargetsTask;
import software.wings.utils.WingsTestConstants;

import com.bertramlabs.plugins.hcl4j.HCLParser;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class TerraformFetchTargetsTaskTest extends WingsBaseTest {
  TerraformFetchTargetsTask terraformFetchTargetsTask = new TerraformFetchTargetsTask(
      DelegateTaskPackage.builder()
          .delegateId(WingsTestConstants.DELEGATE_ID)
          .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())

          .build(),
      null, delegateTaskResponse -> {}, () -> true);
  Map<String, Object> parsedContentWithModulesAndResources, getParsedContentWithoutModulesAndResources;

  @Before
  public void setUp() throws Exception {
    String fileContent = "variable \"access_key\" {}\n"
        + "variable \"secret_key\" {}\n"
        + "    \n"
        + "provider \"aws\" {\n"
        + "       region = \"us-east-1\"\n"
        + "       access_key = \"${var.access_key}\"\n"
        + "       secret_key = \"${var.secret_key}\"\n"
        + "}\n"
        + "\n"
        + "module \"module1\" {\n"
        + "  source  = \"terraform-aws-modules/security-group/aws\"\n"
        + "  version = \"2.9.0\"\n"
        + "}\n"
        + "\n"
        + "module \"module2\" {\n"
        + "\tsource  = \"terraform-aws-modules/security-group/aws\"\n"
        + "  \tversion = \"2.9.0\"\n"
        + "}\n"
        + "\n"
        + "resource \"aws_s3_bucket\" \"example\" {\n"
        + "  bucket = \"provider-explicit-example\"\n"
        + "}\n"
        + "\n"
        + "resource \"aws_s3_bucket\" \"example1\" {\n"
        + "  bucket = \"provider-explicit-example\"\n"
        + "}";

    HCLParser hclParser = new HCLParser();
    parsedContentWithModulesAndResources = hclParser.parse(fileContent);

    fileContent = "variable \"access_key\" {}\n"
        + "variable \"secret_key\" {}\n"
        + "    \n"
        + "provider \"aws\" {\n"
        + "       region = \"us-east-1\"\n"
        + "       access_key = \"${var.access_key}\"\n"
        + "       secret_key = \"${var.secret_key}\"\n"
        + "}\n";

    getParsedContentWithoutModulesAndResources = hclParser.parse(fileContent);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void getTargetModulesTest() {
    List<String> targetModules = terraformFetchTargetsTask.getTargetModules(parsedContentWithModulesAndResources);
    assertThat(
        targetModules.containsAll(Arrays.asList("module.module1", "module.module2")) && targetModules.size() == 2)
        .isTrue();

    targetModules = terraformFetchTargetsTask.getTargetModules(getParsedContentWithoutModulesAndResources);
    assertThat(targetModules.isEmpty()).isTrue();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void getTargetResourcesTest() {
    List<String> targetResources = terraformFetchTargetsTask.getTargetResources(parsedContentWithModulesAndResources);
    assertThat(targetResources.containsAll(Arrays.asList("aws_s3_bucket.example", "aws_s3_bucket.example1"))
        && targetResources.size() == 2)
        .isTrue();

    targetResources = terraformFetchTargetsTask.getTargetResources(getParsedContentWithoutModulesAndResources);
    assertThat(targetResources.isEmpty()).isTrue();
  }
}
