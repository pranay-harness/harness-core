package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import software.wings.beans.Base;
import software.wings.beans.infrastructure.Infrastructure;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureService;
import software.wings.utils.Validator;

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

  /* (non-Javadoc)
   * @see software.wings.service.intfc.InfrastructureService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Infrastructure> list(PageRequest<Infrastructure> req, boolean overview) {
    PageResponse<Infrastructure> infrastructures = wingsPersistence.query(Infrastructure.class, req);
    if (overview) {
      // infrastructures.getResponse().forEach(infrastructure ->
      // infrastructure.setHostUsage(getInfrastructureHostUsage(infrastructure)));
    }
    return infrastructures;
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
