/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.enforcement.services;

import io.harness.enforcement.beans.details.FeatureRestrictionDetailsDTO;
import io.harness.enforcement.beans.internal.RestrictionMetadataMapResponseDTO;
import io.harness.enforcement.beans.metadata.FeatureRestrictionMetadataDTO;
import io.harness.enforcement.constants.FeatureRestrictionName;

import java.util.List;

public interface EnforcementService {
  boolean isFeatureAvailable(FeatureRestrictionName featureRestrictionName, String accountIdentifier);
  void checkAvailabilityOrThrow(FeatureRestrictionName featureRestrictionName, String accountIdentifier);
  FeatureRestrictionMetadataDTO getFeatureMetadata(
      FeatureRestrictionName featureRestrictionName, String accountIdentifier);
  RestrictionMetadataMapResponseDTO getFeatureRestrictionMetadataMap(
      List<FeatureRestrictionName> featureRestrictionNames, String accountIdentifier);
  List<FeatureRestrictionMetadataDTO> getAllFeatureRestrictionMetadata();
  FeatureRestrictionDetailsDTO getFeatureDetail(
      FeatureRestrictionName featureRestrictionName, String accountIdentifier);
  List<FeatureRestrictionDetailsDTO> getFeatureDetails(
      List<FeatureRestrictionName> featureRestrictionNames, String accountIdentifier);
  List<FeatureRestrictionDetailsDTO> getEnabledFeatureDetails(String accountIdentifier);
}
