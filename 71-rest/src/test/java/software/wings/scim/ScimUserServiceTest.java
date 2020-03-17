package software.wings.scim;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import com.google.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.beans.scim.ScimUser;
import software.wings.beans.security.UserGroup;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.UserService;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import javax.ws.rs.core.Response;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScimUserServiceTest extends WingsBaseTest {
  private static final String USER_ID = generateUuid();
  private static final String MEMBERS = "members";
  private static final String ACCOUNT_ID = "accountId";

  @Inject WingsPersistence realWingsPersistence;
  @Mock WingsPersistence wingsPersistence;

  @Mock UserService userService;

  @Inject @InjectMocks ScimUserService scimUserService;

  UpdateOperations<User> updateOperations;
  ObjectMapper mapper = new ObjectMapper();

  @Before
  public void setup() throws IllegalAccessException {
    updateOperations = realWingsPersistence.createUpdateOperations(User.class);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testUpdateGroupRemoveMembersShouldPass() {
    PatchRequest patchRequest = getOktaActivityReplaceOperation();
    User user = new User();
    user.setUuid(USER_ID);

    UserGroup userGroup = new UserGroup();
    userGroup.setMemberIds(Arrays.asList(USER_ID));
    userGroup.setAccountId(ACCOUNT_ID);
    userGroup.setImportedByScim(true);

    when(wingsPersistence.createUpdateOperations(User.class)).thenReturn(updateOperations);
    when(userService.get(ACCOUNT_ID, USER_ID)).thenReturn(user);
    when(wingsPersistence.save(userGroup)).thenReturn("true");
    scimUserService.updateUser(ACCOUNT_ID, USER_ID, patchRequest);

    verify(wingsPersistence, times(1)).update(user, updateOperations);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void TC0_testCreateUser() {
    ScimUser scimUser = new ScimUser();
    Account account = new Account();
    account.setUuid(generateUuid());
    account.setAccountName("account_name");

    scimUser.setUserName("username@harness.io");
    scimUser.setDisplayName("display_name");

    User user = new User();
    user.setEmail("username@harness.io");

    UserInvite userInvite = new UserInvite();

    when(userService.getUserByEmail(anyString(), anyString())).thenReturn(user);
    when(userService.inviteUser(any(UserInvite.class))).thenReturn(userInvite);
    Response response = scimUserService.createUser(scimUser, account.getUuid());

    assertThat(response.getStatus()).isEqualTo(409);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void TC1_testCreateUser() {
    ScimUser scimUser = new ScimUser();
    Account account = new Account();
    account.setUuid(generateUuid());
    account.setAccountName("account_name");

    scimUser.setUserName("username@harness.io");
    scimUser.setDisplayName("display_name");

    User user = new User();
    user.setEmail("username@harness.io");
    user.setDisabled(true);
    user.setUuid(generateUuid());
    user.setName("display_name");

    UserInvite userInvite = new UserInvite();
    userInvite.setUuid(generateUuid());

    when(userService.getUserByEmail(anyString(), anyString())).thenReturn(user);
    when(userService.inviteUser(any(UserInvite.class))).thenReturn(userInvite);
    when(userService.get(account.getUuid(), user.getUuid())).thenReturn(user);
    when(wingsPersistence.createUpdateOperations(User.class)).thenReturn(updateOperations);
    Response response = scimUserService.createUser(scimUser, account.getUuid());

    assertThat(response.getStatus()).isEqualTo(201);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void TC2_testCreateUser() {
    ScimUser scimUser = new ScimUser();
    Account account = new Account();
    account.setUuid(generateUuid());
    account.setAccountName("account_name");

    scimUser.setUserName("username@harness.io");
    scimUser.setDisplayName("display_name");
    setNameForScimUser(scimUser);

    User user = new User();
    user.setEmail("username@harness.io");
    user.setDisabled(true);
    user.setUuid(generateUuid());
    user.setName("display_name");

    UserInvite userInvite = new UserInvite();
    userInvite.setUuid(generateUuid());

    when(userService.getUserByEmail(anyString(), anyString())).thenReturn(null);
    when(userService.inviteUser(any(UserInvite.class))).thenReturn(userInvite);
    when(userService.get(account.getUuid(), user.getUuid())).thenReturn(user);
    when(wingsPersistence.createUpdateOperations(User.class)).thenReturn(updateOperations);
    Response response = scimUserService.createUser(scimUser, account.getUuid());

    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testDeleteUser() {
    Account account = new Account();
    account.setUuid(generateUuid());
    account.setAccountName("account_name");

    ScimUser scimUser = new ScimUser();
    scimUser.setUserName("user_name@harness.io");
    scimUser.setDisplayName("display_name");
    setNameForScimUser(scimUser);

    User user = new User();
    user.setEmail("user_name@harness.io");
    user.setDisabled(false);
    user.setUuid(generateUuid());
    user.setName("display_name");
    user.setAccounts(Arrays.asList(account));

    UserInvite userInvite = new UserInvite();
    userInvite.setUuid(generateUuid());

    scimUserService.deleteUser(user.getUuid(), account.getUuid());
    verify(userService, times(1)).delete(account.getUuid(), user.getUuid());
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testUpdateUser() {
    Account account = new Account();
    account.setUuid(generateUuid());
    account.setAccountName("account_name");

    ScimUser scimUser = new ScimUser();
    scimUser.setUserName("user_name@harness.io");
    scimUser.setDisplayName("display_name");
    scimUser.setActive(true);
    setNameForScimUser(scimUser);

    User user = new User();
    user.setEmail("user_name@harness.io");
    user.setDisabled(true);
    user.setFamilyName("family_name_diff");
    user.setGivenName("given_name_diff");
    user.setUuid(generateUuid());
    user.setName("display_name_diff");
    user.setAccounts(Arrays.asList(account));

    UserInvite userInvite = new UserInvite();
    userInvite.setUuid(generateUuid());

    when(userService.get(account.getUuid(), user.getUuid())).thenReturn(user);
    when(wingsPersistence.createUpdateOperations(User.class)).thenReturn(updateOperations);
    Response response = scimUserService.updateUser(user.getUuid(), account.getUuid(), scimUser);
    verify(wingsPersistence, times(1)).update(user, updateOperations);
    assertThat(response.getStatus()).isEqualTo(202);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testUpdateNullUser() {
    Account account = new Account();
    account.setUuid(generateUuid());
    account.setAccountName("ACCOUNT_NAME");

    ScimUser scimUser = new ScimUser();
    scimUser.setUserName("user_name@harness.io");
    scimUser.setDisplayName("DISPLAY_NAME");
    scimUser.setActive(true);
    setNameForScimUser(scimUser);

    User user = new User();
    user.setEmail("user_name@harness.io");
    user.setDisabled(false);
    user.setUuid(generateUuid());
    user.setName("display_name");
    user.setFamilyName("family_name");
    user.setGivenName("given_name");
    user.setAccounts(Arrays.asList(account));

    UserInvite userInvite = new UserInvite();
    userInvite.setUuid(generateUuid());

    when(userService.get(account.getUuid(), user.getUuid())).thenReturn(null);
    when(wingsPersistence.createUpdateOperations(User.class)).thenReturn(updateOperations);
    Response response = scimUserService.updateUser(user.getUuid(), account.getUuid(), scimUser);
    assertThat(response.getStatus()).isEqualTo(404);
  }

  private PatchRequest getOktaActivityReplaceOperation() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("active", false);
    JsonNode jsonNode;

    try {
      jsonNode = mapper.readTree(jsonObject.toString());
      OktaReplaceOperation replaceOperation = new OktaReplaceOperation(MEMBERS, jsonNode);
      return new PatchRequest(Collections.singletonList(replaceOperation));
    } catch (IOException ioe) {
      log().error("IO Exception while creating okta replace operation in SCIM", ioe);
    }
    return null;
  }

  private void setNameForScimUser(ScimUser scimUser) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("givenName", "given_name");
    jsonObject.addProperty("familyName", "family_name");

    JsonNode jsonNode;

    try {
      jsonNode = mapper.readTree(jsonObject.toString());
      scimUser.setName(jsonNode);
    } catch (IOException ioe) {
      log().error("IO Exception while creating okta replace operation in SCIM", ioe);
    }
  }
}
