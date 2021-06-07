package io.harness.argo.service;

import io.harness.argo.beans.AppSyncOptions;
import io.harness.argo.beans.ArgoApp;
import io.harness.argo.beans.ArgoAppRequest;
import io.harness.argo.beans.ArgoConfigInternal;
import io.harness.argo.beans.ClusterResourceTree;
import io.harness.argo.beans.ManifestDiff;

import java.io.IOException;
import java.util.List;

public interface ArgoCdService {
  ArgoApp fetchApplication(ArgoConfigInternal argoConfig, String argoAppName) throws IOException;
  ArgoApp createApplication(ArgoConfigInternal argoConfig, ArgoAppRequest createAppRequest) throws IOException;
  ArgoApp updateArgoApplication(ArgoConfigInternal argoConfig, ArgoAppRequest createAppRequest) throws IOException;
  ArgoApp syncApp(ArgoConfigInternal argoConfig, String argoAppName, AppSyncOptions syncOptions) throws IOException;
  ClusterResourceTree fetchResourceTree(ArgoConfigInternal argoConfig, String appName) throws IOException;
  List<ManifestDiff> fetchManifestDiff(ArgoConfigInternal argoConfig, String appName) throws IOException;
}
