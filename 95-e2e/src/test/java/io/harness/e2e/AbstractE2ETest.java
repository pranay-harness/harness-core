package io.harness.e2e;

import com.google.inject.Inject;

import graphql.GraphQL;
import io.harness.CategoryTest;
import io.harness.multiline.MultilineStringMixin;
import io.harness.rule.LifecycleRule;
import io.harness.rule.LocalPortalTestRule;
import io.harness.testframework.framework.DelegateExecutor;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.constants.FrameworkConstants;
import io.harness.testframework.framework.utils.FileUtils;
import io.harness.testframework.framework.utils.TestUtils;
import io.harness.testframework.graphql.GraphQLTestMixin;
import io.harness.testframework.restutils.AccountRestUtils;
import io.restassured.RestAssured;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.DataLoaderRegistry;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import software.wings.beans.Account;
import software.wings.graphql.datafetcher.DataLoaderRegistryHelper;
import software.wings.service.intfc.WorkflowExecutionService;

import java.io.IOException;

@Slf4j
public abstract class AbstractE2ETest extends CategoryTest implements GraphQLTestMixin, MultilineStringMixin {
  protected static String bearerToken;
  protected static String qaAccount1 = null;
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public LocalPortalTestRule rule = new LocalPortalTestRule(lifecycleRule.getClosingFactory());
  @Inject DataLoaderRegistryHelper dataLoaderRegistryHelper;

  @Override
  public DataLoaderRegistry getDataLoaderRegistry() {
    return dataLoaderRegistryHelper.getDataLoaderRegistry();
  }

  @Override
  public GraphQL getGraphQL() {
    return rule.getGraphQL();
  }

  @BeforeClass
  public static void setup() {
    Setup.portal();
    RestAssured.useRelaxedHTTPSValidation();
  }

  //  @Inject private AccountGenerator accountGenerator;
  @Inject private DelegateExecutor delegateExecutor;
  //  @Inject OwnerManager ownerManager;
  @Inject private AccountSetupService accountSetupService;
  @Inject private WorkflowExecutionService workflowExecutionService;

  @Getter static Account account;

  @Before
  public void testSetup() throws IOException {
    switch (TestUtils.getExecutionEnvironment()) {
      case FrameworkConstants.LOCAL_ENV:
        logger.info("Setup and Tests running against Local environment");
        doLocalSetup();
        break;
      case FrameworkConstants.QA_ENV:
        logger.info("Setup and Tests running against QA environment");
        doQASetup();
        break;
      default:
        logger.error("Unknown setup detected to run the test");
        System.exit(1);
    }
  }

  @AfterClass
  public static void cleanup() {
    FileUtils.deleteModifiedConfig(AbstractE2ETest.class);
    logger.info("All tests exit");
  }

  private void doLocalSetup() throws IOException {
    account = accountSetupService.ensureAccount();
    delegateExecutor.ensureDelegate(account, AbstractE2ETest.class);
    bearerToken = Setup.getAuthToken("admin@harness.io", "admin");
  }

  private void doQASetup() {
    qaAccount1 = TestUtils.getDecryptedValue("e2etest_qa_account1");
    bearerToken = Setup.getAuthToken("autouser1@harness.io", TestUtils.getDecryptedValue("e2etest_autouser_password"));
    account = AccountRestUtils.getAccount(qaAccount1, bearerToken);
  }
}
