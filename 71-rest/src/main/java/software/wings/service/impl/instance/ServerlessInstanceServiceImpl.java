package software.wings.service.impl.instance;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.validation.Validator.notNullCheck;
import static io.harness.validation.Validator.nullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.eraro.ErrorCode;
import io.harness.exception.NoResultFoundException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.infrastructure.instance.ServerlessInstance;
import software.wings.beans.infrastructure.instance.SyncStatus;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.instance.ServerlessInstanceService;

import java.util.Collection;
import java.util.List;

@Singleton
@Slf4j
public class ServerlessInstanceServiceImpl implements ServerlessInstanceService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AppService appService;

  @Override
  public ServerlessInstance save(ServerlessInstance instance) {
    if (logger.isDebugEnabled()) {
      logger.debug("Begin - ServerlessInstance save called for uuid:" + instance.getUuid()
          + " and infraMappingId:" + instance.getInfraMappingId());
    }
    if (!appService.exist(instance.getAppId())) {
      throw NoResultFoundException.newBuilder()
          .code(ErrorCode.INVALID_ARGUMENT)
          .message("App does not exist: " + instance.getAppId())
          .build();
    }

    ServerlessInstance currentInstance = get(instance.getUuid());
    nullCheck("ServerlessInstance", currentInstance);

    String key = wingsPersistence.save(instance);
    ServerlessInstance updatedInstance =
        wingsPersistence.getWithAppId(ServerlessInstance.class, instance.getAppId(), key);
    if (logger.isDebugEnabled()) {
      logger.debug("End - ServerlessInstance save called for uuid:" + instance.getUuid()
          + " and infraMappingId:" + instance.getInfraMappingId());
    }

    return updatedInstance;
  }

  @Override
  public ServerlessInstance get(String serverlessInstanceId) {
    ServerlessInstance instance = wingsPersistence.get(ServerlessInstance.class, serverlessInstanceId);
    if (instance != null) {
      if (instance.isDeleted()) {
        return null;
      }
    }
    return instance;
  }

  @Override
  public PageResponse<ServerlessInstance> list(PageRequest<ServerlessInstance> pageRequest) {
    pageRequest.addFilter("isDeleted", Operator.EQ, false);
    return wingsPersistence.query(ServerlessInstance.class, pageRequest);
  }

  @Override
  public boolean delete(Collection<String> instanceIds) {
    final Query<ServerlessInstance> query = wingsPersistence.createQuery(ServerlessInstance.class);
    query.field("_id").in(instanceIds);

    long currentTimeMillis = System.currentTimeMillis();
    final UpdateOperations<ServerlessInstance> updateOperations =
        wingsPersistence.createUpdateOperations(ServerlessInstance.class);
    setUnset(updateOperations, "deletedAt", currentTimeMillis);
    setUnset(updateOperations, "isDeleted", true);

    wingsPersistence.update(query, updateOperations);
    return true;
  }

  @Override
  public ServerlessInstance update(ServerlessInstance instance) {
    if (instance == null) {
      return null;
    }

    if (!appService.exist(instance.getAppId())) {
      throw NoResultFoundException.newBuilder()
          .code(ErrorCode.INVALID_ARGUMENT)
          .message("App does not exist: " + instance.getAppId())
          .build();
    }

    notNullCheck("Uid is missing for the instance", instance.getUuid());

    final String uid = wingsPersistence.save(instance);
    return wingsPersistence.getWithAppId(ServerlessInstance.class, instance.getAppId(), uid);
  }

  @Override
  public List<SyncStatus> getSyncStatus(String appId, String serviceId, String envId) {
    PageRequest<SyncStatus> pageRequest = aPageRequest()
                                              .addFilter("appId", EQ, appId)
                                              .addFilter("serviceId", EQ, serviceId)
                                              .addFilter("envId", EQ, envId)
                                              .build();
    PageResponse<SyncStatus> response = wingsPersistence.query(SyncStatus.class, pageRequest);
    return response.getResponse();
  }
}
