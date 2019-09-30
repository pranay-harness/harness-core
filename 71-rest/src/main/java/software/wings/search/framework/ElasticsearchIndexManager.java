package software.wings.search.framework;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.app.MainConfiguration;

/**
 * Class for managing elasticsearch indexes
 *
 * @author utkarsh
 */
@Slf4j
public class ElasticsearchIndexManager {
  @Inject MainConfiguration mainConfiguration;

  public String getIndexName(String type) {
    String indexSuffix = mainConfiguration.getElasticsearchConfig().getIndexSuffix();
    return type.concat(indexSuffix);
  }
}
