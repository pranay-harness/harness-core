/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

/**
 *
 */

package software.wings.api;

import io.harness.beans.SweepingOutput;
import io.harness.context.ContextElementType;

import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;

/**
 * The type Service instance ids param.
 *
 * @author Rishi
 */
@JsonTypeName("serviceInstanceIdsParam")
public class ServiceInstanceIdsParam implements ContextElement, SweepingOutput {
  public static final String SERVICE_INSTANCE_IDS_PARAMS = "SERVICE_INSTANCE_IDS_PARAMS";

  private String serviceId;
  private List<String> instanceIds;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.PARAM;
  }

  @Override
  public String getName() {
    return SERVICE_INSTANCE_IDS_PARAMS;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    return null;
  }

  /**
   * Gets service id.
   *
   * @return the service id
   */
  public String getServiceId() {
    return serviceId;
  }

  /**
   * Sets service id.
   *
   * @param serviceId the service id
   */
  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  /**
   * Gets instance ids.
   *
   * @return the instance ids
   */
  public List<String> getInstanceIds() {
    return instanceIds;
  }

  /**
   * Sets instance ids.
   *
   * @param instanceIds the instance ids
   */
  public void setInstanceIds(List<String> instanceIds) {
    this.instanceIds = instanceIds;
  }

  @Override
  public String getUuid() {
    return null;
  }

  /**
   * Sets uuid.
   *
   * @param uuid the uuid
   */
  public void setUuid(String uuid) {}

  @Override
  public ContextElement cloneMin() {
    return this;
  }

  @Override
  public String getType() {
    return "serviceInstanceIdsParam";
  }

  public static final class ServiceInstanceIdsParamBuilder {
    private String serviceId;
    private List<String> instanceIds;

    private ServiceInstanceIdsParamBuilder() {}

    public static ServiceInstanceIdsParamBuilder aServiceInstanceIdsParam() {
      return new ServiceInstanceIdsParamBuilder();
    }

    public ServiceInstanceIdsParamBuilder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public ServiceInstanceIdsParamBuilder withInstanceIds(List<String> instanceIds) {
      this.instanceIds = instanceIds;
      return this;
    }

    public ServiceInstanceIdsParam build() {
      ServiceInstanceIdsParam serviceInstanceIdsParam = new ServiceInstanceIdsParam();
      serviceInstanceIdsParam.setServiceId(serviceId);
      serviceInstanceIdsParam.setInstanceIds(instanceIds);
      return serviceInstanceIdsParam;
    }
  }
}
