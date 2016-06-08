package software.wings.app;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Class PortalConfig.
 */
public class PortalConfig {
  @JsonProperty private String url;

  /**
   * Gets url.
   *
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets url.
   *
   * @param url the url
   */
  public void setUrl(String url) {
    this.url = url;
  }
}
