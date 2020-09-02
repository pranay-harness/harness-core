package io.harness.grpc;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.grpc.stub.StreamObserver;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.AccountId;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateProfile.DelegateProfileBuilder;
import io.harness.delegate.beans.DelegateProfileScopingRule;
import io.harness.delegateprofile.AddProfileRequest;
import io.harness.delegateprofile.AddProfileResponse;
import io.harness.delegateprofile.DelegateProfileGrpc;
import io.harness.delegateprofile.DelegateProfileServiceGrpc.DelegateProfileServiceImplBase;
import io.harness.delegateprofile.DeleteProfileRequest;
import io.harness.delegateprofile.DeleteProfileResponse;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.service.intfc.DelegateProfileService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class DelegateProfileServiceGrpcImpl extends DelegateProfileServiceImplBase {
  private DelegateProfileService delegateProfileService;

  @Inject
  public DelegateProfileServiceGrpcImpl(DelegateProfileService delegateProfileService) {
    this.delegateProfileService = delegateProfileService;
  }

  @Override
  public void listProfiles(ListProfilesRequest request, StreamObserver<ListProfilesResponse> responseObserver) {
    try {
      throw new NotImplementedException("We need to check if and how do we want to support paging through the grpc");
    } catch (Exception ex) {
      logger.error("Unexpected error occurred while processing list profiles request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void getProfile(GetProfileRequest request, StreamObserver<GetProfileResponse> responseObserver) {
    try {
      DelegateProfile delegateProfile =
          delegateProfileService.get(request.getAccountId().getId(), request.getProfileId().getId());

      if (delegateProfile != null) {
        responseObserver.onNext(GetProfileResponse.newBuilder().setProfile(convert(delegateProfile)).build());
      } else {
        responseObserver.onNext(GetProfileResponse.newBuilder().build());
      }

      responseObserver.onCompleted();
    } catch (Exception ex) {
      logger.error("Unexpected error occurred while processing get profile request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void addProfile(AddProfileRequest request, StreamObserver<AddProfileResponse> responseObserver) {
    try {
      DelegateProfile delegateProfile = delegateProfileService.add(convert(request.getProfile()));

      responseObserver.onNext(AddProfileResponse.newBuilder().setProfile(convert(delegateProfile)).build());
      responseObserver.onCompleted();
    } catch (Exception ex) {
      logger.error("Unexpected error occurred while processing add profile request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void updateProfile(UpdateProfileRequest request, StreamObserver<UpdateProfileResponse> responseObserver) {
    try {
      DelegateProfile delegateProfile = delegateProfileService.update(convert(request.getProfile()));

      responseObserver.onNext(UpdateProfileResponse.newBuilder().setProfile(convert(delegateProfile)).build());
      responseObserver.onCompleted();
    } catch (Exception ex) {
      logger.error("Unexpected error occurred while processing update profile request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void deleteProfile(DeleteProfileRequest request, StreamObserver<DeleteProfileResponse> responseObserver) {
    try {
      delegateProfileService.delete(request.getAccountId().getId(), request.getProfileId().getId());
      responseObserver.onNext(DeleteProfileResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Exception ex) {
      logger.error("Unexpected error occurred while processing delete profile request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void updateProfileSelectors(
      UpdateProfileSelectorsRequest request, StreamObserver<UpdateProfileSelectorsResponse> responseObserver) {
    try {
      List<String> selectors = null;
      if (EmptyPredicate.isNotEmpty(request.getSelectorsList())) {
        selectors = request.getSelectorsList().stream().map(ProfileSelector::getSelector).collect(Collectors.toList());
      }

      delegateProfileService.updateDelegateProfileSelectors(
          request.getProfileId().getId(), request.getAccountId().getId(), selectors);

      responseObserver.onNext(UpdateProfileSelectorsResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Exception ex) {
      logger.error("Unexpected error occurred while processing update profile selectors request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void updateProfileScopingRules(
      UpdateProfileScopingRulesRequest request, StreamObserver<UpdateProfileScopingRulesResponse> responseObserver) {
    try {
      throw new NotImplementedException("Not yet implemented in DelegateProfileService");
    } catch (Exception ex) {
      logger.error("Unexpected error occurred while processing update profile scoping rules request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  private DelegateProfileGrpc convert(DelegateProfile delegateProfile) {
    DelegateProfileGrpc.Builder delegateProfileGrpcBuilder =
        DelegateProfileGrpc.newBuilder()
            .setProfileId(ProfileId.newBuilder().setId(delegateProfile.getUuid()).build())
            .setAccountId(AccountId.newBuilder().setId(delegateProfile.getAccountId()).build())
            .setName(delegateProfile.getName())
            .setPrimary(delegateProfile.isPrimary())
            .setApprovalRequired(delegateProfile.isApprovalRequired());

    if (isNotBlank(delegateProfile.getDescription())) {
      delegateProfileGrpcBuilder.setDescription(delegateProfile.getDescription());
    }

    if (isNotBlank(delegateProfile.getStartupScript())) {
      delegateProfileGrpcBuilder.setStartupScript(delegateProfile.getStartupScript());
    }

    if (EmptyPredicate.isNotEmpty(delegateProfile.getSelectors())) {
      delegateProfileGrpcBuilder.addAllSelectors(
          delegateProfile.getSelectors()
              .stream()
              .map(selector -> ProfileSelector.newBuilder().setSelector(selector).build())
              .collect(Collectors.toList()));
    }

    if (EmptyPredicate.isNotEmpty(delegateProfile.getScopingRules())) {
      delegateProfileGrpcBuilder.addAllScopingRules(
          delegateProfile.getScopingRules()
              .stream()
              .map(scopingRule
                  -> ProfileScopingRule.newBuilder()
                         .setDescription(scopingRule.getDescription())
                         .putAllScopingEntities(convertScopes(scopingRule.getScopingEntities()))
                         .build())
              .collect(Collectors.toList()));
    }

    return delegateProfileGrpcBuilder.build();
  }

  private DelegateProfile convert(DelegateProfileGrpc delegateProfileGrpc) {
    DelegateProfileBuilder delegateProfileBuilder = DelegateProfile.builder()
                                                        .uuid(delegateProfileGrpc.getProfileId().getId())
                                                        .accountId(delegateProfileGrpc.getAccountId().getId())
                                                        .name(delegateProfileGrpc.getName())
                                                        .description(delegateProfileGrpc.getDescription())
                                                        .primary(delegateProfileGrpc.getPrimary())
                                                        .approvalRequired(delegateProfileGrpc.getApprovalRequired())
                                                        .startupScript(delegateProfileGrpc.getStartupScript());

    if (EmptyPredicate.isNotEmpty(delegateProfileGrpc.getScopingRulesList())) {
      delegateProfileBuilder.selectors(delegateProfileGrpc.getSelectorsList()
                                           .stream()
                                           .map(ProfileSelector::getSelector)
                                           .collect(Collectors.toList()));
    }

    if (EmptyPredicate.isNotEmpty(delegateProfileGrpc.getScopingRulesList())) {
      delegateProfileBuilder.scopingRules(
          delegateProfileGrpc.getScopingRulesList()
              .stream()
              .map(grpcScopingRule
                  -> DelegateProfileScopingRule.builder()
                         .description(grpcScopingRule.getDescription())
                         .scopingEntities(convertGrpcScopes(grpcScopingRule.getScopingEntitiesMap()))
                         .build())
              .collect(Collectors.toList()));
    }

    return delegateProfileBuilder.build();
  }

  private Map<String, ScopingValues> convertScopes(Map<String, Set<String>> scopingEntities) {
    Map<String, ScopingValues> grpcScopingEntities = new HashMap<>();

    for (Map.Entry<String, Set<String>> entry : scopingEntities.entrySet()) {
      ScopingValues scopingValues = ScopingValues.newBuilder().addAllValue(entry.getValue()).build();
      grpcScopingEntities.put(entry.getKey(), scopingValues);
    }

    return grpcScopingEntities;
  }

  private Map<String, Set<String>> convertGrpcScopes(Map<String, ScopingValues> grpcScopingEntities) {
    Map<String, Set<String>> scopingEntities = new HashMap<>();

    for (Map.Entry<String, ScopingValues> entry : grpcScopingEntities.entrySet()) {
      scopingEntities.put(entry.getKey(), new HashSet<>(entry.getValue().getValueList()));
    }

    return scopingEntities;
  }
}
