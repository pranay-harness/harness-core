package io.harness.istio.api.networking;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.WingsException;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.CDP)
public class IstioNetworkingApiFactory {
  @Inject private V1Alpha3IstioApiNetworkingHandler v1Alpha3IstioApiNetworkingHandler;
  @Inject private V1Beta1IstioApiNetworkingHandler v1Beta1IstioApiNetworkingHandler;

  public IstioApiNetworkingHandler obtainHandler(String apiVersion) throws InvalidArgumentsException {
    IstioNetworkingApiVersions istioNetworkingApi = IstioNetworkingApiVersions.findByApiVersion(apiVersion);
    if (istioNetworkingApi == null) {
      throw new InvalidArgumentsException(format("%s is not a valid api version", apiVersion));
    }
    switch (istioNetworkingApi) {
      case V1Alpha3:
        return v1Alpha3IstioApiNetworkingHandler;
      case V1Beta1:
        return v1Beta1IstioApiNetworkingHandler;
      default: {
        throw new InvalidArgumentsException(
            format("Invalid api version : %s provided.", apiVersion), WingsException.USER);
      }
    }
  }
}
