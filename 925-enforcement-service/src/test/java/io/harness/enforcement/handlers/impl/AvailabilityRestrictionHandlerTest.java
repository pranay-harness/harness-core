/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.enforcement.handlers.impl;

import static io.harness.rule.OwnerRule.ZHUO;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.ModuleType;
import io.harness.category.element.UnitTests;
import io.harness.enforcement.bases.AvailabilityRestriction;
import io.harness.enforcement.beans.details.FeatureRestrictionDetailsDTO;
import io.harness.enforcement.beans.metadata.AvailabilityRestrictionMetadataDTO;
import io.harness.enforcement.beans.metadata.RestrictionMetadataDTO;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.enforcement.constants.RestrictionType;
import io.harness.enforcement.exceptions.FeatureNotSupportedException;
import io.harness.licensing.Edition;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AvailabilityRestrictionHandlerTest extends CategoryTest {
  private AvailabilityRestrictionHandler handler;
  private FeatureRestrictionName featureRestrictionName = FeatureRestrictionName.TEST1;
  private AvailabilityRestriction restriction;
  private String accountIdentifier = "accountId";
  private ModuleType moduleType = ModuleType.CD;
  private Edition edition = Edition.ENTERPRISE;

  @Before
  public void setup() {
    handler = new AvailabilityRestrictionHandler();
    restriction = new AvailabilityRestriction(RestrictionType.AVAILABILITY, true);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheck() {
    handler.check(featureRestrictionName, restriction, accountIdentifier, moduleType, edition);
  }

  @Test(expected = FeatureNotSupportedException.class)
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckFailed() {
    AvailabilityRestriction invalid = new AvailabilityRestriction(RestrictionType.AVAILABILITY, false);
    handler.check(featureRestrictionName, invalid, accountIdentifier, moduleType, edition);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testFillRestrictionDTO() {
    FeatureRestrictionDetailsDTO dto = FeatureRestrictionDetailsDTO.builder().build();
    handler.fillRestrictionDTO(featureRestrictionName, restriction, accountIdentifier, edition, dto);

    assertThat(dto.getRestrictionType()).isEqualTo(RestrictionType.AVAILABILITY);
    assertThat(dto.getRestriction()).isNotNull();
    assertThat(dto.isAllowed()).isTrue();
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetMetadataDTO() {
    RestrictionMetadataDTO metadataDTO = handler.getMetadataDTO(restriction, null, null);

    AvailabilityRestrictionMetadataDTO dto = (AvailabilityRestrictionMetadataDTO) metadataDTO;
    assertThat(dto.getRestrictionType()).isEqualTo(RestrictionType.AVAILABILITY);
    assertThat(dto.isEnabled()).isEqualTo(true);
  }
}
