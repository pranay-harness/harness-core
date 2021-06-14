package io.harness.ng.core.api.impl;

import static io.harness.beans.Cd1SetupFields.ENV_ID_FIELD;
import static io.harness.beans.Cd1SetupFields.ENV_TYPE_FIELD;
import static io.harness.beans.PageResponse.PageResponseBuilder;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EnvironmentType;
import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.delegate.AccountId;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateProfileDetailsNg;
import io.harness.delegate.beans.DelegateProfileDetailsNg.DelegateProfileDetailsNgBuilder;
import io.harness.delegate.beans.ScopingRuleDetailsNg;
import io.harness.delegateprofile.DelegateProfileGrpc;
import io.harness.delegateprofile.DelegateProfilePageResponseGrpc;
import io.harness.delegateprofile.EmbeddedUserDetails;
import io.harness.delegateprofile.ProfileId;
import io.harness.delegateprofile.ProfileScopingRule;
import io.harness.delegateprofile.ProfileSelector;
import io.harness.delegateprofile.ScopingValues;
import io.harness.exception.DelegateServiceDriverException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.grpc.DelegateProfileServiceGrpcClient;
import io.harness.ng.core.api.DelegateProfileManagerNgService;
import io.harness.owner.OrgIdentifier;
import io.harness.owner.ProjectIdentifier;
import io.harness.paging.PageRequestGrpc;
import io.harness.persistence.HPersistence;

import software.wings.beans.Environment;
import software.wings.beans.User;
import software.wings.security.UserThreadLocal;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;

