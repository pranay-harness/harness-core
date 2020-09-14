package io.harness.batch.processing.pricing.data;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CloudProviderTest extends CategoryTest {
  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testAwsCloudProvider() {
    CloudProvider cloudProvider = CloudProvider.AWS;
    String cloudProviderName = cloudProvider.getCloudProviderName();
    assertThat(cloudProviderName).isEqualTo("amazon");
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGcpCloudProvider() {
    CloudProvider cloudProvider = CloudProvider.GCP;
    String cloudProviderName = cloudProvider.getCloudProviderName();
    assertThat(cloudProviderName).isEqualTo("google");
  }
}
