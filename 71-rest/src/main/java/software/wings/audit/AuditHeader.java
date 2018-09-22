package software.wings.audit;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.Environment;
import software.wings.beans.HttpMethod;
import software.wings.beans.Service;
import software.wings.beans.User;

/**
 * HttpAuditHeader bean class.
 *
 * @author Rishi
 */
@Entity(value = "audits", noClassnameStored = true)
@SuppressFBWarnings({"EQ_DOESNT_OVERRIDE_EQUALS"})
public class AuditHeader extends Base {
  /**
   * The Remote user.
   */
  protected User remoteUser;
  /**
   * The Application.
   */
  protected Application application;
  /**
   * The Component.
   */
  protected Service component;
  /**
   * The Environment.
   */
  protected Environment environment;
  private String url;
  private String resourcePath;
  private String queryParams;
  private HttpMethod requestMethod;
  private String headerString;
  private ResponseType responseType;
  private Integer responseStatusCode;
  private String errorCode;
  private String remoteHostName;
  private Integer remoteHostPort;
  private String remoteIpAddress;
  private String localHostName;
  private String localIpAddress;
  private String requestPayloadUuid;
  private String responsePayloadUuid;
  private Long requestTime;
  private Long responseTime;

  /**
   * Gets query params.
   *
   * @return the query params
   */
  public String getQueryParams() {
    return queryParams;
  }

  /**
   * Sets query params.
   *
   * @param queryParams the query params
   */
  public void setQueryParams(String queryParams) {
    this.queryParams = queryParams;
  }

  /**
   * Gets request method.
   *
   * @return the request method
   */
  public HttpMethod getRequestMethod() {
    return requestMethod;
  }

  /**
   * Sets request method.
   *
   * @param requestMethod the request method
   */
  public void setRequestMethod(HttpMethod requestMethod) {
    this.requestMethod = requestMethod;
  }

  /**
   * Gets resource path.
   *
   * @return the resource path
   */
  public String getResourcePath() {
    return resourcePath;
  }

  /**
   * Sets resource path.
   *
   * @param resourcePath the resource path
   */
  public void setResourcePath(String resourcePath) {
    this.resourcePath = resourcePath;
  }

  /**
   * Gets response status code.
   *
   * @return the response status code
   */
  public Integer getResponseStatusCode() {
    return responseStatusCode;
  }

  /**
   * Sets response status code.
   *
   * @param responseStatusCode the response status code
   */
  public void setResponseStatusCode(Integer responseStatusCode) {
    this.responseStatusCode = responseStatusCode;
  }

  /**
   * Gets response type.
   *
   * @return the response type
   */
  public ResponseType getResponseType() {
    return responseType;
  }

  /**
   * Sets response type.
   *
   * @param responseType the response type
   */
  public void setResponseType(ResponseType responseType) {
    this.responseType = responseType;
  }

  /**
   * Gets error code.
   *
   * @return the error code
   */
  public String getErrorCode() {
    return errorCode;
  }

