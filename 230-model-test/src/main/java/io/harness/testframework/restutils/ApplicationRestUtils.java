/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.restutils;

import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;

import software.wings.beans.Account;
import software.wings.beans.Application;

import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
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

  public static Application updateApplication(
      String bearerToken, Application application, String applicationId, String accountId) {
    GenericType<RestResponse<Application>> applicationType = new GenericType<RestResponse<Application>>() {};

    RestResponse<Application> savedApplicationResponse = Setup.portal()
                                                             .auth()
                                                             .oauth2(bearerToken)
                                                             .queryParam("routingId", accountId)
                                                             .body(application, ObjectMapperType.GSON)
                                                             .contentType(ContentType.JSON)
                                                             .put("/apps/" + applicationId)
                                                             .as(applicationType.getType());

    return savedApplicationResponse.getResource();
  }

  public static int deleteApplication(String bearerToken, String applicationId, String accountId) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("routingId", accountId)
        .contentType(ContentType.JSON)
        .delete("/apps/" + applicationId)
        .statusCode();
  }
}
