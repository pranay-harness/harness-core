/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.http;

import io.harness.http.beans.HttpInternalConfig;
import io.harness.http.beans.HttpInternalResponse;

import java.io.IOException;

public interface HttpService {
  HttpInternalResponse executeUrl(HttpInternalConfig internalConfig) throws IOException;
}
