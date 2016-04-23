package software.wings.audit;

import com.google.common.base.Objects;

import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.Environment;
import software.wings.beans.HttpMethod;
import software.wings.beans.Service;
import software.wings.beans.User;

import java.util.Date;

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
  private Date requestTime;
  private Date responseTime;

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

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("remoteUser", remoteUser)
        .add("application", application)
        .add("component", component)
        .add("environment", environment)
        .add("url", url)
        .add("resourcePath", resourcePath)
        .add("queryParams", queryParams)
        .add("requestMethod", requestMethod)
        .add("headerString", headerString)
        .add("responseType", responseType)
        .add("responseStatusCode", responseStatusCode)
        .add("errorCode", errorCode)
        .add("remoteHostName", remoteHostName)
        .add("remoteHostPort", remoteHostPort)
        .add("remoteIpAddress", remoteIpAddress)
        .add("localHostName", localHostName)
        .add("localIpAddress", localIpAddress)
        .add("requestPayloadUuid", requestPayloadUuid)
        .add("responsePayloadUuid", responsePayloadUuid)
        .add("requestTime", requestTime)
        .add("responseTime", responseTime)
        .toString();
  }

  public enum RequestType { REQUEST, RESPONSE }

  public enum ResponseType { SUCCESS, FAILED }
}
