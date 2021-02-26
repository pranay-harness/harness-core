package io.harness.grpc;

import static io.harness.beans.PageRequest.PageRequestBuilder;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.DelegateProfile.DelegateProfileKeys;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.delegate.AccountId;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateProfile.DelegateProfileBuilder;
import io.harness.delegate.beans.DelegateProfileScopingRule;
import io.harness.delegateprofile.AddProfileRequest;
import io.harness.delegateprofile.AddProfileResponse;
import io.harness.delegateprofile.DelegateProfileGrpc;
import io.harness.delegateprofile.DelegateProfilePageResponseGrpc;
import io.harness.delegateprofile.DelegateProfileServiceGrpc.DelegateProfileServiceImplBase;
import io.harness.delegateprofile.DeleteProfileRequest;
import io.harness.delegateprofile.DeleteProfileResponse;
import io.harness.delegateprofile.EmbeddedUserDetails;
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
import io.harness.paging.PageRequestGrpc;

import software.wings.beans.User;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.DelegateProfileService;
import software.wings.service.intfc.UserService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.grpc.stub.StreamObserver;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@TargetModule(Module._420_DELEGATE_SERVICE)
public class DelegateProfileServiceGrpcImpl extends DelegateProfileServiceImplBase {
  private DelegateProfileService delegateProfileService;
  private UserService userService;

  @Inject
  public DelegateProfileServiceGrpcImpl(DelegateProfileService delegateProfileService, UserService userService) {
    this.delegateProfileService = delegateProfileService;
    this.userService = userService;
  }

