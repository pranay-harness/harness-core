/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc;

import software.wings.beans.CatalogItem;

import java.util.List;
import java.util.Map;

/**
 * The Interface CatalogService.
 *
 * @author Rishi.
 */
public interface CatalogService {
  /**
   * Gets the catalog items.
   *
   * @param catalogType the catalog type
   * @return the catalog items
   */
  List<CatalogItem> getCatalogItems(String catalogType);

  /**
   * Gets the catalogs.
   *
   * @param catalogTypes the catalog types
   * @return the catalogs
   */
  Map<String, List<CatalogItem>> getCatalogs(String... catalogTypes);
}
