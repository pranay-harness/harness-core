package software.wings.service;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.CatalogItem.Builder.aCatalogItem;
import static software.wings.beans.FeatureName.WINRM_SUPPORT;

import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.CatalogItem;
import software.wings.service.impl.CatalogServiceImpl;
import software.wings.service.intfc.CatalogService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.utils.JsonUtils;
import software.wings.utils.YamlUtils;

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

  private static final CatalogItem iisCatalogItem = aCatalogItem().withName("IIS Website").withValue("IIS").build();

  @Test
  public void shouldNotGetIISArtifactType() {
    List<CatalogItem> catalogItems = catalogService.getCatalogItems("ARTIFACT_TYPE");
    assertFalse(catalogItems.stream().anyMatch(item -> item.getValue().equals("IIS")));
  }

  @Test
  public void shouldGetIISArtifactType() {
    when(mockFeatureFlagService.isEnabled(WINRM_SUPPORT, GLOBAL_ACCOUNT_ID)).thenReturn(true);
    CatalogService catalogServiceWithMockFeatureFlag = new CatalogServiceImpl(new YamlUtils(), mockFeatureFlagService);
    List<CatalogItem> catalogItems = catalogServiceWithMockFeatureFlag.getCatalogItems("ARTIFACT_TYPE");
    assertTrue(catalogItems.stream().anyMatch(item -> item.getValue().equals("IIS")));
  }
}
