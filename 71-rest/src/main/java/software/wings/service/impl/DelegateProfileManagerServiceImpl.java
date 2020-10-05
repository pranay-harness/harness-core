package software.wings.service.impl;

import static io.harness.beans.PageResponse.PageResponseBuilder;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.delegate.AccountId;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateProfileDetails;
import io.harness.delegate.beans.DelegateProfileDetails.DelegateProfileDetailsBuilder;
import io.harness.delegate.beans.ScopingRuleDetails;
import io.harness.delegate.beans.ScopingRuleDetails.ScopingRuleDetailsKeys;
import io.harness.delegateprofile.DelegateProfileGrpc;
import io.harness.delegateprofile.DelegateProfilePageResponseGrpc;
import io.harness.delegateprofile.ProfileId;
import io.harness.delegateprofile.ProfileScopingRule;
import io.harness.delegateprofile.ProfileSelector;
import io.harness.delegateprofile.ScopingValues;
import io.harness.grpc.DelegateProfileServiceGrpcClient;
import io.harness.paging.PageRequestGrpc;
import lombok.extern.slf4j.Slf4j;
import software.wings.service.intfc.DelegateProfileManagerService;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
@Slf4j
public class DelegateProfileManagerServiceImpl implements DelegateProfileManagerService {
  @Inject private DelegateProfileServiceGrpcClient delegateProfileServiceGrpcClient;

  @Override
  public PageResponse<DelegateProfileDetails> list(String accountId, PageRequest<DelegateProfileDetails> pageRequest) {
    DelegateProfilePageResponseGrpc pageResponse = delegateProfileServiceGrpcClient.listProfiles(
        AccountId.newBuilder().setId(accountId).build(), convert(pageRequest));

    if (pageResponse == null) {
      return null;
    }

    return convert(pageResponse);
  }

  @Override
  public DelegateProfileDetails get(String accountId, String delegateProfileId) {
    DelegateProfileGrpc delegateProfileGrpc = delegateProfileServiceGrpcClient.getProfile(
        AccountId.newBuilder().setId(accountId).build(), ProfileId.newBuilder().setId(delegateProfileId).build());

    if (delegateProfileGrpc == null) {
      return null;
    }

    return convert(delegateProfileGrpc);
  }

  @Override
  public DelegateProfileDetails update(DelegateProfileDetails delegateProfile) {
    DelegateProfileGrpc updateDelegateProfileGrpc =
        delegateProfileServiceGrpcClient.updateProfile(convert(delegateProfile));

    if (updateDelegateProfileGrpc == null) {
      return null;
    }

    return convert(updateDelegateProfileGrpc);
  }

  @Override
  public DelegateProfileDetails updateScopingRules(
      String accountId, String delegateProfileId, List<ScopingRuleDetails> scopingRules) {
    List<ProfileScopingRule> grpcScopingRules = convert(scopingRules);

    DelegateProfileGrpc delegateProfileGrpc =
        delegateProfileServiceGrpcClient.updateProfileScopingRules(AccountId.newBuilder().setId(accountId).build(),
            ProfileId.newBuilder().setId(delegateProfileId).build(), grpcScopingRules);

    if (delegateProfileGrpc == null) {
      return null;
    }

    return convert(delegateProfileGrpc);
  }

  @Override
  public DelegateProfileDetails updateSelectors(String accountId, String delegateProfileId, List<String> selectors) {
    List<ProfileSelector> grpcSelectors = convertToProfileSelector(selectors);

    DelegateProfileGrpc delegateProfileGrpc =
        delegateProfileServiceGrpcClient.updateProfileSelectors(AccountId.newBuilder().setId(accountId).build(),
            ProfileId.newBuilder().setId(delegateProfileId).build(), grpcSelectors);

    if (delegateProfileGrpc == null) {
      return null;
    }

    return convert(delegateProfileGrpc);
  }

  @Override
  public DelegateProfileDetails add(DelegateProfileDetails delegateProfile) {
    DelegateProfileGrpc delegateProfileGrpc = delegateProfileServiceGrpcClient.addProfile(convert(delegateProfile));

    if (delegateProfileGrpc == null) {
      return null;
    }

    return convert(delegateProfileGrpc);
  }

  @Override
  public void delete(String accountId, String delegateProfileId) {
    delegateProfileServiceGrpcClient.deleteProfile(
        AccountId.newBuilder().setId(accountId).build(), ProfileId.newBuilder().setId(delegateProfileId).build());
  }

  private List<ProfileSelector> convertToProfileSelector(List<String> selectors) {
    if (isEmpty(selectors)) {
      return Collections.emptyList();
    }

    return selectors.stream()
        .map(selector -> ProfileSelector.newBuilder().setSelector(selector).build())
        .collect(Collectors.toList());
  }
  private DelegateProfileGrpc convert(DelegateProfileDetails delegateProfile) {
    DelegateProfileGrpc.Builder delegateProfileGrpcBuilder =
        DelegateProfileGrpc.newBuilder()
            .setAccountId(AccountId.newBuilder().setId(delegateProfile.getAccountId()).build())
            .setName(delegateProfile.getName())
            .setPrimary(delegateProfile.isPrimary())
            .setApprovalRequired(delegateProfile.isApprovalRequired());

    if (isNotBlank(delegateProfile.getUuid())) {
      delegateProfileGrpcBuilder.setProfileId(ProfileId.newBuilder().setId(delegateProfile.getUuid()).build());
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
      delegateProfileGrpcBuilder.addAllScopingRules(delegateProfile.getScopingRules()
                                                        .stream()
                                                        .map(scopingRule
                                                            -> ProfileScopingRule.newBuilder()
                                                                   .setDescription(scopingRule.getDescription())
                                                                   .putAllScopingEntities(convert(scopingRule))
                                                                   .build())
                                                        .collect(Collectors.toList()));
    }

    return delegateProfileGrpcBuilder.build();
  }

