package software.wings.integration;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.lang.String.format;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.License.Builder.aLicense;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.integration.IntegrationTestUtil.randomInt;
import static software.wings.integration.SeedData.randomSeedString;
import static software.wings.service.KmsTest.getKmsConfig;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoCommandException;
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
import org.apache.commons.lang.StringUtils;
import org.assertj.core.util.Lists;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.internal.MultiPartWriter;
import org.junit.BeforeClass;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.app.DatabaseModule;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureName;
import software.wings.beans.KmsConfig;
import software.wings.beans.License;
import software.wings.beans.RestResponse;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.SettingsService;
import software.wings.utils.JsonSubtypeResolver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
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
public abstract class BaseIntegrationTest extends WingsBaseTest {
  protected static Client client;
  protected static final String API_BASE = StringUtils.isBlank(System.getenv().get("BASE_HTTP"))
      ? "https://localhost:9090/api"
      : "http://localhost:9090/api";
  protected static final String adminUserName = "admin@harness.io";
  protected static final char[] adminPassword = "admin".toCharArray();
  protected static final String delegateAccountSecret = "2f6b0988b6fb3370073c3d0505baee59";

  protected static String accountId = "INVALID_ID";
  protected static String userToken = "INVALID_TOKEN";

  protected static final Logger logger = LoggerFactory.getLogger(BaseIntegrationTest.class);
  @Inject protected WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  @Inject protected SettingsService settingsService;
  @Inject protected AppService appService;

  protected static final char[] JENKINS_PASSWORD = "admin".toCharArray();
  protected static final String JENKINS_URL = "http://ec2-34-207-79-21.compute-1.amazonaws.com:8080/";
  protected static final String JENKINS_USERNAME = "admin";
  protected static final String NEXUS_URL = "https://nexus.wings.software";
  protected static final String NEXUS_USERNAME = "admin";
  protected static final char[] NEXUS_PASSWORD = "wings123!".toCharArray();
  protected static final String BAMBOO_URL = "http://ec2-34-205-16-35.compute-1.amazonaws.com:8085/";
  protected static final String BAMBOO_USERNAME = "wingsbuild";
  protected static final char[] BAMBOO_PASSWORD = "0db28aa0f4fc0685df9a216fc7af0ca96254b7c2".toCharArray();
  protected static final String DOCKER_REGISTRY_URL = "https://registry.hub.docker.com/v2/";
  protected static final String DOCKER_USERNAME = "wingsplugins";
  protected static final char[] DOCKER_PASSOWRD = "W!ngs@DockerHub".toCharArray();
  public static final String HARNESS_JENKINS = "Harness Jenkins";
  public static final String HARNESS_KMS = "Harness KMS";

  //  @Rule public ThreadDumpRule threadDumpRule = new ThreadDumpRule();

  @BeforeClass
  public static void setup() throws KeyManagementException, NoSuchAlgorithmException {
    ClientConfig config = new ClientConfig(new JacksonJsonProvider().configure(FAIL_ON_UNKNOWN_PROPERTIES, false));
    config.register(MultiPartWriter.class);
    SSLContext sslcontext = SSLContext.getInstance("TLS");
    sslcontext.init(null, new TrustManager[] {new X509TrustManager() {
      public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
      }

      public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
      }

      public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
  }
}
}, new java.security.SecureRandom());

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
  loginUser(adminUserName, new String(adminPassword));
}

protected Builder getRequestBuilderWithAuthHeader(WebTarget target) {
  return target.request().header("Authorization", "Bearer " + userToken);
}

protected Builder getDelegateRequestBuilderWithAuthHeader(WebTarget target) throws UnknownHostException {
  return target.request().header("Authorization", "Delegate " + getDelegateToken());
}

protected Builder getRequestBuilder(WebTarget target) {
  return target.request();
}

protected void dropDBAndEnsureIndexes() throws IOException, ClassNotFoundException {
  wingsPersistence.getDatastore().getDB().dropDatabase();
  Morphia morphia = new Morphia();
  morphia.getMapper().getOptions().setMapSubPackages(true);
  morphia.mapPackage("software.wings");
  ensureIndex(morphia, wingsPersistence.getDatastore());
}

protected void deleteAllDocuments(List<Class> classes) {
  classes.forEach(cls -> wingsPersistence.getDatastore().delete(wingsPersistence.createQuery(cls)));
}

