package io.harness.restutils;

import io.harness.framework.Setup;
import io.harness.rest.RestResponse;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.path.json.JsonPath;
import org.junit.Assert;
import software.wings.beans.Account;
import software.wings.beans.Service;

import javax.ws.rs.core.GenericType;

public class ServiceRestUtils {
  /**
   *
   * @param appId
   * @param service
   * @return created service details
   */
  public static Service createService(String bearerToken, Account account, String appId, Service service) {
    GenericType<RestResponse<Service>> serviceType = new GenericType<RestResponse<Service>>() {};

    RestResponse<Service> savedServiceResponse = Setup.portal()
                                                     .auth()
                                                     .oauth2(bearerToken)
                                                     .queryParam("accountId", account.getUuid())
                                                     .queryParam("appId", appId)
                                                     .body(service, ObjectMapperType.GSON)
                                                     .contentType(ContentType.JSON)
                                                     .post("/services")
                                                     .as(serviceType.getType());

    return savedServiceResponse.getResource();
  }

  public static String createSSHService(String bearerToken, String appId, Service service) {
    String serviceId = "";

    JsonPath response = Setup.portal()
                            .auth()
                            .oauth2(bearerToken)
                            .queryParam("appId", appId)
                            .body(service, ObjectMapperType.GSON)
                            .contentType(ContentType.JSON)
                            .post("/services")
                            .jsonPath();

    // System.out.println(resp.prettyPrint());
    serviceId = response.getString("resource.uuid");

    if (serviceId.isEmpty()) {
      Assert.fail("Error: 'ServiceId' is NULL or Empty");
    }

    return serviceId;
  }
}