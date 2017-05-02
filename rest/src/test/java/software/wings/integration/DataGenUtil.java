package software.wings.integration;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.JenkinsConfig.Builder.aJenkinsConfig;
import static software.wings.beans.License.Builder.aLicense;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.ServiceVariable.Builder.aServiceVariable;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.SplunkConfig.Builder.aSplunkConfig;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.beans.config.NexusConfig.Builder.aNexusConfig;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.helpers.ext.mail.SmtpConfig.Builder.aSmtpConfig;
import static software.wings.integration.IntegrationTestUtil.randomInt;
import static software.wings.integration.SeedData.containerNames;
import static software.wings.integration.SeedData.envNames;
import static software.wings.integration.SeedData.randomSeedString;
import static software.wings.integration.SeedData.seedNames;
import static software.wings.utils.ArtifactType.WAR;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoCommandException;
import org.assertj.core.util.Lists;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.glassfish.jersey.media.multipart.internal.MultiPartWriter;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
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
import software.wings.beans.AppContainer;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Application;
import software.wings.beans.BambooConfig;
import software.wings.beans.Base;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.License;
import software.wings.beans.RestResponse;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.User;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.utils.JsonSubtypeResolver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import javax.ws.rs.core.Response;

/**
 * Created by anubhaw on 5/6/16.
 */
@Integration
public class DataGenUtil extends BaseIntegrationTest {
  private static final int NUM_APPS = 1; /* Max 1000 */
  private static final int NUM_APP_CONTAINER_PER_APP = 2; /* Max 1000 */
  private static final int NUM_SERVICES_PER_APP = 1; /* Max 1000 */
  private static final int NUM_CONFIG_FILE_PER_SERVICE = 2; /* Max 100  */
  private static final int NUM_ENV_PER_APP = 0; /* Max 6. 4 are created by default */
  private static final int NUM_HOSTS_PER_INFRA = 5; /* No limit */
  private static final int NUM_TAG_GROUPS_PER_ENV = 3; /* Max 10   */
  private static final int TAG_HIERARCHY_DEPTH = 3; /* Max 10   */

  /**
   * The Test folder.
   */
  @Rule public TemporaryFolder testFolder = new TemporaryFolder();

  private List<String> appNames = new ArrayList<>(seedNames);
  private List<String> serviceNames;
  private List<String> configFileNames;
  private SettingAttribute envAttr = null;
  @Inject private WorkflowService workflowService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private AccountService accountService;

