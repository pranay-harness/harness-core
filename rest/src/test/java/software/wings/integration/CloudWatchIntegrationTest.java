package software.wings.integration;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static software.wings.integration.DataGenUtil.AWS_PLAY_GROUND;

import com.google.inject.Inject;

import com.amazonaws.regions.Regions;
import org.junit.Before;
import org.junit.Test;
import software.wings.beans.Application;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.service.impl.cloudwatch.AwsNameSpace;
import software.wings.service.impl.cloudwatch.CloudWatchMetric;
import software.wings.service.intfc.SettingsService;

import java.util.List;
import java.util.Set;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by rsingh on 5/3/18.
 */
public class CloudWatchIntegrationTest extends BaseIntegrationTest {
  @Inject private SettingsService settingsService;

  private String awsConfigId;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
    SettingAttribute settingAttribute =
        settingsService.getByName(accountId, Application.GLOBAL_APP_ID, AWS_PLAY_GROUND);
    assertNotNull(settingAttribute);
    awsConfigId = settingAttribute.getUuid();
    assertTrue(isNotEmpty(awsConfigId));
  }

  @Test
  public void getEc2Metrics() throws Exception {
    WebTarget target = client.target(
        API_BASE + "/cloudwatch/get-metric-names?accountId=" + accountId + "&awsNameSpace=" + AwsNameSpace.EC2);
    RestResponse<List<CloudWatchMetric>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<CloudWatchMetric>>>() {});
    assertTrue(restResponse.getResource().size() > 0);
  }

  @Test
  public void getLoadBalancers() throws Exception {
    WebTarget target = client.target(API_BASE + "/cloudwatch/get-load-balancers?accountId=" + accountId
        + "&settingId=" + awsConfigId + "&region=" + Regions.US_EAST_1.getName());
    RestResponse<Set<String>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<Set<String>>>() {});
    assertTrue(restResponse.getResource().size() > 0);
  }
}
