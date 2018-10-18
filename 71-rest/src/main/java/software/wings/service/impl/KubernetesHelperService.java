package software.wings.service.impl;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER;
import static io.fabric8.kubernetes.client.utils.Utils.isNotNullOrEmpty;
import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.network.Http.getOkHttpClientBuilder;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static okhttp3.ConnectionSpec.CLEARTEXT;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.utils.KubernetesConvention.DASH;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.DoneableHorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscalerList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.internal.HorizontalPodAutoscalerOperationsImpl;
import io.fabric8.kubernetes.client.internal.SSLUtils;
import io.harness.network.Http;
import me.snowdrop.istio.api.model.IstioResource;
import me.snowdrop.istio.api.model.v1.networking.DestinationWeight;
import me.snowdrop.istio.api.model.v1.networking.VirtualService;
import me.snowdrop.istio.client.IstioClient;
import me.snowdrop.istio.client.KubernetesAdapter;
import okhttp3.Authenticator;
import okhttp3.ConnectionSpec;
import okhttp3.Credentials;
import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.yaml.YamlHelper;
import software.wings.yaml.YamlRepresenter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by brett on 2/22/17
 */
@Singleton
public class KubernetesHelperService {
  private static final Logger logger = LoggerFactory.getLogger(KubernetesHelperService.class);

  @Inject private EncryptionService encryptionService;

  public KubernetesClient getKubernetesClient(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    return getKubernetesClient(kubernetesConfig, encryptedDataDetails, StringUtils.EMPTY);
  }

  /**
   * Gets a Kubernetes client.
   */
  public KubernetesClient getKubernetesClient(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String apiVersion) {
    if (!kubernetesConfig.isDecrypted()) {
      encryptionService.decrypt(kubernetesConfig, encryptedDataDetails);
    }
    String namespace = "default";
    ConfigBuilder configBuilder = new ConfigBuilder().withTrustCerts(true);
    if (isNotBlank(kubernetesConfig.getNamespace())) {
      namespace = kubernetesConfig.getNamespace().trim();
      configBuilder.withNamespace(namespace);
    }
    if (isNotBlank(kubernetesConfig.getMasterUrl())) {
      configBuilder.withMasterUrl(kubernetesConfig.getMasterUrl().trim());
    }
    if (isNotBlank(kubernetesConfig.getUsername())) {
      configBuilder.withUsername(kubernetesConfig.getUsername().trim());
    }
    if (kubernetesConfig.getPassword() != null) {
      configBuilder.withPassword(new String(kubernetesConfig.getPassword()).trim());
    }
    if (kubernetesConfig.getCaCert() != null) {
      configBuilder.withCaCertData(encode(kubernetesConfig.getCaCert()));
    }
    if (kubernetesConfig.getClientCert() != null) {
      configBuilder.withClientCertData(encode(kubernetesConfig.getClientCert()));
    }
    if (kubernetesConfig.getClientKey() != null) {
      configBuilder.withClientKeyData(encode(kubernetesConfig.getClientKey()));
    }
    if (kubernetesConfig.getClientKeyPassphrase() != null) {
      configBuilder.withClientKeyPassphrase(new String(kubernetesConfig.getClientKeyPassphrase()).trim());
    }
    if (kubernetesConfig.getServiceAccountToken() != null) {
      configBuilder.withOauthToken(new String(kubernetesConfig.getServiceAccountToken()).trim());
    }
    if (kubernetesConfig.getClientKeyAlgo() != null) {
      configBuilder.withClientKeyAlgo(kubernetesConfig.getClientKeyAlgo().trim());
    }

    if (isNotBlank(apiVersion)) {
      configBuilder.withApiVersion(apiVersion);
    }

    Config config = configBuilder.build();

    OkHttpClient okHttpClient = createHttpClientWithProxySetting(config);
    try (DefaultKubernetesClient client = new DefaultKubernetesClient(okHttpClient, config)) {
      return client.inNamespace(namespace);
    }
  }

  public IstioClient getIstioClient(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    return new IstioClient(new KubernetesAdapter(getKubernetesClient(kubernetesConfig, encryptedDataDetails)));
  }

  public static void printVirtualServiceRouteWeights(
      IstioResource virtualService, String controllerPrefix, ExecutionLogCallback executionLogCallback) {
    VirtualService virtualServiceSpec = (VirtualService) virtualService.getSpec();
    if (isNotEmpty(virtualServiceSpec.getHttp().get(0).getRoute())) {
      List<DestinationWeight> sorted = virtualServiceSpec.getHttp().get(0).getRoute();
      sorted.sort(Comparator.comparing(a -> Integer.valueOf(a.getDestination().getSubset())));
      for (DestinationWeight destinationWeight : sorted) {
        int weight = destinationWeight.getWeight();
        String rev = destinationWeight.getDestination().getSubset();
        executionLogCallback.saveExecutionLog(format("   %s%s%s: %d%%", controllerPrefix, DASH, rev, weight));
      }
    } else {
      executionLogCallback.saveExecutionLog("   None specified");
    }
  }

