package software.wings.service.impl.analysis;

import static io.harness.rule.OwnerRule.SOWMYA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.service.impl.apm.CustomAPMDataCollectionInfo;

import java.util.HashMap;
import java.util.Map;

public class MetricsDataCollectionInfoTest extends WingsBaseTest {
  private MetricsDataCollectionInfo metricsDataCollectionInfo;

  @Before
  public void setUp() {
    metricsDataCollectionInfo = spy(CustomAPMDataCollectionInfo.builder().build());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testSetHostsToGroupNameMap_nullCollection() {
    metricsDataCollectionInfo.setHostsToGroupNameMap(null);
    assertThat(metricsDataCollectionInfo.getHostsToGroupNameMap()).isEqualTo(new HashMap<>());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testSetHostsToGroupNameMap_emptyCollection() {
    metricsDataCollectionInfo.setHostsToGroupNameMap(new HashMap<>());
    assertThat(metricsDataCollectionInfo.getHostsToGroupNameMap()).isEqualTo(new HashMap<>());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testSetHostsToGroupNameMap_hostsWithDot() {
    Map<String, String> hostToGroupNameMap = new HashMap<>();
    hostToGroupNameMap.put("10.3.4.5", "default");
    metricsDataCollectionInfo.setHostsToGroupNameMap(hostToGroupNameMap);
    assertThat(metricsDataCollectionInfo.getHostsToGroupNameMap()).isEqualTo(hostToGroupNameMap);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testSetHostsToGroupNameMap_hostsWithoutDot() {
    Map<String, String> hostToGroupNameMap = new HashMap<>();
    hostToGroupNameMap.put("hostname-without-dot", "default");
    metricsDataCollectionInfo.setHostsToGroupNameMap(hostToGroupNameMap);
    assertThat(metricsDataCollectionInfo.getHostsToGroupNameMap()).isEqualTo(hostToGroupNameMap);
  }
}