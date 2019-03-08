package io.harness.delegate.beans.executioncapability;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Singleton;

import io.harness.delegate.beans.HttpTaskParameters;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

@Singleton
public class HttpCapabilityGenerator implements CapabilityGenerator {
  @Override
  public List<ExecutionCapability> generateDelegateCapabilities(Object[] parameters) {
    HttpTaskParameters httpTaskParameters = (HttpTaskParameters) parameters[0];

    try {
      URI uri = new URI(httpTaskParameters.getUrl());

      return Arrays.asList(HttpConnectionExecutionCapability.builder()
                               .scheme(uri.getScheme())
                               .hostName(getHostName(uri))
                               .port(uri.getPort() == -1 ? null : Integer.toString(uri.getPort()))
                               .build());
    } catch (URISyntaxException e) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, WingsException.USER);
    }
  }

  private String getHostName(URI uri) {
    if (isBlank(uri.getScheme()) && isBlank(uri.getHost())) {
      return uri.toString();
    }

    return uri.getHost();
  }
}
