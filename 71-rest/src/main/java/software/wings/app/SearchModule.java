package software.wings.app;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.search.ElasticsearchServiceImpl;
import software.wings.search.SearchService;
import software.wings.search.entities.application.ApplicationSearchEntity;
import software.wings.search.entities.deployment.DeploymentSearchEntity;
import software.wings.search.entities.environment.EnvironmentSearchEntity;
import software.wings.search.entities.pipeline.PipelineSearchEntity;
import software.wings.search.entities.service.ServiceSearchEntity;
import software.wings.search.entities.workflow.WorkflowSearchEntity;
import software.wings.search.framework.SearchDao;
import software.wings.search.framework.SearchEntity;
import software.wings.search.framework.SynchronousElasticsearchDao;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.client.utils.URIBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

/**
 * Module with Binding for all search related classes.
 *
 * @author utkarsh
 */

@OwnedBy(PL)
@Slf4j
public class SearchModule extends AbstractModule {
  @Provides
  @Singleton
  public RestHighLevelClient getClient(MainConfiguration mainConfiguration) {
    try {
      URI uri = new URIBuilder(mainConfiguration.getElasticsearchConfig().getUri()).build();
      return new RestHighLevelClient(RestClient.builder(new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme())));
    } catch (URISyntaxException e) {
      log.error(
          String.format("Elasticsearch URI %s is invalid", mainConfiguration.getElasticsearchConfig().getUri()), e);
    }
    return null;
  }

  @Override
  protected void configure() {
    bind(SearchService.class).to(ElasticsearchServiceImpl.class);
    bind(SearchDao.class).to(SynchronousElasticsearchDao.class);
    bindEntities();
  }

  private void bindEntities() {
    Multibinder<SearchEntity<?>> searchEntityMultibinder =
        Multibinder.newSetBinder(binder(), new TypeLiteral<SearchEntity<?>>() {});
    searchEntityMultibinder.addBinding().to(ApplicationSearchEntity.class);
    searchEntityMultibinder.addBinding().to(PipelineSearchEntity.class);
    searchEntityMultibinder.addBinding().to(WorkflowSearchEntity.class);
    searchEntityMultibinder.addBinding().to(ServiceSearchEntity.class);
    searchEntityMultibinder.addBinding().to(EnvironmentSearchEntity.class);
    searchEntityMultibinder.addBinding().to(DeploymentSearchEntity.class);
  }
}