  /**
   * Generated Data for across the API use.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    assertThat(NUM_APPS).isBetween(1, 1000);
    assertThat(NUM_APP_CONTAINER_PER_APP).isBetween(1, 1000);
    assertThat(NUM_SERVICES_PER_APP).isBetween(1, 1000);
    assertThat(NUM_ENV_PER_APP).isBetween(0, 10);
    assertThat(NUM_TAG_GROUPS_PER_ENV).isBetween(1, 10);
    assertThat(TAG_HIERARCHY_DEPTH).isBetween(1, 10);

    dropDBAndEnsureIndexes();
  }

  /**
   * Populate data.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  public void populateData() throws IOException {
    createLicenseAndDefaultUser();
    createGlobalSettings();

    List<Application> apps = createApplications();
    Map<String, List<AppContainer>> containers = new HashMap<>();
    Map<String, List<Service>> services = new HashMap<>();
    Map<String, List<Environment>> appEnvs = new HashMap<>();

    //    containers.put(GLOBAL_APP_ID, addAppContainers(GLOBAL_APP_ID)); // TODO:: upload Real Tomcat and Jboss server.

    for (Application application : apps) {
      appEnvs.put(application.getUuid(), addEnvs(application.getUuid()));
      containers.put(application.getUuid(), addAppContainers(application.getUuid()));
      services.put(application.getUuid(), addServices(application.getUuid(), containers.get(GLOBAL_APP_ID)));
      createAppSettings(application.getAccountId());
    }
  }

  private Builder getRequestWithAuthHeader(WebTarget target) {
    return target.request().header("Authorization", "Bearer " + userToken);
  }

  private void createGlobalSettings() {
    WebTarget target = client.target(API_BASE + "/settings/?accountId=" + accountId);
    getRequestWithAuthHeader(target).post(
        Entity.entity(aSettingAttribute()
                          .withName("Wings Jenkins")
                          .withCategory(Category.CONNECTOR)
                          .withAccountId(accountId)
                          .withValue(aJenkinsConfig()
                                         .withAccountId(accountId)
                                         .withJenkinsUrl("https://jenkins.wings.software")
                                         .withUsername("wingsbuild")
                                         .withPassword("0db28aa0f4fc0685df9a216fc7af0ca96254b7c3".toCharArray())
                                         .build())
                          .build(),
            APPLICATION_JSON),
        new GenericType<RestResponse<SettingAttribute>>() {});

    getRequestWithAuthHeader(target).post(Entity.entity(aSettingAttribute()
                                                            .withName("Wings Nexus")
                                                            .withCategory(Category.CONNECTOR)
                                                            .withAccountId(accountId)
                                                            .withValue(aNexusConfig()
                                                                           .withNexusUrl("https://nexus.wings.software")
                                                                           .withUsername("admin")
                                                                           .withPassword("wings123!")
                                                                           .build())
                                                            .build(),
                                              APPLICATION_JSON),
        new GenericType<RestResponse<SettingAttribute>>() {});

    getRequestWithAuthHeader(target).post(
        Entity.entity(aSettingAttribute()
                          .withName("Wings BambooService")
                          .withCategory(Category.CONNECTOR)
                          .withAccountId(accountId)
                          .withValue(BambooConfig.Builder.aBambooConfig()
                                         .withBamboosUrl("http://ec2-54-91-249-58.compute-1.amazonaws.com:8085/")
                                         .withUsername("wingsbuild")
                                         .withPassword("0db28aa0f4fc0685df9a216fc7af0ca96254b7c2")
                                         .build())
                          .build(),
            APPLICATION_JSON),
        new GenericType<RestResponse<SettingAttribute>>() {});

    getRequestWithAuthHeader(target).post(Entity.entity(aSettingAttribute()
                                                            .withCategory(Category.CONNECTOR)
                                                            .withName("SMTP")
                                                            .withAccountId(accountId)
                                                            .withValue(aSmtpConfig()
                                                                           .withFromAddress("wings_test@wings.software")
                                                                           .withUsername("wings_test@wings.software")
                                                                           .withHost("smtp.gmail.com")
                                                                           .withPassword("@wes0me@pp")
                                                                           .withPort(465)
                                                                           .withUseSSL(true)
                                                                           .build())
                                                            .build(),
                                              APPLICATION_JSON),
        new GenericType<RestResponse<SettingAttribute>>() {});
    getRequestWithAuthHeader(target).post(
        Entity.entity(aSettingAttribute()
                          .withCategory(Category.CONNECTOR)
                          .withName("Splunk")
                          .withAccountId(accountId)
                          .withValue(aSplunkConfig()
                                         .withHost("ec2-52-54-103-49.compute-1.amazonaws.com")
                                         .withPort(8089)
                                         .withPassword("W!ngs@Splunk")
                                         .withUsername("admin")
                                         .build())
                          .build(),
            APPLICATION_JSON),
        new GenericType<RestResponse<SettingAttribute>>() {});

    getRequestWithAuthHeader(target).post(
        Entity.entity(aSettingAttribute()
                          .withCategory(Category.CONNECTOR)
                          .withName("AppDynamics")
                          .withAccountId(accountId)
                          .withValue(AppDynamicsConfig.Builder.anAppDynamicsConfig()
                                         .withControllerUrl("https://na774.saas.appdynamics.com/controller")
                                         .withUsername("testuser")
                                         .withAccountname("na774")
                                         .withPassword("testuser123")
                                         .build())
                          .build(),
            APPLICATION_JSON),
        new GenericType<RestResponse<SettingAttribute>>() {});

    /*
    getRequestWithAuthHeader(target).post(Entity.entity(aSettingAttribute().withAccountId(accountId).withName("AWS_CREDENTIALS")
        .withValue(anAwsInfrastructureProviderConfig().withSecretKey("AKIAI6IK4KYQQQEEWEVA").withSecretKey("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").build())
        .build(), APPLICATION_JSON), new GenericType<RestResponse<SettingAttribute>>() {
    });
    */
  }

  private void createAppSettings(String accountId) {
    WebTarget target = client.target(API_BASE + "/settings/?accountId=" + accountId);
    getRequestWithAuthHeader(target).post(
        Entity.entity(
            aSettingAttribute()
                .withAccountId(accountId)
                .withAppId(GLOBAL_APP_ID)
                .withEnvId(GLOBAL_ENV_ID)
                .withName("Wings Key")
                .withValue(aHostConnectionAttributes()
                               .withConnectionType(SSH)
                               .withAccessType(KEY)
                               .withUserName("ubuntu")
                               .withKey("-----BEGIN RSA PRIVATE KEY-----\n"
                                   + "MIIEogIBAAKCAQEArCtMvZebz8vGCUah4C4kThYOAEjrdgaGe8M8w+66jPKEnX1GDXj4mrlIxRxO\n"
                                   + "ErJTwNirPLhIhw/8mGojcsbc5iY7wK6TThJI0uyzUtPfZ1g8zzcQxh7aMOYso/Nxoz6YtO6HRQhd\n"
                                   + "rxiFuadVo+RuVUeBvVBiQauZMoESh1vGZ2r1eTuXKrSiStaafSfVzSEfvtJYNWnPguqcuGlrX3yv\n"
                                   + "sNOlIWzU3YETk0bMG3bejChGAKh35AhZdDO+U4g7zH8NI5KjT9IH7MyKAFxiCPYkNm7Y2Bw8j2eL\n"
                                   + "DIkqIA1YX0VxXBiCC2Vg78o7TxJ7Df7f3V+Q+Xhtj4rRtYCFw1pqBwIDAQABAoIBAGA//LDpNuQe\n"
                                   + "SWIaKJkJcqZs0fr6yRe8YiaCaVAoAAaX9eeNh0I05NaqyrHXNxZgt03SUzio1XMcTtxuSc76ube4\n"
                                   + "nCMF9bfppOi2BzJA3F4MCELXx/raeKRpqX8ms9rNPdW4m8rN+IHQtcGqeMgdBkmKpk9NxwBrjEOd\n"
                                   + "wNwHRI2/Y/ZCApkQDhRPXfEJXnY65SJJ8Vh1NAm6RuiKXv9+8J1//OHAeRfIXTJI4KiwP2EFHeXF\n"
                                   + "6K0EBVEb/M2kg81bh7iq2OoDxBVrF1Uozg4KUK2EMoCe5OidcSdD1G8ICTsRQlb9iW5e/c2UeCrb\n"
                                   + "HGkcmQyvDniyfFmVVymyr0vJTnECgYEA6FsPq4T+M0Cj6yUqsIdijqgpY31iX2BAibrLTOFUYhj3\n"
                                   + "oPpy2bciREXffMGpqiAY8czy3aEroNDC5c7lcwS1HuMgNls0nKuaPLaSg0rSXX9wRn0mYpasBEZ8\n"
                                   + "5pxFX44FnqTDa37Y7MqKykoMpEB71s1DtG9Ug1cMRuPftZZQ5qsCgYEAvbBcEiPFyKf5g2QRVA/k\n"
                                   + "FDQcX9hVm7cvDTo6+Qq6XUwrQ2cm9ZJ+zf+Jak+NSN88GNTzAPCWzd8zbZ2D7q4qAyDcSSy0PR3K\n"
                                   + "bHpLFZnYYOIkSfYcM3CwDhIFTnb9uvG8mypfMFGZ2qUZY/jbI0/cCctsUaXt03g4cM4Q04peehUC\n"
                                   + "gYAcsWoM9z5g2+GiHxPXetB75149D/W+62bs2ylR1B2Ug5rIwUS/h/LuVWaUxGGMRaxu560yGz4E\n"
                                   + "/OKkeFkzS+iF6OxIahjkI/jG+JC9L9csfplByyCbWhnh6UZxP+j9NM+S2KvdMWveSeC7vEs1WVUx\n"
                                   + "oGV0+a6JDY3Rj0BH70kMQwKBgD1ZaK3FPBalnSFNn/0cFpwiLnshMK7oFCOnDaO2QIgkNmnaVtNd\n"
                                   + "yf0+BGeJyxwidwFg/ibzqRJ0eeGd7Cmp0pSocBaKitCpbeqfsuENnNnYyfvRyVUpwQcL9QNnoLBx\n"
                                   + "tppInfi2q5f3hbq7pcRJ89SHIkVV8RFP9JEnVHHWcq/xAoGAJNbaYQMmLOpGRVwt7bdK5FXXV5OX\n"
                                   + "uzSUPICQJsflhj4KPxJ7sdthiFNLslAOyNYEP+mRy90ANbI1x7XildsB2wqBmqiXaQsyHBXRh37j\n"
                                   + "dMX4iYY1mW7JjS9Y2jy7xbxIBYDpwnqHLTMPSKFQpwsi7thP+0DRthj62sCjM/YB7Es=\n"
                                   + "-----END RSA PRIVATE KEY-----")
                               .build())
                .build(),
            APPLICATION_JSON),
        new GenericType<RestResponse<SettingAttribute>>() {});
  }

  private List<Application> createApplications() {
    List<Application> apps = new ArrayList<>();

    WebTarget target = client.target(API_BASE + "/apps?accountId=" + accountId);

    for (int i = 0; i < NUM_APPS; i++) {
      String name = getName(appNames);
      RestResponse<Application> response = getRequestWithAuthHeader(target).post(
          Entity.entity(
              anApplication().withName(name).withDescription(name).withAccountId(accountId).build(), APPLICATION_JSON),
          new GenericType<RestResponse<Application>>() {});
      assertThat(response.getResource()).isInstanceOf(Application.class);
      apps.add(response.getResource());
    }
    return apps;
  }

  private List<Service> addServices(String appId, List<AppContainer> appContainers) throws IOException {
    serviceNames = new ArrayList<>(seedNames);
    WebTarget target = client.target(API_BASE + "/services/?appId=" + appId);
    List<Service> services = new ArrayList<>();

    for (int i = 0; i < NUM_SERVICES_PER_APP; i++) {
      String name = getName(serviceNames);
      Map<String, Object> serviceMap = new HashMap<>();
      serviceMap.put("name", name);
      serviceMap.put("description", randomText(40));
      serviceMap.put("appId", appId);
      serviceMap.put("artifactType", WAR.name());
      //      serviceMap.put("appContainer", appContainers.get(randomInt(0, appContainers.size()))); //TODO:: create
      //      service with tomcat/jboss family container type
      RestResponse<Service> response = getRequestWithAuthHeader(target).post(
          Entity.entity(serviceMap, APPLICATION_JSON), new GenericType<RestResponse<Service>>() { // FIXME
          });
      assertThat(response.getResource()).isInstanceOf(Service.class);
      String serviceId = response.getResource().getUuid();
      Service service = wingsPersistence.get(Service.class, serviceId);
      services.add(service);
      assertThat(service).isNotNull();

      configFileNames = new ArrayList<>(seedNames);
      addConfigFilesToEntity(service, DEFAULT_TEMPLATE_ID, NUM_CONFIG_FILE_PER_SERVICE, SERVICE);
    }
    return services;
  }

  private void addConfigFilesToEntity(
      Base entity, String templateId, int numConfigFilesToBeAdded, EntityType entityType) throws IOException {
    while (numConfigFilesToBeAdded > 0) {
      if (addOneConfigFileToEntity(entity.getAppId(), templateId, entity.getUuid(), entityType)) {
        numConfigFilesToBeAdded--;
      }
    }
  }

  private boolean addOneConfigFileToEntity(String appId, String templateId, String entityId, EntityType entityType)
      throws IOException {
    WebTarget target = client.target(format(API_BASE + "/configs/?appId=%s&entityId=%s&entityType=%s&templateId=%s",
        appId, entityId, entityType, templateId));
    File file = getTestFile(getName(configFileNames) + ".properties");
    FileDataBodyPart filePart = new FileDataBodyPart("file", file);
    FormDataMultiPart multiPart =
        new FormDataMultiPart().field("name", file.getName()).field("relativeFilePath", "configs/" + file.getName());
    multiPart.bodyPart(filePart);
    Response response = getRequestWithAuthHeader(target).post(Entity.entity(multiPart, multiPart.getMediaType()));
    return response.getStatus() == 200;
  }

  private List<AppContainer> addAppContainers(String appId) {
    int containersToBeAdded = NUM_APP_CONTAINER_PER_APP;
    while (containersToBeAdded > 0) {
      if (addOneAppContainer(appId)) {
        containersToBeAdded--;
      }
    }
    return getAppContainers(appId);
  }

  private List<AppContainer> getAppContainers(String appId) {
    RestResponse<PageResponse<AppContainer>> response =
        getRequestWithAuthHeader(
            client.target(API_BASE + "/app-containers/?appId=" + appId + "&accountId=" + accountId))
            .get(new GenericType<RestResponse<PageResponse<AppContainer>>>() {});
    return response.getResource().getResponse();
  }

  private boolean addOneAppContainer(String appId) {
    WebTarget target = client.target(API_BASE + "/app-containers/?appId=" + appId + "&accountId=" + accountId);
    String version = format("%s.%s.%s", randomInt(10), randomInt(100), randomInt(1000));
    String name = containerNames.get(randomInt() % containerNames.size());

    try {
      File file = getTestFile(name);
      FileDataBodyPart filePart = new FileDataBodyPart("file", file);
      FormDataMultiPart multiPart = new FormDataMultiPart()
                                        .field("name", name)
                                        .field("version", version)
                                        .field("description", randomText(20))
                                        .field("sourceType", "FILE_UPLOAD")
                                        .field("standard", "false");
      multiPart.bodyPart(filePart);
      Response response = getRequestWithAuthHeader(target).post(Entity.entity(multiPart, multiPart.getMediaType()));
      return response.getStatus() == 200;
    } catch (IOException e) {
      log().info("Error occured in uploading app container" + e.getMessage());
    }
    return false;
  }

  private List<Environment> addEnvs(String appId) throws IOException {
    List<Environment> environments =
        wingsPersistence.createQuery(Environment.class).field("appId").equal(appId).asList();
    WebTarget target = client.target(API_BASE + "/environments?appId=" + appId);

    for (int i = 0; i < NUM_ENV_PER_APP; i++) {
      RestResponse<Environment> response = getRequestWithAuthHeader(target).post(
          Entity.entity(
              anEnvironment().withAppId(appId).withName(envNames.get(i)).withDescription(randomText(10)).build(),
              APPLICATION_JSON),
          new GenericType<RestResponse<Environment>>() {});
      assertThat(response.getResource()).isInstanceOf(Environment.class);
      environments.add(response.getResource());
    }
    // environments.forEach(this::addHostsToEnv);
    return environments;
  }

  private File getTestFile(String name) throws IOException {
    File file = new File(testFolder.getRoot().getAbsolutePath() + "/" + name);
    if (!file.isFile()) {
      file = testFolder.newFile(name);
    }
    BufferedWriter out = new BufferedWriter(new FileWriter(file));
    out.write(randomText(100));
    out.close();
    return file;
  }

  private String getName(List<String> names) {
    int nameIdx = randomInt(0, names.size());
    String name = names.get(nameIdx);
    names.remove(nameIdx);
    return name;
  }

  private String randomText(int length) { // TODO: choose words start to word end boundary
    int low = randomInt(50);
    int high = length + low > randomSeedString.length() ? randomSeedString.length() - low : length + low;
    return randomSeedString.substring(low, high);
  }
}
