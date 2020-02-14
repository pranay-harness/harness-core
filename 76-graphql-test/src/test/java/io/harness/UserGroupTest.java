package io.harness;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.VARDAN_BANSAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import graphql.ExecutionResult;
import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.AccountGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.multiline.MultilineStringMixin;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;
import io.harness.testframework.graphql.QLTestObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.User;
import software.wings.beans.notification.NotificationSettings;
import software.wings.beans.security.UserGroup;
import software.wings.beans.sso.LdapConnectionSettings;
import software.wings.beans.sso.LdapGroupSettings;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.LdapUserSettings;
import software.wings.beans.sso.SamlSettings;
import software.wings.graphql.schema.type.permissions.QLGroupPermissions;
import software.wings.graphql.schema.type.usergroup.QLLDAPSettings;
import software.wings.graphql.schema.type.usergroup.QLNotificationSettings;
import software.wings.graphql.schema.type.usergroup.QLSSOSetting;
import software.wings.graphql.schema.type.usergroup.QLUserGroup.QLUserGroupKeys;
import software.wings.graphql.schema.type.usergroup.QLUserGroupConnection;
import software.wings.service.intfc.SSOSettingService;
import software.wings.service.intfc.UserGroupService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
public class UserGroupTest extends GraphQLTest {
  @Inject UserGroupService userGroupService;
  @Inject private OwnerManager ownerManager;
  private String accountId;
  private Account account;
  @Inject AccountGenerator accountGenerator;
  private String samlGroupName = "eng";
  private String groupDN = "groupDN";
  private String groupName = "ldapGroup";
  private SamlSettings samlSettings;
  private LdapSettings ldapSettings;
  @Inject SSOSettingService ssoSettingService;

  @Before
  public void setup() {
    final Randomizer.Seed seed = new Randomizer.Seed(0);
    final OwnerManager.Owners owners = ownerManager.create();
    account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    accountId = account.getUuid();
  }

  private NotificationSettings getNotificationSettings() {
    return new NotificationSettings(false, true, Arrays.asList("abc@example.com"), null, null);
  }

  private UserGroup createUserGroup(String name, String description) {
    UserGroup userGroup = UserGroup.builder()
                              .accountId(accountId)
                              .name(name)
                              .description(description)
                              .isSsoLinked(false)
                              .importedByScim(false)
                              .notificationSettings(getNotificationSettings())
                              .build();
    return userGroupService.save(userGroup);
  }

