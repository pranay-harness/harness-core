package software.wings.audit;

import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Application;
import software.wings.beans.Base;
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
public class AuditHeader extends Base {
  protected User remoteUser;
  protected Application application;
  protected Service component;
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

  public String getQueryParams() {
    return queryParams;
  }

  public void setQueryParams(String queryParams) {
    this.queryParams = queryParams;
  }

  public HttpMethod getRequestMethod() {
    return requestMethod;
  }

  public void setRequestMethod(HttpMethod requestMethod) {
    this.requestMethod = requestMethod;
  }

  public String getResourcePath() {
    return resourcePath;
  }

  public void setResourcePath(String resourcePath) {
    this.resourcePath = resourcePath;
  }

  public Integer getResponseStatusCode() {
    return responseStatusCode;
  }

  public void setResponseStatusCode(Integer responseStatusCode) {
    this.responseStatusCode = responseStatusCode;
  }

  public ResponseType getResponseType() {
    return responseType;
  }

  public void setResponseType(ResponseType responseType) {
    this.responseType = responseType;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }

  public User getRemoteUser() {
    return remoteUser;
  }

  public void setRemoteUser(User remoteUser) {
    this.remoteUser = remoteUser;
  }

  public String getRemoteIpAddress() {
    return remoteIpAddress;
  }

  public void setRemoteIpAddress(String remoteIpAddress) {
    this.remoteIpAddress = remoteIpAddress;
  }

  public String getRemoteHostName() {
    return remoteHostName;
  }

  public void setRemoteHostName(String remoteHostName) {
    this.remoteHostName = remoteHostName;
  }

  public Integer getRemoteHostPort() {
    return remoteHostPort;
  }

  public void setRemoteHostPort(Integer remoteHostPort) {
    this.remoteHostPort = remoteHostPort;
  }

  public String getLocalIpAddress() {
    return localIpAddress;
  }

  public void setLocalIpAddress(String localIpAddress) {
    this.localIpAddress = localIpAddress;
  }

  public String getLocalHostName() {
    return localHostName;
  }

  public void setLocalHostName(String localHostName) {
    this.localHostName = localHostName;
  }

  public Application getApplication() {
    return application;
  }

  public void setApplication(Application application) {
    this.application = application;
  }

  public Service getComponent() {
    return component;
  }

  public void setComponent(Service component) {
    this.component = component;
  }

  public Environment getEnvironment() {
    return environment;
  }

  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  public Long getRequestTime() {
    return requestTime;
  }

  public void setRequestTime(Long requestTime) {
    this.requestTime = requestTime;
  }

  public Long getResponseTime() {
    return responseTime;
  }

  public void setResponseTime(Long responseTime) {
    this.responseTime = responseTime;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getHeaderString() {
    return headerString;
  }

  public void setHeaderString(String headerString) {
    this.headerString = headerString;
  }

  public String getRequestPayloadUuid() {
    return requestPayloadUuid;
  }

  public void setRequestPayloadUuid(String requestPayloadUuid) {
    this.requestPayloadUuid = requestPayloadUuid;
  }

  public String getResponsePayloadUuid() {
    return responsePayloadUuid;
  }

  public void setResponsePayloadUuid(String responsePayloadUuid) {
    this.responsePayloadUuid = responsePayloadUuid;
  }

  public enum RequestType { REQUEST, RESPONSE }

  public enum ResponseType { SUCCESS, FAILED }

  public static final class Builder {
    protected User remoteUser;
    protected Application application;
    protected Service component;
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
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    public static Builder anAuditHeader() {
      return new Builder();
    }

    public Builder withRemoteUser(User remoteUser) {
      this.remoteUser = remoteUser;
      return this;
    }

    public Builder withApplication(Application application) {
      this.application = application;
      return this;
    }

    public Builder withComponent(Service component) {
      this.component = component;
      return this;
    }

    public Builder withEnvironment(Environment environment) {
      this.environment = environment;
      return this;
    }

    public Builder withUrl(String url) {
      this.url = url;
      return this;
    }

    public Builder withResourcePath(String resourcePath) {
      this.resourcePath = resourcePath;
      return this;
    }

    public Builder withQueryParams(String queryParams) {
      this.queryParams = queryParams;
      return this;
    }

    public Builder withRequestMethod(HttpMethod requestMethod) {
      this.requestMethod = requestMethod;
      return this;
    }

    public Builder withHeaderString(String headerString) {
      this.headerString = headerString;
      return this;
    }

    public Builder withResponseType(ResponseType responseType) {
      this.responseType = responseType;
      return this;
    }

    public Builder withResponseStatusCode(Integer responseStatusCode) {
      this.responseStatusCode = responseStatusCode;
      return this;
    }

    public Builder withErrorCode(String errorCode) {
      this.errorCode = errorCode;
      return this;
    }

    public Builder withRemoteHostName(String remoteHostName) {
      this.remoteHostName = remoteHostName;
      return this;
    }

    public Builder withRemoteHostPort(Integer remoteHostPort) {
      this.remoteHostPort = remoteHostPort;
      return this;
    }

    public Builder withRemoteIpAddress(String remoteIpAddress) {
      this.remoteIpAddress = remoteIpAddress;
      return this;
    }

    public Builder withLocalHostName(String localHostName) {
      this.localHostName = localHostName;
      return this;
    }

    public Builder withLocalIpAddress(String localIpAddress) {
      this.localIpAddress = localIpAddress;
      return this;
    }

    public Builder withRequestPayloadUuid(String requestPayloadUuid) {
      this.requestPayloadUuid = requestPayloadUuid;
      return this;
    }

    public Builder withResponsePayloadUuid(String responsePayloadUuid) {
      this.responsePayloadUuid = responsePayloadUuid;
      return this;
    }

    public Builder withRequestTime(Long requestTime) {
      this.requestTime = requestTime;
      return this;
    }

    public Builder withResponseTime(Long responseTime) {
      this.responseTime = responseTime;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

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
      auditHeader.setActive(active);
      return auditHeader;
    }
  }
}
