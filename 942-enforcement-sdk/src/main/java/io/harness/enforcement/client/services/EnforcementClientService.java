/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.enforcement.client.services;

import io.harness.enforcement.beans.metadata.RestrictionMetadataDTO;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.enforcement.exceptions.EnforcementServiceConnectionException;
import io.harness.enforcement.exceptions.WrongFeatureStateException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface EnforcementClientService {
  /**
   * Check if enforcement is enabled in current module
   * @return true if enabled, else false
   */
  boolean isEnforcementEnabled();

  /**
   * Check if available for next feature consume
   * @param featureRestrictionName
   * @param accountIdentifier
   * @return true if available, otherwise false
   */
  boolean isAvailable(FeatureRestrictionName featureRestrictionName, String accountIdentifier);

  /**
   * Check if available for next feature consume, increment only works for Rate/Static restriction type
   * @param featureRestrictionName
   * @param accountIdentifier
   * @param increment
   * @return
   */
  boolean isAvailableWithIncrement(
      FeatureRestrictionName featureRestrictionName, String accountIdentifier, long increment);
  /**
   * Check availability for next feature consume
   * @param featureRestrictionName
   * @param accountIdentifier
   * @return
   *
   * @exception throw FeatureNotSupportedException, LimitExceededException when not available
   */
  void checkAvailability(FeatureRestrictionName featureRestrictionName, String accountIdentifier);

  /**
   * Check availability for next feature consume, increment only works for Rate/Static restriction type
   * @param featureRestrictionName
   * @param accountIdentifier
   * @param increment
   */
  void checkAvailabilityWithIncrement(
      FeatureRestrictionName featureRestrictionName, String accountIdentifier, long increment);

  /**
   * Used only in case checking for feature restriction status in other microservices.
   * @param featureRestrictionName
   * @param accountIdentifier
   * @return
   */
  boolean isRemoteFeatureAvailable(FeatureRestrictionName featureRestrictionName, String accountIdentifier);

  /**
   * Used only in case checking for list of feature restriction status in other microservices.
   * @param featureRestrictionNames
   * @param accountIdentifier
   * @return
   */
  Map<FeatureRestrictionName, Boolean> getAvailabilityForRemoteFeatures(
      List<FeatureRestrictionName> featureRestrictionNames, String accountIdentifier);

  /**
   * Get a list of availability
   * @param featureRestrictionNames
   * @param accountIdentifier
   * @return
   */
  Map<FeatureRestrictionName, Boolean> getAvailabilityMap(
      Set<FeatureRestrictionName> featureRestrictionNames, String accountIdentifier);
  Optional<RestrictionMetadataDTO> getRestrictionMetadata(FeatureRestrictionName featureRestrictionName,
      String accountIdentifier) throws WrongFeatureStateException, EnforcementServiceConnectionException;
  Map<FeatureRestrictionName, RestrictionMetadataDTO> getRestrictionMetadataMap(
      List<FeatureRestrictionName> featureRestrictionNames, String accountIdentifier)
      throws WrongFeatureStateException, EnforcementServiceConnectionException;
}
