package io.harness.RestUtils;

import com.google.inject.Singleton;

import io.harness.framework.Setup;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rest.RestResponse;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import software.wings.beans.Service;

import javax.ws.rs.core.GenericType;

@Singleton
public class ServiceRestUtil extends AbstractFunctionalTest {
  /**
   *
   * @param appId
   * @param service
   * @return created service details
   */
  public Service createService(String appId, Service service) {
    GenericType<RestResponse<Service>> serviceType = new GenericType<RestResponse<Service>>() {};

    RestResponse<Service> savedServiceResponse = Setup.portal()
                                                     .auth()
                                                     .oauth2(bearerToken)
                                                     .queryParam("accountId", getAccount().getUuid())
                                                     .queryParam("appId", appId)
                                                     .body(service, ObjectMapperType.GSON)
                                                     .contentType(ContentType.JSON)
                                                     .post("/services")
                                                     .as(serviceType.getType());

    return savedServiceResponse.getResource();
  }
}
