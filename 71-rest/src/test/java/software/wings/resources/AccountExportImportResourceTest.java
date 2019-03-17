package software.wings.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.exportimport.WingsMongoExportImport;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.UserService;
import software.wings.utils.AccountPermissionUtils;

import java.util.Map;

/**
 * @author marklu on 2019-03-01
 */
@Slf4j
public class AccountExportImportResourceTest {
  @Mock private WingsMongoPersistence wingsMongoPersistence;
  @Mock private WingsMongoExportImport wingsMongoExportImport;
  @Mock private AppService appService;
  @Mock private UsageRestrictionsService usageRestrictionsService;
  @Mock private UserService userService;
  @Mock private AccountPermissionUtils accountPermissionUtils;
  @Mock private PersistentScheduler persistentScheduler;

  @Mock private Account account;
  @Mock private User user;

  private JsonArray users;
  private String userId;
  private String accountId;
  private AccountExportImportResource accountExportImportResource;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    accountExportImportResource = new AccountExportImportResource(wingsMongoPersistence, wingsMongoExportImport,
        appService, usageRestrictionsService, userService, accountPermissionUtils, persistentScheduler);

    accountId = UUIDGenerator.generateUuid();
    userId = UUIDGenerator.generateUuid();

    String email = "user@harness.io";

    JsonObject userObject = new JsonObject();
    userObject.add("_id", new JsonPrimitive(userId));
    userObject.add("name", new JsonPrimitive("Harness User"));
    userObject.add("email", new JsonPrimitive(email));

    users = new JsonArray();
    users.add(userObject);

    when(wingsMongoPersistence.get(Account.class, accountId)).thenReturn(account);
    when(userService.getUserByEmail(email)).thenReturn(user);
    when(user.getUuid()).thenReturn(UUIDGenerator.generateUuid());
  }

  @Test
  @Category(UnitTests.class)
  public void testFindClashedUserIdMapping() {
    Map<String, String> userIdMapping = accountExportImportResource.findClashedUserIdMapping(accountId, users);
    assertEquals(1, userIdMapping.size());
    assertTrue(userIdMapping.containsKey(userId));
    verify(wingsMongoPersistence).save(user);

    String usersJson = users.toString();
    String newUsersJson = accountExportImportResource.replaceClashedUserIds(usersJson, userIdMapping);
    log.info(userIdMapping.toString());
    log.info(usersJson);
    log.info(newUsersJson);

    assertNotEquals(newUsersJson, usersJson);
    assertTrue(newUsersJson.indexOf(userId) < 0);
  }
}
