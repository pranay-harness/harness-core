package io.harness.integration;

import static io.harness.rule.OwnerRule.PRANJAL;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.VerificationBaseIntegrationTest;
import io.harness.category.element.IntegrationTests;
import io.harness.resources.intfc.ExperimentalLogAnalysisResource;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.service.impl.analysis.ExpAnalysisInfo;

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
  @Owner(developers = PRANJAL)
  @Category(IntegrationTests.class)
  public void testGetLogExpAnalysisInfo() throws UnknownHostException {
    WebTarget getTarget = client.target(VERIFICATION_API_BASE + "/"
        + "learning-exp" + ExperimentalLogAnalysisResource.ANALYSIS_STATE_GET_EXP_ANALYSIS_INFO_URL
        + "?accountId=" + accountId);

    RestResponse<List<ExpAnalysisInfo>> restResponse = getRequestBuilderWithLearningAuthHeader(getTarget).get(
        new GenericType<RestResponse<List<ExpAnalysisInfo>>>() {});

    assertThat(restResponse.getResource().size() <= 1000).isTrue();
  }
}