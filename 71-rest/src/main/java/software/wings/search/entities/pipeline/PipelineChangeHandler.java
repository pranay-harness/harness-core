package software.wings.search.entities.pipeline;

import com.google.inject.Inject;

import com.mongodb.DBObject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.cache.DefaultEntityCache;
import org.mongodb.morphia.mapping.cache.EntityCache;
import software.wings.beans.Application;
import software.wings.beans.Pipeline;
import software.wings.dl.WingsPersistence;
import software.wings.search.entities.application.ApplicationSearchEntity;
import software.wings.search.entities.pipeline.PipelineView.PipelineViewKeys;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.ElasticsearchDao;
import software.wings.search.framework.SearchEntityUtils;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeType;

import java.util.Optional;

/**
 * The handler which will maintain the pipeline
 * document in the search engine database.
 *
 * @author utkarsh
 */

@Slf4j
public class PipelineChangeHandler implements ChangeHandler {
  @Inject private ElasticsearchDao elasticsearchDao;
  @Inject private WingsPersistence wingsPersistence;
  private static final Mapper mapper = new Mapper();
  private static final EntityCache entityCache = new DefaultEntityCache();

  private boolean handleApplicationChange(ChangeEvent changeEvent) {
    AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
    if (changeEvent.getChangeType() == ChangeType.UPDATE) {
      DBObject document = changeEvent.getChangeDocument();
      Application application =
          (Application) mapper.fromDBObject(advancedDatastore, changeEvent.getEntityType(), document, entityCache);
      application.setUuid(changeEvent.getUuid());

      if (application.getName() != null) {
        String keyToUpdate = PipelineViewKeys.appName;
        String newValue = application.getName();
        String filterKey = PipelineViewKeys.appId;
        String filterValue = application.getUuid();
        return elasticsearchDao.updateKeyInMultipleDocuments(
            PipelineSearchEntity.TYPE, keyToUpdate, newValue, filterKey, filterValue);
      }
    }
    return true;
  }

  private boolean handlePipelineChange(ChangeEvent changeEvent) {
    AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
    switch (changeEvent.getChangeType()) {
      case INSERT:
      case UPDATE: {
        DBObject document = changeEvent.getChangeDocument();
        Pipeline pipeline =
            (Pipeline) mapper.fromDBObject(advancedDatastore, changeEvent.getEntityType(), document, entityCache);
        pipeline.setUuid(changeEvent.getUuid());
        PipelineView pipelineView = PipelineView.fromPipeline(pipeline);

        if (pipelineView.getAppId() != null) {
          Application application = wingsPersistence.get(Application.class, pipeline.getAppId());
          pipelineView.setAppName(application.getName());
        }

        Optional<String> jsonString = SearchEntityUtils.convertToJson(pipelineView);
        if (jsonString.isPresent()) {
          return elasticsearchDao.upsertDocument(PipelineSearchEntity.TYPE, pipelineView.getId(), jsonString.get());
        }
        return false;
      }
      case DELETE: {
        return elasticsearchDao.deleteDocument(PipelineSearchEntity.TYPE, changeEvent.getUuid());
      }
      default:
    }
    return true;
  }

  public boolean handleChange(ChangeEvent changeEvent) {
    boolean isChangeHandled = true;
    if (changeEvent.getEntityType().equals(PipelineSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handlePipelineChange(changeEvent);
    }
    if (changeEvent.getEntityType().equals(ApplicationSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleApplicationChange(changeEvent);
    }
    return isChangeHandled;
  }
}
