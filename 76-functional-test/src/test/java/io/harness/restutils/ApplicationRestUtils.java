package io.harness.restutils;

import io.harness.framework.Setup;
import io.harness.rest.RestResponse;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import software.wings.beans.Account;
import software.wings.beans.Application;

import javax.ws.rs.core.GenericType;

public class ApplicationRestUtils {
  /**
   *
   * @param application
   * @return created Application details
   */
  public static Application createApplication(String bearerToken, Account account, Application application) {
    GenericType<RestResponse<Application>> applicationType = new GenericType<RestResponse<Application>>() {};

    RestResponse<Application> savedApplicationResponse = Setup.portal()
                                                             .auth()
                                                             .oauth2(bearerToken)
                                                             .queryParam("accountId", account.getUuid())
                                                             .body(application, ObjectMapperType.GSON)
                                                             .contentType(ContentType.JSON)
                                                             .post("/apps")
                                                             .as(applicationType.getType());

    return savedApplicationResponse.getResource();
  }
}