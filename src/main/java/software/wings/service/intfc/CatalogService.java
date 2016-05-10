/**
 *
 */
package software.wings.service.intfc;

import software.wings.beans.CatalogItem;

import java.util.List;
import java.util.Map;

/**
 * @author Rishi
 *
 */
public interface CatalogService {
  public List<CatalogItem> getCatalogItems(String catalogType);

  Map<String, List<CatalogItem>> getCatalogs(String... catalogTypes);
}
