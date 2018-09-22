package software.wings.integration;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import software.wings.beans.RestResponse;
import software.wings.service.impl.analysis.LogMLExpAnalysisInfo;
import software.wings.service.intfc.analysis.ExperimentalLogAnalysisResource;

import java.util.List;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Integration tests for ExperimentalLogAnalysisResourceTest
 * Created by Pranjal on 09/14/2018
 */
public class ExperimentalLogAnalysisResourceIntegrationTest extends BaseIntegrationTest {
  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
  }

  @Test
  public void testGetLogExpAnalysisInfo() {
    WebTarget getTarget = client.target(API_BASE + "/"
        + "learning-exp" + ExperimentalLogAnalysisResource.ANALYSIS_STATE_GET_EXP_ANALYSIS_INFO_URL
        + "?accountId=" + accountId);

    RestResponse<List<LogMLExpAnalysisInfo>> restResponse =
        getRequestBuilderWithAuthHeader(getTarget).get(new GenericType<RestResponse<List<LogMLExpAnalysisInfo>>>() {});

    assertTrue(restResponse.getResource().size() <= 1000);
  }
}
