package io.harness.argo.service;

import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.argo.ArgoRestClient;
import io.harness.argo.beans.AppStatus;
import io.harness.argo.beans.AppSyncOptions;
import io.harness.argo.beans.ArgoApp;
import io.harness.argo.beans.ArgoAppRequest;
import io.harness.argo.beans.ArgoConfigInternal;
import io.harness.argo.beans.ArgoToken;
import io.harness.argo.beans.ClusterResourceTree;
import io.harness.argo.beans.ClusterResourceTreeDTO;
import io.harness.argo.beans.ManagedResource;
import io.harness.argo.beans.ManagedResourceList;
import io.harness.argo.beans.ManifestDiff;
import io.harness.argo.beans.RevisionMeta;
import io.harness.argo.beans.UsernamePassword;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.network.Http;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@OwnedBy(HarnessTeam.CDP)
@Singleton
@Slf4j
public class ArgoCdServiceImpl implements ArgoCdService {
  @Inject private ExecutorService executorService;
  public static final String BEARER = "Bearer ";
  private static final String TOKEN_KEY = "TOKEN:";
  // Use a time based cache here
  private String token = null;

  @Override
  public ArgoApp fetchApplication(ArgoConfigInternal argoConfig, String argoAppName) throws IOException {
    ArgoRestClient argoRestClient = createArgoRestClient(argoConfig);
    token = fetchToken(argoConfig, argoRestClient);
    Response<ArgoApp> argoAppResponse = argoRestClient.fetchApp(BEARER + token, argoAppName).execute();

    if (argoAppResponse.isSuccessful()) {
      return argoAppResponse.body();
    }
    throw new InvalidRequestException("Failure in fetching argo App: " + argoAppResponse.message(), USER);
  }

  @Override
  public AppStatus fetchApplicationStatus(ArgoConfigInternal argoConfig, String argoAppName)
      throws IOException, ExecutionException, InterruptedException {
    ArgoRestClient argoRestClient = createArgoRestClient(argoConfig);
    token = fetchToken(argoConfig, argoRestClient);
    Response<ArgoApp> argoAppResponse = argoRestClient.fetchApp(BEARER + token, argoAppName).execute();

    if (argoAppResponse.isSuccessful()) {
      final ArgoApp argoApp = argoAppResponse.body();
      final Future<RevisionMeta> incomingDetailsFuture =
          executorService.submit(() -> fetchRevisionMetadata(argoConfig, argoAppName, argoApp.incomingRevision()));
      final Future<RevisionMeta> syncedDetailsFuture =
          executorService.submit(() -> fetchRevisionMetadata(argoConfig, argoAppName, argoApp.syncedRevision()));
      final RevisionMeta incomingRevision = incomingDetailsFuture.get();
      final RevisionMeta syncedRevision = syncedDetailsFuture.get();
      return AppStatus.fromArgoApp(argoApp, incomingRevision, syncedRevision);
    }
    throw new InvalidRequestException("Failure in fetching argo App: " + argoAppResponse.message(), USER);
  }

  @Override
  public ArgoApp createApplication(ArgoConfigInternal argoConfig, ArgoAppRequest createAppRequest) throws IOException {
    final ArgoApp argoApp = ArgoApp.ArgoApp(createAppRequest);
    ArgoRestClient argoRestClient = createArgoRestClient(argoConfig);
    token = fetchToken(argoConfig, argoRestClient);

    final Response<ArgoApp> argoAppResponse = argoRestClient.createApp(BEARER + token, argoApp).execute();

    if (argoAppResponse.isSuccessful()) {
      return argoAppResponse.body();
    }
    throw new InvalidRequestException("Failure in Creating argo App: " + argoAppResponse.message(), USER);
  }

  @Override
  public ArgoApp updateArgoApplication(ArgoConfigInternal argoConfig, ArgoAppRequest updateAppRequest)
      throws IOException {
    final ArgoApp argoApp = ArgoApp.ArgoApp(updateAppRequest);
    ArgoRestClient argoRestClient = createArgoRestClient(argoConfig);
    token = fetchToken(argoConfig, argoRestClient);

    final Response<ArgoApp> argoAppResponse =
        argoRestClient.updateApp(BEARER + token, updateAppRequest.getName(), argoApp).execute();

    if (argoAppResponse.isSuccessful()) {
      return argoAppResponse.body();
    }
    throw new InvalidRequestException("Failure in Updating argo App: " + argoAppResponse.message(), USER);
  }

