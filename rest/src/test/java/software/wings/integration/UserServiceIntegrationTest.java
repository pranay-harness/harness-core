package software.wings.integration;

import org.junit.Assert;
import org.junit.Test;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage;
import software.wings.beans.RestResponse;

import java.io.IOException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by rsingh on 4/24/17.
 */
public class UserServiceIntegrationTest extends BaseIntegrationTest {
  @Test
  public void testDomainNotAllowed() throws IOException {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=xyz@gmail.com");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    Assert.assertEquals(1, restResponse.getResponseMessages().size());
    final ResponseMessage responseMessage = restResponse.getResponseMessages().get(0);
    Assert.assertEquals(ErrorCode.USER_DOMAIN_NOT_ALLOWED, responseMessage.getCode());
    Assert.assertFalse(restResponse.getResource());
  }

  @Test
  public void testUserExists() throws IOException {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=admin@wings.software");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    Assert.assertEquals(1, restResponse.getResponseMessages().size());
    final ResponseMessage responseMessage = restResponse.getResponseMessages().get(0);
    Assert.assertEquals(ErrorCode.USER_ALREADY_REGISTERED, responseMessage.getCode());
    Assert.assertFalse(restResponse.getResource());
  }

  @Test
  public void testValidEmail() throws IOException {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=raghu@wings.software");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    Assert.assertEquals(0, restResponse.getResponseMessages().size());
    Assert.assertTrue(restResponse.getResource());
  }
}
