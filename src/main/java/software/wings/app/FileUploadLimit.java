package software.wings.app;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by anubhaw on 8/9/16.
 */
public class FileUploadLimit {
  @JsonProperty private long appContainerLimit = 1000000000L;
  @JsonProperty private long configFileLimit = 100000000L;
  @JsonProperty private long hostUploadLimit = 100000000L;

  /**
   * Gets app container.
   *
   * @return the app container
   */
  public long getAppContainerLimit() {
    return appContainerLimit;
  }

  /**
   * Sets app container.
   *
   * @param appContainerLimit the app container
   */
  public void setAppContainerLimit(long appContainerLimit) {
    this.appContainerLimit = appContainerLimit;
  }

  /**
   * Gets config file.
   *
   * @return the config file
   */
  public long getConfigFileLimit() {
    return configFileLimit;
  }

  /**
   * Sets config file.
   *
   * @param configFileLimit the config file
   */
  public void setConfigFileLimit(long configFileLimit) {
    this.configFileLimit = configFileLimit;
  }

  /**
   * Gets host upload limit.
   *
   * @return the host upload limit
   */
  public long getHostUploadLimit() {
    return hostUploadLimit;
  }

  /**
   * Sets host upload limit.
   *
   * @param hostUploadLimit the host upload limit
   */
  public void setHostUploadLimit(long hostUploadLimit) {
    this.hostUploadLimit = hostUploadLimit;
  }
}
