/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.network;

import io.harness.beans.KeyValuePair;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HttpURLHeaderInfo {
  String url;
  List<KeyValuePair> headers;
}