protected void ensureIndex(Morphia morphia, Datastore primaryDatastore) {
  /*
  Morphia auto creates embedded/nested Entity indexes with the parent Entity indexes.
  There is no way to override this behavior.
  https://github.com/mongodb/morphia/issues/706
   */

  morphia.getMapper().getMappedClasses().forEach(mc -> {
    if (mc.getEntityAnnotation() != null && !mc.isAbstract()) {
      // Read Entity level "Indexes" annotation
      List<Indexes> indexesAnnotations = mc.getAnnotations(Indexes.class);
      if (indexesAnnotations != null) {
        indexesAnnotations.stream().flatMap(indexes -> Arrays.stream(indexes.value())).forEach(index -> {
          DatabaseModule.reportDeprecatedUnique(index);

          BasicDBObject keys = new BasicDBObject();
          Arrays.stream(index.fields()).forEach(field -> keys.append(field.value(), 1));
          primaryDatastore.getCollection(mc.getClazz())
              .createIndex(keys, index.options().name(), index.options().unique());
        });
      }

      // Read field level "Indexed" annotation
      for (final MappedField mf : mc.getPersistenceFields()) {
        if (mf.hasAnnotation(Indexed.class)) {
          final Indexed indexed = mf.getAnnotation(Indexed.class);
          DatabaseModule.reportDeprecatedUnique(indexed);

          try {
            primaryDatastore.getCollection(mc.getClazz())
                .createIndex(new BasicDBObject().append(mf.getNameToStore(), 1), null, indexed.options().unique());
          } catch (MongoCommandException mex) {
            logger.error("Index creation failed for class {}", mc.getClazz().getCanonicalName());
            throw mex;
          }
        }
      }
    }
  });
}

protected void createLicenseAndDefaultUser() {
  License license =
      aLicense().withName("Trial").withExpiryDuration(TimeUnit.DAYS.toMillis(365)).withIsActive(true).build();
  wingsPersistence.save(license);
  addAdminUser();
  Account account = wingsPersistence.executeGetOneQuery(wingsPersistence.createQuery(Account.class));
  String oldAccountId = account.getUuid();
  String accountKey = "2f6b0988b6fb3370073c3d0505baee59";
  account.setAccountKey(accountKey);
  account.setLicenseExpiryTime(-1);

  account.setUuid("kmpySmUISimoRrJL6NL73w");
  accountId = "kmpySmUISimoRrJL6NL73w";
  accountService.delete(oldAccountId);
  accountService.save(account);
  // wingsPersistence.save(account);
  // Update account key to make delegate works
  UpdateOperations<Account> accountUpdateOperations = wingsPersistence.createUpdateOperations(Account.class);
  accountUpdateOperations.set("accountKey", accountKey);
  wingsPersistence.update(wingsPersistence.createQuery(Account.class), accountUpdateOperations);

  UpdateOperations<User> userUpdateOperations = wingsPersistence.createUpdateOperations(User.class);
  userUpdateOperations.set("accounts", Lists.newArrayList(account));
  wingsPersistence.update(wingsPersistence.createQuery(User.class), userUpdateOperations);

  UpdateOperations<Role> roleUpdateOperations = wingsPersistence.createUpdateOperations(Role.class);
  roleUpdateOperations.set("accountId", "kmpySmUISimoRrJL6NL73w");
  wingsPersistence.update(wingsPersistence.createQuery(Role.class), roleUpdateOperations);
  loginAdminUser();
}

private void addAdminUser() {
  WebTarget target = client.target(API_BASE + "/users/");
  RestResponse<User> response = target.request().post(
      entity(anUser()
                 .withName("Admin")
                 .withEmail(adminUserName)
                 .withPassword(adminPassword)
                 .withRoles(
                     wingsPersistence
                         .query(Role.class,
                             aPageRequest()
                                 .addFilter(
                                     aSearchFilter().withField("roleType", Operator.EQ, RoleType.ACCOUNT_ADMIN).build())
                                 .build())
                         .getResponse())
                 .withAccountName("Harness Inc")
                 .withCompanyName("Harness Inc")
                 .build(),
          APPLICATION_JSON),
      new GenericType<RestResponse<User>>() {});
  assertThat(response.getResource()).isInstanceOf(User.class);
  wingsPersistence.updateFields(User.class, response.getResource().getUuid(), ImmutableMap.of("emailVerified", true));
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

public String getDelegateToken() throws UnknownHostException {
  JWTClaimsSet jwtClaims = new JWTClaimsSet.Builder()
                               .issuer(InetAddress.getLocalHost().getHostName())
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
    e.printStackTrace();
  }
  try {
    directEncrypter = new DirectEncrypter(new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES"));
  } catch (KeyLengthException e) {
    e.printStackTrace();
  }

  try {
    jwt.encrypt(directEncrypter);
  } catch (JOSEException e) {
    e.printStackTrace();
  }

  return jwt.serialize();
}
protected void enableKmsFeatureFlag() {
  KmsConfig kmsConfig = getKmsConfig();
  kmsConfig.setAccountId(Base.GLOBAL_ACCOUNT_ID);

  WebTarget target = client.target(API_BASE + "/kms/save-global-kms?accountId=" + accountId);
  RestResponse<String> response = getRequestBuilderWithAuthHeader(target).post(
      entity(kmsConfig, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});

  assertNotNull(response.getResource());
  FeatureFlag kmsFeatureFlag =
      wingsPersistence.createQuery(FeatureFlag.class).field("name").equal(FeatureName.KMS.name()).get();
  kmsFeatureFlag.setEnabled(true);
  wingsPersistence.save(kmsFeatureFlag);
}
}
