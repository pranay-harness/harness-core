package software.wings.resources;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.rule.Owner;
import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.MapperOptions;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.exportimport.WingsMongoExportImport;
import software.wings.licensing.LicenseService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.UserService;
import software.wings.utils.AccountPermissionUtils;

import java.util.Map;

/**
 * @author marklu on 2019-03-01
 */
@Slf4j
public class AccountExportImportResourceTest extends CategoryTest {
  @Mock private WingsMongoPersistence wingsMongoPersistence;
  @Mock private Morphia morphia;
  @Mock private WingsMongoExportImport wingsMongoExportImport;
  @Mock private AppService appService;
  @Mock private AccountService accountService;
  @Mock private LicenseService licenseService;
  @Mock private UsageRestrictionsService usageRestrictionsService;
  @Mock private UserService userService;
  @Mock private AuthService authService;
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

    Mapper mapper = mock(Mapper.class);
    MapperOptions mapperOptions = mock(MapperOptions.class);
    when(mapper.getOptions()).thenReturn(mapperOptions);
    when(morphia.getMapper()).thenReturn(mapper);

    accountExportImportResource = new AccountExportImportResource(wingsMongoPersistence, morphia,
        wingsMongoExportImport, accountService, licenseService, appService, authService, usageRestrictionsService,
        userService, accountPermissionUtils, persistentScheduler);

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
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testFindClashedUserIdMapping() {
    Map<String, String> userIdMapping = accountExportImportResource.findClashedUserIdMapping(accountId, users);
    assertThat(userIdMapping).hasSize(1);
    assertThat(userIdMapping.containsKey(userId)).isTrue();
    verify(wingsMongoPersistence).save(user);

    String usersJson = users.toString();
    String newUsersJson = accountExportImportResource.replaceClashedUserIds(usersJson, userIdMapping);
    logger.info(userIdMapping.toString());
    logger.info(usersJson);
    logger.info(newUsersJson);

    assertThat(newUsersJson).isNotEqualTo(usersJson);
    assertThat(newUsersJson.indexOf(userId) < 0).isTrue();
  }
}
