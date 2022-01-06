/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.beans;

/**
 * HttpMethod bean class.
 *
 * @author Rishi
 */
public enum HttpMethod {
  /**
   * Options http method.
   */
  OPTIONS,
  /**
   * Head http method.
   */
  HEAD,
  /**
   * Get http method.
   */
  GET,

  /**
   * Patch http method.
   */
  PATCH,
  /**
   * Post http method.
   */
  POST,
  /**
   * Put http method.
   */
  PUT,
  /**
   * Delete http method.
   */
  DELETE;
}
