package software.wings.beans.appmanifest;

import static io.harness.pcf.model.PcfConstants.VARS_YML;
import static software.wings.beans.appmanifest.ManifestFile.VALUES_YAML_KEY;
import static software.wings.beans.yaml.YamlConstants.OC_PARAMS_FILE;

import software.wings.beans.yaml.YamlConstants;

public enum AppManifestKind {
  VALUES(VALUES_YAML_KEY, YamlConstants.VALUES_FOLDER),
  K8S_MANIFEST(VALUES_YAML_KEY),
  PCF_OVERRIDE(VARS_YML, YamlConstants.PCF_OVERRIDES_FOLDER),
  HELM_CHART_OVERRIDE(VALUES_YAML_KEY),
  OC_PARAMS(OC_PARAMS_FILE, YamlConstants.OC_PARAMS_FOLDER);

  private String defaultFileName;
  private String yamlFolderName;

  AppManifestKind(String defaultFileName, String yamlFolderName) {
    this.defaultFileName = defaultFileName;
    this.yamlFolderName = yamlFolderName;
  }

  AppManifestKind(String defaultFileName) {
    this.defaultFileName = defaultFileName;
  }

  public String getDefaultFileName() {
    return defaultFileName;
  }

  public String getYamlFolderName() {
    return yamlFolderName;
  }
}
