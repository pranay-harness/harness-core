/*
 * Harness feature flag service
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 1.0.0
 * Contact: ff@harness.io
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

package io.harness.cf.openapi;

import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2021-05-11T09:07:44.775-07:00[America/Los_Angeles]")
public class ApiException extends Exception {
  private int code = 0;
  private Map<String, List<String>> responseHeaders = null;
  private String responseBody = null;

  public ApiException() {}

  public ApiException(Throwable throwable) {
    super(throwable);
  }

  public ApiException(String message) {
    super(message);
  }

  public ApiException(
      String message, Throwable throwable, int code, Map<String, List<String>> responseHeaders, String responseBody) {
    super(message, throwable);
    this.code = code;
    this.responseHeaders = responseHeaders;
    this.responseBody = responseBody;
  }

  public ApiException(String message, int code, Map<String, List<String>> responseHeaders, String responseBody) {
    this(message, (Throwable) null, code, responseHeaders, responseBody);
  }

  public ApiException(String message, Throwable throwable, int code, Map<String, List<String>> responseHeaders) {
    this(message, throwable, code, responseHeaders, null);
  }

  public ApiException(int code, Map<String, List<String>> responseHeaders, String responseBody) {
    this((String) null, (Throwable) null, code, responseHeaders, responseBody);
  }

  public ApiException(int code, String message) {
    super(message);
    this.code = code;
  }

  public ApiException(int code, String message, Map<String, List<String>> responseHeaders, String responseBody) {
    this(code, message);
    this.responseHeaders = responseHeaders;
    this.responseBody = responseBody;
  }

  /**
   * Get the HTTP status code.
   *
   * @return HTTP status code
   */
  public int getCode() {
    return code;
  }

  /**
   * Get the HTTP response headers.
   *
   * @return A map of list of string
   */
  public Map<String, List<String>> getResponseHeaders() {
    return responseHeaders;
  }

  /**
   * Get the HTTP response body.
   *
   * @return Response body in the form of string
   */
  public String getResponseBody() {
    return responseBody;
  }
}
