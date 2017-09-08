package software.wings.beans;

public class WebHookToken {
  private String webHookToken;
  private String httpMethod;
  private String payload;

  public String getWebHookToken() {
    return webHookToken;
  }

  public void setWebHookToken(String webHookToken) {
    this.webHookToken = webHookToken;
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  public void setHttpMethod(String httpMethod) {
    this.httpMethod = httpMethod;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }
}
