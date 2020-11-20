package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.delegate.AccountId;
import io.harness.delegate.beans.DelegateProfileDetails;
import io.harness.delegate.beans.ScopingRuleDetails;
import io.harness.delegateprofile.DelegateProfileGrpc;
import io.harness.delegateprofile.DelegateProfilePageResponseGrpc;
import io.harness.delegateprofile.ProfileId;
import io.harness.delegateprofile.ProfileScopingRule;
import io.harness.delegateprofile.ScopingValues;
import io.harness.exception.InvalidArgumentsException;
import io.harness.grpc.DelegateProfileServiceGrpcClient;
import io.harness.paging.PageRequestGrpc;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.tasks.Cd1SetupFields;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.service.impl.DelegateProfileManagerServiceImpl;
import software.wings.service.intfc.AppService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class DelegateProfileManagerServiceTest extends WingsBaseTest {
  private static final String ACCOUNT_ID = generateUuid();
  private static String DELEGATE_PROFILE_ID = generateUuid();
  private Application app = anApplication().uuid(APP_ID).name(APP_NAME).accountId(ACCOUNT_ID).build();

  @Mock private DelegateProfileServiceGrpcClient delegateProfileServiceGrpcClient;
  @InjectMocks private DelegateProfileManagerServiceImpl delegateProfileManagerService;
  @Mock private AppService appService;

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  @Owner(developers = OwnerRule.SANJA)
  @Category(UnitTests.class)
  public void shouldList() {
    PageRequest<DelegateProfileDetails> pageRequest = new PageRequest<>();
    pageRequest.setOffset("0");
    pageRequest.setLimit("0");
    DelegateProfilePageResponseGrpc delegateProfilePageResponseGrpc =
        DelegateProfilePageResponseGrpc.newBuilder().build();

    when(delegateProfileServiceGrpcClient.listProfiles(any(AccountId.class), any(PageRequestGrpc.class)))
        .thenReturn(null)
        .thenReturn(delegateProfilePageResponseGrpc);

    PageResponse<DelegateProfileDetails> delegateProfileDetailsPageResponse =
        delegateProfileManagerService.list(ACCOUNT_ID, pageRequest);
    Assertions.assertThat(delegateProfileDetailsPageResponse).isNull();

    delegateProfileDetailsPageResponse = delegateProfileManagerService.list(ACCOUNT_ID, pageRequest);
    Assertions.assertThat(delegateProfileDetailsPageResponse).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.SANJA)
  @Category(UnitTests.class)
  public void shouldGet() {
    DelegateProfileGrpc delegateProfileGrpc = DelegateProfileGrpc.newBuilder()
                                                  .setAccountId(AccountId.newBuilder().setId(generateUuid()).build())
                                                  .setProfileId(ProfileId.newBuilder().setId(generateUuid()).build())
                                                  .build();

    when(delegateProfileServiceGrpcClient.getProfile(any(AccountId.class), any(ProfileId.class)))
        .thenReturn(null)
        .thenReturn(delegateProfileGrpc);

    DelegateProfileDetails updatedDelegateProfileDetails =
        delegateProfileManagerService.get(ACCOUNT_ID, delegateProfileGrpc.getProfileId().getId());
    Assertions.assertThat(updatedDelegateProfileDetails).isNull();

    updatedDelegateProfileDetails =
        delegateProfileManagerService.get(ACCOUNT_ID, delegateProfileGrpc.getProfileId().getId());
    Assertions.assertThat(updatedDelegateProfileDetails).isNotNull();
    Assertions.assertThat(updatedDelegateProfileDetails.getUuid())
        .isEqualTo(delegateProfileGrpc.getProfileId().getId());
    Assertions.assertThat(updatedDelegateProfileDetails.getAccountId())
        .isEqualTo(delegateProfileGrpc.getAccountId().getId());
  }

  @Test
  @Owner(developers = OwnerRule.SANJA)
  @Category(UnitTests.class)
  public void shouldUpdate() {
    Map<String, ScopingValues> scopingEntities = new HashMap<>();
    scopingEntities.put(Cd1SetupFields.APP_ID_FIELD, ScopingValues.newBuilder().addValue("appId").build());
    scopingEntities.put(
        Cd1SetupFields.ENV_ID_FIELD, ScopingValues.newBuilder().addAllValue(Arrays.asList("env1", "env2")).build());

    DelegateProfileDetails profileDetail = DelegateProfileDetails.builder()
                                               .accountId(ACCOUNT_ID)
                                               .name("test")
                                               .description("description")
                                               .startupScript("startupScript")
                                               .build();
    ScopingRuleDetails scopingRuleDetail = ScopingRuleDetails.builder()
                                               .description("test")
                                               .environmentIds(new HashSet(Arrays.asList("env1", "env2")))
                                               .applicationId("appId")
                                               .build();
    profileDetail.setScopingRules(Arrays.asList(scopingRuleDetail));

    DelegateProfileGrpc delegateProfileGrpc =
        DelegateProfileGrpc.newBuilder()
            .setName("test")
            .setDescription("description")
            .setStartupScript("startupScript")
            .setAccountId(AccountId.newBuilder().setId(ACCOUNT_ID).build())
            .addScopingRules(
                ProfileScopingRule.newBuilder().setDescription("test").putAllScopingEntities(scopingEntities).build())
            .setProfileId(ProfileId.newBuilder().setId(generateUuid()).build())
            .build();

    when(delegateProfileServiceGrpcClient.updateProfile(any(DelegateProfileGrpc.class)))
        .thenReturn(null)
        .thenReturn(delegateProfileGrpc);

    DelegateProfileDetails updatedDelegateProfileDetails = delegateProfileManagerService.update(profileDetail);
    Assertions.assertThat(updatedDelegateProfileDetails).isNull();

    updatedDelegateProfileDetails = delegateProfileManagerService.update(profileDetail);
    Assertions.assertThat(updatedDelegateProfileDetails).isNotNull();
    Assertions.assertThat(updatedDelegateProfileDetails.getUuid())
        .isEqualTo(delegateProfileGrpc.getProfileId().getId());
    Assertions.assertThat(updatedDelegateProfileDetails).isEqualToIgnoringGivenFields(profileDetail, "uuid");
    Assertions.assertThat(updatedDelegateProfileDetails.getAccountId()).isEqualTo(ACCOUNT_ID);
    Assertions.assertThat(updatedDelegateProfileDetails.getDescription()).isEqualTo("description");
    Assertions.assertThat(updatedDelegateProfileDetails.getScopingRules().get(0).getDescription()).isEqualTo("test");
    Assertions.assertThat(updatedDelegateProfileDetails.getScopingRules().get(0).getApplicationId()).isEqualTo("appId");
  }

  @Test
  @Owner(developers = OwnerRule.SANJA)
  @Category(UnitTests.class)
  public void shouldValidateScopesWhenUpdatingProfile() {
    DelegateProfileDetails profileDetail = DelegateProfileDetails.builder()
                                               .accountId(ACCOUNT_ID)
                                               .name("test")
                                               .description("description")
                                               .startupScript("startupScript")
                                               .build();
    ScopingRuleDetails scopingRuleDetail = ScopingRuleDetails.builder().description("test").build();
    profileDetail.setScopingRules(Arrays.asList(scopingRuleDetail));
    assertThatThrownBy(() -> delegateProfileManagerService.update(profileDetail))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage("The Scoping rule requires application!");

    DelegateProfileDetails profileDetail2 = DelegateProfileDetails.builder()
                                                .accountId(ACCOUNT_ID)
                                                .name("test2")
                                                .description("description")
                                                .startupScript("startupScript")
                                                .build();
    ScopingRuleDetails scopingRuleDetail2 = ScopingRuleDetails.builder().applicationId(APP_ID).build();
    ScopingRuleDetails scopingRuleDetail3 = ScopingRuleDetails.builder().applicationId(APP_ID).build();

    when(appService.get(APP_ID, false)).thenReturn(app);

    profileDetail2.setScopingRules(Arrays.asList(scopingRuleDetail2, scopingRuleDetail3));
    assertThatThrownBy(() -> delegateProfileManagerService.update(profileDetail2))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage(APP_NAME + " is already used for a scoping rule!");
  }

  @Test
  @Owner(developers = OwnerRule.SANJA)
  @Category(UnitTests.class)
  public void shouldAdd() {
    Map<String, ScopingValues> scopingEntities = new HashMap<>();
    scopingEntities.put(Cd1SetupFields.APP_ID_FIELD, ScopingValues.newBuilder().addValue("appId").build());
    scopingEntities.put(
        Cd1SetupFields.ENV_ID_FIELD, ScopingValues.newBuilder().addAllValue(Arrays.asList("env1", "env2")).build());

    DelegateProfileDetails profileDetail = DelegateProfileDetails.builder()
                                               .accountId(ACCOUNT_ID)
                                               .name("test")
                                               .description("description")
                                               .startupScript("startupScript")
                                               .build();
    ScopingRuleDetails scopingRuleDetail = ScopingRuleDetails.builder()
                                               .description("test")
                                               .environmentIds(new HashSet(Arrays.asList("env1", "env2")))
                                               .applicationId("appId")
                                               .build();
    profileDetail.setScopingRules(Arrays.asList(scopingRuleDetail));

    DelegateProfileGrpc delegateProfileGrpc =
        DelegateProfileGrpc.newBuilder()
            .setName("test")
            .setDescription("description")
            .setStartupScript("startupScript")
            .setAccountId(AccountId.newBuilder().setId(ACCOUNT_ID).build())
            .addScopingRules(
                ProfileScopingRule.newBuilder().setDescription("test").putAllScopingEntities(scopingEntities).build())
            .setProfileId(ProfileId.newBuilder().setId(generateUuid()).build())
            .build();

    when(delegateProfileServiceGrpcClient.addProfile(any(DelegateProfileGrpc.class))).thenReturn(delegateProfileGrpc);

    DelegateProfileDetails result = delegateProfileManagerService.add(profileDetail);
    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result).isEqualToIgnoringGivenFields(profileDetail, "uuid");
  }

  @Test
  @Owner(developers = OwnerRule.SANJA)
  @Category(UnitTests.class)
  public void shouldValidateScopesWhenAddingProfile() {
    DelegateProfileDetails profileDetail = DelegateProfileDetails.builder()
                                               .accountId(ACCOUNT_ID)
                                               .name("test")
                                               .description("description")
                                               .startupScript("startupScript")
                                               .build();
    ScopingRuleDetails scopingRuleDetail = ScopingRuleDetails.builder().description("test").build();
    profileDetail.setScopingRules(Arrays.asList(scopingRuleDetail));
    assertThatThrownBy(() -> delegateProfileManagerService.add(profileDetail))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage("The Scoping rule requires application!");
  }

  @Test
  @Owner(developers = OwnerRule.SANJA)
  @Category(UnitTests.class)
  public void shouldUpdateScopingRules() {
    DelegateProfileGrpc delegateProfileGrpc = DelegateProfileGrpc.newBuilder()
                                                  .setAccountId(AccountId.newBuilder().setId(generateUuid()).build())
                                                  .setProfileId(ProfileId.newBuilder().setId(generateUuid()).build())
                                                  .build();

    ScopingRuleDetails scopingRuleDetail = ScopingRuleDetails.builder()
                                               .description("test")
                                               .applicationId("appId")
                                               .environmentIds(new HashSet<>(asList("PROD")))
                                               .build();

    when(delegateProfileServiceGrpcClient.updateProfileScopingRules(
             any(AccountId.class), any(ProfileId.class), anyList()))
        .thenReturn(null)
        .thenReturn(delegateProfileGrpc);

    DelegateProfileDetails updatedDelegateProfileDetails = delegateProfileManagerService.updateScopingRules(
        ACCOUNT_ID, delegateProfileGrpc.getProfileId().getId(), asList(scopingRuleDetail));
    Assertions.assertThat(updatedDelegateProfileDetails).isNull();

    updatedDelegateProfileDetails = delegateProfileManagerService.updateScopingRules(
        ACCOUNT_ID, delegateProfileGrpc.getProfileId().getId(), asList(scopingRuleDetail));
    Assertions.assertThat(updatedDelegateProfileDetails).isNotNull();
    Assertions.assertThat(updatedDelegateProfileDetails.getUuid())
        .isEqualTo(delegateProfileGrpc.getProfileId().getId());
    Assertions.assertThat(updatedDelegateProfileDetails.getAccountId())
        .isEqualTo(delegateProfileGrpc.getAccountId().getId());
  }

  @Test
  @Owner(developers = OwnerRule.SANJA)
  @Category(UnitTests.class)
  public void shouldValidateScopesWhenUpdatingScopingRule() {
    DelegateProfileDetails profileDetail = DelegateProfileDetails.builder()
                                               .accountId(ACCOUNT_ID)
                                               .name("test")
                                               .description("description")
                                               .startupScript("startupScript")
                                               .build();
    ScopingRuleDetails scopingRuleDetail = ScopingRuleDetails.builder().description("test").build();
    profileDetail.setScopingRules(Arrays.asList(scopingRuleDetail));
    assertThatThrownBy(
        () -> delegateProfileManagerService.updateScopingRules(ACCOUNT_ID, generateUuid(), asList(scopingRuleDetail)))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage("The Scoping rule requires application!");
  }

  @Test
  @Owner(developers = OwnerRule.SANJA)
  @Category(UnitTests.class)
  public void shouldDelete() {
    delegateProfileManagerService.delete(ACCOUNT_ID, DELEGATE_PROFILE_ID);

    AccountId accountId = AccountId.newBuilder().setId(ACCOUNT_ID).build();
    ProfileId profileId = ProfileId.newBuilder().setId(DELEGATE_PROFILE_ID).build();
    verify(delegateProfileServiceGrpcClient, times(1)).deleteProfile(eq(accountId), eq(profileId));
  }

  @Test
  @Owner(developers = OwnerRule.VUK)
  @Category(UnitTests.class)
  public void shouldUpdateSelectors() {
    DelegateProfileGrpc delegateProfileGrpc = DelegateProfileGrpc.newBuilder()
                                                  .setAccountId(AccountId.newBuilder().setId(generateUuid()).build())
                                                  .setProfileId(ProfileId.newBuilder().setId(generateUuid()).build())
                                                  .build();

    List<String> selectors = Arrays.asList("selectors");

    when(delegateProfileServiceGrpcClient.updateProfileSelectors(any(AccountId.class), any(ProfileId.class), anyList()))
        .thenReturn(null)
        .thenReturn(delegateProfileGrpc);

    DelegateProfileDetails updatedDelegateProfileDetails = delegateProfileManagerService.updateSelectors(
        ACCOUNT_ID, delegateProfileGrpc.getProfileId().getId(), selectors);
    Assertions.assertThat(updatedDelegateProfileDetails).isNull();

    updatedDelegateProfileDetails = delegateProfileManagerService.updateSelectors(
        ACCOUNT_ID, delegateProfileGrpc.getProfileId().getId(), selectors);

    Assertions.assertThat(updatedDelegateProfileDetails).isNotNull();
    Assertions.assertThat(updatedDelegateProfileDetails.getUuid())
        .isEqualTo(delegateProfileGrpc.getProfileId().getId());
    Assertions.assertThat(updatedDelegateProfileDetails.getAccountId())
        .isEqualTo(delegateProfileGrpc.getAccountId().getId());
  }

  @Test
  @Owner(developers = OwnerRule.VUK)
  @Category(UnitTests.class)
  public void shouldGenerateScopingRuleDescription() {
    ScopingValues scopingValuesAppId = ScopingValues.newBuilder().addValue("appId").build();
    ScopingValues scopingValuesEnvTypeId = ScopingValues.newBuilder().addValue("envTypeId").build();

    Map<String, ScopingValues> scopingEntities = new HashMap<>();
    scopingEntities.put("applicationId", scopingValuesAppId);
    scopingEntities.put("environmentTypeId", scopingValuesEnvTypeId);

    String description = delegateProfileManagerService.generateScopingRuleDescription(scopingEntities);

    Assertions.assertThat(description).isNotNull();
    Assertions.assertThat(description).isEqualTo("environmentTypeId: envTypeId; applicationId: appId; ");
  }
}
