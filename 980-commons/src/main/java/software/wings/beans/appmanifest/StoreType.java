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
