/**
 *
 */
package software.wings.service.impl;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.CatalogItem;
import software.wings.exception.WingsException;
import software.wings.service.intfc.CatalogService;
import software.wings.utils.YamlUtils;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Rishi
 */
@Singleton
public class CatalogServiceImpl implements CatalogService {
  private final Logger logger = LoggerFactory.getLogger(CatalogServiceImpl.class);
  private Map<String, List<CatalogItem>> catalogs;

  @Inject
  public CatalogServiceImpl(YamlUtils yamlUtils) {
    try {
      URL url = this.getClass().getResource("/configs/catalogs.yml");
      String yaml = Resources.toString(url, Charsets.UTF_8);
      catalogs = yamlUtils.read(yaml, new TypeReference<Map<String, List<CatalogItem>>>() {});
      for (List<CatalogItem> catalogItems : catalogs.values()) {
        Collections.sort(catalogItems, CatalogItem.displayOrderComparator);
      }
    } catch (Exception e) {
      logger.error("Error in initializing catalog", e);
      throw new WingsException(e);
    }
  }

  @Override
  public List<CatalogItem> getCatalogItems(String catalogType) {
    if (catalogs == null) {
      return null;
    }
    return catalogs.get(catalogType);
  }

  @Override
  public Map<String, List<CatalogItem>> getCatalogs(String... catalogTypes) {
    if (catalogs == null) {
      return null;
    }
    if (catalogTypes == null || catalogTypes.length == 0) {
      return catalogs;
    }

    Map<String, List<CatalogItem>> maps = new HashMap<>();
    for (String catalogType : catalogTypes) {
      maps.put(catalogType, catalogs.get(catalogType));
    }
    return maps;
  }
}
