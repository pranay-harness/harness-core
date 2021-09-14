/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.appmanifest;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public enum StoreType {
  Local,
  Remote,
  HelmSourceRepo,
  HelmChartRepo,
  KustomizeSourceRepo,
  OC_TEMPLATES,
  CUSTOM,
  CUSTOM_OPENSHIFT_TEMPLATE,
  VALUES_YAML_FROM_HELM_REPO
}
