/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.restutils;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;

import software.wings.beans.ServiceVariable;

import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import java.util.List;
import javax.ws.rs.core.GenericType;

public class ServiceVariablesUtils {
  public static ServiceVariable addServiceVariable(String bearerToken, ServiceVariable serviceVariable) {
    RestResponse<ServiceVariable> addedServiceVariable =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("appId", serviceVariable.getAppId())
            .body(serviceVariable, ObjectMapperType.JACKSON_2)
            .contentType(ContentType.JSON)
            .post("/service-variables")
            .as(new GenericType<RestResponse<ServiceVariable>>() {}.getType());

    assertThat(addedServiceVariable.getResource()).isNotNull();

    return addedServiceVariable.getResource();
  }

  public static List<ServiceVariable> getServiceVariables(String bearerToken, String appId) {
    RestResponse<PageResponse<ServiceVariable>> serviceReslonseList =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("appId", appId)
            .contentType(ContentType.JSON)
            .get("/service-variables")
            .as(new GenericType<RestResponse<PageResponse<ServiceVariable>>>() {}.getType());

    assertThat(serviceReslonseList.getResource()).isNotNull();

    return serviceReslonseList.getResource().getResponse();
  }

  public static ServiceVariable getServiceVariable(String bearerToken, ServiceVariable serviceVariable) {
    List<ServiceVariable> serviceVariableList = getServiceVariables(bearerToken, serviceVariable.getAppId());
    if (serviceVariableList == null) {
      return null;
    }
    if (serviceVariableList.size() == 0) {
      return null;
    }
    for (ServiceVariable serviceVariable1 : serviceVariableList) {
      if (serviceVariable.getName().equals(serviceVariable1.getName())
          && serviceVariable.getEntityId().equals(serviceVariable1.getEntityId())) {
        return serviceVariable1;
      }
    }
    return null;
  }

  public static boolean doesServiceVariableExists(
      List<ServiceVariable> serviceVariableList, ServiceVariable serviceVariable) {
    if (serviceVariableList == null) {
      return false;
    }
    if (serviceVariableList.size() == 0) {
      return false;
    }
    boolean[] exists = new boolean[1];
    exists[0] = false;
    serviceVariableList.forEach(item -> {
      if (item.getName().equals(serviceVariable.getName())) {
        exists[0] = true;
      }
    });
    return exists[0];
  }

  public static ServiceVariable addOrGetServiceVariable(String bearerToken, ServiceVariable serviceVariable) {
    if (getServiceVariable(bearerToken, serviceVariable) == null) {
      return addServiceVariable(bearerToken, serviceVariable);
    } else {
      return getServiceVariable(bearerToken, serviceVariable);
    }
  }
}
