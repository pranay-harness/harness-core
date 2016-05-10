/**
 *
 */
package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.wings.WingsBaseUnitTest;
import software.wings.beans.CatalogItem;
import software.wings.beans.CatalogNames;
import software.wings.service.intfc.CatalogService;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/**
 * @author Rishi
 *
 */
public class CatalogServiceTest extends WingsBaseUnitTest {
  @Inject private CatalogService catalogService;

  @Test
  public void shouldGetCardviewSortBy() {
    List<CatalogItem> catalogItems = catalogService.getCatalogItems(CatalogNames.CARD_VIEW_SORT_BY);
    assertThat(catalogItems).isNotNull();
    assertThat(catalogItems.size()).isEqualTo(3);
  }

  @Test
  public void shouldGetCatalogs() {
    Map<String, List<CatalogItem>> catalogs =
        catalogService.getCatalogs(CatalogNames.CARD_VIEW_SORT_BY, CatalogNames.ARTIFACT_TYPE);
    assertThat(catalogs).isNotNull();
    assertThat(catalogs.size()).isEqualTo(2);
  }
}
