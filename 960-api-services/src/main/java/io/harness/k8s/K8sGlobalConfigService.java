/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.k8s;

import io.harness.k8s.model.HelmVersion;

public interface K8sGlobalConfigService {
  String getKubectlPath();
  String getGoTemplateClientPath();
  String getHelmPath(HelmVersion helmVersion);
  String getChartMuseumPath();
  String getOcPath();
  String getKustomizePath();
  String getScmPath();
}
