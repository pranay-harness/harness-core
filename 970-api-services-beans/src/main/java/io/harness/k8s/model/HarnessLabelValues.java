/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.k8s.model;

public interface HarnessLabelValues {
  String trackCanary = "canary";
  String trackStable = "stable";
  String colorGreen = "green";
  String colorBlue = "blue";
  String colorDefault = "green";
}