@Singleton
@ValidateOnExecution
@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateProfileManagerNgServiceImpl implements DelegateProfileManagerNgService {
  public static final String ENVIRONMENT_TYPE = "Environment Type";
  public static final String ENVIRONMENT = "Environment";
  public static final String PRODUCTION = "Production";
  public static final String NON_PRODUCTION = "Non-Production";

  @Inject private DelegateProfileServiceGrpcClient delegateProfileServiceGrpcClient;
  @Inject private HPersistence hPersistence;

  @Override
  public PageResponse<DelegateProfileDetailsNg> list(
      String accountId, PageRequest<DelegateProfileDetailsNg> pageRequest, String orgId, String projectId) {
    OrgIdentifier orgIdentifier = isNotBlank(orgId) ? OrgIdentifier.newBuilder().setId(orgId).build() : null;
    ProjectIdentifier projectIdentifier =
        isNotBlank(projectId) ? ProjectIdentifier.newBuilder().setId(projectId).build() : null;

    DelegateProfilePageResponseGrpc pageResponse;
    try {
      pageResponse = delegateProfileServiceGrpcClient.listProfiles(AccountId.newBuilder().setId(accountId).build(),
          convert(pageRequest), true, orgIdentifier, projectIdentifier);
    } catch (DelegateServiceDriverException ex) {
      throw new InvalidRequestException(ex.getMessage(), ex);
    }

    if (pageResponse == null) {
      return null;
    }

    return convert(pageResponse);
  }

  @Override
  public DelegateProfileDetailsNg get(String accountId, String delegateProfileId) {
    DelegateProfileGrpc delegateProfileGrpc;
    try {
      delegateProfileGrpc = delegateProfileServiceGrpcClient.getProfile(
          AccountId.newBuilder().setId(accountId).build(), ProfileId.newBuilder().setId(delegateProfileId).build());
    } catch (DelegateServiceDriverException ex) {
      throw new InvalidRequestException(ex.getMessage(), ex);
    }

    if (delegateProfileGrpc == null) {
      return null;
    }

    return convert(delegateProfileGrpc);
  }

  @Override
  public DelegateProfileDetailsNg update(DelegateProfileDetailsNg delegateProfile) {
    validateScopingRules(delegateProfile.getScopingRules());
    DelegateProfileGrpc updateDelegateProfileGrpc;
    try {
      updateDelegateProfileGrpc = delegateProfileServiceGrpcClient.updateProfile(convert(delegateProfile));
    } catch (DelegateServiceDriverException ex) {
      throw new InvalidRequestException(ex.getMessage(), ex);
    }

    if (updateDelegateProfileGrpc == null) {
      return null;
    }

    return convert(updateDelegateProfileGrpc);
  }

  @Override
  public DelegateProfileDetailsNg updateScopingRules(
      String accountId, String delegateProfileId, List<ScopingRuleDetailsNg> scopingRules) {
    validateScopingRules(scopingRules);
    List<ProfileScopingRule> grpcScopingRules = convert(scopingRules);

    DelegateProfileGrpc delegateProfileGrpc;
    try {
      delegateProfileGrpc =
          delegateProfileServiceGrpcClient.updateProfileScopingRules(AccountId.newBuilder().setId(accountId).build(),
              ProfileId.newBuilder().setId(delegateProfileId).build(), grpcScopingRules);
    } catch (DelegateServiceDriverException ex) {
      throw new InvalidRequestException(ex.getMessage(), ex);
    }

    if (delegateProfileGrpc == null) {
      return null;
    }

    return convert(delegateProfileGrpc);
  }

  @Override
  public DelegateProfileDetailsNg updateSelectors(String accountId, String delegateProfileId, List<String> selectors) {
    List<ProfileSelector> grpcSelectors = convertToProfileSelector(selectors);

    DelegateProfileGrpc delegateProfileGrpc;
    try {
      delegateProfileGrpc =
          delegateProfileServiceGrpcClient.updateProfileSelectors(AccountId.newBuilder().setId(accountId).build(),
              ProfileId.newBuilder().setId(delegateProfileId).build(), grpcSelectors);
    } catch (DelegateServiceDriverException ex) {
      throw new InvalidRequestException(ex.getMessage(), ex);
    }

    if (delegateProfileGrpc == null) {
      return null;
    }

    return convert(delegateProfileGrpc);
  }

  @Override
  public DelegateProfileDetailsNg add(DelegateProfileDetailsNg delegateProfile) {
    validateScopingRules(delegateProfile.getScopingRules());

    DelegateProfileGrpc delegateProfileGrpc;
    try {
      delegateProfileGrpc = delegateProfileServiceGrpcClient.addProfile(convert(delegateProfile));
    } catch (DelegateServiceDriverException ex) {
      throw new InvalidRequestException(ex.getMessage(), ex);
    }

    if (delegateProfileGrpc == null) {
      return null;
    }

    return convert(delegateProfileGrpc);
  }

  @Override
  public void delete(String accountId, String delegateProfileId) {
    try {
      delegateProfileServiceGrpcClient.deleteProfile(
          AccountId.newBuilder().setId(accountId).build(), ProfileId.newBuilder().setId(delegateProfileId).build());
    } catch (DelegateServiceDriverException ex) {
      throw new InvalidRequestException(ex.getMessage(), ex);
    }
  }

  @VisibleForTesting
  public String generateScopingRuleDescription(Map<String, ScopingValues> scopingEntities) {
    StringBuilder descriptionBuilder = new StringBuilder();

    generateDescriptionFromScopingEntities(scopingEntities, descriptionBuilder, ENVIRONMENT_TYPE, ENV_TYPE_FIELD);

    generateDescriptionFromScopingEntities(scopingEntities, descriptionBuilder, ENVIRONMENT, ENV_ID_FIELD);

    return descriptionBuilder.toString();
  }

  private void generateDescriptionFromScopingEntities(Map<String, ScopingValues> scopingEntities,
      StringBuilder descriptionBuilder, String entityName, String entityKey) {
    ScopingValues scopingValues = scopingEntities.get(entityKey);

    if (scopingValues != null) {
      String entityValues = String.join(",", retrieveScopingRuleEntitiesNames(entityKey, scopingValues.getValueList()));

      if (isNotEmpty(entityValues)) {
        descriptionBuilder.append(entityName).append(": ").append(entityValues).append("; ");
      }
    }
  }

  @VisibleForTesting
  public List<String> retrieveScopingRuleEntitiesNames(String key, List<String> scopingEntitiesIds) {
    List<String> entitiesNames = new ArrayList<>();

    for (String entityId : scopingEntitiesIds) {
      switch (key) {
        case ENV_ID_FIELD:
          entitiesNames.add(fetchEnvName(entityId));
          break;
        case ENV_TYPE_FIELD:
          entitiesNames.add(fetchEnvType(entityId));
          break;
        default:
          throw new RuntimeException("Invalid key " + key);
      }
    }

    return entitiesNames;
  }

  private String fetchEnvName(String environmentId) {
    Environment environment = hPersistence.get(Environment.class, environmentId);
    if (environment != null) {
      return environment.getName();
    }
    return environmentId;
  }

  private String fetchEnvType(String environmentTypeId) {
    if (environmentTypeId.equals(EnvironmentType.PROD.name())) {
      return PRODUCTION;
    } else if (environmentTypeId.equals(EnvironmentType.NON_PROD.name())) {
      return NON_PRODUCTION;
    } else {
      return "";
    }
  }

  private List<ProfileSelector> convertToProfileSelector(List<String> selectors) {
    if (isEmpty(selectors)) {
      return Collections.emptyList();
    }

    return selectors.stream()
        .map(selector -> ProfileSelector.newBuilder().setSelector(selector).build())
        .collect(Collectors.toList());
  }

  private DelegateProfileGrpc convert(DelegateProfileDetailsNg delegateProfile) {
    DelegateProfileGrpc.Builder delegateProfileGrpcBuilder =
        DelegateProfileGrpc.newBuilder()
            .setAccountId(AccountId.newBuilder().setId(delegateProfile.getAccountId()).build())
            .setName(delegateProfile.getName())
            .setPrimary(delegateProfile.isPrimary())
            .setApprovalRequired(delegateProfile.isApprovalRequired())
            .setNg(true);

    if (delegateProfile.getCreatedBy() != null) {
      delegateProfileGrpcBuilder.setCreatedBy(EmbeddedUserDetails.newBuilder()
                                                  .setUuid(delegateProfile.getCreatedBy().getUuid())
                                                  .setName(delegateProfile.getCreatedBy().getName())
                                                  .setEmail(delegateProfile.getCreatedBy().getEmail())
                                                  .build());
    } else {
      delegateProfileGrpcBuilder.setCreatedBy(getEmbeddedUser());
    }

    delegateProfileGrpcBuilder.setLastUpdatedBy(getEmbeddedUser());

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
      delegateProfileGrpcBuilder.addAllScopingRules(convert(delegateProfile.getScopingRules()));
    }

    if (isNotBlank(delegateProfile.getIdentifier())) {
      delegateProfileGrpcBuilder.setIdentifier(delegateProfile.getIdentifier());
    }

    if (isNotBlank(delegateProfile.getOrgIdentifier())) {
      delegateProfileGrpcBuilder.setOrgIdentifier(
          OrgIdentifier.newBuilder().setId(delegateProfile.getOrgIdentifier()).build());
    }

    if (isNotBlank(delegateProfile.getProjectIdentifier())) {
      delegateProfileGrpcBuilder.setProjectIdentifier(
          ProjectIdentifier.newBuilder().setId(delegateProfile.getProjectIdentifier()).build());
    }

    return delegateProfileGrpcBuilder.build();
  }

  private List<ProfileScopingRule> convert(List<ScopingRuleDetailsNg> scopingRules) {
    return scopingRules.stream()
        .map(scopingRule -> {
          Map<String, ScopingValues> scopingEntities = convert(scopingRule);

          return ProfileScopingRule.newBuilder()
              .setDescription(generateScopingRuleDescription(scopingEntities))
              .putAllScopingEntities(scopingEntities)
              .build();
        })
        .collect(Collectors.toList());
  }

  private Map<String, ScopingValues> convert(ScopingRuleDetailsNg scopingRule) {
    Map<String, ScopingValues> scopingEntities = new HashMap<>();

    if (isNotBlank(scopingRule.getEnvironmentTypeId())) {
      scopingEntities.put(
          ENV_TYPE_FIELD, ScopingValues.newBuilder().addValue(scopingRule.getEnvironmentTypeId()).build());
    }

    if (isNotEmpty(scopingRule.getEnvironmentIds())) {
      scopingEntities.put(
          ENV_ID_FIELD, ScopingValues.newBuilder().addAllValue(scopingRule.getEnvironmentIds()).build());
    }

    return scopingEntities;
  }

  private DelegateProfileDetailsNg convert(DelegateProfileGrpc delegateProfileGrpc) {
    DelegateProfileDetailsNgBuilder delegateProfileDetailsNgBuilder =
        DelegateProfileDetailsNg.builder()
            .uuid(delegateProfileGrpc.getProfileId().getId())
            .accountId(delegateProfileGrpc.getAccountId().getId())
            .name(delegateProfileGrpc.getName())
            .description(delegateProfileGrpc.getDescription())
            .primary(delegateProfileGrpc.getPrimary())
            .approvalRequired(delegateProfileGrpc.getApprovalRequired())
            .startupScript(delegateProfileGrpc.getStartupScript())
            .numberOfDelegates(delegateProfileGrpc.getNumberOfDelegates())
            .createdAt(delegateProfileGrpc.getCreatedAt())
            .lastUpdatedAt(delegateProfileGrpc.getLastUpdatedAt());

    if (delegateProfileGrpc.hasCreatedBy()) {
      delegateProfileDetailsNgBuilder.createdBy(io.harness.delegate.beans.EmbeddedUserDetails.builder()
                                                    .uuid(delegateProfileGrpc.getCreatedBy().getUuid())
                                                    .name(delegateProfileGrpc.getCreatedBy().getName())
                                                    .email(delegateProfileGrpc.getCreatedBy().getEmail())
                                                    .build());
    }

    if (delegateProfileGrpc.hasLastUpdatedBy()) {
      delegateProfileDetailsNgBuilder.lastUpdatedBy(io.harness.delegate.beans.EmbeddedUserDetails.builder()
                                                        .uuid(delegateProfileGrpc.getLastUpdatedBy().getUuid())
                                                        .name(delegateProfileGrpc.getLastUpdatedBy().getName())
                                                        .email(delegateProfileGrpc.getLastUpdatedBy().getEmail())
                                                        .build());
    }

    if (isNotEmpty(delegateProfileGrpc.getSelectorsList())) {
      delegateProfileDetailsNgBuilder.selectors(delegateProfileGrpc.getSelectorsList()
                                                    .stream()
                                                    .map(ProfileSelector::getSelector)
                                                    .collect(Collectors.toList()));
    }

    if (isNotEmpty(delegateProfileGrpc.getScopingRulesList())) {
      delegateProfileDetailsNgBuilder.scopingRules(
          convertScopingRuleDetailsToNG(delegateProfileGrpc.getScopingRulesList()));
    }

    if (isNotBlank(delegateProfileGrpc.getIdentifier())) {
      delegateProfileDetailsNgBuilder.identifier(delegateProfileGrpc.getIdentifier());
    }

    if (delegateProfileGrpc.hasOrgIdentifier()) {
      delegateProfileDetailsNgBuilder.orgIdentifier(delegateProfileGrpc.getOrgIdentifier().getId());
    }

    if (delegateProfileGrpc.hasProjectIdentifier()) {
      delegateProfileDetailsNgBuilder.projectIdentifier(delegateProfileGrpc.getProjectIdentifier().getId());
    }

    return delegateProfileDetailsNgBuilder.build();
  }

  private PageRequestGrpc convert(PageRequest pageRequest) {
    PageRequestGrpc.Builder pageRequestGrpcBuilder = PageRequestGrpc.newBuilder();

    if (isNotBlank(pageRequest.getLimit())) {
      pageRequestGrpcBuilder.setLimit(pageRequest.getLimit());
    }

    if (!isEmpty(pageRequest.getFieldsExcluded())) {
      pageRequestGrpcBuilder.addAllFieldsExcluded(pageRequest.getFieldsExcluded());
    }

    if (!isEmpty(pageRequest.getFieldsIncluded())) {
      pageRequestGrpcBuilder.addAllFieldsIncluded(pageRequest.getFieldsIncluded());
    }

    if (isNotBlank(pageRequest.getOffset())) {
      pageRequestGrpcBuilder.setOffset(pageRequest.getOffset());
    }

    return pageRequestGrpcBuilder.build();
  }

  private PageResponse<DelegateProfileDetailsNg> convert(DelegateProfilePageResponseGrpc pageResponse) {
    PageResponseBuilder<DelegateProfileDetailsNg> responseBuilder = PageResponseBuilder.aPageResponse();
    List<DelegateProfileDetailsNg> responseList =
        pageResponse.getResponseList().stream().map(this::convert).collect(Collectors.toList());
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
    Stream<String> fieldsExcludedStream = pageRequestGrpc.getFieldsExcludedList().stream();
    fieldsExcluded = fieldsExcludedStream.collect(Collectors.toList()).toArray(fieldsExcluded);
    requestBuilder.addFieldsExcluded(fieldsExcluded);

    String[] fieldsIncluded = new String[pageRequestGrpc.getFieldsIncludedList().size()];
    Stream<String> fieldsIncludedStream = pageRequestGrpc.getFieldsIncludedList().stream();
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

  private void validateScopingRules(List<ScopingRuleDetailsNg> scopingRules) {
    if (isEmpty(scopingRules)) {
      return;
    }

    for (ScopingRuleDetailsNg scopingRule : scopingRules) {
      if (isBlank(scopingRule.getEnvironmentTypeId()) && isEmpty(scopingRule.getEnvironmentIds())) {
        throw new InvalidArgumentsException("The Scoping rule is empty.");
      }
    }
  }

  private EmbeddedUserDetails getEmbeddedUser() {
    User user = UserThreadLocal.get();
    if (user == null) {
      return EmbeddedUserDetails.newBuilder().build();
    }
    return EmbeddedUserDetails.newBuilder()
        .setUuid(user.getUuid())
        .setEmail(user.getEmail())
        .setName(user.getName())
        .build();
  }

  private List<ScopingRuleDetailsNg> convertScopingRuleDetailsToNG(List<ProfileScopingRule> profileScopingRules) {
    return profileScopingRules.stream()
        .map(grpcScopingRule
            -> ScopingRuleDetailsNg.builder()
                   .description(grpcScopingRule.getDescription())
                   .environmentTypeId(extractScopingEntityId(grpcScopingRule.getScopingEntitiesMap(), ENV_TYPE_FIELD))
                   .environmentIds(extractScopingEntityIds(grpcScopingRule.getScopingEntitiesMap(), ENV_ID_FIELD))
                   .build())
        .collect(Collectors.toList());
  }
}
