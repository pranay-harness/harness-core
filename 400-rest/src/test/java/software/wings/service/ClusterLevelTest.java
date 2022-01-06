/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service;

import static io.harness.rule.OwnerRule.RAGHU;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.service.intfc.analysis.ClusterLevel;

import java.util.Collection;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Created by rsingh on 5/29/18.
 */
@RunWith(Parameterized.class)
public class ClusterLevelTest extends WingsBaseTest {
  @Parameter() public ClusterLevel level;
  @Parameter(1) public ClusterLevel heartbeatLevel;
  @Parameter(2) public ClusterLevel nextLevel;

  @Parameters
  public static Collection<Object[]> data() {
    return asList(new Object[][] {{ClusterLevel.L0, ClusterLevel.H0, ClusterLevel.L1},
        {ClusterLevel.L1, ClusterLevel.H1, ClusterLevel.L2}, {ClusterLevel.L2, ClusterLevel.H2, ClusterLevel.L2},
        {ClusterLevel.H0, ClusterLevel.H0, ClusterLevel.H1}, {ClusterLevel.H1, ClusterLevel.H1, ClusterLevel.H2},
        {ClusterLevel.H2, ClusterLevel.H2, ClusterLevel.HF}, {ClusterLevel.HF, ClusterLevel.HF, ClusterLevel.HF}

    });
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testHeartbeatAndNextClusterLevel() {
    if (heartbeatLevel == null) {
      try {
        ClusterLevel.getHeartBeatLevel(level);
        fail("should have thrown exception");
      } catch (RuntimeException e) {
        // expected
      }
    } else {
      assertThat(ClusterLevel.getHeartBeatLevel(level)).isEqualTo(heartbeatLevel);
    }

    assertThat(level.next()).isEqualTo(nextLevel);
  }
}
