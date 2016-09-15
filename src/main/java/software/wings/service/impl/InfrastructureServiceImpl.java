package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.infrastructure.StaticInfrastructure.Builder.aStaticInfrastructure;

import software.wings.beans.Base;
import software.wings.beans.ErrorCodes;
import software.wings.beans.infrastructure.Infrastructure;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureService;
import software.wings.utils.Validator;

import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * The Class InfrastructureServiceImpl.
 */
@ValidateOnExecution
@Singleton
public class InfrastructureServiceImpl implements InfrastructureService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private HostService hostService;
  @Inject private ExecutorService executorService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.InfrastructureService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Infrastructure> list(PageRequest<Infrastructure> req) {
    return wingsPersistence.query(Infrastructure.class, req);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.InfrastructureService#save(software.wings.beans.infrastructure.Infrastructure)
   */
  @Override
  public Infrastructure save(Infrastructure infrastructure) {
    return wingsPersistence.saveAndGet(Infrastructure.class, infrastructure);
  }

  @Override
  public void delete(String infraId) {
    // TODO:: INFRA:
    boolean deleted = wingsPersistence.delete(wingsPersistence.createQuery(Infrastructure.class)
                                                  .field("appId")
                                                  .equal(Base.GLOBAL_APP_ID)
                                                  .field(ID_KEY)
                                                  .equal(infraId));
    if (deleted) {
      executorService.submit(() -> hostService.deleteByInfra(infraId));
    }
  }

  @Override
  public Infrastructure getInfraByEnvId(String envId) {
    // TODO:: INFRA:
    return wingsPersistence.createQuery(Infrastructure.class).field("appId").equal(GLOBAL_APP_ID).get();
  }

  @Override
  public void createDefaultInfrastructure() {
    List<Infrastructure> infrastructures =
        wingsPersistence.createQuery(Infrastructure.class).field("appId").equal(GLOBAL_APP_ID).asList();
    if (infrastructures.size() == 0) {
      wingsPersistence.save(aStaticInfrastructure().withAppId(GLOBAL_APP_ID).withName("Static Infrastructure").build());
    }
  }

  @Override
  public String getDefaultInfrastructureId() {
    List<Infrastructure> infrastructures =
        wingsPersistence.createQuery(Infrastructure.class).field("appId").equal(GLOBAL_APP_ID).asList();
    if (infrastructures.size() == 0) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST);
    }
    return infrastructures.get(0).getUuid();
  }

  @Override
  public Infrastructure get(String infraId) {
    Infrastructure infrastructure = wingsPersistence.createQuery(Infrastructure.class)
                                        .field("appId")
                                        .equal(Base.GLOBAL_APP_ID)
                                        .field(ID_KEY)
                                        .equal(infraId)
                                        .get();
    Validator.notNullCheck("Infrastructure", infrastructure);
    return infrastructure;
  }
}
