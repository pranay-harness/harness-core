package io.harness;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.network.Localhost.getLocalHostName;
import static java.lang.String.format;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;

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
import io.harness.exception.WingsException;
import io.harness.security.VerificationTokenGenerator;
import io.harness.service.intfc.LearningEngineService;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.internal.MultiPartWriter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.RestResponse;
import software.wings.beans.ServiceSecretKey;
import software.wings.beans.ServiceSecretKey.ServiceType;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.utils.JsonSubtypeResolver;
import software.wings.utils.WingsIntegrationTestConstants;

import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Date;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Created by rsingh on 9/25/18.
 */
public abstract class VerificationBaseIntegrationTest
    extends VerificationBaseTest implements WingsIntegrationTestConstants {
  public static String VERIFICATION_API_BASE = "https://localhost:7070/verification";

  protected static final Logger logger = LoggerFactory.getLogger(VerificationBaseIntegrationTest.class);
  protected static Client client;

  protected String accountId = "INVALID_ID";
  protected String userToken = "INVALID_TOKEN";
  @Inject protected WingsPersistence wingsPersistence;
  @Inject protected LearningEngineService learningEngineService;

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
    System.setProperty(VerificationTokenGenerator.VERIFICATION_SERVICE_SECRET,
        learningEngineService.getServiceSecretKey(ServiceType.LEARNING_ENGINE));
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

  protected Builder getRequestBuilderWithLearningAuthHeader(WebTarget target) {
    return target.request().header("Authorization", "LearningEngine " + getLearningToken());
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

  protected Builder getDelegateRequestBuilderWithAuthHeader(WebTarget target) {
    return target.request().header("Authorization", "Delegate " + getDelegateToken());
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

  protected Application createApp(String appName) {
    WebTarget target = client.target(API_BASE + "/apps?accountId=" + accountId);
    Application app = anApplication().withName(appName).withDescription(appName).withAccountId(accountId).build();
    Response response = getRequestBuilderWithAuthHeader(target).post(entity(app, APPLICATION_JSON));
    if (response.getStatus() != Status.OK.getStatusCode()) {
      log().error("Non-ok-status. Headers: {}", response.getHeaders());
    }
    assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
    RestResponse<Application> restResponse = response.readEntity(new GenericType<RestResponse<Application>>() {});

    assertThat(restResponse.getResource().getName()).isEqualTo(appName);
    return restResponse.getResource();
  }
}
