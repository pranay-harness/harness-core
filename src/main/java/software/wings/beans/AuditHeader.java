package software.wings.beans;

import java.util.Date;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

/**
 *  HttpAuditHeader bean class.
 *
 *
 * @author Rishi
 *
 */
@Entity(value = "audits", noClassnameStored = true)
public class AuditHeader extends Base {
  public enum ResponseType { SUCCESS, FAILED }

  private String url;

  private String resourcePath;

  private String queryParams;

  private HTTPMethod requestMethod;

  private String headerString;

  private ResponseType responseType;

  private Integer responseStatusCode;

  private String errorCode;

  private String remoteHostName;

  private Integer remoteHostPort;

  private String remoteIpAddress;

  protected User remoteUser;

  private String localHostName;

  private String localIpAddress;

  @Reference protected Application application;

  //@Reference
  protected Service component;

  //@Reference
  protected Environment environment;

  private Date requestTime;

  private Date responseTime;

  public String getQueryParams() {
    return queryParams;
  }

  public void setQueryParams(String queryParams) {
    this.queryParams = queryParams;
  }

  public HTTPMethod getRequestMethod() {
    return requestMethod;
  }

  public void setRequestMethod(HTTPMethod requestMethod) {
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

  public Date getRequestTime() {
    return requestTime;
  }

  public void setRequestTime(Date requestTime) {
    this.requestTime = requestTime;
  }

  public Date getResponseTime() {
    return responseTime;
  }

  public void setResponseTime(Date responseTime) {
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

  @Override
  public String toString() {
    return "AuditHeader [url=" + url + ", resourcePath=" + resourcePath + ", queryParams=" + queryParams
        + ", requestMethod=" + requestMethod + ", headerString=" + headerString + ", responseType=" + responseType
        + ", responseStatusCode=" + responseStatusCode + ", errorCode=" + errorCode
        + ", remoteHostName=" + remoteHostName + ", remoteHostPort=" + remoteHostPort
        + ", remoteIpAddress=" + remoteIpAddress + ", remoteUser=" + remoteUser + ", localHostName=" + localHostName
        + ", localIpAddress=" + localIpAddress + ", application=" + application + ", component=" + component
        + ", environment=" + environment + ", requestTime=" + requestTime + ", responseTime=" + responseTime + "]";
  }
}
