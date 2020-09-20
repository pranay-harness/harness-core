package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.AccountId;
import io.harness.delegate.beans.DelegateProfileDetails;
import io.harness.delegate.beans.DelegateProfileDetails.DelegateProfileDetailsBuilder;
import io.harness.delegate.beans.ScopingRuleDetails;
import io.harness.delegate.beans.ScopingRuleDetails.ScopingRuleDetailsKeys;
import io.harness.delegateprofile.DelegateProfileGrpc;
import io.harness.delegateprofile.ProfileId;
import io.harness.delegateprofile.ProfileScopingRule;
import io.harness.delegateprofile.ProfileSelector;
import io.harness.delegateprofile.ScopingValues;
import io.harness.exception.UnsupportedOperationException;
import io.harness.grpc.DelegateProfileServiceGrpcClient;
import lombok.extern.slf4j.Slf4j;
import software.wings.service.intfc.DelegateProfileManagerService;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
@Slf4j
public class DelegateProfileManagerServiceImpl implements DelegateProfileManagerService {
  @Inject private DelegateProfileServiceGrpcClient delegateProfileServiceGrpcClient;

  @Override
  public List<DelegateProfileDetails> list(String accountId) {
    logger.info("List delegate profiles");
    throw new UnsupportedOperationException("not implemented");
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
    logger.info("Update delegate profile");
    throw new UnsupportedOperationException("not implemented");
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
    logger.info("Update delegate profile selectors");
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public DelegateProfileDetails add(DelegateProfileDetails delegateProfile) {
    logger.info("Add delegate profile");
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void delete(String accountId, String delegateProfileId) {
    logger.info("Delete delegate profile");
    throw new UnsupportedOperationException("not implemented");
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