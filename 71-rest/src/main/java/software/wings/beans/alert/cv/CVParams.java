package software.wings.beans.alert.cv;

import lombok.Value;

@Value
public class CVParams {
  private String appId;
  private String envId;
  private String cvConfigurationId;
}
