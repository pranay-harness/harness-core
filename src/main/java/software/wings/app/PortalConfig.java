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
  @JsonProperty private String companyName;

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

  /**
   * Gets allowed domains.
   *
   * @return the allowed domains
   */
  public List<String> getAllowedDomains() {
    return allowedDomains;
  }

  /**
   * Sets allowed domains.
   *
   * @param allowedDomains the allowed domains
   */
  public void setAllowedDomains(String allowedDomains) {
    this.allowedDomains = Splitter.on(",").trimResults().omitEmptyStrings().splitToList(allowedDomains);
  }

  /**
   * Gets company name.
   *
   * @return the company name
   */
  public String getCompanyName() {
    return companyName;
  }

  /**
   * Sets company name.
   *
   * @param companyName the company name
   */
  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }
}
