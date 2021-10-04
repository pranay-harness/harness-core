package io.harness.istio.api.networking;

import lombok.Getter;

public enum IstioNetworkingApiVersions {
  V1Alpha3("networking.istio.io/v1alpha3"),
  V1Beta1("networking.istio.io/v1beta1");

  @Getter private final String apiVersion;

  IstioNetworkingApiVersions(String apiVersion) {
    this.apiVersion = apiVersion;
  }
}
