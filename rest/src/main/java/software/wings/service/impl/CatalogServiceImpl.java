/**
 *
 */

package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.CatalogItem.Builder.aCatalogItem;
import static software.wings.beans.FeatureName.WINRM_SUPPORT;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.CatalogItem;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.service.intfc.CatalogService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.utils.YamlUtils;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Class CatalogServiceImpl.
 *
 * @author Rishi
 */
@Singleton
public class CatalogServiceImpl implements CatalogService {
  private static final Logger logger = LoggerFactory.getLogger(CatalogServiceImpl.class);
  private Map<String, List<CatalogItem>> catalogs;
  private static final CatalogItem iisCatalogItem = aCatalogItem().withName("IIS Website").withValue("IIS").build();

  /**
   * Instantiates a new catalog service impl.
   *
   * @param yamlUtils the yaml utils
   */
  @Inject
  public CatalogServiceImpl(YamlUtils yamlUtils, FeatureFlagService featureFlagService) {
    try {
      URL url = this.getClass().getResource(Constants.STATIC_CATALOG_URL);
      String yaml = Resources.toString(url, Charsets.UTF_8);
      catalogs = yamlUtils.read(yaml, new TypeReference<Map<String, List<CatalogItem>>>() {});

      if (featureFlagService.isEnabled(WINRM_SUPPORT, GLOBAL_ACCOUNT_ID)) {
        catalogs.get("ARTIFACT_TYPE").add(iisCatalogItem);
      }

      for (List<CatalogItem> catalogItems : catalogs.values()) {
        Collections.sort(catalogItems, CatalogItem.displayOrderComparator);
      }
    } catch (Exception e) {
      throw new WingsException(e);
    }
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.CatalogService#getCatalogItems(java.lang.String)
   */
  @Override
  public List<CatalogItem> getCatalogItems(String catalogType) {
    if (catalogs == null) {
      return null;
    }
    return catalogs.get(catalogType);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.CatalogService#getCatalogs(java.lang.String[])
   */
  @Override
  public Map<String, List<CatalogItem>> getCatalogs(String... catalogTypes) {
    if (catalogs == null) {
      return null;
    }
    if (isEmpty(catalogTypes)) {
      return catalogs;
    }

    Map<String, List<CatalogItem>> maps = new HashMap<>();
    for (String catalogType : catalogTypes) {
      maps.put(catalogType, catalogs.get(catalogType));
    }
    return maps;
  }
}
