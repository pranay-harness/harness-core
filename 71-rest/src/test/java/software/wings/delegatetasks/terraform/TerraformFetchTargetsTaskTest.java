package software.wings.delegatetasks.terraform;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;

import com.bertramlabs.plugins.hcl4j.HCLParser;
import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.delegatetasks.TerraformFetchTargetsTask;
import software.wings.utils.WingsTestConstants;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TerraformFetchTargetsTaskTest extends WingsBaseTest {
  TerraformFetchTargetsTask terraformFetchTargetsTask = new TerraformFetchTargetsTask(WingsTestConstants.DELEGATE_ID,
      DelegateTask.builder().data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build()).build(),
      delegateTaskResponse -> {}, () -> true);
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
