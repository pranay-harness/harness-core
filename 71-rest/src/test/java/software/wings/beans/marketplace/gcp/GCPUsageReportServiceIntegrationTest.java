package software.wings.beans.marketplace.gcp;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.rule.Owner;
import lombok.val;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.marketplace.gcp.GCPUsageReport.GCPUsageReportKeys;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;
import software.wings.service.impl.marketplace.gcp.GCPMarketPlaceServiceIntegrationTest;
import software.wings.service.impl.marketplace.gcp.GCPUsageReportServiceImpl;

import java.time.Instant;

public class GCPUsageReportServiceIntegrationTest extends BaseIntegrationTest {
  @Inject private GCPUsageReportServiceImpl gcpUsageReportService;
  @Inject private WingsPersistence persistence;
  private boolean indexesEnsured;

  // namespacing accountId so that other tests are not impacted by this
  private static final String SOME_ACCOUNT_ID =
      "gcp-account-id-" + GCPUsageReportServiceIntegrationTest.class.getSimpleName();

  private static final String SOME_CONSUMER_ID =
      "gcp-consumer-id-" + GCPUsageReportServiceIntegrationTest.class.getSimpleName();

  private static final String SOME_OPERATION_ID =
      "gcp-operation-id-" + GCPUsageReportServiceIntegrationTest.class.getSimpleName();

  private static final String SOME_ENTITLEMENT_NAME =
      "gcp-entitlement-" + GCPMarketPlaceServiceIntegrationTest.class.getSimpleName();

  @Before
  public void ensureIndices() {
    if (!indexesEnsured) {
      persistence.getDatastore(GCPUsageReport.class).ensureIndexes(GCPUsageReport.class);
      indexesEnsured = true;
    }
  }

  @Test
  @Owner(developers = HITESH)
  @Category(DeprecatedIntegrationTests.class)
  public void testCreateGCPUsageReport() {
    val gcpUsageReport = getSampleGCPUsageReport();
    val id = persistence.save(gcpUsageReport);
    assertThat(id).isNotNull();

    val fetchedGCPUsageReport = persistence.get(GCPUsageReport.class, id);
    assertThat(gcpUsageReport).isEqualTo(fetchedGCPUsageReport);

    val newId = persistence.save(gcpUsageReport);
    assertThat(newId).isNotNull();

    assertThat(newId).isEqualTo(id);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(DeprecatedIntegrationTests.class)
  public void testLastGCPUsageReportTime() {
    val gcpUsageReport = getSampleGCPUsageReport();
    val id = persistence.save(gcpUsageReport);
    assertThat(id).isNotNull();

    val lastGCPUsageReportTime = gcpUsageReportService.fetchLastGCPUsageReportTime(SOME_ACCOUNT_ID);
    assertThat(gcpUsageReport.getEndTimestamp()).isEqualTo(lastGCPUsageReportTime);
  }

  private GCPUsageReport getSampleGCPUsageReport() {
    val startInstance = Instant.now();
    val endInstance = startInstance.plusSeconds(1);
    return new GCPUsageReport(
        SOME_ACCOUNT_ID, SOME_CONSUMER_ID, SOME_OPERATION_ID, SOME_ENTITLEMENT_NAME, startInstance, endInstance, 5);
  }

  @After
  public void clearCollection() {
    val ds = persistence.getDatastore(GCPUsageReport.class);
    ds.delete(ds.createQuery(GCPUsageReport.class).filter(GCPUsageReportKeys.accountId, SOME_ACCOUNT_ID));
  }
}
