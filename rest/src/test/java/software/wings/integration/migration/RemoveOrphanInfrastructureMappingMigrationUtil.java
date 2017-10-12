package software.wings.integration.migration;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.InfrastructureMapping;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;

import java.util.List;

/**
 * Migration script to delete orphaned infrastructure mappings.
 * @author brett on 10/11/17
 */
@Integration
@Ignore
public class RemoveOrphanInfrastructureMappingMigrationUtil extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private InfrastructureMappingService infrastructureMappingService;

  @Test
  public void removeOrphanedInfraMappings() {
    List<InfrastructureMapping> infraMappings = wingsPersistence.createQuery(InfrastructureMapping.class).asList();
    int deleted = 0;
    for (InfrastructureMapping infraMapping : infraMappings) {
      if (!appService.exist(infraMapping.getAppId())
          || !serviceResourceService.exist(infraMapping.getAppId(), infraMapping.getServiceId())
          || !serviceTemplateService.exist(infraMapping.getAppId(), infraMapping.getServiceTemplateId())) {
        wingsPersistence.delete(infraMapping);
        deleted++;
      }
    }
    System.out.println("Complete. Deleted " + deleted + " infrastructure mappings.");
  }
}
