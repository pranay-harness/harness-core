/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.applicationmanifest;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.rule.OwnerRule.INDER;
import static io.harness.rule.OwnerRule.PRABU;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_MANIFEST_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.eq;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.category.element.UnitTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.appmanifest.HelmChart;
import software.wings.beans.appmanifest.HelmChart.HelmChartKeys;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.applicationmanifest.HelmChartService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

@OwnedBy(CDP)
public class HelmChartServiceTest extends WingsBaseTest {
  private String APPLICATION_MANIFEST_ID = "APPLICATION_MANIFEST_ID";
  @Inject private HPersistence persistence;

  @Mock private ApplicationManifestService applicationManifestService;
  @Inject @InjectMocks private HelmChartService helmChartService;

  private HelmChart helmChart = generateHelmChartWithVersion("1.0");

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void shouldCreateHelmChart() {
    HelmChart outHelmChart = helmChartService.create(helmChart);
    assertThat(outHelmChart).isEqualTo(helmChart);
  }

  private HelmChart generateHelmChartWithVersion(String version) {
    return HelmChart.builder()
        .accountId(ACCOUNT_ID)
        .appId(APP_ID)
        .uuid(UUID + version)
        .applicationManifestId(APPLICATION_MANIFEST_ID)
        .serviceId(SERVICE_ID)
        .version(version)
        .build();
  }

  @Test
  @Owner(developers = PRABU)
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
    HelmChart newHelmChart2 = HelmChart.builder()
                                  .accountId(ACCOUNT_ID)
                                  .appId(APP_ID)
                                  .uuid("uuid2")
                                  .applicationManifestId(APPLICATION_MANIFEST_ID + 2)
                                  .serviceId(SERVICE_ID)
                                  .build();

    helmChartService.create(newHelmChart2);
    List<HelmChart> helmCharts =
        helmChartService.listHelmChartsForService(aPageRequest()
                                                      .addFilter(HelmChartKeys.appId, EQ, APP_ID)
                                                      .addFilter(HelmChartKeys.serviceId, EQ, SERVICE_ID)
                                                      .build());
    assertThat(helmCharts).containsExactlyInAnyOrder(helmChart, newHelmChart2);

    Mockito.when(applicationManifestService.getNamesForIds(eq(APP_ID), anySet()))
        .thenReturn(ImmutableMap.of(
            APPLICATION_MANIFEST_ID, APP_MANIFEST_NAME, APPLICATION_MANIFEST_ID + 2, APP_MANIFEST_NAME + 2));
    Map<String, List<HelmChart>> appManifestHelmChartMap =
        helmChartService.listHelmChartsForService(APP_ID, SERVICE_ID, null, new PageRequest<>());

    assertThat(appManifestHelmChartMap.get(APP_MANIFEST_NAME)).containsOnly(helmChart);
    assertThat(appManifestHelmChartMap.get(APP_MANIFEST_NAME + 2)).containsOnly(newHelmChart2);
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void shouldGetHelmChart() {
    helmChartService.create(helmChart);
    HelmChart outHelmChart = helmChartService.get(APP_ID, UUID + "1.0");
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

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldGetLastCollectedHelmChart() {
    helmChartService.create(helmChart);
    HelmChart newHelmChart = generateHelmChartWithVersion("2.0");
    helmChartService.create(newHelmChart);
    HelmChart outHelmChart = helmChartService.getLastCollectedManifest(ACCOUNT_ID, APPLICATION_MANIFEST_ID);
    assertThat(outHelmChart).isEqualTo(newHelmChart);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testListHelmChartsForApplicationManifest() {
    helmChartService.create(helmChart);
    HelmChart newHelmChart = HelmChart.builder()
                                 .accountId(ACCOUNT_ID)
                                 .appId(APP_ID)
                                 .uuid(UUID + 2)
                                 .applicationManifestId(APPLICATION_MANIFEST_ID + 2)
                                 .serviceId(SERVICE_ID)
                                 .build();
    helmChartService.create(newHelmChart);
    List<HelmChart> helmCharts = helmChartService.listHelmChartsForAppManifest(ACCOUNT_ID, APPLICATION_MANIFEST_ID);
    assertThat(helmCharts).containsOnly(helmChart);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldListByIds() {
    helmChartService.create(helmChart);
    HelmChart newHelmChart = generateHelmChartWithVersion("2.0");
    HelmChart newHelmChart2 = generateHelmChartWithVersion("3.0");
    helmChartService.create(newHelmChart);
    helmChartService.create(newHelmChart2);
    List<HelmChart> outHelmCharts = helmChartService.listByIds(ACCOUNT_ID, Arrays.asList("UUID1.0", "UUID2.0"));
    assertThat(outHelmCharts).containsExactlyInAnyOrder(helmChart, newHelmChart);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testDeleteHelmCharts() {
    List<HelmChart> helmCharts =
        Arrays.asList(helmChart, generateHelmChartWithVersion("2.0"), generateHelmChartWithVersion("3.0"));
    helmCharts.forEach(helmChart -> helmChartService.create(helmChart));
    assertThat(helmChartService.deleteHelmChartsByVersions(
                   ACCOUNT_ID, APPLICATION_MANIFEST_ID, new HashSet(Arrays.asList("2.0", "3.0"))))
        .isTrue();

    List<HelmChart> finalHelmCharts =
        helmChartService.listHelmChartsForAppManifest(ACCOUNT_ID, APPLICATION_MANIFEST_ID);
    assertThat(finalHelmCharts).containsOnly(helmChart);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testAddHelmCharts() {
    helmChartService.create(helmChart);
    HelmChart helmChart2 = generateHelmChartWithVersion("2.0");
    HelmChart helmChart3 = generateHelmChartWithVersion("3.0");
    List<HelmChart> helmCharts = Arrays.asList(helmChart, helmChart2, helmChart3);
    assertThat(helmChartService.addCollectedHelmCharts(ACCOUNT_ID, APPLICATION_MANIFEST_ID, helmCharts)).isTrue();

    List<HelmChart> finalHelmCharts =
        helmChartService.listHelmChartsForAppManifest(ACCOUNT_ID, APPLICATION_MANIFEST_ID);
    assertThat(finalHelmCharts.size()).isEqualTo(3);
    assertThat(finalHelmCharts).containsExactlyInAnyOrder(helmChart, helmChart2, helmChart3);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testGetHelmChartsWithVersion() {
    helmChartService.create(helmChart);
    HelmChart helmChart2 = generateHelmChartWithVersion("2.0");
    helmChartService.addCollectedHelmCharts(ACCOUNT_ID, APPLICATION_MANIFEST_ID, Arrays.asList(helmChart, helmChart2));
    HelmChart helmChartWithVersion =
        helmChartService.getManifestByVersionNumber(ACCOUNT_ID, APPLICATION_MANIFEST_ID, "2.0");
    assertThat(helmChartWithVersion).isEqualTo(helmChart2);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testGetHelmChartsMatchingRegex() {
    helmChartService.create(helmChart);
    HelmChart helmChart2 = generateHelmChartWithVersion("2.1");
    HelmChart helmChart3 = generateHelmChartWithVersion("1.1");
    helmChartService.addCollectedHelmCharts(
        ACCOUNT_ID, APPLICATION_MANIFEST_ID, Arrays.asList(helmChart, helmChart2, helmChart3));
    HelmChart helmChartWithVersion =
        helmChartService.getLastCollectedManifestMatchingRegex(ACCOUNT_ID, APPLICATION_MANIFEST_ID, "2\\.*");
    assertThat(helmChartWithVersion).isEqualTo(helmChart2);
  }
}
