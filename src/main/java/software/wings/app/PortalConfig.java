package software.wings.app;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.util.StdConverter;

import java.util.List;

/**
 * The Class PortalConfig.
 */
public class PortalConfig {
  @JsonProperty(defaultValue = "https://localhost:8000") private String url = "https://localhost:8000";
  private List<String> allowedDomains = Lists.newArrayList();
  @JsonProperty(defaultValue = "") private String companyName = "";
  @JsonProperty(defaultValue = "/api/users/verify") private String verificationUrl = "/api/users/verify/";
  private Long authTokenExpiryInMillis = 24 * 60 * 60 * 1000L;

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
  @JsonProperty(defaultValue = "")
  public String getAllowedDomains() {
    return Joiner.on(",").join(allowedDomains);
  }

  public List<String> getAllowedDomainsList() {
    return isEmpty(allowedDomains) ? Lists.newArrayList() : allowedDomains;
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

  /**
   * Getter for property 'verificationUrl'.
   *
   * @return Value for property 'verificationUrl'.
   */
  public String getVerificationUrl() {
    return verificationUrl;
  }

  /**
   * Setter for property 'verificationUrl'.
   *
   * @param verificationUrl Value to set for property 'verificationUrl'.
   */
  public void setVerificationUrl(String verificationUrl) {
    this.verificationUrl = verificationUrl;
  }

  public Long getAuthTokenExpiryInMillis() {
    return authTokenExpiryInMillis;
  }

  public void setAuthTokenExpiryInMillis(Long authTokenExpiryInMillis) {
    this.authTokenExpiryInMillis = authTokenExpiryInMillis;
  }

  public static class ArrayConverter extends StdConverter<List<String>, String> {
    @Override
    public String convert(List<String> value) {
      return Joiner.on(",").join(value);
    }
  }
}
