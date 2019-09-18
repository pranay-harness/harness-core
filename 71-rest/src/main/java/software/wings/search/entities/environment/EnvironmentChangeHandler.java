package software.wings.search.entities.environment;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DBObject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.cache.EntityCache;
import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.Environment;
import software.wings.dl.WingsPersistence;
import software.wings.search.entities.application.ApplicationSearchEntity;
import software.wings.search.entities.environment.EnvironmentView.EnvironmentViewKeys;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.SearchDao;
import software.wings.search.framework.SearchEntityUtils;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeType;

import java.util.Optional;

@Slf4j
@Singleton
public class EnvironmentChangeHandler implements ChangeHandler {
  @Inject private SearchDao searchDao;
  @Inject private WingsPersistence wingsPersistence;
  private static final Mapper mapper = SearchEntityUtils.getMapper();
  private static final EntityCache entityCache = SearchEntityUtils.getEntityCache();

  private boolean handleApplicationChange(ChangeEvent changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE) {
      DBObject document = changeEvent.getChanges();
      if (document.containsField(ApplicationKeys.name)) {
        String keyToUpdate = EnvironmentViewKeys.appName;
        String newValue = document.get(ApplicationKeys.name).toString();
        String filterKey = EnvironmentViewKeys.appId;
        String filterValue = changeEvent.getUuid();
        return searchDao.updateKeyInMultipleDocuments(
            EnvironmentSearchEntity.TYPE, keyToUpdate, newValue, filterKey, filterValue);
      }
    }
    return true;
  }

  private boolean handleEnvironmentChange(ChangeEvent changeEvent) {
    AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
    switch (changeEvent.getChangeType()) {
      case INSERT:
      case UPDATE: {
        DBObject document = changeEvent.getFullDocument();
        Environment environment =
            (Environment) mapper.fromDBObject(advancedDatastore, changeEvent.getEntityType(), document, entityCache);
        environment.setUuid(changeEvent.getUuid());
        EnvironmentView environmentView = EnvironmentView.fromEnvironment(environment);

        if (environmentView.getAppId() != null && changeEvent.getChangeType().equals(ChangeType.INSERT)) {
          Application application = wingsPersistence.get(Application.class, environment.getAppId());
          environmentView.setAppName(application.getName());
        }

        Optional<String> jsonString = SearchEntityUtils.convertToJson(environmentView);
        if (jsonString.isPresent()) {
          return searchDao.upsertDocument(EnvironmentSearchEntity.TYPE, environmentView.getId(), jsonString.get());
        }
        return false;
      }
      case DELETE: {
        return searchDao.deleteDocument(EnvironmentSearchEntity.TYPE, changeEvent.getUuid());
      }
      default: { break; }
    }
    return true;
  }

  public boolean handleChange(ChangeEvent changeEvent) {
    boolean isChangeHandled = true;
    if (changeEvent.getEntityType().equals(EnvironmentSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleEnvironmentChange(changeEvent);
    }
    if (changeEvent.getEntityType().equals(ApplicationSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleApplicationChange(changeEvent);
    }
    return isChangeHandled;
  }
}
