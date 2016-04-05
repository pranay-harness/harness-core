package software.wings.beans;

import com.google.common.base.MoreObjects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("responseMessages", responseMessages).toString();
  }

  public static class Builder<T> {
    private List<ResponseMessage> responseMessages = new ArrayList<>();
    private Map<String, Object> metaData;
    private T resource;

    private Builder() {}

    public static Builder aRestResponse() {
      return new Builder();
    }

    public Builder withResponseMessages(List<ResponseMessage> responseMessages) {
      this.responseMessages = responseMessages;
      return this;
    }

    public Builder withMetaData(Map<String, Object> metaData) {
      this.metaData = metaData;
      return this;
    }

    public Builder withResource(T resource) {
      this.resource = resource;
      return this;
    }

    public Builder but() {
      return aRestResponse().withResponseMessages(responseMessages).withMetaData(metaData).withResource(resource);
    }

    public RestResponse build() {
      RestResponse restResponse = new RestResponse();
      restResponse.setResponseMessages(responseMessages);
      restResponse.setMetaData(metaData);
      restResponse.setResource(resource);
      return restResponse;
    }
  }
}
