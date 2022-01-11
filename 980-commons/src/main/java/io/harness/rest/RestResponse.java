/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.rest;

import io.harness.eraro.ResponseMessage;

import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Class RestResponse.
 *
 * @param <T> the generic type
 */
public class RestResponse<T> extends RestRequest<T> {
  private List<ResponseMessage> responseMessages = new ArrayList<>();

  /**
   * Instantiates a new rest response.
   */
  public RestResponse() {
    this(null);
  }

  /**
   * Instantiates a new rest response.
   *
   * @param resource the resource
   */
  public RestResponse(T resource) {
    super(resource);
  }

  /**
   * Gets response messages.
   *
   * @return the response messages
   */
  public List<ResponseMessage> getResponseMessages() {
    return responseMessages;
  }

  /**
   * Sets response messages.
   *
   * @param responseMessages the response messages
   */
  public void setResponseMessages(List<ResponseMessage> responseMessages) {
    this.responseMessages = responseMessages;
  }

  /**
   * Adds a response message.
   *
   * @param responseMessage the response message
   */
  public void addResponseMessage(ResponseMessage responseMessage) {
    if (responseMessage != null) {
      this.responseMessages.add(responseMessage);
    }
  }

  /* (non-Javadoc)
   * @see software.wings.beans.RestRequest#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("responseMessages", responseMessages).toString();
  }

  /**
   * The Class Builder.
   *
   * @param <T> the generic type
   */
  public static class Builder<T> {
    private List<ResponseMessage> responseMessages = new ArrayList<>();
    private Map<String, Object> metaData;
    private T resource;

    private Builder() {}

    /**
     * A rest response.
     *
     * @param <T> the generic type
     * @return the builder
     */
    public static <T> Builder<T> aRestResponse() {
      return new Builder<>();
    }

    /**
     * But.
     *
     * @return copy of Builder object.
     */
    public Builder but() {
      return aRestResponse().withResponseMessages(responseMessages).withMetaData(metaData).withResource(resource);
    }

    /**
     * With resource.
     *
     * @param resource the resource
     * @return the builder
     */
    public Builder<T> withResource(T resource) {
      this.resource = resource;
      return this;
    }

    /**
     * With meta data.
     *
     * @param metaData the meta data
     * @return the builder
     */
    public Builder withMetaData(Map<String, Object> metaData) {
      this.metaData = metaData;
      return this;
    }

    /**
     * With response messages.
     *
     * @param responseMessages the response messages
     * @return the builder
     */
    public Builder withResponseMessages(List<ResponseMessage> responseMessages) {
      this.responseMessages = responseMessages;
      return this;
    }

    /**
     * Builds a RestResponse object.
     *
     * @return RestResponse object.
     */
    public RestResponse build() {
      RestResponse restResponse = new RestResponse();
      restResponse.setResponseMessages(responseMessages);
      restResponse.setMetaData(metaData);
      restResponse.setResource(resource);
      return restResponse;
    }
  }
}
