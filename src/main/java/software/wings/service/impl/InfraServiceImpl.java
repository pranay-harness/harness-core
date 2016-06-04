package software.wings.service.impl;

import software.wings.beans.Infra;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.InfraService;

import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * The Class InfraServiceImpl.
 */
public class InfraServiceImpl implements InfraService {
  @Inject private WingsPersistence wingsPersistence;

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

  /* (non-Javadoc)
   * @see software.wings.service.intfc.InfraService#delete(java.lang.String)
   */
  @Override
  public void delete(String infraId) {
    wingsPersistence.delete(Infra.class, infraId);
  }
}
