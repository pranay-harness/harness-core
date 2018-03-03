package software.wings.integration.migration.legacy;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;

import java.util.List;

/**
 * Migration script to set usePublicDns on AWS infrastructure mappings to true since that has been the default.
 * @author brett on 10/1/17
 */
@Integration
@Ignore
public class AwsInfrastructureMappingPublicDnsMigrationUtil extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(AwsInfrastructureMappingPublicDnsMigrationUtil.class);

  @Inject private WingsPersistence wingsPersistence;

  @Test
  public void setAwsInfraMappingsUsePublicDns() {
    List<InfrastructureMapping> awsInfraMappings = wingsPersistence.createQuery(InfrastructureMapping.class)
                                                       .field("infraMappingType")
                                                       .equal(InfrastructureMappingType.AWS_SSH.name())
                                                       .asList();
    int updated = 0;
    for (InfrastructureMapping infraMapping : awsInfraMappings) {
      AwsInfrastructureMapping awsInfrastructureMapping = (AwsInfrastructureMapping) infraMapping;
      if (!awsInfrastructureMapping.isUsePublicDns()) {
        wingsPersistence.updateField(AwsInfrastructureMapping.class, infraMapping.getUuid(), "usePublicDns", true);
        updated++;
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          logger.error("", e);
        }
      }
    }
    System.out.println("Complete. Updated " + updated + " infrastructure mappings.");
  }
}
