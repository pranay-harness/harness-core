package software.wings.integration;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import software.wings.beans.RestResponse;
import software.wings.service.intfc.analysis.LogAnalysisResource;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

public class ElkResourceIntegrationTest extends BaseIntegrationTest {
  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
  }

  @Test
  public void validateQuery() {
    WebTarget getTarget = client.target(API_BASE + "/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL
        + LogAnalysisResource.ELK_VALIDATE_QUERY + "?accountId=" + accountId + "&query=(.*exception.*)");

    RestResponse<Boolean> restResponse =
        getRequestBuilderWithAuthHeader(getTarget).get(new GenericType<RestResponse<Boolean>>() {});
    assertTrue(restResponse.getResource());
  }

  @Test
  public void validateQueryFail() {
    WebTarget getTarget = client.target(API_BASE + "/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL
        + LogAnalysisResource.ELK_VALIDATE_QUERY + "?accountId=" + accountId + "&query=(.*exception.*))");

    try {
      getRequestBuilderWithAuthHeader(getTarget).get(new GenericType<RestResponse<Boolean>>() {});
      fail();
    } catch (BadRequestException e) {
    }
  }
}
