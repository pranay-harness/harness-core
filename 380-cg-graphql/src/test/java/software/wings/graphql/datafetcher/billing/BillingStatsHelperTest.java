/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.HITESH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class BillingStatsHelperTest extends WingsBaseTest {
  @Inject @InjectMocks QLBillingStatsHelper statsHelper;

  @Test
  @Owner(developers = HITESH)
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
