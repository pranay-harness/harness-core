package io.harness.restutils;

import static org.junit.Assert.assertNotNull;

import io.harness.beans.PageResponse;
import io.harness.framework.Setup;
import io.harness.rest.RestResponse;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import software.wings.beans.ServiceVariable;

import java.util.List;
import javax.ws.rs.core.GenericType;

public class ServiceVariablesUtils {
  public static ServiceVariable addServiceVariable(String bearerToken, ServiceVariable serviceVariable) {
    RestResponse<ServiceVariable> addedServiceVariable =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("appId", serviceVariable.getAppId())
            .body(serviceVariable, ObjectMapperType.GSON)
            .contentType(ContentType.JSON)
            .post("/service-variables")
            .as(new GenericType<RestResponse<ServiceVariable>>() {}.getType());

    assertNotNull(addedServiceVariable.getResource());

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

    assertNotNull(serviceReslonseList.getResource());

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
