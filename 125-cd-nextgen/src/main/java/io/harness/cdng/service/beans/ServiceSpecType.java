/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.service.beans;

public interface ServiceSpecType {
  String KUBERNETES = "Kubernetes";
  String SSH = "Ssh";
  String ECS = "Ecs";
  String NATIVE_HELM = "NativeHelm";
  String PCF = "Pcf";
}
