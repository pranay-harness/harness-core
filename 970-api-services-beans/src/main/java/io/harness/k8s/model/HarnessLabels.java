/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.k8s.model;

public interface HarnessLabels {
  String releaseName = "harness.io/release-name";
  String track = "harness.io/track";
  String color = "harness.io/color";
}
