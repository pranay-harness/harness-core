package software.wings.beans;

import java.util.ArrayList;
import java.util.List;

public class RestResponse<T> extends RestRequest<T> {
  private List<ResponseMessage> responseMessages = new ArrayList<>();

  public RestResponse() {
    this(null);
  }
  public RestResponse(T resource) {
    super(resource);
  }
  public List<ResponseMessage> getResponseMessages() {
    return responseMessages;
  }
  public void setResponseMessages(List<ResponseMessage> responseMessages) {
    this.responseMessages = responseMessages;
  }
}
