package software.wings.search.framework;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.app.MainConfiguration;

@Slf4j
public class ElasticsearchIndexManager {
  @Inject MainConfiguration mainConfiguration;

  public String getIndexName(String type) {
    String INDEX_SUFFIX = mainConfiguration.getElasticsearchConfig().getIndexSuffix();
    return type.concat(INDEX_SUFFIX);
  }
}
