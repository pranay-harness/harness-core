package io.harness.RestUtils;

import com.google.inject.Singleton;

import io.harness.framework.Setup;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rest.RestResponse;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import software.wings.beans.Application;

import javax.ws.rs.core.GenericType;

@Singleton
public class ApplicationRestUtils extends AbstractFunctionalTest {
  /**
   *
   * @param application
   * @return created Application details
   */
  public Application createApplication(Application application) {
    GenericType<RestResponse<Application>> applicationType = new GenericType<RestResponse<Application>>() {};

    RestResponse<Application> savedApplicationResponse = Setup.portal()
                                                             .auth()
                                                             .oauth2(bearerToken)
                                                             .queryParam("accountId", getAccount().getUuid())
                                                             .body(application, ObjectMapperType.GSON)
                                                             .contentType(ContentType.JSON)
                                                             .post("/apps")
                                                             .as(applicationType.getType());

    return savedApplicationResponse.getResource();
  }
}