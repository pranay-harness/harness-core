package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import software.wings.beans.Infra;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfraService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * The Class InfraServiceImpl.
 */
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
