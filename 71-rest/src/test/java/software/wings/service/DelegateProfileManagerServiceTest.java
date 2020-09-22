package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.ScopingRuleDetails.ScopingRuleDetailsKeys;
import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.delegate.AccountId;
import io.harness.delegate.beans.DelegateProfileDetails;
import io.harness.delegate.beans.ScopingRuleDetails;
import io.harness.delegateprofile.DelegateProfileGrpc;
import io.harness.delegateprofile.ProfileId;
import io.harness.delegateprofile.ProfileScopingRule;
import io.harness.delegateprofile.ScopingValues;
import io.harness.exception.UnsupportedOperationException;
import io.harness.grpc.DelegateProfileServiceGrpcClient;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.DelegateProfileManagerServiceImpl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class DelegateProfileManagerServiceTest extends WingsBaseTest {
  private static final String ACCOUNT_ID = generateUuid();
  private static String DELEGATE_PROFILE_ID = generateUuid();

  @Mock private DelegateProfileServiceGrpcClient delegateProfileServiceGrpcClient;
  @InjectMocks private DelegateProfileManagerServiceImpl delegateProfileManagerService;

  @Rule public ExpectedException thrown = ExpectedException.none();
  @Inject private WingsPersistence wingsPersistence;

  @Test
  @Owner(developers = OwnerRule.SANJA)
  @Category(UnitTests.class)
  public void shouldList() {
    thrown.expect(UnsupportedOperationException.class);
    thrown.expectMessage("not implemented");
    delegateProfileManagerService.list(ACCOUNT_ID);
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
    thrown.expect(UnsupportedOperationException.class);
    thrown.expectMessage("not implemented");
    DelegateProfileDetails profileDetail =
        DelegateProfileDetails.builder().accountId(ACCOUNT_ID).uuid(DELEGATE_PROFILE_ID).name("test").build();
    delegateProfileManagerService.update(profileDetail);
  }

  @Test
  @Owner(developers = OwnerRule.SANJA)
  @Category(UnitTests.class)
  public void shouldAdd() {
    Map<String, ScopingValues> scopingEntities = new HashMap<>();
    scopingEntities.put(ScopingRuleDetailsKeys.applicationId, ScopingValues.newBuilder().addValue("appId").build());
    scopingEntities.put(ScopingRuleDetailsKeys.environmentIds,
        ScopingValues.newBuilder().addAllValue(Arrays.asList("env1", "env2")).build());

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
  public void shouldUpdateScopingRules() {
    DelegateProfileGrpc delegateProfileGrpc = DelegateProfileGrpc.newBuilder()
                                                  .setAccountId(AccountId.newBuilder().setId(generateUuid()).build())
                                                  .setProfileId(ProfileId.newBuilder().setId(generateUuid()).build())
                                                  .build();

    ScopingRuleDetails scopingRuleDetail =
        ScopingRuleDetails.builder().description("test").environmentIds(new HashSet<>(asList("PROD"))).build();

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
}
