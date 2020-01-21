package io.harness;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import graphql.ExecutionResult;
import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.AccountGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.rule.Owner;
import io.harness.testframework.graphql.QLTestObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Account;
import software.wings.beans.notification.NotificationSettings;
import software.wings.beans.security.UserGroup;
import software.wings.graphql.schema.type.usergroup.QLUserGroup.QLUserGroupKeys;
import software.wings.graphql.schema.type.usergroup.QLUserGroupConnection;
import software.wings.service.intfc.UserGroupService;

import java.util.Arrays;

@Slf4j
public class UserGroupTest extends GraphQLTest {
  @Inject UserGroupService userGroupService;
  @Inject private OwnerManager ownerManager;
  private String accountId;
  @Inject AccountGenerator accountGenerator;

  @Before
  public void setup() {
    final Randomizer.Seed seed = new Randomizer.Seed(0);
    final OwnerManager.Owners owners = ownerManager.create();
    final Account account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
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

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryUserGroup() {
    final Randomizer.Seed seed = new Randomizer.Seed(0);
    final OwnerManager.Owners owners = ownerManager.create();
    owners.add(EmbeddedUser.builder().uuid(generateUuid()).build());
    String name = "AccountPermission-UserGroup-" + System.currentTimeMillis();
    String description = "\"Test UserGroup\"";
    UserGroup userGroup = createUserGroup(name, description);

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
             }
        }*/ userGroup.getUuid());

      final QLTestObject qlTestObject = qlExecute(query, accountId);
      assertThat(qlTestObject.get(QLUserGroupKeys.id)).isEqualTo(userGroup.getUuid());
      assertThat(qlTestObject.get(QLUserGroupKeys.name)).isEqualTo(userGroup.getName());
      assertThat(qlTestObject.get(QLUserGroupKeys.description)).isEqualTo(userGroup.getDescription());
      assertThat(qlTestObject.get(QLUserGroupKeys.isSSOLinked)).isEqualTo(userGroup.isSsoLinked());
      assertThat(qlTestObject.get(QLUserGroupKeys.importedByScim)).isEqualTo(userGroup.isImportedByScim());
    }
  }

  private String getUpdatedUserGroupGQL(String userGroupId) {
    String updatedUserGroup = $GQL(
        /* {
              name: "gqltests",
              description: "descc",
              permissions: {
                   accountPermissions: ADMINISTER_OTHER_ACCOUNT_FUNCTIONS
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
           requestId: "abc"
         }
       */
        userGroupId);
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
    requestId
  }
}*/ getUpdatedUserGroupGQL(userGroup.getUuid()));
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
        .isEqualTo(
            "Exception while fetching data (/userGroup) : Invalid request: No userGroup exists with the id blah");
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
      String query =
          $GQL(/*
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
                       requestId: "abc",
                       userGroupId: "%s"
                       }){
                      requestId
                    }
            }*/ userGroup1.getUuid());
    final QLTestObject qlTestObject = qlExecute(query, accountId);
    UserGroup userGroup = userGroupService.get(accountId, generateUuid());
    if (userGroup != null) {
      assertThat(false).isTrue();
    }
  }
}
