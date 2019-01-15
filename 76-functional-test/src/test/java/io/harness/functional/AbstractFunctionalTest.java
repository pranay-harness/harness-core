package io.harness.functional;

import static io.harness.generator.AccountGenerator.Accounts.GENERIC_TEST;
import static io.harness.threading.Morpheus.sleep;
import static io.restassured.RestAssured.given;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.fabric8.utils.Strings;
import io.harness.category.element.FunctionalTests;
import io.harness.generator.AccountGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.resource.Project;
import io.harness.rule.FunctionalTestRule;
import io.harness.rule.LifecycleRule;
import io.harness.threading.Puller;
import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import software.wings.beans.Account;
import software.wings.beans.RestResponse;
import software.wings.beans.User;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.ws.rs.core.GenericType;

public abstract class AbstractFunctionalTest implements FunctionalTests {
  private static final Logger logger = LoggerFactory.getLogger(AbstractFunctionalTest.class);

  private static final String alpnJar =
      "org/mortbay/jetty/alpn/alpn-boot/8.1.11.v20170118/alpn-boot-8.1.11.v20170118.jar";

  protected static String bearerToken;
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public FunctionalTestRule rule = new FunctionalTestRule(lifecycleRule.getClosingFactory());

  protected static boolean failedAlready;

  private static void executeLocalManager() throws IOException {
    if (failedAlready) {
      return;
    }

    try {
      final Path jar = Paths.get(Project.rootDirectory(), "71-rest", "target", "rest-capsule.jar");
      final Path config = Paths.get(Project.rootDirectory(), "71-rest", "config.yml");
      String alpn = System.getProperty("user.home") + "/.m2/repository/" + alpnJar;

      if (!new File(alpn).exists()) {
        // if maven repo is not in the home dir, this might be a jenkins job, check in the special location.
        alpn = "/home/jenkins/maven-repositories/0/" + alpnJar;
        if (!new File(alpn).exists()) {
          throw new RuntimeException("Missing alpn file");
        }
      }

      for (int i = 0; i < 10; i++) {
        logger.info("***");
      }

      final List<String> command =
          asList("java", "-Xms1024m", "-Xmx4096m", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:+PrintGCDetails",
              "-XX:+PrintGCDateStamps", "-Xloggc:mygclogfilename.gc", "-XX:+UseParallelGC", "-XX:MaxGCPauseMillis=500",
              "-Dfile.encoding=UTF-8", "-Xbootclasspath/p:" + alpn, "-jar", jar.toString(), config.toString());

      logger.info(Strings.join(command, " "));

      ProcessExecutor processExecutor = new ProcessExecutor();
      processExecutor.directory(new File(Project.rootDirectory()));
      processExecutor.command(command);

      processExecutor.redirectOutput(System.out);
      processExecutor.redirectError(System.err);

      processExecutor.start();

      Puller.pullFor(ofSeconds(60), () -> {
        try {
          given().when().get("/version").then().statusCode(200);
        } catch (Exception exception) {
          sleep(ofSeconds(10));
          return false;
        }
        return true;
      });
    } catch (RuntimeException | IOException exception) {
      failedAlready = true;
      throw exception;
    }
  }

  @BeforeClass
  public static void setup() throws IOException {
    String port = System.getProperty("server.port");
    if (port == null) {
      RestAssured.port = Integer.valueOf(9090);
    } else {
      RestAssured.port = Integer.valueOf(port);
    }

    String basePath = System.getProperty("server.base");
    if (basePath == null) {
      basePath = "/api";
    }
    RestAssured.basePath = basePath;

    String baseHost = System.getProperty("server.host");
    if (baseHost == null) {
      baseHost = "https://localhost";
    }
    RestAssured.baseURI = baseHost;

    //    RestAssured.authentication = basic("admin@harness.io","admin");
    RestAssured.useRelaxedHTTPSValidation();

    // Verify if api is ready
    try {
      given().when().get("/version").then().statusCode(200);
    } catch (Exception exception) {
      if (exception instanceof ConnectException) {
        executeLocalManager();
        given().when().get("/version").then().statusCode(200);
      } else {
        throw exception;
      }
    }
  }

  @Inject private AccountGenerator accountGenerator;
  @Inject OwnerManager ownerManager;

  Account account;

  @Before
  public void testSetup() {
    final Seed seed = new Seed(0);
    Owners owners = ownerManager.create();

    account = accountGenerator.ensurePredefined(seed, owners, GENERIC_TEST);

    String basicAuthValue =
        "Basic " + encodeBase64String(String.format("%s:%s", "admin@harness.io", "admin").getBytes());

    GenericType<RestResponse<User>> genericType = new GenericType<RestResponse<User>>() {

    };
    RestResponse<User> userRestResponse =
        given().header("Authorization", basicAuthValue).get("/users/login").as(genericType.getType());

    assertThat(userRestResponse).isNotNull();
    User user = userRestResponse.getResource();
    assertThat(user).isNotNull();
    bearerToken = user.getToken();
  }

  protected void resetCache() {
    RestResponse<User> userRestResponse = given()
                                              .auth()
                                              .oauth2(bearerToken)
                                              .queryParam("accountId", account.getUuid())
                                              .put("/users/reset-cache")
                                              .as(new GenericType<RestResponse<User>>() {}.getType());
    assertThat(userRestResponse).isNotNull();
  }
}
