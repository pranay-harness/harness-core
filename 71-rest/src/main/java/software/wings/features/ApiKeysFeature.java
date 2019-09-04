package software.wings.features;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import software.wings.beans.ApiKeyEntry;
import software.wings.beans.ApiKeyEntry.ApiKeyEntryKeys;
import software.wings.beans.EntityType;
import software.wings.features.api.AbstractPremiumFeature;
import software.wings.features.api.ComplianceByRemovingUsage;
import software.wings.features.api.FeatureRestrictions;
import software.wings.features.api.Usage;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.ApiKeyService;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class ApiKeysFeature extends AbstractPremiumFeature implements ComplianceByRemovingUsage {
  public static final String FEATURE_NAME = "API_KEYS";

  private final ApiKeyService apiKeyService;

  @Inject
  public ApiKeysFeature(
      AccountService accountService, FeatureRestrictions featureRestrictions, ApiKeyService apiKeyService) {
    super(accountService, featureRestrictions);
    this.apiKeyService = apiKeyService;
  }

  @Override
  public boolean removeUsageForCompliance(String accountId, String targetAccountType) {
    if (isUsageCompliantWithRestrictions(accountId, targetAccountType)) {
      return true;
    }
    apiKeyService.deleteByAccountId(accountId);

    return isUsageCompliantWithRestrictions(accountId, targetAccountType);
  }

  @Override
  public boolean isBeingUsed(String accountId) {
    return !getUsages(accountId).isEmpty();
  }

  @Override
  public Collection<Usage> getDisallowedUsages(String accountId, String targetAccountType) {
    if (isAvailable(targetAccountType)) {
      return Collections.emptyList();
    }

    return getUsages(accountId);
  }

  private Collection<Usage> getUsages(String accountId) {
    return getApiKeys(accountId).stream().map(ApiKeysFeature::toUsage).collect(Collectors.toList());
  }

  private static Usage toUsage(ApiKeyEntry apiKey) {
    return Usage.builder()
        .entityId(apiKey.getUuid())
        .entityName(apiKey.getName())
        .entityType(EntityType.API_KEY.name())
        .build();
  }

  @Override
  public String getFeatureName() {
    return FEATURE_NAME;
  }

  private List<ApiKeyEntry> getApiKeys(String accountId) {
    PageRequest<ApiKeyEntry> request = aPageRequest().addFilter(ApiKeyEntryKeys.accountId, EQ, accountId).build();
    return apiKeyService.list(request, accountId, false, false).getResponse();
  }
}
