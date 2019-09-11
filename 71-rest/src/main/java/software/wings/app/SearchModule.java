package software.wings.app;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;

import io.harness.manage.ManagedExecutorService;
import io.harness.persistence.PersistentEntity;
import io.harness.threading.ThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.client.utils.URIBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import software.wings.search.entities.application.ApplicationSearchEntity;
import software.wings.search.entities.pipeline.PipelineSearchEntity;
import software.wings.search.framework.SearchEntity;
import software.wings.service.impl.ElasticsearchServiceImpl;
import software.wings.service.intfc.SearchService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Module with Binding for all search related classes.
 *
 * @author utkarsh
 */

@Slf4j
public class SearchModule extends AbstractModule {
  @Provides
  public RestHighLevelClient getClient(MainConfiguration mainConfiguration) {
    try {
      URI uri = new URIBuilder(mainConfiguration.getElasticsearchConfig().getUri()).build();
      return new RestHighLevelClient(RestClient.builder(new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme())));
    } catch (URISyntaxException e) {
      logger.error(
          String.format("Elasticsearch URI %s is invalid", mainConfiguration.getElasticsearchConfig().getUri()), e);
    }
    return null;
  }

  protected void configure() {
    bind(SearchService.class).to(ElasticsearchServiceImpl.class);
    bind(ManagedExecutorService.class)
        .toInstance(new ManagedExecutorService(ThreadPool.create(4, 20, 1, TimeUnit.SECONDS)));
    bind(ScheduledExecutorService.class).toInstance(Executors.newScheduledThreadPool(1));
    bindEntities();
  }

  private void bindEntities() {
    MapBinder<Class<? extends PersistentEntity>, SearchEntity<?>> sourceEntitiesToSearchEntitiesMap =
        MapBinder.newMapBinder(
            binder(), new TypeLiteral<Class<? extends PersistentEntity>>() {}, new TypeLiteral<SearchEntity<?>>() {});

    sourceEntitiesToSearchEntitiesMap.addBinding(ApplicationSearchEntity.SOURCE_ENTITY_CLASS)
        .to(ApplicationSearchEntity.class);
    sourceEntitiesToSearchEntitiesMap.addBinding(PipelineSearchEntity.SOURCE_ENTITY_CLASS)
        .to(PipelineSearchEntity.class);
  }
}
