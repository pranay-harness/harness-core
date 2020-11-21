package io.harness.grpc;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.MARKO;
import static io.harness.rule.OwnerRule.SANJA;
import static io.harness.rule.OwnerRule.VUK;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.MockableTestMixin;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.delegate.AccountId;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateProfileScopingRule;
import io.harness.delegateprofile.DelegateProfileGrpc;
import io.harness.delegateprofile.DelegateProfilePageResponseGrpc;
import io.harness.delegateprofile.DelegateProfileServiceGrpc;
import io.harness.delegateprofile.ProfileId;
import io.harness.delegateprofile.ProfileScopingRule;
import io.harness.delegateprofile.ProfileSelector;
import io.harness.delegateprofile.ScopingValues;
import io.harness.exception.DelegateServiceDriverException;
import io.harness.paging.PageRequestGrpc;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.User;
import software.wings.security.PermissionAttribute;
import software.wings.service.intfc.DelegateProfileService;
import software.wings.service.intfc.UserService;

import com.google.common.collect.ImmutableMap;
import io.grpc.Channel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class DelegateProfileServiceGrpcImplTest extends WingsBaseTest implements MockableTestMixin {
  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private static final String NAME = "name";
  private static final String DESCRIPTION = "description";
  private static final String STARTUP_SCRIPT =
      "if which terraform; then\n  echo found terraform\nelse\n  curl -O https://releases.hashicorp.com/terraform/0.11.11/terraform_0.11.11_linux_amd64.zip\n  unzip terraform_0.11.11_linux_amd64.zip\n  chmod 755 terraform\n  mv terraform /usr/local/bin\nfi";
  private static final String SCOPING_ENTITY_KEY_APP_ID = "APP_ID";
  private static final String SCOPING_ENTITY_KEY_ENV_ID = "ENV_ID";
  private static final String SCOPING_RULE_DESCRIPTION = "rule";
  private static final String SELECTOR = "selector";

  private DelegateProfileServiceGrpcClient delegateProfileServiceGrpcClient;
  private DelegateProfileServiceGrpcImpl delegateProfileServiceGrpcImpl;

  private DelegateProfileService delegateProfileService;
  private UserService userService;

  private Server server;
  private Logger mockClientLogger;
  private Logger mockServerLogger;

  @Before
  public void setUp() throws Exception {
    mockClientLogger = mock(Logger.class);
    mockServerLogger = mock(Logger.class);
    setStaticFieldValue(DelegateServiceGrpcClient.class, "log", mockClientLogger);
    setStaticFieldValue(DelegateServiceGrpcClient.class, "log", mockServerLogger);

    String serverName = InProcessServerBuilder.generateName();
    Channel channel = grpcCleanup.register(InProcessChannelBuilder.forName(serverName).build());

    DelegateProfileServiceGrpc.DelegateProfileServiceBlockingStub delegateProfileServiceBlockingStub =
        DelegateProfileServiceGrpc.newBlockingStub(channel);
    delegateProfileServiceGrpcClient = new DelegateProfileServiceGrpcClient(delegateProfileServiceBlockingStub);

    delegateProfileService = mock(DelegateProfileService.class);
    userService = mock(UserService.class);
    when(userService.getUserFromCacheOrDB(anyString())).thenReturn(new User());
    delegateProfileServiceGrpcImpl = new DelegateProfileServiceGrpcImpl(delegateProfileService, userService);

    server = InProcessServerBuilder.forName(serverName)
                 .directExecutor()
                 .addService(delegateProfileServiceGrpcImpl)
                 .build()
                 .start();
    grpcCleanup.register(server);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testListProfiles() {
    String accountId = generateUuid();
    DelegateProfile delegateProfile = DelegateProfile.builder().accountId(accountId).description("description").build();

    PageResponse<DelegateProfile> pageResponse = new PageResponse<>();
    pageResponse.add(delegateProfile);

    when(delegateProfileService.list(any()))
        .thenThrow(new RuntimeException())
        .thenReturn(null)
        .thenReturn(pageResponse);

    // Test exception
    PageRequestGrpc pageRequestGrpc = PageRequestGrpc.newBuilder().setOffset("0").build();
    assertThatThrownBy(()
                           -> delegateProfileServiceGrpcClient.listProfiles(
                               AccountId.newBuilder().setId(accountId).build(), pageRequestGrpc))
        .isInstanceOf(DelegateServiceDriverException.class)
        .hasMessage("Unexpected error occurred while listing profiles.");

    DelegateProfilePageResponseGrpc delegateProfilePageResponseGrpc =
        delegateProfileServiceGrpcClient.listProfiles(AccountId.newBuilder().setId(accountId).build(), pageRequestGrpc);

    assertThat(delegateProfilePageResponseGrpc).isNotNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testGetProfile() {
    String accountId = generateUuid();
    String profileId = generateUuid();

    Map<String, Set<String>> scopingEntities = new HashMap<>();
    scopingEntities.put(SCOPING_ENTITY_KEY_APP_ID, new HashSet<>(Arrays.asList("app_id1", "app_id2")));
    scopingEntities.put(SCOPING_ENTITY_KEY_ENV_ID, new HashSet<>(Arrays.asList("env_id1")));

    DelegateProfileScopingRule scopingRule = DelegateProfileScopingRule.builder()
                                                 .description(SCOPING_RULE_DESCRIPTION)
                                                 .scopingEntities(scopingEntities)
                                                 .build();

    DelegateProfile delegateProfile = DelegateProfile.builder()
                                          .accountId(accountId)
                                          .uuid(profileId)
                                          .name(NAME)
                                          .description(DESCRIPTION)
                                          .primary(true)
                                          .approvalRequired(true)
                                          .startupScript(STARTUP_SCRIPT)
                                          .selectors(Arrays.asList(SELECTOR))
                                          .scopingRules(Arrays.asList(scopingRule))
                                          .build();

    when(delegateProfileService.get(accountId, profileId))
        .thenThrow(new RuntimeException())
        .thenReturn(null)
        .thenReturn(delegateProfile);

    // Test exception
    assertThatThrownBy(
        ()
            -> delegateProfileServiceGrpcClient.getProfile(
                AccountId.newBuilder().setId(accountId).build(), ProfileId.newBuilder().setId(profileId).build()))
        .isInstanceOf(DelegateServiceDriverException.class)
        .hasMessage("Unexpected error occurred while getting profile.");

    // Test no profile found
    DelegateProfileGrpc delegateProfileGrpc = delegateProfileServiceGrpcClient.getProfile(
        AccountId.newBuilder().setId(accountId).build(), ProfileId.newBuilder().setId(profileId).build());

    assertThat(delegateProfileGrpc).isNull();

    // Test profile found
    delegateProfileGrpc = delegateProfileServiceGrpcClient.getProfile(
        AccountId.newBuilder().setId(accountId).build(), ProfileId.newBuilder().setId(profileId).build());

    // Covers convert method also
    assertThat(delegateProfileGrpc).isNotNull();
    assertThat(delegateProfileGrpc.getAccountId()).isNotNull();
    assertThat(delegateProfileGrpc.getAccountId().getId()).isEqualTo(delegateProfile.getAccountId());
    assertThat(delegateProfileGrpc.getProfileId()).isNotNull();
    assertThat(delegateProfileGrpc.getProfileId().getId()).isEqualTo(delegateProfile.getUuid());
    assertThat(delegateProfileGrpc.getName()).isEqualTo(delegateProfile.getName());
    assertThat(delegateProfileGrpc.getDescription()).isEqualTo(delegateProfile.getDescription());
    assertThat(delegateProfileGrpc.getPrimary()).isEqualTo(delegateProfile.isPrimary());
    assertThat(delegateProfileGrpc.getApprovalRequired()).isEqualTo(delegateProfile.isApprovalRequired());
    assertThat(delegateProfileGrpc.getStartupScript()).isEqualTo(delegateProfile.getStartupScript());
    assertThat(delegateProfileGrpc.getSelectorsList()).isNotEmpty();
    assertThat(delegateProfileGrpc.getSelectorsList().get(0).getSelector()).isEqualTo(SELECTOR);
    assertThat(delegateProfileGrpc.getScopingRulesList()).isNotEmpty();
    assertThat(delegateProfileGrpc.getScopingRulesList().get(0)).isNotNull();
    assertThat(delegateProfileGrpc.getScopingRulesList().get(0).getDescription()).isEqualTo(SCOPING_RULE_DESCRIPTION);
    assertThat(delegateProfileGrpc.getScopingRulesList().get(0).getScopingEntitiesMap()).isNotNull();
    assertThat(delegateProfileGrpc.getScopingRulesList().get(0).getScopingEntitiesMap().get(SCOPING_ENTITY_KEY_APP_ID))
        .isNotNull();
    assertThat(delegateProfileGrpc.getScopingRulesList()
                   .get(0)
                   .getScopingEntitiesMap()
                   .get(SCOPING_ENTITY_KEY_APP_ID)
                   .getValueList())
        .contains("app_id1", "app_id2");
    assertThat(delegateProfileGrpc.getScopingRulesList().get(0).getScopingEntitiesMap().get(SCOPING_ENTITY_KEY_ENV_ID))
        .isNotNull();
    assertThat(delegateProfileGrpc.getScopingRulesList()
                   .get(0)
                   .getScopingEntitiesMap()
                   .get(SCOPING_ENTITY_KEY_ENV_ID)
                   .getValueList())
        .contains("env_id1");
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testAddProfile() {
    String accountId = generateUuid();
    String profileId = generateUuid();

    Map<String, ScopingValues> grpcScopingEntities = new HashMap<>();
    grpcScopingEntities.put(
        SCOPING_ENTITY_KEY_APP_ID, ScopingValues.newBuilder().addAllValue(Arrays.asList("app_id1", "app_id2")).build());
    grpcScopingEntities.put(
        SCOPING_ENTITY_KEY_ENV_ID, ScopingValues.newBuilder().addAllValue(Arrays.asList("env_id1")).build());

    DelegateProfileGrpc delegateProfileGrpc =
        DelegateProfileGrpc.newBuilder()
            .setAccountId(AccountId.newBuilder().setId(accountId).build())
            .setProfileId(ProfileId.newBuilder().setId(profileId).build())
            .setName(NAME)
            .setDescription(DESCRIPTION)
            .setPrimary(true)
            .setApprovalRequired(true)
            .setStartupScript(STARTUP_SCRIPT)
            .addAllSelectors(Arrays.asList(ProfileSelector.newBuilder().setSelector(SELECTOR).build()))
            .addAllScopingRules(Arrays.asList(ProfileScopingRule.newBuilder()
                                                  .setDescription(SCOPING_RULE_DESCRIPTION)
                                                  .putAllScopingEntities(grpcScopingEntities)
                                                  .build()))
            .build();

    Map<String, Set<String>> scopingEntities = new HashMap<>();
    scopingEntities.put(SCOPING_ENTITY_KEY_APP_ID, new HashSet<>(Arrays.asList("app_id1", "app_id2")));
    scopingEntities.put(SCOPING_ENTITY_KEY_ENV_ID, new HashSet<>(Arrays.asList("env_id1")));

    DelegateProfileScopingRule scopingRule = DelegateProfileScopingRule.builder()
                                                 .description(SCOPING_RULE_DESCRIPTION)
                                                 .scopingEntities(scopingEntities)
                                                 .build();

    DelegateProfile delegateProfile = DelegateProfile.builder()
                                          .accountId(accountId)
                                          .uuid(profileId)
                                          .name(NAME)
                                          .description(DESCRIPTION)
                                          .primary(true)
                                          .approvalRequired(true)
                                          .startupScript(STARTUP_SCRIPT)
                                          .selectors(Arrays.asList(SELECTOR))
                                          .scopingRules(Arrays.asList(scopingRule))
                                          .build();

    when(delegateProfileService.add(any(DelegateProfile.class)))
        .thenThrow(new RuntimeException())
        .thenReturn(delegateProfile);

    // Test exception
    assertThatThrownBy(() -> delegateProfileServiceGrpcClient.addProfile(delegateProfileGrpc))
        .isInstanceOf(DelegateServiceDriverException.class)
        .hasMessage("Unexpected error occurred while adding profile.");

    // Test profile added
    DelegateProfileGrpc savedDelegateProfileGrpc = delegateProfileServiceGrpcClient.addProfile(delegateProfileGrpc);

    // Covers convert method also
    assertThat(savedDelegateProfileGrpc).isNotNull();
    assertThat(savedDelegateProfileGrpc.getAccountId()).isNotNull();
    assertThat(savedDelegateProfileGrpc.getAccountId().getId()).isEqualTo(delegateProfile.getAccountId());
    assertThat(savedDelegateProfileGrpc.getProfileId()).isNotNull();
    assertThat(savedDelegateProfileGrpc.getProfileId().getId()).isEqualTo(delegateProfile.getUuid());
    assertThat(savedDelegateProfileGrpc.getName()).isEqualTo(delegateProfile.getName());
    assertThat(savedDelegateProfileGrpc.getDescription()).isEqualTo(delegateProfile.getDescription());
    assertThat(savedDelegateProfileGrpc.getPrimary()).isEqualTo(delegateProfile.isPrimary());
    assertThat(savedDelegateProfileGrpc.getApprovalRequired()).isEqualTo(delegateProfile.isApprovalRequired());
    assertThat(savedDelegateProfileGrpc.getStartupScript()).isEqualTo(delegateProfile.getStartupScript());
    assertThat(delegateProfileGrpc.getSelectorsList()).isNotEmpty();
    assertThat(delegateProfileGrpc.getSelectorsList().get(0).getSelector()).isEqualTo(SELECTOR);
    assertThat(delegateProfileGrpc.getScopingRulesList()).isNotEmpty();
    assertThat(savedDelegateProfileGrpc.getScopingRulesList().get(0)).isNotNull();
    assertThat(savedDelegateProfileGrpc.getScopingRulesList().get(0).getDescription())
        .isEqualTo(SCOPING_RULE_DESCRIPTION);
    assertThat(savedDelegateProfileGrpc.getScopingRulesList().get(0).getScopingEntitiesMap()).isNotNull();
    assertThat(
        savedDelegateProfileGrpc.getScopingRulesList().get(0).getScopingEntitiesMap().get(SCOPING_ENTITY_KEY_APP_ID))
        .isNotNull();
    assertThat(savedDelegateProfileGrpc.getScopingRulesList()
                   .get(0)
                   .getScopingEntitiesMap()
                   .get(SCOPING_ENTITY_KEY_APP_ID)
                   .getValueList())
        .contains("app_id1", "app_id2");
    assertThat(
        savedDelegateProfileGrpc.getScopingRulesList().get(0).getScopingEntitiesMap().get(SCOPING_ENTITY_KEY_ENV_ID))
        .isNotNull();
    assertThat(savedDelegateProfileGrpc.getScopingRulesList()
                   .get(0)
                   .getScopingEntitiesMap()
                   .get(SCOPING_ENTITY_KEY_ENV_ID)
                   .getValueList())
        .contains("env_id1");
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void testAddProfileShouldValidateScopes() {
    String accountId = generateUuid();
    String profileId = generateUuid();

    Map<String, ScopingValues> grpcScopingEntities = new HashMap<>();

    DelegateProfileGrpc delegateProfileGrpc =
        DelegateProfileGrpc.newBuilder()
            .setAccountId(AccountId.newBuilder().setId(accountId).build())
            .setProfileId(ProfileId.newBuilder().setId(profileId).build())
            .setName(NAME)
            .setDescription(DESCRIPTION)
            .setPrimary(true)
            .setApprovalRequired(true)
            .setStartupScript(STARTUP_SCRIPT)
            .addAllSelectors(Arrays.asList(ProfileSelector.newBuilder().setSelector(SELECTOR).build()))
            .addAllScopingRules(Arrays.asList(ProfileScopingRule.newBuilder()
                                                  .setDescription(SCOPING_RULE_DESCRIPTION)
                                                  .putAllScopingEntities(grpcScopingEntities)
                                                  .build()))
            .build();

    assertThatThrownBy(() -> delegateProfileServiceGrpcClient.addProfile(delegateProfileGrpc))
        .isInstanceOf(DelegateServiceDriverException.class)
        .hasMessage("Scoping rule should have at least one scoping value set!");
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testUpdateProfile() {
    String accountId = generateUuid();
    String profileId = generateUuid();

    DelegateProfileGrpc delegateProfileGrpc = DelegateProfileGrpc.newBuilder()
                                                  .setAccountId(AccountId.newBuilder().setId(accountId).build())
                                                  .setProfileId(ProfileId.newBuilder().setId(profileId).build())
                                                  .setName(NAME)
                                                  .build();

    DelegateProfile delegateProfile = DelegateProfile.builder().accountId(accountId).uuid(profileId).name(NAME).build();

    when(delegateProfileService.update(any(DelegateProfile.class)))
        .thenThrow(new RuntimeException())
        .thenReturn(delegateProfile);

    // Test exception
    assertThatThrownBy(() -> delegateProfileServiceGrpcClient.updateProfile(delegateProfileGrpc))
        .isInstanceOf(DelegateServiceDriverException.class)
        .hasMessage("Unexpected error occurred while updating profile.");

    // Test profile updated
    DelegateProfileGrpc savedDelegateProfileGrpc = delegateProfileServiceGrpcClient.updateProfile(delegateProfileGrpc);
    assertThat(savedDelegateProfileGrpc).isEqualTo(delegateProfileGrpc);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void testUpdateProfileShouldValidateScopes() {
    String accountId = generateUuid();
    String profileId = generateUuid();

    Map<String, ScopingValues> grpcScopingEntities = new HashMap<>();

    DelegateProfileGrpc delegateProfileGrpc =
        DelegateProfileGrpc.newBuilder()
            .setAccountId(AccountId.newBuilder().setId(accountId).build())
            .setProfileId(ProfileId.newBuilder().setId(profileId).build())
            .setName(NAME)
            .setDescription(DESCRIPTION)
            .setPrimary(true)
            .setApprovalRequired(true)
            .setStartupScript(STARTUP_SCRIPT)
            .addAllSelectors(Arrays.asList(ProfileSelector.newBuilder().setSelector(SELECTOR).build()))
            .addAllScopingRules(Arrays.asList(ProfileScopingRule.newBuilder()
                                                  .setDescription(SCOPING_RULE_DESCRIPTION)
                                                  .putAllScopingEntities(grpcScopingEntities)
                                                  .build()))
            .build();

    assertThatThrownBy(() -> delegateProfileServiceGrpcClient.updateProfile(delegateProfileGrpc))
        .isInstanceOf(DelegateServiceDriverException.class)
        .hasMessage("Scoping rule should have at least one scoping value set!");
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testDeleteProfile() {
    String accountId = generateUuid();
    String profileId = generateUuid();

    doThrow(new RuntimeException()).when(delegateProfileService).delete(accountId, profileId);

    // Test exception
    assertThatThrownBy(
        ()
            -> delegateProfileServiceGrpcClient.deleteProfile(
                AccountId.newBuilder().setId(accountId).build(), ProfileId.newBuilder().setId(profileId).build()))
        .isInstanceOf(DelegateServiceDriverException.class)
        .hasMessage("Unexpected error occurred while deleting profile.");

    // Test profile deleted
    doNothing().when(delegateProfileService).delete(accountId, profileId);
    try {
      delegateProfileServiceGrpcClient.deleteProfile(
          AccountId.newBuilder().setId(accountId).build(), ProfileId.newBuilder().setId(profileId).build());
    } catch (Exception ex) {
      fail("Unexpected error occurred while testing profile deletion");
    }
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testUpdateProfileSelectors() {
    String accountId = generateUuid();
    String profileId = generateUuid();

    doThrow(new RuntimeException())
        .when(delegateProfileService)
        .updateDelegateProfileSelectors(profileId, accountId, null);

    // Test exception
    assertThatThrownBy(
        ()
            -> delegateProfileServiceGrpcClient.updateProfileSelectors(
                AccountId.newBuilder().setId(accountId).build(), ProfileId.newBuilder().setId(profileId).build(), null))
        .isInstanceOf(DelegateServiceDriverException.class)
        .hasMessage("Unexpected error occurred while updating profile selectors.");

    // Test update profile selectors
    try {
      delegateProfileServiceGrpcClient.updateProfileSelectors(AccountId.newBuilder().setId(accountId).build(),
          ProfileId.newBuilder().setId(profileId).build(),
          Arrays.asList(ProfileSelector.newBuilder().setSelector("test").build()));

      ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
      verify(delegateProfileService, times(2))
          .updateDelegateProfileSelectors(eq(profileId), eq(accountId), argumentCaptor.capture());

      List<PermissionAttribute> selectors = argumentCaptor.getValue();
      assertThat(selectors).hasSize(1);
      assertThat(selectors.get(0)).isEqualTo("test");
    } catch (Exception ex) {
      fail("Unexpected error occurred while testing update of the profile selectors");
    }
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testUpdateProfileScopingRules() {
    String accountId = generateUuid();
    String profileId = generateUuid();

    doThrow(new RuntimeException()).when(delegateProfileService).updateScopingRules(accountId, profileId, null);

    // Test exception
    assertThatThrownBy(
        ()
            -> delegateProfileServiceGrpcClient.updateProfileScopingRules(
                AccountId.newBuilder().setId(accountId).build(), ProfileId.newBuilder().setId(profileId).build(), null))
        .isInstanceOf(DelegateServiceDriverException.class)
        .hasMessage("Unexpected error occurred while updating profile scoping rules.");

    // Test update scoping rules
    Map<String, ScopingValues> profileScopingRuleValues =
        ImmutableMap.of("testKey", ScopingValues.newBuilder().addAllValue(Arrays.asList("scopingValues")).build());

    when(delegateProfileService.updateScopingRules(eq(accountId), eq(profileId), any(List.class)))
        .thenReturn(DelegateProfile.builder().uuid(profileId).accountId(accountId).name(NAME).build());

    DelegateProfileGrpc delegateProfileGrpc = delegateProfileServiceGrpcClient.updateProfileScopingRules(
        AccountId.newBuilder().setId(accountId).build(), ProfileId.newBuilder().setId(profileId).build(),
        Arrays.asList(ProfileScopingRule.newBuilder()
                          .setDescription("testDescription")
                          .putAllScopingEntities(profileScopingRuleValues)
                          .build()));

    assertThat(delegateProfileGrpc).isNotNull();
    ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
    verify(delegateProfileService, times(2)).updateScopingRules(eq(accountId), eq(profileId), argumentCaptor.capture());

    List<PermissionAttribute> scopingEntities = argumentCaptor.getValue();
    assertThat(scopingEntities).isNotNull();
    assertThat(scopingEntities).hasSize(1);

    // Test profile not found
    when(delegateProfileService.updateScopingRules(accountId, profileId, null)).thenReturn(null);

    delegateProfileGrpc = delegateProfileServiceGrpcClient.updateProfileScopingRules(
        AccountId.newBuilder().setId(accountId).build(), ProfileId.newBuilder().setId(profileId).build(), null);

    assertThat(delegateProfileGrpc).isNull();
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void testUpdateScopingRulesShouldValidateScopes() {
    String accountId = generateUuid();
    String profileId = generateUuid();

    Map<String, ScopingValues> grpcScopingEntities = new HashMap<>();

    assertThatThrownBy(
        ()
            -> delegateProfileServiceGrpcClient.updateProfileScopingRules(
                AccountId.newBuilder().setId(accountId).build(), ProfileId.newBuilder().setId(profileId).build(),
                Arrays.asList(ProfileScopingRule.newBuilder()
                                  .setDescription(SCOPING_RULE_DESCRIPTION)
                                  .putAllScopingEntities(grpcScopingEntities)
                                  .build())))
        .isInstanceOf(DelegateServiceDriverException.class)
        .hasMessage("Scoping rule should have at least one scoping value set!");
  }
}
