package io.harness.restutils;

import static org.junit.Assert.assertNotNull;

import io.harness.beans.PageResponse;
import io.harness.framework.Setup;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rest.RestResponse;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import software.wings.beans.ServiceVariable;

import java.util.List;
import javax.ws.rs.core.GenericType;

public class ServiceVariablesUtils extends AbstractFunctionalTest {
  public ServiceVariable addServiceVariable(ServiceVariable serviceVariable) {
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

  public List<ServiceVariable> getServiceVariables(String appId) {
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

  public ServiceVariable getServiceVariable(ServiceVariable serviceVariable) {
    List<ServiceVariable> serviceVariableList = getServiceVariables(serviceVariable.getAppId());
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

  public boolean doesServiceVariableExists(List<ServiceVariable> serviceVariableList, ServiceVariable serviceVariable) {
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

  public ServiceVariable addOrGetServiceVariable(ServiceVariable serviceVariable) {
    if (getServiceVariable(serviceVariable) == null) {
      return addServiceVariable(serviceVariable);
    } else {
      return getServiceVariable(serviceVariable);
    }
  }
}
