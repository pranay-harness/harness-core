package migrations.all;

import static junit.framework.TestCase.assertNotNull;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.common.Constants.HARNESS_NAME;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.PORTAL_URL;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.EntityType;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.yaml.gitSync.YamlGitConfig;
import software.wings.yaml.gitSync.YamlGitConfig.SyncMode;

public class YamlGitConfigAppMigrationTest extends WingsBaseTest {
  private static final String BRANCH = "branch";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private YamlGitService yamlGitService;
  @Inject private AccountService accountService;

  @InjectMocks @Inject private YamlGitConfigAppMigration yamlGitConfigAppMigration;

  @Before
  public void setUp() {
    Account account = anAccount()
                          .withCompanyName(HARNESS_NAME)
                          .withAccountName(HARNESS_NAME)
                          .withAccountKey("ACCOUNT_KEY")
                          .withUuid(ACCOUNT_ID)
                          .build();
    accountService.save(account);

    YamlGitConfig yamlGitConfig = YamlGitConfig.builder()
                                      .accountId(ACCOUNT_ID)
                                      .branchName(BRANCH)
                                      .url(PORTAL_URL)
                                      .syncMode(SyncMode.NONE)
                                      .entityId(ACCOUNT_ID)
                                      .entityType(EntityType.ACCOUNT)
                                      .build();
    yamlGitConfig.setAppId(Base.GLOBAL_APP_ID);
    yamlGitService.save(yamlGitConfig);
  }

  @Test
  public void testMigration() {
    Application application = createApplication();

    yamlGitConfigAppMigration.migrate();

    YamlGitConfig savedYamlGitConfig = yamlGitService.get(ACCOUNT_ID, application.getUuid(), EntityType.APPLICATION);
    assertNotNull(savedYamlGitConfig);
    assertThat(savedYamlGitConfig.getAppId()).isEqualTo(application.getUuid());
    assertThat(savedYamlGitConfig.getEntityId()).isEqualTo(application.getUuid());
    assertThat(savedYamlGitConfig.getEntityType()).isEqualTo(EntityType.APPLICATION);
  }

  @Test
  public void testMigrationWithExistingAppYamlGitConfig() {
    Application application = createApplication();

    YamlGitConfig yamlGitConfig = YamlGitConfig.builder()
                                      .accountId(ACCOUNT_ID)
                                      .branchName(BRANCH)
                                      .url(PORTAL_URL)
                                      .syncMode(SyncMode.NONE)
                                      .entityId(application.getUuid())
                                      .entityType(EntityType.APPLICATION)
                                      .build();
    yamlGitConfig.setAppId(Base.GLOBAL_APP_ID);
    yamlGitService.save(yamlGitConfig);

    yamlGitConfigAppMigration.migrate();
    YamlGitConfig savedYamlGitConfig = yamlGitService.get(ACCOUNT_ID, application.getUuid(), EntityType.APPLICATION);
    assertNotNull(savedYamlGitConfig);
  }

  private Application createApplication() {
    Application app = anApplication().withName("AppA").withAccountId(ACCOUNT_ID).build();

    return wingsPersistence.saveAndGet(Application.class, app);
  }
}
