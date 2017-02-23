package software.wings.beans;

/**
 * Created by anubhaw on 2/22/17.
 */
public class HostValidationResponse {
  private String hostName;
  private String status;
  private String error;

  /**
   * Gets host name.
   *
   * @return the host name
   */
  public String getHostName() {
    return hostName;
  }

  /**
   * Sets host name.
   *
   * @param hostName the host name
   */
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  /**
   * Gets status.
   *
   * @return the status
   */
  public String getStatus() {
    return status;
  }

  /**
   * Sets status.
   *
   * @param status the status
   */
  public void setStatus(String status) {
    this.status = status;
  }

  /**
   * Gets error.
   *
   * @return the error
   */
  public String getError() {
    return error;
  }

  /**
   * Sets error.
   *
   * @param error the error
   */
  public void setError(String error) {
    this.error = error;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String hostName;
    private String status;
    private String error;

    private Builder() {}

    /**
     * A host name validation response builder.
     *
     * @return the builder
     */
    public static Builder aHostNameValidationResponse() {
      return new Builder();
    }

    /**
     * With host name builder.
     *
     * @param hostName the host name
     * @return the builder
     */
    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    /**
     * With status builder.
     *
     * @param status the status
     * @return the builder
     */
    public Builder withStatus(String status) {
      this.status = status;
      return this;
    }

    /**
     * With error builder.
     *
     * @param error the error
     * @return the builder
     */
    public Builder withError(String error) {
      this.error = error;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aHostNameValidationResponse().withHostName(hostName).withStatus(status).withError(error);
    }

    /**
     * Build host name validation response.
     *
     * @return the host name validation response
     */
    public HostValidationResponse build() {
      HostValidationResponse hostValidationResponse = new HostValidationResponse();
      hostValidationResponse.setHostName(hostName);
      hostValidationResponse.setStatus(status);
      hostValidationResponse.setError(error);
      return hostValidationResponse;
    }
  }
}
