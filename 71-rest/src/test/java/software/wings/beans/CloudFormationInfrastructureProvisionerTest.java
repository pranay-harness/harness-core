package software.wings.beans;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.CloudFormationInfrastructureProvisioner.CloudFormationInfrastructureProvisionerBuilder;

public class CloudFormationInfrastructureProvisionerTest {
  @Test
  @Category(UnitTests.class)
  public void testCloudFormationProvisioner() {
    CloudFormationInfrastructureProvisionerBuilder builder = CloudFormationInfrastructureProvisioner.builder();
    builder.sourceType("TEMPLATE_BODY");
    assertThat(builder.build().provisionByBody()).isTrue();
    builder = CloudFormationInfrastructureProvisioner.builder();
    builder.sourceType("TEMPLATE_URL");
    assertThat(builder.build().provisionByUrl()).isTrue();
  }
}