package io.harness.http;

import io.harness.http.beans.HttpInternalConfig;
import io.harness.http.beans.HttpInternalResponse;

import java.io.IOException;

public interface HttpService {
  HttpInternalResponse executeUrl(HttpInternalConfig internalConfig) throws IOException;
}
