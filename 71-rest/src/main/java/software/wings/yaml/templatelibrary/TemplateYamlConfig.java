package software.wings.yaml.templatelibrary;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import software.wings.beans.yaml.YamlConstants;
import software.wings.beans.yaml.YamlType;
import software.wings.yaml.YamlVersion;

public abstract class TemplateYamlConfig {
  private YamlType yamlType;
  private YamlVersion.Type yamlVersionType;
  private String templateLibraryFolderName;

  public static TemplateYamlConfig getInstance(String appId) {
    return isBlank(appId) || GLOBAL_APP_ID.equals(appId) ? GlobalTemplateYamlConfig.instance
                                                         : ApplicationTemplateYamlConfig.instance;
  }

  private TemplateYamlConfig(YamlType yamlType, YamlVersion.Type yamlVersionType, String templateLibraryFolderName) {
    this.yamlType = yamlType;
    this.yamlVersionType = yamlVersionType;
    this.templateLibraryFolderName = templateLibraryFolderName;
  }

  public YamlType getYamlType() {
    return yamlType;
  }

  public YamlVersion.Type getYamlVersionType() {
    return yamlVersionType;
  }

  public String getTemplateLibraryFolderName() {
    return templateLibraryFolderName;
  }

  static class GlobalTemplateYamlConfig extends TemplateYamlConfig {
    private static final TemplateYamlConfig instance = new GlobalTemplateYamlConfig();
    GlobalTemplateYamlConfig() {
      super(YamlType.GLOBAL_TEMPLATE_LIBRARY, YamlVersion.Type.GLOBAL_TEMPLATE_LIBRARY,
          YamlConstants.GLOBAL_TEMPLATE_LIBRARY_FOLDER);
    }
  }

  private static class ApplicationTemplateYamlConfig extends TemplateYamlConfig {
    private static final TemplateYamlConfig instance = new ApplicationTemplateYamlConfig();

    ApplicationTemplateYamlConfig() {
      super(YamlType.APPLICATION_TEMPLATE_LIBRARY, YamlVersion.Type.APPLICATION_TEMPLATE_LIBRARY,
          YamlConstants.APPLICATION_TEMPLATE_LIBRARY_FOLDER);
    }
  }
}
