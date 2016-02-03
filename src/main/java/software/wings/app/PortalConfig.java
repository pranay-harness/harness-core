package software.wings.app;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PortalConfig {
  @JsonProperty private String url;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }
}
