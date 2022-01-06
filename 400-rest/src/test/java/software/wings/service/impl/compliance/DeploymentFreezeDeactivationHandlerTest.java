/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.compliance;

import static io.harness.rule.OwnerRule.PRABU;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import static org.mockito.Matchers.any;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.governance.TimeRangeBasedFreezeConfig;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.governance.GovernanceConfig;
import software.wings.resources.stats.model.TimeRange;
import software.wings.service.impl.deployment.checks.DeploymentFreezeUtils;

import com.google.inject.Inject;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@TargetModule(HarnessModule._953_EVENTS_API)
@RunWith(PowerMockRunner.class)
@OwnedBy(HarnessTeam.CDC)
@PrepareForTest({System.class, DeploymentFreezeDeactivationHandler.class})
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
public class DeploymentFreezeDeactivationHandlerTest extends WingsBaseTest {
  @Mock DeploymentFreezeUtils deploymentFreezeUtils;
  @Inject @InjectMocks DeploymentFreezeDeactivationHandler deploymentFreezeDeactivationHandler;

  private long mockCurrentTime = 1000000L;

  @Before
  public void setup() {
    initMocks(this);
    mockStatic(System.class);
    when(System.currentTimeMillis()).thenReturn(mockCurrentTime);
  }
  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldDoNothingForNoFreezeWindows() {
    deploymentFreezeDeactivationHandler.handle(GovernanceConfig.builder().build());
    Mockito.verify(deploymentFreezeUtils, Mockito.never()).handleDeActivationEvent(any(), Matchers.anyString());
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldDoNothingForNoMatchingWindows() {
    GovernanceConfig governanceConfig =
        GovernanceConfig.builder()
            .timeRangeBasedFreezeConfigs(
                Arrays.asList(TimeRangeBasedFreezeConfig.builder()
                                  .timeRange(new TimeRange(1000, 2000, "Asia/Kolkatta", false, null, null, null, false))
                                  .build()))
            .build();
    deploymentFreezeDeactivationHandler.handle(governanceConfig);
    Mockito.verify(deploymentFreezeUtils, Mockito.never()).handleDeActivationEvent(any(), Matchers.anyString());
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldHandleActivationEventForMatchingWindows() {
    TimeRangeBasedFreezeConfig window1 =
        TimeRangeBasedFreezeConfig.builder()
            .timeRange(new TimeRange(1000, 2000, "Asia/Kolkatta", false, null, null, null, false))
            .build();
    TimeRangeBasedFreezeConfig window2 = TimeRangeBasedFreezeConfig.builder()
                                             .timeRange(new TimeRange(mockCurrentTime - 5000, mockCurrentTime - 1000,
                                                 "Asia/Kolkatta", false, null, null, null, false))
                                             .build();
    TimeRangeBasedFreezeConfig window3 = TimeRangeBasedFreezeConfig.builder()
                                             .timeRange(new TimeRange(mockCurrentTime - 5000, mockCurrentTime - 1000,
                                                 "Asia/Kolkatta", false, null, null, null, false))
                                             .build();
    GovernanceConfig governanceConfig = GovernanceConfig.builder()
                                            .accountId(ACCOUNT_ID)
                                            .timeRangeBasedFreezeConfigs(Arrays.asList(window1, window2, window3))
                                            .build();

    deploymentFreezeDeactivationHandler.handle(governanceConfig);
    Mockito.verify(deploymentFreezeUtils, Mockito.times(2)).handleDeActivationEvent(any(), Matchers.eq(ACCOUNT_ID));
  }
}
