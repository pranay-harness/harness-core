/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
