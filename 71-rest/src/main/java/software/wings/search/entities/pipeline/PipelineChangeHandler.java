package software.wings.search.entities.pipeline;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DBObject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.cache.EntityCache;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.Pipeline;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.dl.WingsPersistence;
import software.wings.search.entities.application.ApplicationSearchEntity;
import software.wings.search.entities.pipeline.PipelineView.PipelineViewKeys;
import software.wings.search.entities.service.ServiceSearchEntity;
import software.wings.search.entities.workflow.WorkflowSearchEntity;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.SearchDao;
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
@Singleton
public class PipelineChangeHandler implements ChangeHandler {
  @Inject private SearchDao searchDao;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private PipelineViewBuilder pipelineViewBuilder;
  private static final Mapper mapper = SearchEntityUtils.getMapper();
  private static final EntityCache entityCache = SearchEntityUtils.getEntityCache();

  private boolean handleApplicationChange(ChangeEvent changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE) {
      DBObject document = changeEvent.getChanges();
      if (document.containsField(ApplicationKeys.name)) {
        String keyToUpdate = PipelineViewKeys.appName;
        String newValue = document.get(ApplicationKeys.name).toString();
        String filterKey = PipelineViewKeys.appId;
        String filterValue = changeEvent.getUuid();
        return searchDao.updateKeyInMultipleDocuments(
            PipelineSearchEntity.TYPE, keyToUpdate, newValue, filterKey, filterValue);
      }
    }
    return true;
  }

  private boolean handleServiceChange(ChangeEvent changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE) {
      DBObject document = changeEvent.getChanges();
      if (document.containsField(ServiceKeys.name)) {
        String entityType = PipelineViewKeys.services;
        String newNameValue = document.get(ServiceKeys.name).toString();
        String filterId = changeEvent.getUuid();
        return searchDao.updateListInMultipleDocuments(PipelineSearchEntity.TYPE, entityType, newNameValue, filterId);
      }
    }
    return true;
  }

  private boolean handleWorkflowChange(ChangeEvent changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE) {
      DBObject document = changeEvent.getChanges();
      if (document.containsField(WorkflowKeys.name)) {
        String entityType = PipelineViewKeys.workflows;
        String newNameValue = document.get(WorkflowKeys.name).toString();
        String filterId = changeEvent.getUuid();
        return searchDao.updateListInMultipleDocuments(PipelineSearchEntity.TYPE, entityType, newNameValue, filterId);
      }
    }
    return true;
  }

  private boolean handlePipelineChange(ChangeEvent changeEvent) {
    AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
    switch (changeEvent.getChangeType()) {
      case INSERT: {
        DBObject document = changeEvent.getFullDocument();
        Pipeline pipeline =
            (Pipeline) mapper.fromDBObject(advancedDatastore, changeEvent.getEntityType(), document, entityCache);
        pipeline.setUuid(changeEvent.getUuid());
        PipelineView pipelineView = pipelineViewBuilder.createPipelineView(pipeline);

        Optional<String> jsonString = SearchEntityUtils.convertToJson(pipelineView);
        if (jsonString.isPresent()) {
          return searchDao.upsertDocument(PipelineSearchEntity.TYPE, pipelineView.getId(), jsonString.get());
        }
        return false;
      }
      case UPDATE: {
        DBObject changeDocument = changeEvent.getChanges();
        DBObject fullDocument = changeEvent.getFullDocument();
        Pipeline pipeline =
            (Pipeline) mapper.fromDBObject(advancedDatastore, changeEvent.getEntityType(), fullDocument, entityCache);
        pipeline.setUuid(changeEvent.getUuid());
        PipelineView pipelineView = pipelineViewBuilder.createPipelineView(pipeline, changeDocument);
        Optional<String> jsonString = SearchEntityUtils.convertToJson(pipelineView);
        if (jsonString.isPresent()) {
          return searchDao.upsertDocument(PipelineSearchEntity.TYPE, pipelineView.getId(), jsonString.get());
        }
        return false;
      }
      case DELETE: {
        return searchDao.deleteDocument(PipelineSearchEntity.TYPE, changeEvent.getUuid());
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
    if (changeEvent.getEntityType().equals(WorkflowSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleWorkflowChange(changeEvent);
    }
    if (changeEvent.getEntityType().equals(ServiceSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleServiceChange(changeEvent);
    }
    return isChangeHandled;
  }
}
