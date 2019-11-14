package software.wings.graphql.datafetcher.billing;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;

public class BillingStatsHelperTest extends WingsBaseTest {
  @Inject @InjectMocks QLBillingStatsHelper statsHelper;

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void TestStatsHelper() {
    String appId = statsHelper.getEntityName(BillingDataMetaDataFields.APPID, "appId");
    String serviceId = statsHelper.getEntityName(BillingDataMetaDataFields.SERVICEID, "serviceId");
    String clusterId = statsHelper.getEntityName(BillingDataMetaDataFields.CLUSTERID, "clusterId");
    String region = statsHelper.getEntityName(BillingDataMetaDataFields.REGION, "Region");
    String envId = statsHelper.getEntityName(BillingDataMetaDataFields.ENVID, "envId");

    assertThat(appId).isEqualTo("appId");
    assertThat(serviceId).isEqualTo("serviceId");
    assertThat(clusterId).isEqualTo("clusterId");
    assertThat(region).isEqualTo("Region");
    assertThat(envId).isEqualTo("envId");
  }
}
