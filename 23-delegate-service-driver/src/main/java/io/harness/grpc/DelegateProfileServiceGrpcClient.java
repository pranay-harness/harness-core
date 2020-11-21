package io.harness.grpc;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.delegate.AccountId;
import io.harness.delegateprofile.AddProfileRequest;
import io.harness.delegateprofile.AddProfileResponse;
import io.harness.delegateprofile.DelegateProfileGrpc;
import io.harness.delegateprofile.DelegateProfilePageResponseGrpc;
import io.harness.delegateprofile.DelegateProfileServiceGrpc.DelegateProfileServiceBlockingStub;
import io.harness.delegateprofile.DeleteProfileRequest;
import io.harness.delegateprofile.GetProfileRequest;
import io.harness.delegateprofile.GetProfileResponse;
import io.harness.delegateprofile.ListProfilesRequest;
import io.harness.delegateprofile.ListProfilesResponse;
import io.harness.delegateprofile.ProfileId;
import io.harness.delegateprofile.ProfileScopingRule;
import io.harness.delegateprofile.ProfileSelector;
import io.harness.delegateprofile.ScopingValues;
import io.harness.delegateprofile.UpdateProfileRequest;
import io.harness.delegateprofile.UpdateProfileResponse;
import io.harness.delegateprofile.UpdateProfileScopingRulesRequest;
import io.harness.delegateprofile.UpdateProfileScopingRulesResponse;
import io.harness.delegateprofile.UpdateProfileSelectorsRequest;
import io.harness.delegateprofile.UpdateProfileSelectorsResponse;
import io.harness.exception.DelegateServiceDriverException;
import io.harness.paging.PageRequestGrpc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.grpc.StatusRuntimeException;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class DelegateProfileServiceGrpcClient {
  private final DelegateProfileServiceBlockingStub delegateProfileServiceBlockingStub;

  @Inject
  public DelegateProfileServiceGrpcClient(DelegateProfileServiceBlockingStub delegateProfileServiceBlockingStub) {
    this.delegateProfileServiceBlockingStub = delegateProfileServiceBlockingStub;
  }

  public DelegateProfilePageResponseGrpc listProfiles(AccountId accountId, PageRequestGrpc pageRequest) {
    try {
      ListProfilesResponse listProfilesResponse = delegateProfileServiceBlockingStub.listProfiles(
          ListProfilesRequest.newBuilder().setAccountId(accountId).setPageRequest(pageRequest).build());

      return listProfilesResponse.getResponse();
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while listing profiles.", ex);
    }
  }

  public DelegateProfileGrpc getProfile(AccountId accountId, ProfileId profileId) {
    try {
      GetProfileResponse getProfileResponse = delegateProfileServiceBlockingStub.getProfile(
          GetProfileRequest.newBuilder().setAccountId(accountId).setProfileId(profileId).build());

      if (!getProfileResponse.hasProfile()) {
        return null;
      }

      return getProfileResponse.getProfile();
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while getting profile.", ex);
    }
  }

  public DelegateProfileGrpc addProfile(DelegateProfileGrpc delegateProfileGrpc) {
    try {
      validateScopingRules(delegateProfileGrpc.getScopingRulesList());
      AddProfileResponse addProfileResponse = delegateProfileServiceBlockingStub.addProfile(
          AddProfileRequest.newBuilder().setProfile(delegateProfileGrpc).build());

      return addProfileResponse.getProfile();
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while adding profile.", ex);
    }
  }

  public DelegateProfileGrpc updateProfile(DelegateProfileGrpc delegateProfileGrpc) {
    try {
      validateScopingRules(delegateProfileGrpc.getScopingRulesList());
      UpdateProfileResponse updateProfileResponse = delegateProfileServiceBlockingStub.updateProfile(
          UpdateProfileRequest.newBuilder().setProfile(delegateProfileGrpc).build());

      if (!updateProfileResponse.hasProfile()) {
        return null;
      }

      return updateProfileResponse.getProfile();
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while updating profile.", ex);
    }
  }

  public void deleteProfile(AccountId accountId, ProfileId profileId) {
    try {
      delegateProfileServiceBlockingStub.deleteProfile(
          DeleteProfileRequest.newBuilder().setAccountId(accountId).setProfileId(profileId).build());
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while deleting profile.", ex);
    }
  }

  public DelegateProfileGrpc updateProfileSelectors(
      AccountId accountId, ProfileId profileId, List<ProfileSelector> selectors) {
    try {
      if (selectors == null) {
        selectors = Collections.emptyList();
      }

      UpdateProfileSelectorsResponse updateProfileSelectorsResponse =
          delegateProfileServiceBlockingStub.updateProfileSelectors(UpdateProfileSelectorsRequest.newBuilder()
                                                                        .setAccountId(accountId)
                                                                        .setProfileId(profileId)
                                                                        .addAllSelectors(selectors)
                                                                        .build());

      if (!updateProfileSelectorsResponse.hasProfile()) {
        return null;
      }

      return updateProfileSelectorsResponse.getProfile();

    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while updating profile selectors.", ex);
    }
  }

  public DelegateProfileGrpc updateProfileScopingRules(
      AccountId accountId, ProfileId profileId, List<ProfileScopingRule> scopingRules) {
    try {
      if (scopingRules == null) {
        scopingRules = Collections.emptyList();
      }
      validateScopingRules(scopingRules);

      UpdateProfileScopingRulesResponse updateProfileScopingRulesResponse =
          delegateProfileServiceBlockingStub.updateProfileScopingRules(UpdateProfileScopingRulesRequest.newBuilder()
                                                                           .setAccountId(accountId)
                                                                           .setProfileId(profileId)
                                                                           .addAllScopingRules(scopingRules)
                                                                           .build());

      if (!updateProfileScopingRulesResponse.hasProfile()) {
        return null;
      }

      return updateProfileScopingRulesResponse.getProfile();

    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while updating profile scoping rules.", ex);
    }
  }

  private void validateScopingRules(List<ProfileScopingRule> scopingRules) {
    if (isEmpty(scopingRules)) {
      return;
    }
    for (ProfileScopingRule scopingRule : scopingRules) {
      boolean hasAtLeastOneScopingValue = false;
      if (scopingRule.getScopingEntitiesMap() != null && scopingRule.getScopingEntitiesMap().keySet().size() > 0) {
        for (String entityKey : scopingRule.getScopingEntitiesMap().keySet()) {
          ScopingValues values = scopingRule.getScopingEntitiesMap().get(entityKey);
          if (values != null && values.getValueCount() > 0) {
            hasAtLeastOneScopingValue = true;
          }
        }
      }
      if (!hasAtLeastOneScopingValue) {
        throw new DelegateServiceDriverException("Scoping rule should have at least one scoping value set!");
      }
    }
  }
}
