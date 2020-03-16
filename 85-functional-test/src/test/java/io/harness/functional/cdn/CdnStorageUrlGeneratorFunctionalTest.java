package io.harness.functional.cdn;

import static io.harness.UrlConnectionMixin.checkIfFileExists;
import static io.harness.generator.ApplicationGenerator.Applications.GENERIC_TEST;
import static io.harness.generator.OwnerManager.Owners;
import static io.harness.generator.Randomizer.Seed;
import static io.harness.rule.OwnerRule.VIKAS;
import static io.harness.testframework.framework.Setup.portal;
import static io.restassured.http.ContentType.JSON;
import static java.net.HttpURLConnection.HTTP_OK;
import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.FeatureName.USE_CDN_FOR_STORAGE_FILES;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.OwnerManager;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.service.intfc.FeatureFlagService;

import java.io.IOException;
import javax.ws.rs.core.GenericType;

@Slf4j
public class CdnStorageUrlGeneratorFunctionalTest extends AbstractFunctionalTest {
  private static final String DELEGATE_JAR_VERSION = "50300";
  private static final String WATCHER_JAR_VERSION = "50100";

  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  private final Seed seed = new Seed(0);
  @Inject private FeatureFlagService featureFlagService;

  @Before
  public void setUp() {
    Owners owners = ownerManager.create();
    Application application = applicationGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
    assertThat(application).isNotNull();
    featureFlagService.enableAccount(USE_CDN_FOR_STORAGE_FILES, application.getAccountId());
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(FunctionalTests.class)
  @Ignore("Unable to reach URL intermittently")
  public void testDelegateSignedUrlGeneration() throws IOException {
    GenericType<RestResponse<String>> returnType = new GenericType<RestResponse<String>>() {};
    RestResponse<String> signedUrl = portal()
                                         .auth()
                                         .oauth2(bearerToken)
                                         .contentType(JSON)
                                         .queryParam("accountId", getAccount().getUuid())
                                         .get("/agent/infra-download/default/delegate/" + DELEGATE_JAR_VERSION)
                                         .as(returnType.getType());

    assertEquals(HTTP_OK, checkIfFileExists(signedUrl.getResource()));
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(FunctionalTests.class)
  @Ignore("Unable to reach URL intermittently")
  public void testWatcherUrlGeneration() throws IOException {
    GenericType<RestResponse<String>> returnType = new GenericType<RestResponse<String>>() {};
    RestResponse<String> signedUrl = portal()
                                         .auth()
                                         .oauth2(bearerToken)
                                         .contentType(JSON)
                                         .queryParam("accountId", getAccount().getUuid())
                                         .get("/agent/infra-download/default/watcher/" + WATCHER_JAR_VERSION)
                                         .as(returnType.getType());

    assertEquals(HTTP_OK, checkIfFileExists(signedUrl.getResource()));
  }
}
