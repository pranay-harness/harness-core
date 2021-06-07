package io.harness.argo.service;

import io.harness.argo.beans.ArgoConfigInternal;

import java.io.IOException;

public interface ArgoCdService {
  public String fetchApplication(ArgoConfigInternal argoConfig, String argoAppName) throws IOException;
}
