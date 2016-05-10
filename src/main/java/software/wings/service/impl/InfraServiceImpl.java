package software.wings.service.impl;

import software.wings.beans.Infra;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.InfraService;

import javax.inject.Inject;

public class InfraServiceImpl implements InfraService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<Infra> list(PageRequest<Infra> req) {
    return wingsPersistence.query(Infra.class, req);
  }

  @Override
  public Infra save(Infra infra) {
    return wingsPersistence.saveAndGet(Infra.class, infra);
  }

  @Override
  public void delete(String infraId) {
    wingsPersistence.delete(Infra.class, infraId);
  }
}
