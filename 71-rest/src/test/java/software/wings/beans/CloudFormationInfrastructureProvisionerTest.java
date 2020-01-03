package software.wings.beans;

import static io.harness.rule.OwnerRule.SATYAM;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.CloudFormationInfrastructureProvisioner.CloudFormationInfrastructureProvisionerBuilder;

public class CloudFormationInfrastructureProvisionerTest extends CategoryTest {
  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testCloudFormationProvisioner() {
    CloudFormationInfrastructureProvisionerBuilder builder = CloudFormationInfrastructureProvisioner.builder();
    builder.sourceType("TEMPLATE_BODY");
    assertThat(builder.build().provisionByBody()).isTrue();
    builder = CloudFormationInfrastructureProvisioner.builder();
    builder.sourceType("TEMPLATE_URL");
    assertThat(builder.build().provisionByUrl()).isTrue();
    builder = CloudFormationInfrastructureProvisioner.builder();
    builder.sourceType("GIT");
    assertThat(builder.build().provisionByGit()).isTrue();
  }
}