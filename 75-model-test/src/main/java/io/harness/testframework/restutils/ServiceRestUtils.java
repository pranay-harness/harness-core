package io.harness.testframework.restutils;

import io.harness.testframework.framework.Setup;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.path.json.JsonPath;
import org.junit.Assert;
import software.wings.beans.Service;

public class ServiceRestUtils {
  /**
   *
   * @param appId
   * @param service
   * @return created service details
   */
  public static String createService(String bearerToken, String accountId, String appId, Service service) {
    JsonPath response = Setup.portal()
                            .auth()
                            .oauth2(bearerToken)
                            .queryParam("accountId", accountId)
                            .queryParam("appId", appId)
                            .body(service, ObjectMapperType.GSON)
                            .contentType(ContentType.JSON)
                            .post("/services")
                            .jsonPath();

    return response.getString("resource.uuid");
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