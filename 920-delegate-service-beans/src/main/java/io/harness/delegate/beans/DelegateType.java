/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans;

public final class DelegateType {
  public static final String SHELL_SCRIPT = "SHELL_SCRIPT";
  public static final String DOCKER = "DOCKER";
  public static final String KUBERNETES = "KUBERNETES";
  public static final String CE_KUBERNETES = "CE_KUBERNETES";
  public static final String HELM_DELEGATE = "HELM_DELEGATE";
  public static final String ECS = "ECS";

  private DelegateType() {}
}
