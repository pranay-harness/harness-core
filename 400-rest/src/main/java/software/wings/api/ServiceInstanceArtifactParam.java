/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.api;

import io.harness.context.ContextElementType;

import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

public class ServiceInstanceArtifactParam implements ContextElement {
  public static final String SERVICE_INSTANCE_ARTIFACT_PARAMS = "SERVICE_INSTANCE_ARTIFACT_PARAMS";

  private Map<String, String> instanceArtifactMap = new HashMap<>();

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.PARAM;
  }

  @Override
  public String getUuid() {
    return null;
  }

  @Override
  public String getName() {
    return SERVICE_INSTANCE_ARTIFACT_PARAMS;
  }

  public Map<String, String> getInstanceArtifactMap() {
    return instanceArtifactMap;
  }

  public void setInstanceArtifactMap(Map<String, String> instanceArtifactMap) {
    this.instanceArtifactMap = instanceArtifactMap;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    return null;
  }

  @Override
  public ContextElement cloneMin() {
    return this;
  }
}
