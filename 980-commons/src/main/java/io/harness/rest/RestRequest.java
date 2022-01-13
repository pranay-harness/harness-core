/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.rest;

import com.google.common.base.MoreObjects;
import java.util.HashMap;
import java.util.Map;

/**
 * The Class RestRequest.
 *
 * @param <T> the generic type
 */
public class RestRequest<T> {
  private Map<String, Object> metaData;

  private T resource;

  /**
   * Instantiates a new rest request.
   */
  public RestRequest() {
    this(null);
  }

  /**
   * Instantiates a new rest request.
   *
   * @param resource the resource
   */
  public RestRequest(T resource) {
    this.resource = resource;
    metaData = new HashMap<>();
  }

  /**
   * Gets meta data.
   *
   * @return the meta data
   */
  public Map<String, Object> getMetaData() {
    return metaData;
  }

  /**
   * Sets meta data.
   *
   * @param metaData the meta data
   */
  public void setMetaData(Map<String, Object> metaData) {
    this.metaData = metaData;
  }

  /**
   * Gets resource.
   *
   * @return the resource
   */
  public T getResource() {
    return resource;
  }

  /**
   * Sets resource.
   *
   * @param resource the resource
   */
  public void setResource(T resource) {
    this.resource = resource;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("metaData", metaData).add("resource", resource).toString();
  }
}
