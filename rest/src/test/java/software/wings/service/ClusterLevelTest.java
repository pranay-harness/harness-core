package software.wings.service;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import software.wings.WingsBaseTest;
import software.wings.service.intfc.analysis.ClusterLevel;

import java.util.Collection;

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
        {ClusterLevel.H0, null, ClusterLevel.H1}, {ClusterLevel.H1, null, ClusterLevel.H2},
        {ClusterLevel.H2, null, ClusterLevel.HF}, {ClusterLevel.HF, null, ClusterLevel.HF}

    });
  }

  @Test
  public void testHeartbeatAndNextClusterLevel() {
    if (heartbeatLevel == null) {
      try {
        ClusterLevel.getHeartBeatLevel(level);
        fail("should have thrown exception");
      } catch (RuntimeException e) {
        // expected
      }
    } else {
      assertEquals(heartbeatLevel, ClusterLevel.getHeartBeatLevel(level));
    }

    assertEquals(nextLevel, level.next());
  }
}
