package migrations.all;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Base;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationGroup.NotificationGroupBuilder;
import software.wings.service.impl.yaml.YamlChangeSetHelper;
import software.wings.service.intfc.NotificationSetupService;

public class AddIsDefaultToExistingNotificationGroupsTest extends WingsBaseTest {
  @Inject private AddIsDefaultToExistingNotificationGroups migrator;
  @InjectMocks @Inject private NotificationSetupService notificationSetupService;
  @Mock YamlChangeSetHelper yamlChangeSetHelper;
  private static final String ACCOUNT_ID = "123";

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void shouldMigrate() {
    doNothing().when(yamlChangeSetHelper).notificationGroupYamlChangeAsync(any(), any());
    doNothing().when(yamlChangeSetHelper).notificationGroupYamlChangeSet(any(), any());

    NotificationGroup notificationGroup =
        notificationSetupService.createNotificationGroup(NotificationGroupBuilder.aNotificationGroup()
                                                             .withAccountId(ACCOUNT_ID)
                                                             .withAppId(Base.GLOBAL_APP_ID)
                                                             .withName("test" + System.currentTimeMillis())
                                                             .withEditable(true)
                                                             .build());

    migrator.migrate();
    notificationGroup = notificationSetupService.readNotificationGroup(ACCOUNT_ID, notificationGroup.getUuid());
    assertFalse(notificationGroup.isDefaultNotificationGroupForAccount());
    notificationSetupService.deleteNotificationGroups(ACCOUNT_ID, notificationGroup.getUuid());
  }
}
