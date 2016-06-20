package software.wings.app;

import com.google.common.base.Splitter;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * The Class PortalConfig.
 */
public class PortalConfig {
  @JsonProperty private String url;
  @JsonProperty private List<String> allowedDomains;

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

  public List<String> getAllowedDomains() {
    return allowedDomains;
  }

  public void setAllowedDomains(String allowedDomains) {
    this.allowedDomains = Splitter.on(",").trimResults().omitEmptyStrings().splitToList(allowedDomains);
  }
}
