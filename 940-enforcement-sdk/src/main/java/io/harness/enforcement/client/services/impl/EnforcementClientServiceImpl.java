package io.harness.enforcement.client.services.impl;

import static io.harness.remote.client.NGRestUtils.getResponse;

import io.harness.enforcement.beans.metadata.AvailabilityRestrictionMetadataDTO;
import io.harness.enforcement.beans.metadata.FeatureRestrictionMetadataDTO;
import io.harness.enforcement.beans.metadata.RateLimitRestrictionMetadataDTO;
import io.harness.enforcement.beans.metadata.RestrictionMetadataDTO;
import io.harness.enforcement.beans.metadata.StaticLimitRestrictionMetadataDTO;
import io.harness.enforcement.client.EnforcementClient;
import io.harness.enforcement.client.EnforcementClientConfiguration;
import io.harness.enforcement.client.services.EnforcementClientService;
import io.harness.enforcement.client.services.EnforcementSdkRegisterService;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.enforcement.exceptions.FeatureNotSupportedException;
import io.harness.enforcement.exceptions.LimitExceededException;
import io.harness.enforcement.utils.SuggestionMessageUtils;
import io.harness.exception.UnexpectedException;
import io.harness.licensing.Edition;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class EnforcementClientServiceImpl implements EnforcementClientService {
  private final EnforcementClient enforcementClient;
  private final EnforcementSdkRegisterService enforcementSdkRegisterService;
  private final EnforcementClientConfiguration enforcementClientConfiguration;

  private static final String UNLIMITED = "unlimited";
  private static final String ENABLED = "enabled";

  @Inject
  public EnforcementClientServiceImpl(EnforcementClient enforcementClient,
      EnforcementSdkRegisterService enforcementSdkRegisterService,
      EnforcementClientConfiguration enforcementClientConfiguration) {
    this.enforcementClient = enforcementClient;
    this.enforcementSdkRegisterService = enforcementSdkRegisterService;
    this.enforcementClientConfiguration = enforcementClientConfiguration;
  }

  @Override
  public boolean isAvailable(FeatureRestrictionName featureRestrictionName, String accountIdentifier) {
    try {
      checkAvailability(featureRestrictionName, accountIdentifier);
    } catch (Exception e) {
      log.error(String.format("Enforcement check on feature [%s] for account [%s] does not succeed",
                    featureRestrictionName, accountIdentifier),
          e);
      return false;
    }
    return true;
  }

  @Override
  public void checkAvailability(FeatureRestrictionName featureRestrictionName, String accountIdentifier) {
    if (!enforcementClientConfiguration.isEnforcementCheckEnabled()) {
      return;
    }

    FeatureRestrictionMetadataDTO featureMetadataDTO;
    try {
      featureMetadataDTO =
          getResponse(enforcementClient.getFeatureRestrictionMetadata(featureRestrictionName, accountIdentifier));
    } catch (UnexpectedException e) {
      log.error("Not able to fetch feature restriction metadata from ng-manager, failover to bypass the check", e);
      return;
    }

    Edition edition = featureMetadataDTO.getEdition();
    RestrictionMetadataDTO currentRestriction = featureMetadataDTO.getRestrictionMetadata().get(edition);
    switch (currentRestriction.getRestrictionType()) {
      case AVAILABILITY:
        AvailabilityRestrictionMetadataDTO availabilityRestrictionMetadataDTO =
            (AvailabilityRestrictionMetadataDTO) currentRestriction;
        if (!availabilityRestrictionMetadataDTO.isEnabled()) {
          throw new FeatureNotSupportedException(SuggestionMessageUtils.generateSuggestionMessage(
              "Feature is not enabled", edition, featureMetadataDTO.getRestrictionMetadata(), ENABLED));
        }
        break;
      case STATIC_LIMIT:
        StaticLimitRestrictionMetadataDTO staticLimitRestrictionMetadataDTO =
            (StaticLimitRestrictionMetadataDTO) currentRestriction;
        RestrictionUsageInterface staticUsage = enforcementSdkRegisterService.get(featureRestrictionName);
        if (verifyExceedLimit(
                staticLimitRestrictionMetadataDTO.getLimit(), staticUsage.getCurrentValue(accountIdentifier))) {
          throw new LimitExceededException(SuggestionMessageUtils.generateSuggestionMessage(
              String.format(
                  "Exceeded static limitation. Current Limit: %s", staticLimitRestrictionMetadataDTO.getLimit()),
              edition, featureMetadataDTO.getRestrictionMetadata(), ENABLED));
        }
        break;
      case RATE_LIMIT:
        RateLimitRestrictionMetadataDTO rateLimitRestriction = (RateLimitRestrictionMetadataDTO) currentRestriction;
        RestrictionUsageInterface rateUsage = enforcementSdkRegisterService.get(featureRestrictionName);
        if (verifyExceedLimit(rateLimitRestriction.getLimit(), rateUsage.getCurrentValue(accountIdentifier))) {
          throw new LimitExceededException(SuggestionMessageUtils.generateSuggestionMessage(
              String.format("Exceeded rate limitation. Current Limit: %s", rateLimitRestriction.getLimit()), edition,
              featureMetadataDTO.getRestrictionMetadata(), ENABLED));
        }
        break;
      default:
        throw new IllegalArgumentException("Unknown restriction type");
    }
  }

  private boolean verifyExceedLimit(long limit, long count) {
    return limit <= count;
  }
}
