package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Infra.InfraBuilder.anInfra;
import static software.wings.beans.Infra.InfraType.STATIC;

import software.wings.beans.Infra;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfraService;
import software.wings.utils.Validator;

import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * The Class InfraServiceImpl.
 */
@ValidateOnExecution
@Singleton
public class InfraServiceImpl implements InfraService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private HostService hostService;
  @Inject private ExecutorService executorService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.InfraService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Infra> list(PageRequest<Infra> req) {
    return wingsPersistence.query(Infra.class, req);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.InfraService#save(software.wings.beans.Infra)
   */
  @Override
  public Infra save(Infra infra) {
    return wingsPersistence.saveAndGet(Infra.class, infra);
  }

  @Override
  public void deleteByEnv(String appId, String envId) {
    List<Infra> infras =
        wingsPersistence.createQuery(Infra.class).field("appId").equal(appId).field("envId").equal(envId).asList();
    infras.forEach(infra -> delete(appId, envId, infra.getUuid()));
  }

  @Override
  public Infra createDefaultInfraForEnvironment(String appId, String envId) {
    return save(anInfra().withAppId(appId).withEnvId(envId).withInfraType(STATIC).build());
  }

  @Override
  public Infra getInfraByEnvId(String appId, String envId) {
    Infra infra =
        wingsPersistence.createQuery(Infra.class).field("appId").equal(appId).field("envId").equal(envId).get();
    Validator.notNullCheck("Infrastructure", infra);
    return infra;
  }

  @Override
  public void delete(String appId, String envId, String infraId) {
    boolean deleted = wingsPersistence.delete(wingsPersistence.createQuery(Infra.class)
                                                  .field("appId")
                                                  .equal(appId)
                                                  .field("envId")
                                                  .equal(envId)
                                                  .field(ID_KEY)
                                                  .equal(infraId));
    if (deleted) {
      executorService.submit(() -> hostService.deleteByInfra(appId, infraId));
    }
  }
}