  /**
   * This is copied version of io.fabric8.kubernetes.client.utils.HttpClientUtils.createHttpClient()
   * with 1 addition, setting NO_PROXY flag on OkHttpClient if applicable.
   *
   * Once kubernetes library is updated to provide this support, we should get rid of this and
   * use DefaultKubernetesClient(config) constructor version as it internally call
   * super(createHttpClient(config), config)
   */
  @SuppressFBWarnings({"NP_LOAD_OF_KNOWN_NULL_VALUE", "NP_ALWAYS_NULL"})
  private OkHttpClient createHttpClientWithProxySetting(final Config config) {
    try {
      OkHttpClient.Builder httpClientBuilder = getOkHttpClientBuilder();
      httpClientBuilder.proxy(Http.checkAndGetNonProxyIfApplicable(config.getMasterUrl()));

      // Follow any redirects
      httpClientBuilder.followRedirects(true);
      httpClientBuilder.followSslRedirects(true);

      if (config.isTrustCerts()) {
        httpClientBuilder.hostnameVerifier(new HostnameVerifier() {
          @Override
          public boolean verify(String s, SSLSession sslSession) {
            return true;
          }
        });
      }

      TrustManager[] trustManagers = SSLUtils.trustManagers(config);
      KeyManager[] keyManagers = SSLUtils.keyManagers(config);

      if (keyManagers != null || trustManagers != null || config.isTrustCerts()) {
        X509TrustManager trustManager = null;
        if (trustManagers != null && trustManagers.length == 1) {
          trustManager = (X509TrustManager) trustManagers[0];
        }

        try {
          SSLContext sslContext = SSLUtils.sslContext(keyManagers, trustManagers, config.isTrustCerts());
          httpClientBuilder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
        } catch (GeneralSecurityException e) {
          throw new AssertionError(); // The system has no TLS. Just give up.
        }
      } else {
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(keyManagers, trustManagers, null);
        httpClientBuilder.sslSocketFactory(context.getSocketFactory(), (X509TrustManager) trustManagers[0]);
      }

      httpClientBuilder.addInterceptor(new Interceptor() {
        @Override
        public Response intercept(Chain chain) throws IOException {
          Request request = chain.request();
          if (isNotNullOrEmpty(config.getUsername()) && isNotNullOrEmpty(config.getPassword())) {
            Request authReq =
                chain.request()
                    .newBuilder()
                    .addHeader("Authorization", Credentials.basic(config.getUsername(), config.getPassword()))
                    .build();
            return chain.proceed(authReq);
          } else if (isNotNullOrEmpty(config.getOauthToken())) {
            Request authReq =
                chain.request().newBuilder().addHeader("Authorization", "Bearer " + config.getOauthToken()).build();
            return chain.proceed(authReq);
          }
          return chain.proceed(request);
        }
      });

      Logger reqLogger = LoggerFactory.getLogger(HttpLoggingInterceptor.class);
      if (reqLogger.isTraceEnabled()) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        httpClientBuilder.addNetworkInterceptor(loggingInterceptor);
      }

      if (config.getConnectionTimeout() > 0) {
        httpClientBuilder.connectTimeout(config.getConnectionTimeout(), TimeUnit.MILLISECONDS);
      }

      if (config.getRequestTimeout() > 0) {
        httpClientBuilder.readTimeout(config.getRequestTimeout(), TimeUnit.MILLISECONDS);
      }

      if (config.getWebsocketPingInterval() > 0) {
        httpClientBuilder.pingInterval(config.getWebsocketPingInterval(), TimeUnit.MILLISECONDS);
      }

      if (config.getMaxConcurrentRequestsPerHost() > 0) {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequestsPerHost(config.getMaxConcurrentRequestsPerHost());
        httpClientBuilder.dispatcher(dispatcher);
      }

      // Only check proxy if it's a full URL with protocol
      if (config.getMasterUrl().toLowerCase().startsWith(Config.HTTP_PROTOCOL_PREFIX)
          || config.getMasterUrl().startsWith(Config.HTTPS_PROTOCOL_PREFIX)) {
        try {
          URL proxyUrl = getProxyUrl(config);
          if (proxyUrl != null) {
            httpClientBuilder.proxy(
                new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyUrl.getHost(), proxyUrl.getPort())));

            if (config.getProxyUsername() != null) {
              httpClientBuilder.proxyAuthenticator(new Authenticator() {
                @Override
                public Request authenticate(Route route, Response response) throws IOException {
                  String credential = Credentials.basic(config.getProxyUsername(), config.getProxyPassword());
                  return response.request().newBuilder().header("Proxy-Authorization", credential).build();
                }
              });
            }
          }

        } catch (MalformedURLException e) {
          throw new KubernetesClientException("Invalid proxy server configuration", e);
        }
      }

      if (isNotEmpty(config.getUserAgent())) {
        httpClientBuilder.addNetworkInterceptor(new Interceptor() {
          @Override
          public Response intercept(Chain chain) throws IOException {
            Request agent = chain.request().newBuilder().header("User-Agent", config.getUserAgent()).build();
            return chain.proceed(agent);
          }
        });
      }

      if (config.getTlsVersions() != null && config.getTlsVersions().length > 0) {
        ConnectionSpec spec =
            new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).tlsVersions(config.getTlsVersions()).build();
        httpClientBuilder.connectionSpecs(asList(spec, CLEARTEXT));
      }

      return httpClientBuilder.build();
    } catch (RuntimeException | CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException
        | UnrecoverableKeyException | InvalidKeySpecException | KeyManagementException e) {
      throw KubernetesClientException.launderThrowable(e);
    }
  }

  private URL getProxyUrl(Config config) throws MalformedURLException {
    URL master = new URL(config.getMasterUrl());
    String host = master.getHost();
    if (config.getNoProxy() != null) {
      for (String noProxy : config.getNoProxy()) {
        if (host.endsWith(noProxy)) {
          return null;
        }
      }
    }
    String proxy = config.getHttpsProxy();
    if (master.getProtocol().equals("http")) {
      proxy = config.getHttpProxy();
    }
    if (proxy != null) {
      return new URL(proxy);
    }
    return null;
  }

  private String encode(char[] value) {
    String encodedValue = new String(value).trim();
    if (isNotBlank(encodedValue) && encodedValue.startsWith("-----BEGIN ")) {
      encodedValue = encodeBase64(encodedValue);
    }
    return encodedValue;
  }

  public static String toYaml(Object entity) throws JsonProcessingException {
    return new ObjectMapper(new YAMLFactory().configure(WRITE_DOC_START_MARKER, false))
        .setSerializationInclusion(NON_EMPTY)
        .writeValueAsString(entity);
  }

  public static String toDisplayYaml(Object entity) {
    Yaml yaml = new Yaml(new YamlRepresenter(true), YamlHelper.getDumperOptions());
    return YamlHelper.cleanupYaml(yaml.dump(entity));
  }

  public NonNamespaceOperation<HorizontalPodAutoscaler, HorizontalPodAutoscalerList, DoneableHorizontalPodAutoscaler,
      Resource<HorizontalPodAutoscaler, DoneableHorizontalPodAutoscaler>>
  hpaOperationsForCustomMetricHPA(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String apiName) {
    DefaultKubernetesClient kubernetesClient =
        (DefaultKubernetesClient) getKubernetesClient(kubernetesConfig, encryptedDataDetails, apiName);

    /*
     * Following constructor invocation content is copied from HorizontalPodAutoscalerOperationsImpl(OkHttpClient
     * client, Config config, String namespace){...}, except we are passing apiName, where as in above mentioned one its
     * hardcoded as "v1".
     *
     * Following call does exactly what
     * getKubernetesClient(kubernetesConfig,encryptedDataDetails).autoscaling().horizontalPodAutoscalers()) does, Only
     * diff is, here its based on apiVersion we passed. So for "v2beta1" version, we needed to take this approach, as
     * there was this issue with fabric8 library, that
     * getKubernetesClient(kubernetesConfig,encryptedDataDetails).autoscaling().horizontalPodAutoscalers()) always
     * returns client with "v1" apiVersion.
     * */
    MixedOperation<HorizontalPodAutoscaler, HorizontalPodAutoscalerList, DoneableHorizontalPodAutoscaler,
        Resource<HorizontalPodAutoscaler, DoneableHorizontalPodAutoscaler>> mixedOperation =
        new HorizontalPodAutoscalerOperationsImpl(kubernetesClient.getHttpClient(), kubernetesClient.getConfiguration(),
            apiName, kubernetesClient.getNamespace(), null, true, null, null, false, -1, new TreeMap<String, String>(),
            new TreeMap<String, String>(), new TreeMap<String, String[]>(), new TreeMap<String, String[]>(),
            new TreeMap<String, String>());

    return mixedOperation.inNamespace(kubernetesConfig.getNamespace());
  }

  public NonNamespaceOperation<HorizontalPodAutoscaler, HorizontalPodAutoscalerList, DoneableHorizontalPodAutoscaler,
      Resource<HorizontalPodAutoscaler, DoneableHorizontalPodAutoscaler>>
  hpaOperations(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    return getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .autoscaling()
        .horizontalPodAutoscalers()
        .inNamespace(kubernetesConfig.getNamespace());
  }

  /**
   * Separates apiVersion for apiGroup/apiVersion combination.
   * @param apiVersion  The apiVersion or apiGroup/apiVersion combo.
   * @return            Just the apiVersion part without the apiGroup.
   */
  public String trimVersion(String apiVersion) {
    if (apiVersion == null) {
      return null;
    } else {
      String[] versionParts = apiVersion.split("/");
      return versionParts[versionParts.length - 1];
    }
  }
}
