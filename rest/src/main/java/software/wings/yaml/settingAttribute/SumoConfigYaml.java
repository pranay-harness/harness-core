package software.wings.yaml.settingAttribute;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SumoConfig;

@Data
@Builder
public class SumoConfigYaml extends SettingAttributeYaml {
  private String sumoUrl;
  private String accessId;
  private String accessKey;

  public SumoConfigYaml() {
    super();
  }

  public SumoConfigYaml(SettingAttribute settingAttribute) {
    super(settingAttribute);

    SumoConfig sumoConfig = (SumoConfig) settingAttribute.getValue();
    this.setSumoUrl(sumoConfig.getSumoUrl());
  }

  public SumoConfigYaml(String sumoUrl, String accessId, String accessKey) {
    this.setSumoUrl(sumoUrl);
    this.setAccessId(accessId);
    this.setAccessKey(accessKey);
  }
}