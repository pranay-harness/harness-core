/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.enforcement.handlers.impl;

import static io.harness.remote.client.NGRestUtils.getResponse;

import io.harness.ModuleType;
import io.harness.enforcement.bases.CustomRestriction;
import io.harness.enforcement.bases.Restriction;
import io.harness.enforcement.beans.CustomRestrictionEvaluationDTO;
import io.harness.enforcement.beans.details.FeatureRestrictionDetailsDTO;
import io.harness.enforcement.beans.metadata.CustomRestrictionMetadataDTO;
import io.harness.enforcement.beans.metadata.RestrictionMetadataDTO;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.enforcement.exceptions.FeatureNotSupportedException;
import io.harness.enforcement.handlers.RestrictionHandler;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.licensing.Edition;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class CustomRestrictionHandler implements RestrictionHandler {
  @Override
  public void check(FeatureRestrictionName featureRestrictionName, Restriction restriction, String accountIdentifier,
      ModuleType moduleType, Edition edition) {
    checkWithUsage(featureRestrictionName, restriction, accountIdentifier, 0, moduleType, edition);
  }

  @Override
  public void checkWithUsage(FeatureRestrictionName featureRestrictionName, Restriction restriction,
      String accountIdentifier, long currentCount, ModuleType moduleType, Edition edition) {
    CustomRestriction customRestriction = (CustomRestriction) restriction;
    try {
      Boolean response =
          fetchSdkEvaluationResult(customRestriction, featureRestrictionName, accountIdentifier, edition, moduleType);
      if (!response) {
        throw new FeatureNotSupportedException(
            String.format("FeatureRestriction [%s] is not enabled", featureRestrictionName.name()));
      }
    } catch (UnexpectedException e) {
      log.error(String.format("Failed to call evaluateCustomFeatureRestriction for FeatureRestriction [%s]",
                    featureRestrictionName),
          e);
      return;
    }
  }

  @Override
  public void fillRestrictionDTO(FeatureRestrictionName featureRestrictionName, Restriction restriction,
      String accountIdentifier, Edition edition, FeatureRestrictionDetailsDTO featureDetailsDTO) {
    featureDetailsDTO.setRestrictionType(restriction.getRestrictionType());
    CustomRestriction customRestriction = (CustomRestriction) restriction;
    try {
      Boolean response = fetchSdkEvaluationResult(
          customRestriction, featureRestrictionName, accountIdentifier, edition, featureDetailsDTO.getModuleType());
      if (Boolean.TRUE.equals(response)) {
        featureDetailsDTO.setAllowed(true);
      } else {
        featureDetailsDTO.setAllowed(false);
      }
    } catch (UnexpectedException e) {
      log.error(String.format("Failed to call evaluateCustomFeatureRestriction for FeatureRestriction [%s]",
                    featureRestrictionName),
          e);
      featureDetailsDTO.setAllowed(true);
    } catch (InvalidRequestException e) {
      log.error(String.format("Check custom restriction for [%s] is failed", featureRestrictionName), e);
      featureDetailsDTO.setAllowed(false);
    }
  }

  @Override
  public RestrictionMetadataDTO getMetadataDTO(
      Restriction restriction, String accountIdentifier, ModuleType moduleType) {
    return CustomRestrictionMetadataDTO.builder().restrictionType(restriction.getRestrictionType()).build();
  }

  private boolean fetchSdkEvaluationResult(CustomRestriction customRestriction,
      FeatureRestrictionName featureRestrictionName, String accountIdentifier, Edition edition, ModuleType moduleType) {
    return getResponse(customRestriction.getEnforcementSdkClient().evaluateCustomFeatureRestriction(
        featureRestrictionName, accountIdentifier,
        CustomRestrictionEvaluationDTO.builder()
            .featureRestrictionName(featureRestrictionName)
            .accountIdentifier(accountIdentifier)
            .moduleType(moduleType)
            .edition(edition)
            .build()));
  }
}
