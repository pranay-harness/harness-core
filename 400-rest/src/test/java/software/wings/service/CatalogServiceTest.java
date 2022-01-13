/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service;

import static io.harness.rule.OwnerRule.UNKNOWN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.ff.FeatureFlagService;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.CatalogItem;
import software.wings.service.intfc.CatalogService;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

/**
 * The Class CatalogServiceTest.
 *
 * @author Rishi.
 */
@Slf4j
public class CatalogServiceTest extends WingsBaseTest {
  @Mock private FeatureFlagService mockFeatureFlagService;
  @Inject @InjectMocks private CatalogService catalogService;

  /**
   * Should get cardview sort by.
   */
  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldGetCardviewSortBy() {
    List<CatalogItem> catalogItems = catalogService.getCatalogItems("CARD_VIEW_SORT_BY");
    log.debug("catalogItems: {}", catalogItems);
    assertThat(catalogItems).isNotNull();
    assertThat(catalogItems.size()).isEqualTo(3);
  }

  /**
   * Should get catalogs.
   */
  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldGetCatalogs() {
    Map<String, List<CatalogItem>> catalogs = catalogService.getCatalogs("CARD_VIEW_SORT_BY", "ARTIFACT_TYPE");
    assertThat(catalogs).isNotNull();
    assertThat(catalogs.size()).isEqualTo(2);
  }
}
