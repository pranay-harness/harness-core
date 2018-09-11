package software.wings.integration;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.network.Localhost.getLocalHostName;
import static java.lang.String.format;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.integration.IntegrationTestUtil.randomInt;
import static software.wings.integration.SeedData.randomSeedString;

import com.google.inject.Inject;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.internal.MultiPartWriter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.RestResponse;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.ServiceSecretKey;
import software.wings.beans.ServiceSecretKey.ServiceType;
import software.wings.beans.User;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.impl.security.SecretManagementDelegateServiceImpl;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.JsonSubtypeResolver;
import software.wings.utils.WingsIntegrationTestConstants;

import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by rsingh on 4/24/17.
 */
public abstract class BaseIntegrationTest extends WingsBaseTest implements WingsIntegrationTestConstants {
  protected static Client client;

  protected static final Logger logger = LoggerFactory.getLogger(BaseIntegrationTest.class);
  @Inject protected WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  @Inject protected SettingsService settingsService;
  @Inject protected AppService appService;
  @Inject protected LearningEngineService learningEngineService;
  @Inject protected KmsService kmsService;
  @Inject protected SecretManager secretManager;
  @Inject protected AuthHandler authHandler;
  @Mock private DelegateProxyFactory delegateProxyFactory;

  protected String accountId = "INVALID_ID";
  protected String userToken = "INVALID_TOKEN";
  protected final int TIMES_TO_REPEAT = 3;
  protected final int SUCCESS_COUNT = 1;

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
    //    setInternalState(wingsPersistence, "featureFlagService", featureFlagService);
  }

  protected String loginUser(final String userName, final String password) {
    String basicAuthValue = "Basic " + encodeBase64String(format("%s:%s", userName, password).getBytes());
    RestResponse<User> response;
    response = client.target(API_BASE + "/users/login")
                   .request()
                   .header("Authorization", basicAuthValue)
                   .get(new GenericType<RestResponse<User>>() {});
    if (response.getResource() != null) {
      User loggedInUser = response.getResource();
      userToken = loggedInUser.getToken();
      accountId = loggedInUser.getAccounts().get(0).getUuid();
    }
    return userToken;
  }

  protected void loginAdminUser() {
    loginUser(adminUserEmail, new String(adminPassword));
  }

  protected Builder getRequestBuilderWithAuthHeader(WebTarget target) {
    return target.request().header("Authorization", "Bearer " + userToken);
  }

  protected Builder getDelegateRequestBuilderWithAuthHeader(WebTarget target) throws UnknownHostException {
    return target.request().header("Authorization", "Delegate " + getDelegateToken());
  }

  protected Builder getRequestBuilderWithLearningAuthHeader(WebTarget target) throws UnknownHostException {
    return target.request().header("Authorization", "LearningEngine " + getLearningToken());
  }

  protected Builder getRequestBuilder(WebTarget target) {
    return target.request();
  }

  protected void deleteAllDocuments(List<Class> classes) {
    classes.forEach(cls -> wingsPersistence.getDatastore().delete(wingsPersistence.createQuery(cls)));
  }

  protected Application createApp(String appName) {
    WebTarget target = client.target(API_BASE + "/apps?accountId=" + accountId);
    RestResponse<Application> response = getRequestBuilderWithAuthHeader(target).post(
        entity(anApplication().withName(appName).withDescription(appName).withAccountId(accountId).build(),
            APPLICATION_JSON),
        new GenericType<RestResponse<Application>>() {});
    assertThat(response.getResource()).isInstanceOf(Application.class);
    assertThat(response.getResource().getName()).isEqualTo(appName);
    return response.getResource();
  }

  protected Service createService(String appId, Map<String, Object> serviceMap) {
    WebTarget target = client.target(API_BASE + "/services/?appId=" + appId);

    RestResponse<Service> response = getRequestBuilderWithAuthHeader(target).post(
        entity(serviceMap, APPLICATION_JSON), new GenericType<RestResponse<Service>>() {});
    assertThat(response.getResource()).isInstanceOf(Service.class);
    String serviceId = response.getResource().getUuid();
    Service service = wingsPersistence.get(Service.class, serviceId);
    assertThat(service).isNotNull();
    assertThat(service.getUuid()).isNotNull();
    return service;
  }

  protected void deleteApp(String appId) {
    WebTarget target = client.target(API_BASE + "/artifactstreams/?appId=" + appId);
    RestResponse response = getRequestBuilderWithAuthHeader(target).delete(new GenericType<RestResponse>() {

    });
    assertThat(response).isNotNull();
  }

  protected void deleteService(String serviceId) {
    WebTarget target = client.target(API_BASE + "/services/?appId=" + serviceId);
    RestResponse response = getRequestBuilderWithAuthHeader(target).delete(new GenericType<RestResponse>() {

    });
    assertThat(response).isNotNull();
  }

  protected String randomText(int length) { // TODO: choose words start to word end boundary
    int low = randomInt(50);
    int high = length + low > randomSeedString.length() ? randomSeedString.length() - low : length + low;
    return randomSeedString.substring(low, high);
  }

  public String getDelegateToken() {
    JWTClaimsSet jwtClaims = new JWTClaimsSet.Builder()
                                 .issuer(getLocalHostName())
                                 .subject(accountId)
                                 .audience("https://localhost:9090")
                                 .expirationTime(new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5)))
                                 .notBeforeTime(new Date())
                                 .issueTime(new Date())
                                 .jwtID(UUID.randomUUID().toString())
                                 .build();

    JWEHeader header = new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A128GCM);
    EncryptedJWT jwt = new EncryptedJWT(header, jwtClaims);
    DirectEncrypter directEncrypter = null;
    byte[] encodedKey = new byte[0];
    try {
      encodedKey = Hex.decodeHex(delegateAccountSecret.toCharArray());
    } catch (DecoderException e) {
      logger.error("", e);
    }
    try {
      directEncrypter = new DirectEncrypter(new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES"));
    } catch (KeyLengthException e) {
      logger.error("", e);
    }

    try {
      jwt.encrypt(directEncrypter);
    } catch (JOSEException e) {
      logger.error("", e);
    }

    return jwt.serialize();
  }

  protected void signupAndLogin(String email, String pwd) throws InterruptedException {
    final String name = generateUuid();
    final String accountName = generateUuid();
    final String companyName = generateUuid();
    logger.info("signing up with email {} pwd {} name {} accounName {} companyName {}", email, pwd, name, accountName,
        companyName);
    WebTarget target = client.target(API_BASE + "/users");
    RestResponse<User> response = target.request().post(
        entity(anUser()
                   .withName(name)
                   .withEmail(email)
                   .withPassword(pwd.toCharArray())
                   .withRoles(wingsPersistence
                                  .query(Role.class,
                                      aPageRequest().addFilter("roleType", Operator.EQ, RoleType.ACCOUNT_ADMIN).build())
                                  .getResponse())
                   .withAccountName(accountName)
                   .withCompanyName(companyName)
                   .build(),
            APPLICATION_JSON),
        new GenericType<RestResponse<User>>() {});
    assertEquals(0, response.getResponseMessages().size());
    wingsPersistence.update(wingsPersistence.createQuery(User.class).filter("email", email),
        wingsPersistence.createUpdateOperations(User.class).set("emailVerified", true));
    Thread.sleep(2000);
    loginUser(email, pwd);
  }

  private String getLearningToken() {
    try {
      String learningServiceSecret = wingsPersistence.createQuery(ServiceSecretKey.class)
                                         .filter("serviceType", ServiceType.LEARNING_ENGINE)
                                         .get()
                                         .getServiceSecret();
      logger.info("learningServiceSecret: " + learningServiceSecret);
      Algorithm algorithm = Algorithm.HMAC256(learningServiceSecret);
      return JWT.create()
          .withIssuer("Harness Inc")
          .withIssuedAt(new Date())
          .withExpiresAt(new Date(System.currentTimeMillis() + 4 * 60 * 60 * 1000)) // 4 hrs
          .sign(algorithm);
    } catch (UnsupportedEncodingException | JWTCreationException exception) {
      throw new WingsException(GENERAL_ERROR).addParam("message", "reset password link could not be generated");
    }
  }
}