  @Override
  public void listProfiles(ListProfilesRequest request, StreamObserver<ListProfilesResponse> responseObserver) {
    try {
      PageRequest<DelegateProfile> pageRequest = convertGrpcPageRequest(request.getPageRequest());
      pageRequest.addFilter(DelegateProfileKeys.accountId, SearchFilter.Operator.EQ, request.getAccountId().getId());
      PageResponse<DelegateProfile> pageResponse = delegateProfileService.list(pageRequest);
      if (pageResponse != null) {
        DelegateProfilePageResponseGrpc response = convertPageResponse(pageResponse);
        responseObserver.onNext(ListProfilesResponse.newBuilder().setResponse(response).build());
      } else {
        responseObserver.onNext(ListProfilesResponse.newBuilder().build());
      }
      responseObserver.onCompleted();
    } catch (Exception ex) {
      log.error("Unexpected error occurred while processing list profiles request.", ex);
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
      log.error("Unexpected error occurred while processing get profile request.", ex);
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
      log.error("Unexpected error occurred while processing add profile request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void updateProfile(UpdateProfileRequest request, StreamObserver<UpdateProfileResponse> responseObserver) {
    try {
      DelegateProfile delegateProfile = delegateProfileService.update(convert(request.getProfile()));

      if (delegateProfile != null) {
        responseObserver.onNext(UpdateProfileResponse.newBuilder().setProfile(convert(delegateProfile)).build());
      } else {
        responseObserver.onNext(UpdateProfileResponse.newBuilder().build());
      }

      responseObserver.onCompleted();

    } catch (Exception ex) {
      log.error("Unexpected error occurred while processing update profile request.", ex);
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
      log.error("Unexpected error occurred while processing delete profile request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void updateProfileSelectors(
      UpdateProfileSelectorsRequest request, StreamObserver<UpdateProfileSelectorsResponse> responseObserver) {
    try {
      List<String> selectors = null;
      if (isNotEmpty(request.getSelectorsList())) {
        selectors = request.getSelectorsList().stream().map(ProfileSelector::getSelector).collect(Collectors.toList());
      }

      DelegateProfile updatedDelegateProfile = delegateProfileService.updateDelegateProfileSelectors(
          request.getProfileId().getId(), request.getAccountId().getId(), selectors);

      if (updatedDelegateProfile != null) {
        responseObserver.onNext(
            UpdateProfileSelectorsResponse.newBuilder().setProfile(convert(updatedDelegateProfile)).build());
      } else {
        responseObserver.onNext(UpdateProfileSelectorsResponse.newBuilder().build());
      }

      responseObserver.onCompleted();
    } catch (Exception ex) {
      log.error("Unexpected error occurred while processing update profile selectors request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void updateProfileScopingRules(
      UpdateProfileScopingRulesRequest request, StreamObserver<UpdateProfileScopingRulesResponse> responseObserver) {
    try {
      List<DelegateProfileScopingRule> scopingRules = null;
      if (isNotEmpty(request.getScopingRulesList())) {
        scopingRules = request.getScopingRulesList()
                           .stream()
                           .map(scopingRule
                               -> DelegateProfileScopingRule.builder()
                                      .description(scopingRule.getDescription())
                                      .scopingEntities(convertGrpcScopes(scopingRule.getScopingEntitiesMap()))
                                      .build())
                           .collect(Collectors.toList());
      }

      DelegateProfile updatedDelegateProfile = delegateProfileService.updateScopingRules(
          request.getAccountId().getId(), request.getProfileId().getId(), scopingRules);

      if (updatedDelegateProfile != null) {
        responseObserver.onNext(
            UpdateProfileScopingRulesResponse.newBuilder().setProfile(convert(updatedDelegateProfile)).build());
      } else {
        responseObserver.onNext(UpdateProfileScopingRulesResponse.newBuilder().build());
      }

      responseObserver.onCompleted();
    } catch (Exception ex) {
      log.error("Unexpected error occurred while processing update profile scoping rules request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  private DelegateProfileGrpc convert(DelegateProfile delegateProfile) {
    DelegateProfileGrpc.Builder delegateProfileGrpcBuilder =
        DelegateProfileGrpc.newBuilder()
            .setPrimary(delegateProfile.isPrimary())
            .setApprovalRequired(delegateProfile.isApprovalRequired());

    if (delegateProfile.getCreatedBy() != null) {
      delegateProfileGrpcBuilder.setCreatedBy(EmbeddedUserDetails.newBuilder()
                                                  .setUuid(delegateProfile.getCreatedBy().getUuid())
                                                  .setName(delegateProfile.getCreatedBy().getName())
                                                  .setEmail(delegateProfile.getCreatedBy().getEmail())
                                                  .build());
    }

    if (delegateProfile.getLastUpdatedBy() != null) {
      delegateProfileGrpcBuilder.setLastUpdatedBy(EmbeddedUserDetails.newBuilder()
                                                      .setUuid(delegateProfile.getLastUpdatedBy().getUuid())
                                                      .setName(delegateProfile.getLastUpdatedBy().getName())
                                                      .setEmail(delegateProfile.getLastUpdatedBy().getEmail())
                                                      .build());
    }

    if (isNotBlank(delegateProfile.getName())) {
      delegateProfileGrpcBuilder.setName(delegateProfile.getName());
    }
    if (isNotBlank(delegateProfile.getUuid())) {
      delegateProfileGrpcBuilder.setProfileId(ProfileId.newBuilder().setId(delegateProfile.getUuid()).build());
    }

    if (isNotBlank(delegateProfile.getAccountId())) {
      delegateProfileGrpcBuilder.setAccountId(AccountId.newBuilder().setId(delegateProfile.getAccountId()).build());
    }

    if (isNotBlank(delegateProfile.getDescription())) {
      delegateProfileGrpcBuilder.setDescription(delegateProfile.getDescription());
    }

    if (isNotBlank(delegateProfile.getStartupScript())) {
      delegateProfileGrpcBuilder.setStartupScript(delegateProfile.getStartupScript());
    }

    if (isNotEmpty(delegateProfile.getSelectors())) {
      delegateProfileGrpcBuilder.addAllSelectors(
          delegateProfile.getSelectors()
              .stream()
              .map(selector -> ProfileSelector.newBuilder().setSelector(selector).build())
              .collect(Collectors.toList()));
    }

    if (isNotEmpty(delegateProfile.getScopingRules())) {
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

    if (isNotBlank(delegateProfile.getIdentifier())) {
      delegateProfileGrpcBuilder.setIdentifier(delegateProfile.getIdentifier());
    }

    List<String> delegatesForProfile =
        delegateProfileService.getDelegatesForProfile(delegateProfile.getAccountId(), delegateProfile.getUuid());

    if (isNotEmpty(delegatesForProfile)) {
      delegateProfileGrpcBuilder.setNumberOfDelegates(delegatesForProfile.size());
    }

    return delegateProfileGrpcBuilder.build();
  }

  private DelegateProfile convert(DelegateProfileGrpc delegateProfileGrpc) {
    DelegateProfileBuilder delegateProfileBuilder = DelegateProfile.builder()
                                                        .accountId(delegateProfileGrpc.getAccountId().getId())
                                                        .name(delegateProfileGrpc.getName())
                                                        .description(delegateProfileGrpc.getDescription())
                                                        .primary(delegateProfileGrpc.getPrimary())
                                                        .approvalRequired(delegateProfileGrpc.getApprovalRequired())
                                                        .startupScript(delegateProfileGrpc.getStartupScript());

    if (delegateProfileGrpc.hasCreatedBy()) {
      delegateProfileBuilder.createdBy(EmbeddedUser.builder()
                                           .uuid(delegateProfileGrpc.getCreatedBy().getUuid())
                                           .name(delegateProfileGrpc.getCreatedBy().getName())
                                           .email(delegateProfileGrpc.getCreatedBy().getEmail())
                                           .build());
    }

    if (delegateProfileGrpc.hasLastUpdatedBy()) {
      User user = userService.getUserFromCacheOrDB(delegateProfileGrpc.getLastUpdatedBy().getUuid());
      UserThreadLocal.set(user);
    }

    if (delegateProfileGrpc.getProfileId() != null && isNotBlank(delegateProfileGrpc.getProfileId().getId())) {
      delegateProfileBuilder.uuid(delegateProfileGrpc.getProfileId().getId());
    }

    if (isNotEmpty(delegateProfileGrpc.getSelectorsList())) {
      delegateProfileBuilder.selectors(delegateProfileGrpc.getSelectorsList()
                                           .stream()
                                           .map(ProfileSelector::getSelector)
                                           .collect(Collectors.toList()));
    }

    if (isNotEmpty(delegateProfileGrpc.getScopingRulesList())) {
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

    if (isNotEmpty(delegateProfileGrpc.getIdentifier())) {
      delegateProfileBuilder.identifier(delegateProfileGrpc.getIdentifier());
    }

    return delegateProfileBuilder.build();
  }

  private Map<String, ScopingValues> convertScopes(Map<String, Set<String>> scopingEntities) {
    Map<String, ScopingValues> grpcScopingEntities = new HashMap<>();

    if (scopingEntities != null) {
      for (Map.Entry<String, Set<String>> entry : scopingEntities.entrySet()) {
        ScopingValues scopingValues = ScopingValues.newBuilder().addAllValue(entry.getValue()).build();
        grpcScopingEntities.put(entry.getKey(), scopingValues);
      }
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

  private PageRequest<DelegateProfile> convertGrpcPageRequest(PageRequestGrpc pageRequestGrpc) {
    PageRequestBuilder requestBuilder = PageRequestBuilder.aPageRequest();

    String[] fieldsExcluded = new String[pageRequestGrpc.getFieldsExcludedList().size()];
    Stream<String> fieldsExcludedStream = pageRequestGrpc.getFieldsExcludedList().stream().map(e -> e.toString());
    fieldsExcluded = fieldsExcludedStream.collect(Collectors.toList()).toArray(fieldsExcluded);
    requestBuilder.addFieldsExcluded(fieldsExcluded);

    String[] fieldsIncluded = new String[pageRequestGrpc.getFieldsIncludedList().size()];
    Stream<String> fieldsIncludedStream = pageRequestGrpc.getFieldsIncludedList().stream().map(e -> e.toString());
    fieldsIncluded = fieldsIncludedStream.collect(Collectors.toList()).toArray(fieldsIncluded);
    requestBuilder.addFieldsIncluded(fieldsIncluded);

    requestBuilder.withLimit(pageRequestGrpc.getLimit());
    requestBuilder.withOffset(pageRequestGrpc.getOffset());

    return requestBuilder.build();
  }

  private PageRequestGrpc convert(PageRequest pageRequest) {
    return PageRequestGrpc.newBuilder()
        .setLimit(pageRequest.getLimit())
        .addAllFieldsExcluded(pageRequest.getFieldsExcluded())
        .addAllFieldsIncluded(pageRequest.getFieldsIncluded())
        .setOffset(pageRequest.getOffset())
        .build();
  }

  private DelegateProfilePageResponseGrpc convertPageResponse(PageResponse<DelegateProfile> pageResponse) {
    DelegateProfilePageResponseGrpc.Builder builder = DelegateProfilePageResponseGrpc.newBuilder();
    builder.setPageRequest(convert(pageResponse));
    builder.addAllResponse(pageResponse.getResponse().stream().map(e -> convert(e)).collect(Collectors.toList()));
    builder.setTotal(pageResponse.getTotal());
    return builder.build();
  }
}
