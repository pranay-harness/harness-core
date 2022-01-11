/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.restutils;

import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;

import software.wings.beans.Service;

import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.path.json.JsonPath;
import javax.ws.rs.core.GenericType;
import org.junit.Assert;

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

  public static Service updateService(String bearerToken, String accountId, String appId, Service service) {
    GenericType<RestResponse<Service>> serviceType = new GenericType<RestResponse<Service>>() {};

    RestResponse<Service> serviceRestResponse = Setup.portal()
                                                    .auth()
                                                    .oauth2(bearerToken)
                                                    .queryParam("accountId", accountId)
                                                    .queryParam("appId", appId)
                                                    .body(service, ObjectMapperType.GSON)
                                                    .contentType(ContentType.JSON)
                                                    .put("/services/" + service.getUuid())
                                                    .as(serviceType.getType());
    return serviceRestResponse.getResource();
  }

  public static int deleteService(String bearerToken, String appId, String serviceId) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("appId", appId)
        .contentType(ContentType.JSON)
        .delete("/services/" + serviceId)
        .statusCode();
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