  /**
   * Sets error code.
   *
   * @param errorCode the error code
   */
  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }

  /**
   * Gets remote user.
   *
   * @return the remote user
   */
  public User getRemoteUser() {
    return remoteUser;
  }

  /**
   * Sets remote user.
   *
   * @param remoteUser the remote user
   */
  public void setRemoteUser(User remoteUser) {
    this.remoteUser = remoteUser;
  }

  /**
   * Gets remote ip address.
   *
   * @return the remote ip address
   */
  public String getRemoteIpAddress() {
    return remoteIpAddress;
  }

  /**
   * Sets remote ip address.
   *
   * @param remoteIpAddress the remote ip address
   */
  public void setRemoteIpAddress(String remoteIpAddress) {
    this.remoteIpAddress = remoteIpAddress;
  }

  /**
   * Gets remote host name.
   *
   * @return the remote host name
   */
  public String getRemoteHostName() {
    return remoteHostName;
  }

  /**
   * Sets remote host name.
   *
   * @param remoteHostName the remote host name
   */
  public void setRemoteHostName(String remoteHostName) {
    this.remoteHostName = remoteHostName;
  }

  /**
   * Gets remote host port.
   *
   * @return the remote host port
   */
  public Integer getRemoteHostPort() {
    return remoteHostPort;
  }

  /**
   * Sets remote host port.
   *
   * @param remoteHostPort the remote host port
   */
  public void setRemoteHostPort(Integer remoteHostPort) {
    this.remoteHostPort = remoteHostPort;
  }

  /**
   * Gets local ip address.
   *
   * @return the local ip address
   */
  public String getLocalIpAddress() {
    return localIpAddress;
  }

  /**
   * Sets local ip address.
   *
   * @param localIpAddress the local ip address
   */
  public void setLocalIpAddress(String localIpAddress) {
    this.localIpAddress = localIpAddress;
  }

  /**
   * Gets local host name.
   *
   * @return the local host name
   */
  public String getLocalHostName() {
    return localHostName;
  }

  /**
   * Sets local host name.
   *
   * @param localHostName the local host name
   */
  public void setLocalHostName(String localHostName) {
    this.localHostName = localHostName;
  }

  /**
   * Gets application.
   *
   * @return the application
   */
  public Application getApplication() {
    return application;
  }

  /**
   * Sets application.
   *
   * @param application the application
   */
  public void setApplication(Application application) {
    this.application = application;
  }

  /**
   * Gets component.
   *
   * @return the component
   */
  public Service getComponent() {
    return component;
  }

  /**
   * Sets component.
   *
   * @param component the component
   */
  public void setComponent(Service component) {
    this.component = component;
  }

  /**
   * Gets environment.
   *
   * @return the environment
   */
  public Environment getEnvironment() {
    return environment;
  }

  /**
   * Sets environment.
   *
   * @param environment the environment
   */
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  /**
   * Gets request time.
   *
   * @return the request time
   */
  public Long getRequestTime() {
    return requestTime;
  }

  /**
   * Sets request time.
   *
   * @param requestTime the request time
   */
  public void setRequestTime(Long requestTime) {
    this.requestTime = requestTime;
  }

  /**
   * Gets response time.
   *
   * @return the response time
   */
  public Long getResponseTime() {
    return responseTime;
  }

  /**
   * Sets response time.
   *
   * @param responseTime the response time
   */
  public void setResponseTime(Long responseTime) {
    this.responseTime = responseTime;
  }

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
   * Gets header string.
   *
   * @return the header string
   */
  public String getHeaderString() {
    return headerString;
  }

  /**
   * Sets header string.
   *
   * @param headerString the header string
   */
  public void setHeaderString(String headerString) {
    this.headerString = headerString;
  }

  /**
   * Gets request payload uuid.
   *
   * @return the request payload uuid
   */
  public String getRequestPayloadUuid() {
    return requestPayloadUuid;
  }

  /**
   * Sets request payload uuid.
   *
   * @param requestPayloadUuid the request payload uuid
   */
  public void setRequestPayloadUuid(String requestPayloadUuid) {
    this.requestPayloadUuid = requestPayloadUuid;
  }

  /**
   * Gets response payload uuid.
   *
   * @return the response payload uuid
   */
  public String getResponsePayloadUuid() {
    return responsePayloadUuid;
  }

  /**
   * Sets response payload uuid.
   *
   * @param responsePayloadUuid the response payload uuid
   */
  public void setResponsePayloadUuid(String responsePayloadUuid) {
    this.responsePayloadUuid = responsePayloadUuid;
  }

  /**
   * The Enum RequestType.
   */
  public enum RequestType {
    /**
     * Request request type.
     */
    REQUEST,
    /**
     * Response request type.
     */
    RESPONSE
  }

  /**
   * The Enum ResponseType.
   */
  public enum ResponseType {
    /**
     * Success response type.
     */
    SUCCESS,
    /**
     * Failed response type.
     */
    FAILED
  }

  /**
   * The Class Builder.
   */
  public static final class Builder {
    /**
     * The Remote user.
     */
    protected User remoteUser;
    /**
     * The Application.
     */
    protected Application application;
    /**
     * The Component.
     */
    protected Service component;
    /**
     * The Environment.
     */
    protected Environment environment;
    private String url;
    private String resourcePath;
    private String queryParams;
    private HttpMethod requestMethod;
    private String headerString;
    private ResponseType responseType;
    private Integer responseStatusCode;
    private String errorCode;
    private String remoteHostName;
    private Integer remoteHostPort;
    private String remoteIpAddress;
    private String localHostName;
    private String localIpAddress;
    private String requestPayloadUuid;
    private String responsePayloadUuid;
    private Long requestTime;
    private Long responseTime;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    /**
     * An audit header.
     *
     * @return the builder
     */
    public static Builder anAuditHeader() {
      return new Builder();
    }

    /**
     * With remote user.
     *
     * @param remoteUser the remote user
     * @return the builder
     */
    public Builder withRemoteUser(User remoteUser) {
      this.remoteUser = remoteUser;
      return this;
    }

    /**
     * With application.
     *
     * @param application the application
     * @return the builder
     */
    public Builder withApplication(Application application) {
      this.application = application;
      return this;
    }

    /**
     * With component.
     *
     * @param component the component
     * @return the builder
     */
    public Builder withComponent(Service component) {
      this.component = component;
      return this;
    }

    /**
     * With environment.
     *
     * @param environment the environment
     * @return the builder
     */
    public Builder withEnvironment(Environment environment) {
      this.environment = environment;
      return this;
    }

    /**
     * With url.
     *
     * @param url the url
     * @return the builder
     */
    public Builder withUrl(String url) {
      this.url = url;
      return this;
    }

    /**
     * With resource path.
     *
     * @param resourcePath the resource path
     * @return the builder
     */
    public Builder withResourcePath(String resourcePath) {
      this.resourcePath = resourcePath;
      return this;
    }

    /**
     * With query params.
     *
     * @param queryParams the query params
     * @return the builder
     */
    public Builder withQueryParams(String queryParams) {
      this.queryParams = queryParams;
      return this;
    }

    /**
     * With request method.
     *
     * @param requestMethod the request method
     * @return the builder
     */
    public Builder withRequestMethod(HttpMethod requestMethod) {
      this.requestMethod = requestMethod;
      return this;
    }

    /**
     * With header string.
     *
     * @param headerString the header string
     * @return the builder
     */
    public Builder withHeaderString(String headerString) {
      this.headerString = headerString;
      return this;
    }

    /**
     * With response type.
     *
     * @param responseType the response type
     * @return the builder
     */
    public Builder withResponseType(ResponseType responseType) {
      this.responseType = responseType;
      return this;
    }

    /**
     * With response status code.
     *
     * @param responseStatusCode the response status code
     * @return the builder
     */
    public Builder withResponseStatusCode(Integer responseStatusCode) {
      this.responseStatusCode = responseStatusCode;
      return this;
    }

    /**
     * With error code.
     *
     * @param errorCode the error code
     * @return the builder
     */
    public Builder withErrorCode(String errorCode) {
      this.errorCode = errorCode;
      return this;
    }

    /**
     * With remote host name.
     *
     * @param remoteHostName the remote host name
     * @return the builder
     */
    public Builder withRemoteHostName(String remoteHostName) {
      this.remoteHostName = remoteHostName;
      return this;
    }

    /**
     * With remote host port.
     *
     * @param remoteHostPort the remote host port
     * @return the builder
     */
    public Builder withRemoteHostPort(Integer remoteHostPort) {
      this.remoteHostPort = remoteHostPort;
      return this;
    }

    /**
     * With remote ip address.
     *
     * @param remoteIpAddress the remote ip address
     * @return the builder
     */
    public Builder withRemoteIpAddress(String remoteIpAddress) {
      this.remoteIpAddress = remoteIpAddress;
      return this;
    }

    /**
     * With local host name.
     *
     * @param localHostName the local host name
     * @return the builder
     */
    public Builder withLocalHostName(String localHostName) {
      this.localHostName = localHostName;
      return this;
    }

    /**
     * With local ip address.
     *
     * @param localIpAddress the local ip address
     * @return the builder
     */
    public Builder withLocalIpAddress(String localIpAddress) {
      this.localIpAddress = localIpAddress;
      return this;
    }

    /**
     * With request payload uuid.
     *
     * @param requestPayloadUuid the request payload uuid
     * @return the builder
     */
    public Builder withRequestPayloadUuid(String requestPayloadUuid) {
      this.requestPayloadUuid = requestPayloadUuid;
      return this;
    }

    /**
     * With response payload uuid.
     *
     * @param responsePayloadUuid the response payload uuid
     * @return the builder
     */
    public Builder withResponsePayloadUuid(String responsePayloadUuid) {
      this.responsePayloadUuid = responsePayloadUuid;
      return this;
    }

    /**
     * With request time.
     *
     * @param requestTime the request time
     * @return the builder
     */
    public Builder withRequestTime(Long requestTime) {
      this.requestTime = requestTime;
      return this;
    }

    /**
     * With response time.
     *
     * @param responseTime the response time
     * @return the builder
     */
    public Builder withResponseTime(Long responseTime) {
      this.responseTime = responseTime;
      return this;
    }

    /**
     * With uuid.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * Builds the.
     *
     * @return the audit header
     */
    public AuditHeader build() {
      AuditHeader auditHeader = new AuditHeader();
      auditHeader.setRemoteUser(remoteUser);
      auditHeader.setApplication(application);
      auditHeader.setComponent(component);
      auditHeader.setEnvironment(environment);
      auditHeader.setUrl(url);
      auditHeader.setResourcePath(resourcePath);
      auditHeader.setQueryParams(queryParams);
      auditHeader.setRequestMethod(requestMethod);
      auditHeader.setHeaderString(headerString);
      auditHeader.setResponseType(responseType);
      auditHeader.setResponseStatusCode(responseStatusCode);
      auditHeader.setErrorCode(errorCode);
      auditHeader.setRemoteHostName(remoteHostName);
      auditHeader.setRemoteHostPort(remoteHostPort);
      auditHeader.setRemoteIpAddress(remoteIpAddress);
      auditHeader.setLocalHostName(localHostName);
      auditHeader.setLocalIpAddress(localIpAddress);
      auditHeader.setRequestPayloadUuid(requestPayloadUuid);
      auditHeader.setResponsePayloadUuid(responsePayloadUuid);
      auditHeader.setRequestTime(requestTime);
      auditHeader.setResponseTime(responseTime);
      auditHeader.setUuid(uuid);
      auditHeader.setAppId(appId);
      auditHeader.setCreatedBy(createdBy);
      auditHeader.setCreatedAt(createdAt);
      auditHeader.setLastUpdatedBy(lastUpdatedBy);
      auditHeader.setLastUpdatedAt(lastUpdatedAt);
      return auditHeader;
    }
  }
}
