/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.enforcement.client.services.EnforcementClientService;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.yaml.schema.beans.YamlSchemaMetadata;
import io.harness.yaml.schema.beans.YamlSchemaWithDetails;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.PIPELINE)
public class FeatureRestrictionsGetter {
  private final EnforcementClientService enforcementClientService;
  @Inject
  FeatureRestrictionsGetter(EnforcementClientService enforcementClientService) {
    this.enforcementClientService = enforcementClientService;
  }
  public Map<String, Boolean> getFeatureRestrictionsAvailability(
      List<YamlSchemaWithDetails> yamlSchemaWithDetailsList, String accountIdentifier) {
    List<YamlSchemaMetadata> yamlSchemaMetadataList = yamlSchemaWithDetailsList.stream()
                                                          .map(YamlSchemaWithDetails::getYamlSchemaMetadata)
                                                          .collect(Collectors.toList());
    Set<FeatureRestrictionName> featureRestrictionNameSet = new HashSet<>();
    for (YamlSchemaMetadata yamlSchemaMetadata : yamlSchemaMetadataList) {
      if (yamlSchemaMetadata.getFeatureRestrictions() != null) {
        featureRestrictionNameSet.addAll(yamlSchemaMetadata.getFeatureRestrictions()
                                             .stream()
                                             .map(FeatureRestrictionName::valueOf)
                                             .collect(Collectors.toList()));
      }
    }
    Map<FeatureRestrictionName, Boolean> featureRestrictionMap =
        enforcementClientService.getAvailabilityMap(featureRestrictionNameSet, accountIdentifier);
    if (featureRestrictionMap == null) {
      return Collections.emptyMap();
    }
    Map<String, Boolean> featureRestrictions = new HashMap<>();
    for (Map.Entry<FeatureRestrictionName, Boolean> entry : featureRestrictionMap.entrySet()) {
      featureRestrictions.put(String.valueOf(entry.getKey()), entry.getValue());
    }
    return featureRestrictions;
  }
}
