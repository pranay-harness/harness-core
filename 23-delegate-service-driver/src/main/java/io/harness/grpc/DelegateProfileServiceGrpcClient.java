package io.harness.grpc;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.grpc.StatusRuntimeException;
import io.harness.delegate.AccountId;
import io.harness.delegateprofile.AddProfileRequest;
import io.harness.delegateprofile.AddProfileResponse;
import io.harness.delegateprofile.DelegateProfileGrpc;
import io.harness.delegateprofile.DelegateProfileServiceGrpc.DelegateProfileServiceBlockingStub;
import io.harness.delegateprofile.DeleteProfileRequest;
import io.harness.delegateprofile.GetProfileRequest;
import io.harness.delegateprofile.GetProfileResponse;
import io.harness.delegateprofile.ListProfilesRequest;
import io.harness.delegateprofile.ListProfilesResponse;
import io.harness.delegateprofile.ProfileId;
import io.harness.delegateprofile.ProfileScopingRule;
import io.harness.delegateprofile.ProfileSelector;
import io.harness.delegateprofile.UpdateProfileRequest;
import io.harness.delegateprofile.UpdateProfileResponse;
import io.harness.delegateprofile.UpdateProfileScopingRulesRequest;
import io.harness.delegateprofile.UpdateProfileSelectorsRequest;
import io.harness.exception.DelegateServiceDriverException;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Singleton
@Slf4j
public class DelegateProfileServiceGrpcClient {
  private final DelegateProfileServiceBlockingStub delegateProfileServiceBlockingStub;

  @Inject
  public DelegateProfileServiceGrpcClient(DelegateProfileServiceBlockingStub delegateProfileServiceBlockingStub) {
    this.delegateProfileServiceBlockingStub = delegateProfileServiceBlockingStub;
  }

  public List<DelegateProfileGrpc> listProfiles(AccountId accountId) {
    try {
      ListProfilesResponse listProfilesResponse = delegateProfileServiceBlockingStub.listProfiles(
          ListProfilesRequest.newBuilder().setAccountId(accountId).build());

      return listProfilesResponse.getProfilesList();
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
      AddProfileResponse addProfileResponse = delegateProfileServiceBlockingStub.addProfile(
          AddProfileRequest.newBuilder().setProfile(delegateProfileGrpc).build());

      return addProfileResponse.getProfile();
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while adding profile.", ex);
    }
  }

  public DelegateProfileGrpc updateProfile(DelegateProfileGrpc delegateProfileGrpc) {
    try {
      UpdateProfileResponse updateProfileResponse = delegateProfileServiceBlockingStub.updateProfile(
          UpdateProfileRequest.newBuilder().setProfile(delegateProfileGrpc).build());

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

  public void updateProfileSelectors(AccountId accountId, ProfileId profileId, List<ProfileSelector> selectors) {
    try {
      if (selectors == null) {
        selectors = Collections.emptyList();
      }

      delegateProfileServiceBlockingStub.updateProfileSelectors(UpdateProfileSelectorsRequest.newBuilder()
                                                                    .setAccountId(accountId)
                                                                    .setProfileId(profileId)
                                                                    .addAllSelectors(selectors)
                                                                    .build());
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while updating profile selectors.", ex);
    }
  }

  public void updateProfileScopingRules(
      AccountId accountId, ProfileId profileId, List<ProfileScopingRule> scopingRules) {
    try {
      if (scopingRules == null) {
        scopingRules = Collections.emptyList();
      }

      delegateProfileServiceBlockingStub.updateProfileScopingRules(UpdateProfileScopingRulesRequest.newBuilder()
                                                                       .setAccountId(accountId)
                                                                       .setProfileId(profileId)
                                                                       .addAllScopingRules(scopingRules)
                                                                       .build());
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while updating profile scoping rules.", ex);
    }
  }
}
