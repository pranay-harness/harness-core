package io.harness.integration;

import static org.junit.Assert.assertTrue;

import io.harness.VerificationBaseIntegrationTest;
import io.harness.resources.intfc.ExperimentalLogAnalysisResource;
import org.junit.Before;
import org.junit.Test;
import software.wings.beans.RestResponse;
import software.wings.service.impl.analysis.LogMLExpAnalysisInfo;

import java.net.UnknownHostException;
import java.util.List;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by Pranjal on 09/26/2018
 */
public class ExperimentalLogAnalysisResourceIntegrationTest extends VerificationBaseIntegrationTest {
  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
  }

  @Test
  public void testGetLogExpAnalysisInfo() throws UnknownHostException {
    WebTarget getTarget = client.target(VERIFICATION_API_BASE + "/"
        + "learning-exp" + ExperimentalLogAnalysisResource.ANALYSIS_STATE_GET_EXP_ANALYSIS_INFO_URL
        + "?accountId=" + accountId);

    RestResponse<List<LogMLExpAnalysisInfo>> restResponse = getRequestBuilderWithLearningAuthHeader(getTarget).get(
        new GenericType<RestResponse<List<LogMLExpAnalysisInfo>>>() {});

    assertTrue(restResponse.getResource().size() <= 1000);
  }
}