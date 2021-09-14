/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.k8s.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K8sDelegateTaskParams {
  String kubectlPath;
  String kubeconfigPath;
  String workingDirectory;
  String goTemplateClientPath;
  String helmPath;
  String ocPath;
  String kustomizeBinaryPath;
}
