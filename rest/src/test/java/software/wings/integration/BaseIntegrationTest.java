package software.wings.integration;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import com.google.inject.Inject;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.internal.MultiPartWriter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.security.SecretManagementDelegateServiceImpl;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.JsonSubtypeResolver;
import software.wings.utils.WingsIntegrationTestConstants;

import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;

/**
 * Created by rsingh on 4/24/17.
 */
public abstract class BaseIntegrationTest extends WingsBaseTest implements WingsIntegrationTestConstants {
  protected static Client client;
  protected String accountId;

  protected static final Logger logger = LoggerFactory.getLogger(BaseIntegrationTest.class);

  @Inject protected UserResourceRestClient userResourceRestClient;
  @Inject protected WingsPersistence wingsPersistence;
  @Inject protected SettingsService settingsService;
  @Inject protected KmsService kmsService;
  @Inject protected SecretManager secretManager;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  //  @Rule public ThreadDumpRule threadDumpRule = new ThreadDumpRule();

  @BeforeClass
  public static void setup() throws KeyManagementException, NoSuchAlgorithmException {
    ClientConfig config = new ClientConfig(new JacksonJsonProvider().configure(FAIL_ON_UNKNOWN_PROPERTIES, false));
    config.register(MultiPartWriter.class);
    SSLContext sslcontext = SSLContext.getInstance("TLS");
    X509TrustManager x509TrustManager = new X509TrustManager() {
      public void checkClientTrusted(X509Certificate[] arg0, String arg1) {}

      public void checkServerTrusted(X509Certificate[] arg0, String arg1) {}

      public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
      }
    };
    sslcontext.init(null, new TrustManager[] {x509TrustManager}, new java.security.SecureRandom());

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.setSubtypeResolver(new JsonSubtypeResolver(objectMapper.getSubtypeResolver()));
    JacksonJaxbJsonProvider jacksonProvider = new JacksonJaxbJsonProvider();
    jacksonProvider.setMapper(objectMapper);

    client = ClientBuilder.newBuilder()
                 .sslContext(sslcontext)
                 .hostnameVerifier((s1, s2) -> true)
                 .register(MultiPartFeature.class)
                 .register(jacksonProvider)
                 .build();
  }

  @Before
  public void setUp() throws Exception {
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class)))
        .thenReturn(new SecretManagementDelegateServiceImpl());
    setInternalState(kmsService, "delegateProxyFactory", delegateProxyFactory);
    setInternalState(secretManager, "kmsService", kmsService);
    setInternalState(wingsPersistence, "secretManager", secretManager);
    accountId = userResourceRestClient.getSeedAccount(client).getUuid();
  }

  protected Builder getRequestBuilderWithAuthHeader(WebTarget target) {
    return target.request().header("Authorization", "Bearer " + userResourceRestClient.getUserToken(client));
  }

  protected Builder getDelegateRequestBuilderWithAuthHeader(WebTarget target) throws UnknownHostException {
    return target.request().header("Authorization", "Delegate " + userResourceRestClient.getDelegateToken(client));
  }

  protected Builder getRequestBuilder(WebTarget target) {
    return target.request();
  }

  // TODO: remove this ASAP - deleteAllDocuments should not be called blindly
  protected void deleteAllDocuments(List<Class> classes) {
    classes.forEach(cls -> wingsPersistence.getDatastore().delete(wingsPersistence.createQuery(cls)));
  }

  protected void loginAdminUser() {
    userResourceRestClient.getUserToken(client);
  }
}
