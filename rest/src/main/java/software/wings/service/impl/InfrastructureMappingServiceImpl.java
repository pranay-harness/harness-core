package software.wings.service.impl;

import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.InfrastructureMapping;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.InfrastructureMappingService;

import javax.inject.Inject;
import javax.validation.Valid;

/**
 * Created by anubhaw on 1/10/17.
 */
@Singleton
public class InfrastructureMappingServiceImpl implements InfrastructureMappingService {
  @Inject private WingsPersistence wingsPersistence;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public PageResponse<InfrastructureMapping> list(PageRequest<InfrastructureMapping> pageRequest) {
    return wingsPersistence.query(InfrastructureMapping.class, pageRequest);
  }

  @Override
  public InfrastructureMapping save(@Valid InfrastructureMapping infrastructureMapping) {
    return wingsPersistence.saveAndGet(InfrastructureMapping.class, infrastructureMapping);
  }

  @Override
  public InfrastructureMapping get(String appId, String infraMappingId) {
    return wingsPersistence.get(InfrastructureMapping.class, appId, infraMappingId);
  }

  @Override
  public InfrastructureMapping update(@Valid InfrastructureMapping infrastructureMapping) {
    return save(infrastructureMapping);
  }

  @Override
  public void delete(String appId, String infraMappingId) {
    wingsPersistence.delete(InfrastructureMapping.class, appId, infraMappingId);
  }
}
