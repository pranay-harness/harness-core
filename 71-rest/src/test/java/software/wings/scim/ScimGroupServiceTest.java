package software.wings.scim;

import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import com.google.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import software.wings.beans.User;
import software.wings.beans.security.UserGroup;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.UserGroupService;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScimGroupServiceTest extends WingsBaseTest {
  private static final String USER_ID_1 = "id1";
  private static final String USER_ID_2 = "id2";
  private static final String VALUE = "value";
  private static final String MEMBERS = "members";
  private static final String GROUP_ID = "groupId";
  private static final String ACCOUNT_ID = "accountId";
  private static final String RANDOM_ID = "Random";

  @Inject WingsPersistence realWingsPersistence;
  @Mock WingsPersistence wingsPersistence;

  @Mock UserGroupService userGroupService;
  @Inject @InjectMocks ScimGroupService scimGroupService;

  UpdateOperations<UserGroup> updateOperations;
  ObjectMapper mapper = new ObjectMapper();

  @Before
  public void setup() throws IllegalAccessException {
    updateOperations = realWingsPersistence.createUpdateOperations(UserGroup.class);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testUpdateGroupRemoveMembersShouldPass() {
    PatchRequest patchRequest = getRemoveOperation();

    UserGroup userGroup = UserGroup.builder().memberIds(Arrays.asList(RANDOM_ID)).build();
    when(userGroupService.get(anyString(), anyString())).thenReturn(userGroup);
    when(wingsPersistence.createUpdateOperations(UserGroup.class)).thenReturn(updateOperations);

    when(wingsPersistence.update(any(UserGroup.class), any())).thenReturn(null);
    scimGroupService.updateGroup(GROUP_ID, ACCOUNT_ID, patchRequest);

    verify(wingsPersistence, times(0)).update(userGroup, updateOperations);

    userGroup.setMemberIds(Arrays.asList(USER_ID_1, USER_ID_2));
    scimGroupService.updateGroup(GROUP_ID, ACCOUNT_ID, patchRequest);

    verify(wingsPersistence, times(1)).update(userGroup, updateOperations);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testUpdateGroupRemoveMembersShouldPassOkta() {
    PatchRequest patchRequest = getOktaRemoveOperation();

    UserGroup userGroup = UserGroup.builder().memberIds(Arrays.asList(RANDOM_ID)).build();
    when(userGroupService.get(anyString(), anyString())).thenReturn(userGroup);
    when(wingsPersistence.createUpdateOperations(UserGroup.class)).thenReturn(updateOperations);

    when(wingsPersistence.update(any(UserGroup.class), any())).thenReturn(null);
    scimGroupService.updateGroup(GROUP_ID, ACCOUNT_ID, patchRequest);

    verify(wingsPersistence, times(0)).update(userGroup, updateOperations);

    userGroup.setMemberIds(Arrays.asList(USER_ID_1, USER_ID_2));
    scimGroupService.updateGroup(GROUP_ID, ACCOUNT_ID, patchRequest);

    verify(wingsPersistence, times(1)).update(userGroup, updateOperations);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testUpdateGroupAddMembersShouldPass() {
    PatchRequest patchRequest = getAddOperation();

    UserGroup userGroup = UserGroup.builder().memberIds(Arrays.asList(USER_ID_2)).build();
    User user = new User();
    user.setUuid(USER_ID_2);
    when(userGroupService.get(anyString(), anyString())).thenReturn(userGroup);
    when(wingsPersistence.get(eq(User.class), anyString())).thenReturn(user);
    when(wingsPersistence.createUpdateOperations(UserGroup.class)).thenReturn(updateOperations);

    when(wingsPersistence.update(any(UserGroup.class), any())).thenReturn(null);
    scimGroupService.updateGroup(GROUP_ID, ACCOUNT_ID, patchRequest);

    verify(wingsPersistence, times(1)).update(userGroup, updateOperations);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testUpdateGroupAddMembersShouldPassOkta() {
    PatchRequest patchRequest = getOktaAddOperation();

    UserGroup userGroup = UserGroup.builder().memberIds(Arrays.asList(USER_ID_2)).build();
    User user = new User();
    user.setUuid(USER_ID_2);
    when(userGroupService.get(anyString(), anyString())).thenReturn(userGroup);
    when(wingsPersistence.get(eq(User.class), anyString())).thenReturn(user);
    when(wingsPersistence.createUpdateOperations(UserGroup.class)).thenReturn(updateOperations);

    when(wingsPersistence.update(any(UserGroup.class), any())).thenReturn(null);
    scimGroupService.updateGroup(GROUP_ID, ACCOUNT_ID, patchRequest);

    verify(wingsPersistence, times(1)).update(userGroup, updateOperations);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testUpdateGroupAddMembersShouldFail() {
    updateOperations = realWingsPersistence.createUpdateOperations(UserGroup.class);
    PatchRequest patchRequest = getInvalidOperation();
    User user = new User();
    user.setUuid(USER_ID_2);

    UserGroup userGroup = UserGroup.builder().memberIds(Collections.emptyList()).build();
    when(userGroupService.get(anyString(), anyString())).thenReturn(userGroup);
    when(wingsPersistence.createUpdateOperations(UserGroup.class)).thenReturn(updateOperations);
    when(wingsPersistence.get(eq(User.class), anyString())).thenReturn(user);

    when(wingsPersistence.update(any(UserGroup.class), any())).thenReturn(null);
    scimGroupService.updateGroup(GROUP_ID, ACCOUNT_ID, patchRequest);

    verify(wingsPersistence, times(0)).update(userGroup, updateOperations);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testUpdateGroupAddMembersShouldFailOkta() {
    updateOperations = realWingsPersistence.createUpdateOperations(UserGroup.class);
    PatchRequest patchRequest = getInvalidOktaOperation();
    User user = new User();
    user.setUuid(USER_ID_2);

    UserGroup userGroup = UserGroup.builder().memberIds(Collections.emptyList()).build();
    when(userGroupService.get(anyString(), anyString())).thenReturn(userGroup);
    when(wingsPersistence.createUpdateOperations(UserGroup.class)).thenReturn(updateOperations);
    when(wingsPersistence.get(eq(User.class), anyString())).thenReturn(user);

    when(wingsPersistence.update(any(UserGroup.class), any())).thenReturn(null);
    scimGroupService.updateGroup(GROUP_ID, ACCOUNT_ID, patchRequest);

    verify(wingsPersistence, times(0)).update(userGroup, updateOperations);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testReplaceOperationOkta() {
    PatchRequest patchRequest = getOktaReplaceOperation();

    UserGroup userGroup = UserGroup.builder().memberIds(Collections.emptyList()).build();
    when(userGroupService.get(anyString(), anyString())).thenReturn(userGroup);
    when(wingsPersistence.createUpdateOperations(UserGroup.class)).thenReturn(updateOperations);

    scimGroupService.updateGroup(GROUP_ID, ACCOUNT_ID, patchRequest);
    verify(wingsPersistence, times(1)).update(userGroup, updateOperations);
  }

  private PatchRequest getOktaReplaceOperation() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("id", USER_ID_1);
    jsonObject.addProperty("displayName", USER_ID_2);
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

  private PatchRequest getAddOperation() {
    ArrayNode operation = mapper.createArrayNode();
    JsonNode x = mapper.createObjectNode();
    ((ObjectNode) x).put(VALUE, USER_ID_1);
    operation.add(x);
    AddOperation addOperation = new AddOperation(MEMBERS, operation);
    return new PatchRequest(Collections.singletonList(addOperation));
  }

  private PatchRequest getOktaAddOperation() {
    ArrayNode operation = mapper.createArrayNode();
    JsonNode x = mapper.createObjectNode();
    ((ObjectNode) x).put(VALUE, USER_ID_1);
    operation.add(x);
    OktaAddOperation addOperation = new OktaAddOperation(MEMBERS, operation);
    return new PatchRequest(Collections.singletonList(addOperation));
  }

  private PatchRequest getInvalidOperation() {
    ArrayNode operation = mapper.createArrayNode();
    JsonNode x = mapper.createObjectNode();
    ((ObjectNode) x).put(VALUE, USER_ID_1);
    operation.add(x);
    AddOperation addOperation = new AddOperation(RANDOM_ID, operation);
    return new PatchRequest(Collections.singletonList(addOperation));
  }

  private PatchRequest getInvalidOktaOperation() {
    ArrayNode operation = mapper.createArrayNode();
    JsonNode x = mapper.createObjectNode();
    ((ObjectNode) x).put(VALUE, USER_ID_1);
    operation.add(x);
    OktaAddOperation addOperation = new OktaAddOperation(RANDOM_ID, operation);
    return new PatchRequest(Collections.singletonList(addOperation));
  }

  private PatchRequest getRemoveOperation() {
    ArrayNode operation = mapper.createArrayNode();
    JsonNode x = mapper.createObjectNode();
    ((ObjectNode) x).put(VALUE, USER_ID_1);
    operation.add(x);
    RemoveOperation removeOperation = new RemoveOperation(MEMBERS, operation);
    return new PatchRequest(Collections.singletonList(removeOperation));
  }

  private PatchRequest getOktaRemoveOperation() {
    ArrayNode operation = mapper.createArrayNode();
    JsonNode x = mapper.createObjectNode();
    ((ObjectNode) x).put(VALUE, USER_ID_1);
    operation.add(x);
    OktaRemoveOperation removeOperation = new OktaRemoveOperation(MEMBERS, operation);
    return new PatchRequest(Collections.singletonList(removeOperation));
  }
}