  private List<ProfileScopingRule> convert(List<ScopingRuleDetails> scopingRules) {
    if (isEmpty(scopingRules)) {
      return Collections.emptyList();
    }

    return scopingRules.stream()
        .map(scopingRule
            -> ProfileScopingRule.newBuilder()
                   .setDescription(scopingRule.getDescription())
                   .putAllScopingEntities(convert(scopingRule))
                   .build())
        .collect(Collectors.toList());
  }

  private Map<String, ScopingValues> convert(ScopingRuleDetails scopingRule) {
    Map<String, ScopingValues> scopingEntities = new HashMap<>();

    if (isNotBlank(scopingRule.getApplicationId())) {
      scopingEntities.put(ScopingRuleDetailsKeys.applicationId,
          ScopingValues.newBuilder().addValue(scopingRule.getApplicationId()).build());
    }

    if (isNotEmpty(scopingRule.getEnvironmentIds())) {
      scopingEntities.put(ScopingRuleDetailsKeys.environmentIds,
          ScopingValues.newBuilder().addAllValue(scopingRule.getEnvironmentIds()).build());
    }

    if (isNotEmpty(scopingRule.getServiceIds())) {
      scopingEntities.put(ScopingRuleDetailsKeys.serviceIds,
          ScopingValues.newBuilder().addAllValue(scopingRule.getServiceIds()).build());
    }

    return scopingEntities;
  }

  private DelegateProfileDetails convert(DelegateProfileGrpc delegateProfileGrpc) {
    DelegateProfileDetailsBuilder delegateProfileDetailsBuilder =
        DelegateProfileDetails.builder()
            .uuid(delegateProfileGrpc.getProfileId().getId())
            .accountId(delegateProfileGrpc.getAccountId().getId())
            .name(delegateProfileGrpc.getName())
            .description(delegateProfileGrpc.getDescription())
            .primary(delegateProfileGrpc.getPrimary())
            .approvalRequired(delegateProfileGrpc.getApprovalRequired())
            .startupScript(delegateProfileGrpc.getStartupScript());

    if (isNotEmpty(delegateProfileGrpc.getSelectorsList())) {
      delegateProfileDetailsBuilder.selectors(delegateProfileGrpc.getSelectorsList()
                                                  .stream()
                                                  .map(ProfileSelector::getSelector)
                                                  .collect(Collectors.toList()));
    }

    if (isNotEmpty(delegateProfileGrpc.getScopingRulesList())) {
      delegateProfileDetailsBuilder.scopingRules(
          delegateProfileGrpc.getScopingRulesList()
              .stream()
              .map(grpcScopingRule
                  -> ScopingRuleDetails.builder()
                         .description(grpcScopingRule.getDescription())
                         .applicationId(extractScopingEntityId(
                             grpcScopingRule.getScopingEntitiesMap(), ScopingRuleDetailsKeys.applicationId))
                         .environmentIds(extractScopingEntityIds(
                             grpcScopingRule.getScopingEntitiesMap(), ScopingRuleDetailsKeys.environmentIds))
                         .serviceIds(extractScopingEntityIds(
                             grpcScopingRule.getScopingEntitiesMap(), ScopingRuleDetailsKeys.serviceIds))
                         .build())
              .collect(Collectors.toList()));
    }

    return delegateProfileDetailsBuilder.build();
  }

  private PageRequestGrpc convert(PageRequest pageRequest) {
    return PageRequestGrpc.newBuilder()
        .setLimit(pageRequest.getLimit())
        .addAllFieldsExcluded(pageRequest.getFieldsExcluded())
        .addAllFieldsIncluded(pageRequest.getFieldsIncluded())
        .setOffset(pageRequest.getOffset())
        .build();
  }

  private PageResponse<DelegateProfileDetails> convert(DelegateProfilePageResponseGrpc pageResponse) {
    PageResponseBuilder<DelegateProfileDetails> responseBuilder = PageResponseBuilder.aPageResponse();
    List<DelegateProfileDetails> responseList =
        pageResponse.getResponseList().stream().map(a -> convert(a)).collect(Collectors.toList());
    responseBuilder.withResponse(responseList);
    responseBuilder.withTotal(pageResponse.getTotal());
    PageRequest<DelegateProfile> pageRequest = convertGrpcPageRequest(pageResponse.getPageRequest());
    responseBuilder.withFieldsExcluded(pageRequest.getFieldsExcluded());
    responseBuilder.withFieldsIncluded(pageRequest.getFieldsIncluded());
    responseBuilder.withLimit(pageRequest.getLimit());
    responseBuilder.withOffset(pageRequest.getOffset());
    return responseBuilder.build();
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

  private String extractScopingEntityId(Map<String, ScopingValues> scopingEntitiesMap, String entityId) {
    ScopingValues scopingValues = scopingEntitiesMap.get(entityId);

    if (scopingValues == null) {
      return null;
    }

    return scopingValues.getValueList().get(0);
  }

  private Set<String> extractScopingEntityIds(Map<String, ScopingValues> scopingEntitiesMap, String entityId) {
    ScopingValues scopingValues = scopingEntitiesMap.get(entityId);

    if (scopingValues == null) {
      return null;
    }

    return new HashSet<>(scopingValues.getValueList());
  }
}