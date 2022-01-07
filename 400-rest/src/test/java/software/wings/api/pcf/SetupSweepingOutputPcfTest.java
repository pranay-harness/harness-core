/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api.pcf;

import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.pcf.CfAppSetupTimeDetails;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SetupSweepingOutputPcfTest extends CategoryTest {
  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldFetchPcfVariableInfo() {
    String guid = "1234_0";
    String name = "appName_0";

    String guid1 = "1234_1";
    String name1 = "appName_1";

    List<String> tempRoutes = Arrays.asList("r1", "r2");
    List<String> finalRoutes = Arrays.asList("r3", "r4");
    SetupSweepingOutputPcf setupSweepingOutputPcf =
        SetupSweepingOutputPcf.builder()
            .newPcfApplicationDetails(
                CfAppSetupTimeDetails.builder().applicationGuid(guid1).applicationName(name1).urls(tempRoutes).build())
            .appDetailsToBeDownsized(Collections.singletonList(
                CfAppSetupTimeDetails.builder().applicationGuid(guid).applicationName(name).urls(finalRoutes).build()))
            .routeMaps(finalRoutes)
            .tempRouteMap(tempRoutes)
            .mostRecentInactiveAppVersionDetails(CfAppSetupTimeDetails.builder()
                                                     .applicationName("app1")
                                                     .applicationGuid("g1")
                                                     .initialInstanceCount(1)
                                                     .build())
            .tags(Collections.singletonList("delegate1"))
            .build();

    InfoVariables appPcfVariables = setupSweepingOutputPcf.fetchPcfVariableInfo();
    assertThat(appPcfVariables).isNotNull();
    assertThat(appPcfVariables.getNewAppName()).isEqualTo(name1);
    assertThat(appPcfVariables.getNewAppGuid()).isEqualTo(guid1);
    assertThat(appPcfVariables.getTempRoutes()).isEqualTo(tempRoutes);

    assertThat(appPcfVariables.getOldAppName()).isEqualTo(name);
    assertThat(appPcfVariables.getOldAppGuid()).isEqualTo(guid);
    assertThat(appPcfVariables.getOldAppRoutes()).isEqualTo(finalRoutes);

    assertThat(appPcfVariables.getTempRoutes()).isEqualTo(tempRoutes);
    assertThat(appPcfVariables.getFinalRoutes()).isEqualTo(finalRoutes);

    assertThat(appPcfVariables.getMostRecentInactiveAppVersionName()).isEqualTo("app1");
    assertThat(appPcfVariables.getMostRecentInactiveAppVersionGuid()).isEqualTo("g1");
    assertThat(appPcfVariables.getMostRecentInactiveAppVersionRunningInstances()).isEqualTo(1);
  }
}
