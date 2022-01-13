/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.creator.filters;

import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.pms.cdng.sample.cd.creator.filters.CdFilter;
import io.harness.pms.filter.creation.FilterCreationResponse;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CDNGFilterCreationResponseMergerTest extends CategoryTest {
  CDNGFilterCreationResponseMerger cdngFilterCreationResponseMerger = new CDNGFilterCreationResponseMerger();
  FilterCreationResponse finalResponse = FilterCreationResponse.builder().build();
  FilterCreationResponse currentResponse = FilterCreationResponse.builder().build();

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void cdngFilter() {
    currentResponse.setPipelineFilter(CdFilter.builder().build());
    cdngFilterCreationResponseMerger.mergeFilterCreationResponse(finalResponse, currentResponse);
    assertThat(finalResponse.getPipelineFilter()).isNotNull();
  }
}
