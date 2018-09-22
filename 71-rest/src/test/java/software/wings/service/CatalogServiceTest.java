package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.CatalogItem;
import software.wings.service.intfc.CatalogService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.utils.JsonUtils;

import java.util.List;
import java.util.Map;

/**
 * The Class CatalogServiceTest.
 *
 * @author Rishi.
 */
public class CatalogServiceTest extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);
  @Mock private FeatureFlagService mockFeatureFlagService;
  @Inject @InjectMocks private CatalogService catalogService;

  /**
   * Should get cardview sort by.
   */
  @Test
  public void shouldGetCardviewSortBy() {
    List<CatalogItem> catalogItems = catalogService.getCatalogItems("CARD_VIEW_SORT_BY");
    logger.debug("catalogItems: {}", catalogItems);
    assertThat(catalogItems).isNotNull();
    assertThat(catalogItems.size()).isEqualTo(3);
  }

  /**
   * Should get catalogs.
   */
  @Test
  public void shouldGetCatalogs() {
    Map<String, List<CatalogItem>> catalogs = catalogService.getCatalogs("CARD_VIEW_SORT_BY", "ARTIFACT_TYPE");
    assertThat(catalogs).isNotNull();
    assertThat(catalogs.size()).isEqualTo(2);
  }
}
