package io.harness.argo.service;

import io.harness.argo.beans.AppStatus;
import io.harness.argo.beans.AppSyncOptions;
import io.harness.argo.beans.ArgoApp;
import io.harness.argo.beans.ArgoAppRequest;
import io.harness.argo.beans.ArgoConfigInternal;
import io.harness.argo.beans.ClusterResourceTreeDTO;
import io.harness.argo.beans.ManifestDiff;
import io.harness.argo.beans.RevisionMeta;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public interface ArgoCdService {
  ArgoApp fetchApplication(ArgoConfigInternal argoConfig, String argoAppName) throws IOException;
  AppStatus fetchApplicationStatus(ArgoConfigInternal argoConfig, String argoAppName) throws IOException, ExecutionException, InterruptedException;
  ArgoApp createApplication(ArgoConfigInternal argoConfig, ArgoAppRequest createAppRequest) throws IOException;
  ArgoApp updateArgoApplication(ArgoConfigInternal argoConfig, ArgoAppRequest createAppRequest) throws IOException;
  ArgoApp syncApp(ArgoConfigInternal argoConfig, String argoAppName, AppSyncOptions syncOptions) throws IOException;
  ClusterResourceTreeDTO fetchResourceTree(ArgoConfigInternal argoConfig, String appName) throws IOException;
  List<ManifestDiff> fetchManifestDiff(ArgoConfigInternal argoConfig, String appName) throws IOException;
  RevisionMeta fetchRevisionMetadata(ArgoConfigInternal argoConfig, String appName, String revision) throws IOException;
}
