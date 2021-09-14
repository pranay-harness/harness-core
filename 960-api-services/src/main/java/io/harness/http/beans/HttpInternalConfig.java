/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.http.beans;

import io.harness.beans.KeyValuePair;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HttpInternalConfig {
  String method;
  String url;
  String header;
  Map<String, String> requestHeaders;
  List<KeyValuePair> headers;
  String body;
  int socketTimeoutMillis;
  boolean useProxy;
  boolean isCertValidationRequired;
}
