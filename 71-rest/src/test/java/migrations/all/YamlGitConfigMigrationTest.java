package migrations.all;

import static junit.framework.TestCase.assertNull;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.PORTAL_URL;

import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.EntityType;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.yaml.gitSync.YamlGitConfig;
import software.wings.yaml.gitSync.YamlGitConfig.SyncMode;

public class YamlGitConfigMigrationTest extends WingsBaseTest {
  private static final String BRANCH = "branch";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private YamlGitService yamlGitService;

  @InjectMocks @Inject private YamlGitConfigMigration yamlGitConfigMigration;

  @Test
  public void testMigration() {
    YamlGitConfig yamlGitConfig = YamlGitConfig.builder()
                                      .accountId(ACCOUNT_ID)
                                      .branchName(BRANCH)
                                      .url(PORTAL_URL)
                                      .syncMode(SyncMode.NONE)
                                      .build();
    yamlGitConfig.setAppId(APP_ID);

    yamlGitConfig = wingsPersistence.saveAndGet(YamlGitConfig.class, yamlGitConfig);
    assertNull(yamlGitConfig.getEntityId());
    assertNull(yamlGitConfig.getEntityType());

    yamlGitConfigMigration.migrate();

    yamlGitConfig = yamlGitService.get(ACCOUNT_ID, ACCOUNT_ID, EntityType.ACCOUNT);
    assertThat(yamlGitConfig.getEntityId()).isEqualTo(ACCOUNT_ID);
    assertThat(yamlGitConfig.getEntityType()).isEqualTo(EntityType.ACCOUNT);
  }
}
