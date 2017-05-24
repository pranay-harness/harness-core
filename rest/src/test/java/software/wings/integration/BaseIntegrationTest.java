package software.wings.integration;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.License.Builder.aLicense;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoCommandException;
import org.apache.commons.lang.StringUtils;
import org.assertj.core.util.Lists;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.internal.MultiPartWriter;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.License;
import software.wings.beans.RestResponse;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SettingsService;
import software.wings.utils.JsonSubtypeResolver;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
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
  protected static final String adminUserName = "admin@wings.software";
  protected static final char[] adminPassword = "admin".toCharArray();

  protected static String accountId = "INVALID_ID";
  protected static String userToken = "INVALID_TOKEN";

  protected final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject protected WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  @Inject protected SettingsService settingsService;

  @Rule public ThreadDumpRule threadDumpRule = new ThreadDumpRule();

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
          BasicDBObject keys = new BasicDBObject();
          Arrays.stream(index.fields()).forEach(field -> keys.append(field.value(), 1));
          primaryDatastore.getCollection(mc.getClazz())
              .createIndex(keys, index.options().name(), index.unique() || index.options().unique());
        });
      }

      // Read field level "Indexed" annotation
      for (final MappedField mf : mc.getPersistenceFields()) {
        if (mf.hasAnnotation(Indexed.class)) {
          final Indexed indexed = mf.getAnnotation(Indexed.class);
          try {
            primaryDatastore.getCollection(mc.getClazz())
                .createIndex(new BasicDBObject().append(mf.getNameToStore(), 1), null,
                    indexed.unique() || indexed.options().unique());
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
      Entity.entity(
          anUser()
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
              .withAccountName("Wings Software")
              .withCompanyName("Wings Software")
              .build(),
          APPLICATION_JSON),
      new GenericType<RestResponse<User>>() {});
  assertThat(response.getResource()).isInstanceOf(User.class);
  wingsPersistence.updateFields(User.class, response.getResource().getUuid(), ImmutableMap.of("emailVerified", true));
}
}
