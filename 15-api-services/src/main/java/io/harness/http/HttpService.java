package io.harness.http;

import io.harness.http.beans.HttpInternalConfig;
import io.harness.http.beans.HttpInternalResponse;

public interface HttpService {
  HttpInternalResponse executeUrl(HttpInternalConfig internalConfig);
}
