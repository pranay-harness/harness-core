package software.wings.search.entities.application;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestHighLevelClient;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.cache.DefaultEntityCache;
import org.mongodb.morphia.mapping.cache.EntityCache;
import software.wings.beans.Application;
import software.wings.dl.WingsPersistence;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.ElasticsearchUtils;
import software.wings.search.framework.SearchEntityUtils;
import software.wings.search.framework.changestreams.ChangeEvent;

import java.util.Optional;

/**
 * The handler which will maintain the application document
 * in the search engine database.
 *
 * @author utkarsh
 */

@Slf4j
@Singleton
public class ApplicationChangeHandler implements ChangeHandler {
  @Inject private RestHighLevelClient elasticsearchClient;
  @Inject private WingsPersistence wingsPersistence;
  private static final Mapper mapper = new Mapper();
  private static final EntityCache entityCache = new DefaultEntityCache();

  private boolean handleApplicationChange(ChangeEvent changeEvent) {
    AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
    switch (changeEvent.getChangeType()) {
      case INSERT:
      case UPDATE: {
        logger.info(changeEvent.getChangeDocument().toString());
        Application application = (Application) mapper.fromDBObject(
            advancedDatastore, changeEvent.getEntityType(), changeEvent.getChangeDocument(), entityCache);
        application.setUuid(changeEvent.getUuid());

        ApplicationView applicationView = ApplicationView.fromApplication(application);
        Optional<String> applicationViewJson = SearchEntityUtils.convertToJson(applicationView);
        if (applicationViewJson.isPresent()) {
          return ElasticsearchUtils.upsertDocument(
              elasticsearchClient, ApplicationSearchEntity.TYPE, applicationView.getId(), applicationViewJson.get());
        }
        return false;
      }
      case DELETE: {
        return ElasticsearchUtils.deleteDocument(
            elasticsearchClient, ApplicationSearchEntity.TYPE, changeEvent.getUuid());
      }
      default:
    }
    return true;
  }

  public boolean handleChange(ChangeEvent changeEvent) {
    if (changeEvent.getEntityType().getSimpleName().equals(
            ApplicationSearchEntity.SOURCE_ENTITY_CLASS.getSimpleName())) {
      return handleApplicationChange(changeEvent);
    }
    return true;
  }
}
