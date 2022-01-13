/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.restutils;

import io.harness.exception.WingsException;
import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;

import software.wings.beans.stats.DeploymentStatistics;
import software.wings.beans.stats.ServiceInstanceStatistics;

import com.google.inject.Singleton;
import io.restassured.http.ContentType;
import javax.ws.rs.core.GenericType;

@Singleton
public class MainDashboardRestUtils {
  public static DeploymentStatistics checkDeployments(String bearerToken, String accountId) {
    GenericType<RestResponse<DeploymentStatistics>> dashboardType =
        new GenericType<RestResponse<DeploymentStatistics>>() {};

    RestResponse<DeploymentStatistics> savedDashboardResponse = Setup.portal()
                                                                    .auth()
                                                                    .oauth2(bearerToken)
                                                                    .queryParam("accountId", accountId)
                                                                    .contentType(ContentType.JSON)
                                                                    .get("/statistics/deployment-stats")
                                                                    .as(dashboardType.getType());

    if (savedDashboardResponse.getResource() == null) {
      throw new WingsException(String.valueOf(savedDashboardResponse.getResponseMessages()));
    }

    return savedDashboardResponse.getResource();
  }

  public static ServiceInstanceStatistics checkMostActiveServices(String bearerToken, String accountId) {
    GenericType<RestResponse<ServiceInstanceStatistics>> dashboardType =
        new GenericType<RestResponse<ServiceInstanceStatistics>>() {};

    RestResponse<ServiceInstanceStatistics> savedDashboardResponse = Setup.portal()
                                                                         .auth()
                                                                         .oauth2(bearerToken)
                                                                         .queryParam("accountId", accountId)
                                                                         .contentType(ContentType.JSON)
                                                                         .get("/statistics/service-instance-stats")
                                                                         .as(dashboardType.getType());

    if (savedDashboardResponse.getResource() == null) {
      throw new WingsException(String.valueOf(savedDashboardResponse.getResponseMessages()));
    }

    return savedDashboardResponse.getResource();
  }
}
