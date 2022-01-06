/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.enforcement.client.example;

import io.harness.enforcement.beans.CustomRestrictionEvaluationDTO;
import io.harness.enforcement.client.custom.CustomRestrictionInterface;
import io.harness.licensing.Edition;

import com.google.inject.Singleton;

@Singleton
public class ExampleCustomImpl implements CustomRestrictionInterface {
  @Override
  public boolean evaluateCustomRestriction(CustomRestrictionEvaluationDTO customFeatureEvaluationDTO) {
    Edition edition = customFeatureEvaluationDTO.getEdition();
    if (Edition.TEAM.equals(edition)) {
      return true;
    } else {
      return false;
    }
  }
}
