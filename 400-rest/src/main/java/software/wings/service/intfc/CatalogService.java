/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
