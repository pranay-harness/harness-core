package software.wings.sm.states;

import static org.mockito.Mockito.when;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.regions.Regions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

/**
 * Created by peeyushaggarwal on 9/12/16.
 */
@Ignore
public class ElasticLoadBalancerStatesTest extends WingsBaseTest {
  private ElasticLoadBalancerEnableState elasticLoadBalancerEnableState = new ElasticLoadBalancerEnableState("Enable");
  private ElasticLoadBalancerDisableState elasticLoadBalancerDisableState =
      new ElasticLoadBalancerDisableState("Disable");
  @Mock private ExecutionContext executionContext;

  @Before
  public void setUp() {
    System.setProperty(SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY, "true");

    when(executionContext.getContextElement(ContextElementType.INSTANCE))
        .thenReturn(
            anInstanceElement().withHostElement(aHostElement().withHostName("ip-172-31-53-200").build()).build());

    elasticLoadBalancerEnableState.setRegion(Regions.US_EAST_1);
    elasticLoadBalancerEnableState.setAccessKey("AKIAIJ5H5UG5TUB3L2QQ");
    elasticLoadBalancerEnableState.setSecretKey("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");
    elasticLoadBalancerEnableState.setLoadBalancerName("testlb");

    elasticLoadBalancerDisableState.setRegion(Regions.US_EAST_1);
    elasticLoadBalancerDisableState.setAccessKey("AKIAIJ5H5UG5TUB3L2QQ");
    elasticLoadBalancerDisableState.setSecretKey("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");
    elasticLoadBalancerDisableState.setLoadBalancerName("testlb");
  }

  @Test
  public void test() {
    elasticLoadBalancerDisableState.execute(executionContext);
    elasticLoadBalancerEnableState.execute(executionContext);
  }
}
