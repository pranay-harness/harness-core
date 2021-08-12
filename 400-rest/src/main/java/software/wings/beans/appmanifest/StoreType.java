package software.wings.beans.appmanifest;

import static io.harness.annotations.dev.HarnessModule._980_COMMONS;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(CDP)
@TargetModule(_980_COMMONS)
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
