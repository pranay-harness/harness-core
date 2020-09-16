package software.wings.service.impl.applicationmanifest;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.rule.OwnerRule.INDER;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.UUID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.appmanifest.HelmChart;
import software.wings.beans.appmanifest.HelmChart.HelmChartKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.applicationmanifest.HelmChartService;

import java.util.List;

public class HelmChartServiceTest extends WingsBaseTest {
  private String APPLICATION_MANIFEST_ID = "APPLICATION_MANIFEST_ID";
  @Inject private WingsPersistence wingsPersistence;

  @Inject private HelmChartService helmChartService;

  private HelmChart helmChart = HelmChart.builder()
                                    .accountId(ACCOUNT_ID)
                                    .appId(APP_ID)
                                    .uuid(UUID)
                                    .applicationManifestId(APPLICATION_MANIFEST_ID)
                                    .serviceId(SERVICE_ID)
                                    .build();

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void shouldCreateHelmChart() {
    HelmChart outHelmChart = helmChartService.create(helmChart);
    assertThat(outHelmChart).isEqualTo(helmChart);
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testListHelmChartsForService() {
    helmChartService.create(helmChart);
    HelmChart newHelmChart = HelmChart.builder()
                                 .accountId(ACCOUNT_ID)
                                 .appId(APP_ID)
                                 .uuid("uuid")
                                 .applicationManifestId(APPLICATION_MANIFEST_ID)
                                 .serviceId("serviceId")
                                 .build();
    helmChartService.create(newHelmChart);
    List<HelmChart> helmCharts =
        helmChartService.listHelmChartsForService(aPageRequest()
                                                      .addFilter(HelmChartKeys.appId, EQ, APP_ID)
                                                      .addFilter(HelmChartKeys.serviceId, EQ, SERVICE_ID)
                                                      .build());
    assertThat(helmCharts).containsOnly(helmChart);
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void shouldGetHelmChart() {
    helmChartService.create(helmChart);
    HelmChart outHelmChart = helmChartService.get(APP_ID, UUID);
    assertThat(outHelmChart).isEqualTo(helmChart);
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void shouldPruneByApplicationManifest() {
    helmChartService.create(helmChart);
    helmChartService.pruneByApplicationManifest(APP_ID, APPLICATION_MANIFEST_ID);
    assertThat(helmChartService.get(APP_ID, UUID)).isNull();
  }
}
