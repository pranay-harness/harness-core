package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.delegate.AccountId;
import io.harness.delegate.beans.DelegateProfileDetails;
import io.harness.delegate.beans.ScopingRuleDetails;
import io.harness.delegateprofile.DelegateProfileGrpc;
import io.harness.delegateprofile.ProfileId;
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

import java.util.HashSet;

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
    thrown.expect(UnsupportedOperationException.class);
    thrown.expectMessage("not implemented");
    DelegateProfileDetails profileDetail = DelegateProfileDetails.builder().accountId(ACCOUNT_ID).name("test").build();
    delegateProfileManagerService.add(profileDetail);
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
    thrown.expect(UnsupportedOperationException.class);
    thrown.expectMessage("not implemented");
    delegateProfileManagerService.delete(ACCOUNT_ID, DELEGATE_PROFILE_ID);
  }

  @Test
  @Owner(developers = OwnerRule.MARKO)
  @Category(UnitTests.class)
  public void shouldUpdateSelectors() {
    thrown.expect(UnsupportedOperationException.class);
    thrown.expectMessage("not implemented");
    delegateProfileManagerService.updateSelectors(ACCOUNT_ID, DELEGATE_PROFILE_ID, asList("selector"));
  }
}
