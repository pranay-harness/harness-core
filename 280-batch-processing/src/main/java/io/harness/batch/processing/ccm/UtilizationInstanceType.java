/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.ccm;

public class UtilizationInstanceType {
  public static final String ECS_CLUSTER = "ECS_CLUSTER";
  public static final String ECS_SERVICE = "ECS_SERVICE";
  public static final String K8S_POD = "K8S_POD";
  public static final String K8S_NODE = "K8S_NODE";
  public static final String K8S_PV = "K8S_PV";

  private UtilizationInstanceType() {}
}