  @Override
  public ArgoApp syncApp(ArgoConfigInternal argoConfig, String argoAppName, AppSyncOptions syncOptions)
      throws IOException {
    ArgoRestClient argoRestClient = createArgoRestClient(argoConfig);
    token = fetchToken(argoConfig, argoRestClient);

    final Response<ArgoApp> appSyncResponse =
        argoRestClient.syncApp(BEARER + token, argoAppName, syncOptions).execute();

    if (appSyncResponse.isSuccessful()) {
      return appSyncResponse.body();
    }
    throw new InvalidRequestException("Failure in sync argo App: " + appSyncResponse.message(), USER);
  }

  @Override
  public ClusterResourceTreeDTO fetchResourceTree(ArgoConfigInternal argoConfig, String appName) throws IOException {
    ArgoRestClient argoRestClient = createArgoRestClient(argoConfig);
    token = fetchToken(argoConfig, argoRestClient);

    final Response<ClusterResourceTree> resourceTreeResponse =
        argoRestClient.fetchResourceTree(BEARER + token, appName).execute();

    if (resourceTreeResponse.isSuccessful()) {
      return new ClusterResourceTreeDTO(resourceTreeResponse.body(), appName);
    }
    throw new InvalidRequestException("Failure in fetching resource tree " + resourceTreeResponse.message(), USER);
  }

  @Override
  public List<ManifestDiff> fetchManifestDiff(ArgoConfigInternal argoConfig, String appName) throws IOException {
    ArgoRestClient argoRestClient = createArgoRestClient(argoConfig);
    token = fetchToken(argoConfig, argoRestClient);

    final Response<ManagedResourceList> resourceStates =
        argoRestClient.fetchResourceStates(BEARER + token, appName).execute();

    List<ManifestDiff> manifestDiffs = new ArrayList<>();
    if (resourceStates.isSuccessful()) {
      final ManagedResourceList managedResourceList = resourceStates.body();
      final List<ManagedResource> items = managedResourceList.getItems();

      if (EmptyPredicate.isNotEmpty(items)) {
        manifestDiffs =
            items.stream()
                .map(r
                    -> ManifestDiff.builder()
                           .resourceIdentifier(String.format("%s/%s/%s", r.getNamespace(), r.getKind(), r.getName()))
                           .clusterManifest(prepareYaml(r.getNormalizedLiveState()))
                           .gitManifest(prepareYaml(r.getPredictedLiveState()))
                           .build())
                .collect(Collectors.toList());
      }
      return manifestDiffs;
    }

    throw new InvalidRequestException("Failure in fetching resource manifest diffs " + resourceStates.message(), USER);
  }

  @Override
  public RevisionMeta fetchRevisionMetadata(ArgoConfigInternal argoConfig, String appName, String revision)
      throws IOException {
    ArgoRestClient argoRestClient = createArgoRestClient(argoConfig);
    token = fetchToken(argoConfig, argoRestClient);

    final Response<RevisionMeta> revisionMetaResponse =
        argoRestClient.revisionMeta(BEARER + token, appName, revision).execute();

    if (revisionMetaResponse.isSuccessful()) {
      return revisionMetaResponse.body();
    }
    throw new InvalidRequestException("Failure in fetching revision metadata " + revisionMetaResponse.message(), USER);
  }

  private String fetchToken(ArgoConfigInternal argoConfig, ArgoRestClient argoRestClient) throws IOException {
    if (token != null) {
      return token;
    }
    Response<ArgoToken> argoCdToken = argoRestClient
                                          .fetchToken(UsernamePassword.builder()
                                                          .username(argoConfig.getUsername())
                                                          .password(String.valueOf(argoConfig.getPassword()))
                                                          .build())
                                          .execute();
    if (argoCdToken.isSuccessful()) {
      //
    }

    return argoCdToken.body().getToken();
  }

  private ArgoRestClient createArgoRestClient(ArgoConfigInternal argoConfig) {
    OkHttpClient okHttpClient =
        Http.getOkHttpClient(argoConfig.getArgoServerUrl(), argoConfig.isCertValidationRequired());
    Retrofit retrofit = new Retrofit.Builder()
                            .client(okHttpClient)
                            .baseUrl(argoConfig.getArgoServerUrl())
                            .addConverterFactory(JacksonConverterFactory.create())
                            .build();
    return retrofit.create(ArgoRestClient.class);
  }

  private String prepareYaml(String str) {
    Yaml yaml = new Yaml();
    final Object load = yaml.load(str);
    return yaml.dump(load);
  }
}