  private UserGroup createUserGroup(String name, String description, List<String> userIds) {
    UserGroup userGroup = UserGroup.builder()
                              .accountId(accountId)
                              .isSsoLinked(false)
                              .name(name)
                              .description(description)
                              .notificationSettings(getNotificationSettings())
                              .memberIds(userIds)
                              .importedByScim(false)
                              .build();
    return userGroupService.save(userGroup);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryUserGroup() {
    final Randomizer.Seed seed = new Randomizer.Seed(0);
    final OwnerManager.Owners owners = ownerManager.create();
    owners.add(EmbeddedUser.builder().uuid(generateUuid()).build());
    String name = "AccountPermission-UserGroup-" + System.currentTimeMillis();
    String description = "\"Test UserGroup\"";
    final User user = accountGenerator.ensureUser(
        "userId", random(String.class), random(String.class), random(String.class).toCharArray(), account);
    UserGroup userGroup = createUserGroup(name, description, Collections.singletonList(user.getUuid()));

    {
      String query = $GQL(/*
        query{
            userGroup(userGroupId:"%s"){
                   id
                   name
                   description
                   isSSOLinked
                   importedByScim
                   notificationSettings {
                       sendNotificationToMembers
                       sendMailToNewMembers
                       groupEmailAddresses
                       slackNotificationSetting {
                            slackChannelName
                            slackWebhookURL
                      }
                  }
                  users(limit:1,offset:0){
                    nodes{
                      id
                      name
                      email
                      isEmailVerified
                    }
                 }
             }
        }*/ userGroup.getUuid());

      final QLTestObject qlTestObject = qlExecute(query, accountId);
      final UserGroupResult userGroupResult = JsonUtils.convertValue(qlTestObject.getMap(), UserGroupResult.class);
      assertThat(qlTestObject.get(QLUserGroupKeys.id)).isEqualTo(userGroup.getUuid());
      assertThat(qlTestObject.get(QLUserGroupKeys.name)).isEqualTo(userGroup.getName());
      assertThat(qlTestObject.get(QLUserGroupKeys.description)).isEqualTo(userGroup.getDescription());
      assertThat(qlTestObject.get(QLUserGroupKeys.isSSOLinked)).isEqualTo(userGroup.isSsoLinked());
      assertThat(qlTestObject.get(QLUserGroupKeys.importedByScim)).isEqualTo(userGroup.isImportedByScim());
      assertThat(userGroupResult.users.getNodes().size()).isEqualTo(1);
      assertThat(userGroupResult.users.getNodes().get(0).getId()).isEqualTo(user.getUuid());
    }
  }
  @Data
  private static class UserGroupResult {
    String id;
    String name;

    software.wings.graphql.schema.type.user.QLUserConnection users;
    QLSSOSetting ssoSetting;
  }

  private LdapSettings createLdapSettings() {
    LdapConnectionSettings connectionSettings = new LdapConnectionSettings();
    connectionSettings.setBindDN("testBindDN");
    connectionSettings.setBindPassword("testBindPassword");
    LdapUserSettings userSettings = new LdapUserSettings();
    userSettings.setBaseDN("testBaseDN");
    List<LdapUserSettings> userSettingsList = new ArrayList<>(Arrays.asList(userSettings));
    LdapGroupSettings groupSettings = new LdapGroupSettings();
    groupSettings.setBaseDN("testBaseDN");
    ldapSettings =
        new LdapSettings("testSettings", accountId, connectionSettings, userSettingsList, Arrays.asList(groupSettings));
    return ssoSettingService.createLdapSettings(ldapSettings);
  }

  private String getCreateUserGroupGQL(String userGroupName, String userId) {
    String newUserGroup = $GQL(
        /* {
              name: "%s",
              description: "descc",
              permissions: {
                   accountPermissions: {
                    accountPermissionTypes: ADMINISTER_OTHER_ACCOUNT_FUNCTIONS
                  }
              },
              notificationSettings: {
                 sendMailToNewMembers: true,
                 sendNotificationToMembers: true,
                 slackNotificationSetting:
                     {
                       slackWebhookURL: "https://abc",
                       slackChannelName: "cool"
                     },
                 groupEmailAddresses: "abc@gmail.com"
              },
              ssoSetting: {
                ldapSettings:  {
                    ssoProviderId: "%s",
                    groupDN: "%s",
                    groupName: "%s"
                 }
               },
               userIds :"%s",
              clientMutationId: "abc"
         }
       */
        userGroupName, ldapSettings.getUuid(), groupDN, groupName, userId);
    return newUserGroup;
  }

  @Data
  private static class TestUserGroup {
    String name;
    String id;
    String description;
    QLGroupPermissions permissions;
    QLLDAPSettings ssoSetting;
    Boolean isSSOLinked;
    Boolean importedByScim;
    QLNotificationSettings notificationSettings;
    String clientMutationId;
  }
  @Data
  private static class CreateUserGroupResult {
    TestUserGroup userGroup;
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testCreateUserGroup() {
    ldapSettings = createLdapSettings();
    final User user = accountGenerator.ensureUser(
        "userId", random(String.class), random(String.class), random(String.class).toCharArray(), account);
    {
      String query = $GQL(/*
        mutation{
             createUserGroup(input:%s){
             userGroup{
                   id
                   name
                   description
                   isSSOLinked
                   importedByScim
                   ssoSetting{
                      ... on LDAPSettings{
                          ssoProviderId
                          groupDN
                          groupName
                       }
                   }
                   notificationSettings {
                       sendNotificationToMembers
                       sendMailToNewMembers
                       groupEmailAddresses
                       slackNotificationSetting {
                            slackChannelName
                            slackWebhookURL
                      }
                  }
                  users(limit:1,offset:0){
                    nodes{
                      id
                      name
                      email
                      isEmailVerified
                    }
                 }
              }
           }
      }*/ getCreateUserGroupGQL("NewUserGroup", user.getUuid()));
      final QLTestObject qlTestObject = qlExecute(query, accountId);
      final CreateUserGroupResult userGroupResult =
          JsonUtils.convertValue(qlTestObject.getMap(), CreateUserGroupResult.class);
      assertThat(userGroupResult.getUserGroup().getId()).isNotNull();
      assertThat(userGroupResult.getUserGroup().getName()).isEqualTo("NewUserGroup");
      assertThat(userGroupResult.getUserGroup().getDescription()).isEqualTo("descc");
      assertThat(userGroupResult.getUserGroup().getIsSSOLinked()).isEqualTo(true);
      assertThat(userGroupResult.getUserGroup().getImportedByScim()).isEqualTo(false);
      QLLDAPSettings ldapSettingsOut = userGroupResult.getUserGroup().getSsoSetting();
      assertThat(ldapSettingsOut.getGroupDN()).isEqualTo(groupDN);
      assertThat(ldapSettingsOut.getGroupName()).isEqualTo(groupName);
      assertThat(ldapSettingsOut.getSsoProviderId()).isEqualTo(ldapSettings.getUuid());
    }
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testCreatingUserGroupWithDuplicateName() {
    ldapSettings = createLdapSettings();
    UserGroup userGroup = createUserGroup("UserGroupUpdate", "description");
    final User user = accountGenerator.ensureUser(
        "userId", random(String.class), random(String.class), random(String.class).toCharArray(), account);
    {
      String query = $GQL(/*
        mutation{
             createUserGroup(input:%s){
             userGroup{
                   id
                   name
              }
           }
      }*/ getCreateUserGroupGQL("UserGroupUpdate", user.getUuid()));
      final ExecutionResult result = qlResult(query, accountId);
      assertThat(result.getErrors().size()).isEqualTo(1);
      assertThat(result.getErrors().get(0).getMessage())
          .isEqualTo(
              "Exception while fetching data (/createUserGroup) : Invalid request: A user group already exists with the name UserGroupUpdate");
    }
  }

  private String getUpdatedUserGroupGQL(String groupName, String userGroupId) {
    String updatedUserGroup = $GQL(
        /* {
              name: "%s",
              description: "descc",
              permissions: {
                   accountPermissions: {
                    accountPermissionTypes: ADMINISTER_OTHER_ACCOUNT_FUNCTIONS
                  }
              },
              userGroupId : "%s",
              notificationSettings: {
                 sendMailToNewMembers: true,
                 sendNotificationToMembers: true,
                 slackNotificationSetting:
                     {
                       slackWebhookURL: "https://abc",
                       slackChannelName: "cool"
                     },
                 groupEmailAddresses: "abc@gmail.com"
              },
           clientMutationId: "abc"
         }
       */
        groupName, userGroupId);
    return updatedUserGroup;
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testUpdateUserGroup() {
    final Randomizer.Seed seed = new Randomizer.Seed(0);
    final OwnerManager.Owners owners = ownerManager.create();
    owners.add(EmbeddedUser.builder().uuid(generateUuid()).build());
    String name = "AccountPermission-UserGroup-" + System.currentTimeMillis();
    String description = "\"Test UserGroup\"";
    UserGroup userGroup = createUserGroup(name, description);

    {
      String query = $GQL(/*
mutation{
  updateUserGroup(input:%s){
    clientMutationId
  }
}*/ getUpdatedUserGroupGQL("gqltests", userGroup.getUuid()));
      final ExecutionResult result = qlResult(query, accountId);
      UserGroup updatedUserGroup = userGroupService.get(userGroup.getAccountId(), userGroup.getUuid());
      assertThat(userGroup.getUuid()).isEqualTo(updatedUserGroup.getUuid());
      assertThat(updatedUserGroup.getName()).isEqualTo("gqltests");
      assertThat(updatedUserGroup.getDescription()).isEqualTo("descc");
      assertThat(updatedUserGroup.getNotificationSettings().isUseIndividualEmails()).isEqualTo(true);
      assertThat(updatedUserGroup.getNotificationSettings().isSendMailToNewMembers()).isEqualTo(true);
      assertThat(updatedUserGroup.getNotificationSettings().getSlackConfig().getOutgoingWebhookUrl())
          .isEqualTo("https://abc");
      assertThat(updatedUserGroup.getNotificationSettings().getSlackConfig().getName()).isEqualTo("cool");
      assertThat(updatedUserGroup.getNotificationSettings().getEmailAddresses().contains("abc@gmail.com")).isTrue();
    }
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testUpdatingUserGroupWithDuplicateName() {
    UserGroup userGroup = createUserGroup("GroupNameWhichAlreadyExists", "description");
    UserGroup userGroup1 = createUserGroup("someUserGroup", "description");
    {
      String query = $GQL(/*
      mutation{
        updateUserGroup(input:%s){
          clientMutationId
        }
      }*/ getUpdatedUserGroupGQL("GroupNameWhichAlreadyExists", userGroup1.getUuid()));
      final ExecutionResult result = qlResult(query, accountId);
      assertThat(result.getErrors().size()).isEqualTo(1);
      assertThat(result.getErrors().get(0).getMessage())
          .isEqualTo(
              "Exception while fetching data (/updateUserGroup) : Invalid request: A user group already exists with the name GroupNameWhichAlreadyExists");
    }
  }

  private String getUpdatedSSOSettingsUserGroupGQL(String userGroupId) {
    String updatedUserGroup = $GQL(
        /* {
              name: "gqltests",
              description: "descc",
              permissions: {
                   accountPermissions: {
                    accountPermissionTypes: ADMINISTER_OTHER_ACCOUNT_FUNCTIONS
                  }
              },
              userGroupId : "%s",
              ssoSetting:  {
                  ldapSettings: null,
                  samlSettings: {
                      groupName: "%s",
                      ssoProviderId: "%s"
                   }
              }
              clientMutationId: "abc"
         }
       */
        userGroupId, samlGroupName, samlSettings.getUuid());
    return updatedUserGroup;
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testUpdatingSSOSettingsUserGroup() {
    final Randomizer.Seed seed = new Randomizer.Seed(0);
    final OwnerManager.Owners owners = ownerManager.create();
    owners.add(EmbeddedUser.builder().uuid(generateUuid()).build());
    String name = "AccountPermission-UserGroup-" + System.currentTimeMillis();
    String description = "\"Test UserGroup\"";

    samlSettings = SamlSettings.builder()
                       .metaDataFile("TestMetaDataFile")
                       .url("TestURL")
                       .accountId(accountId)
                       .displayName("Okta")
                       .origin("TestOrigin")
                       .build();

    samlSettings = ssoSettingService.saveSamlSettings(samlSettings);
    UserGroup userGroup = createUserGroup(name, description);

    {
      String query = $GQL(/*
mutation{
  updateUserGroup(input:%s){
    clientMutationId
  }
}*/ getUpdatedSSOSettingsUserGroupGQL(userGroup.getUuid()));
      final ExecutionResult result = qlResult(query, accountId);
      UserGroup updatedUserGroup = userGroupService.get(userGroup.getAccountId(), userGroup.getUuid());
      assertThat(userGroup.getUuid()).isEqualTo(updatedUserGroup.getUuid());
      assertThat(updatedUserGroup.getName()).isEqualTo("gqltests");
      assertThat(updatedUserGroup.getDescription()).isEqualTo("descc");
      assertThat(updatedUserGroup.getSsoGroupName()).isEqualTo(samlGroupName);
      assertThat(updatedUserGroup.getSsoGroupId()).isEqualTo(samlGroupName);
      assertThat(updatedUserGroup.getLinkedSsoId()).isEqualTo(samlSettings.getUuid());
      assertThat(updatedUserGroup.isSsoLinked()).isEqualTo(true);
      assertThat(updatedUserGroup.getLinkedSsoDisplayName()).isEqualTo(samlSettings.getDisplayName());
      assertThat(updatedUserGroup.getLinkedSsoType().toString()).isEqualTo("SAML");
    }
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryMissingUserGroup() {
    String query =
            $GQL(/*
            query{
                userGroup(userGroupId:"blah"){
                    id
                    name
                    description
                 }
            }*/);
    final ExecutionResult result = qlResult(query, accountId);
    assertThat(result.getErrors().size()).isEqualTo(1);

    assertThat(result.getErrors().get(0).getMessage())
        .isEqualTo("Exception while fetching data (/userGroup) : Invalid request: No User Group exists");
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryUserGroups() {
    String userGroup1Name = "AccountPermission-UserGroup1-" + System.currentTimeMillis();
    String userGroup2Name = "AccountPermission-UserGroup2-" + System.currentTimeMillis();
    String userGroup3Name = "AccountPermission-UserGroup3-" + System.currentTimeMillis();
    String description = "\"Test UserGroup\"";
    final UserGroup userGroup1 = createUserGroup(userGroup1Name, description);
    final UserGroup userGroup2 = createUserGroup(userGroup2Name, description);
    final UserGroup userGroup3 = createUserGroup(userGroup3Name, description);

    {
      String query = $GQL(
          /*
       query{
        userGroups(limit: 2){
             nodes {
                id
                name
                description
               }
            }
   }*/);

      QLUserGroupConnection userGroupConnection =
          qlExecute(QLUserGroupConnection.class, query, userGroup1.getAccountId());
      assertThat(userGroupConnection.getNodes().size()).isEqualTo(2);
    }

    {
      String query = $GQL(
          /*
   query{
     userGroups(limit: 2, offset: 1){
         nodes {
                id
                name
                description
              }
        }
   }*/);

      QLUserGroupConnection userGroupConnection =
          qlExecute(QLUserGroupConnection.class, query, userGroup1.getAccountId());
      assertThat(userGroupConnection.getNodes().size()).isEqualTo(2);
    }
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testDeleteUserGroup() {
    final UserGroup userGroup1 = createUserGroup("userGroup1Name", "description");
    String query = $GQL(/*
                 mutation {
                    deleteUserGroup(input:{
                       clientMutationId: "abc",
                       userGroupId: "%s"
                       }){
                      clientMutationId
                    }
            }*/ userGroup1.getUuid());
    final QLTestObject qlTestObject = qlExecute(query, accountId);
    UserGroup userGroup = userGroupService.get(accountId, generateUuid());
    if (userGroup != null) {
      assertThat(false).isTrue();
    }
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category({GraphQLTests.class, UnitTests.class})
  public void test_userGroupByName() {
    String userGroupQueryPattern = MultilineStringMixin.$.GQL(/*
  {
  userGroupByName(name:"%s"){
    name
    id
    description
  }
}
*/ UserGroupTest.class);
    String query = String.format(userGroupQueryPattern, "harnessUserGroup");
    final Account account =
        accountGenerator.ensureAccount(random(String.class), random(String.class), AccountType.TRIAL);
    UserGroup userGroup = createUserGroup("harnessUserGroup", "sample user group");

    final QLTestObject qlUserGroupObject = qlExecute(query, account.getUuid());
    assertThat(qlUserGroupObject.get(QLUserGroupKeys.name)).isEqualTo(userGroup.getName());
    assertThat(qlUserGroupObject.get(QLUserGroupKeys.description)).isEqualTo(userGroup.getDescription());
  }
}
