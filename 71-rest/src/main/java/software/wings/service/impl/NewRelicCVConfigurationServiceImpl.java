package software.wings.service.impl;

import com.google.inject.Inject;

import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.verification.NewRelicCVConfigurationService;
import software.wings.verification.CVConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;

/**
 * @author Vaibhav Tulsyan
 * 11/Oct/2018
 */
public class NewRelicCVConfigurationServiceImpl implements NewRelicCVConfigurationService {
  private static final Logger logger = LoggerFactory.getLogger(NewRelicCVConfigurationServiceImpl.class);

  @Inject WingsPersistence wingsPersistence;

  public UpdateOperations<CVConfiguration> getUpdateOperations(NewRelicCVServiceConfiguration obj) {
    logger.info("Updating NewRelic CV Service Configuration with UUID - " + obj.getUuid());
    return wingsPersistence.createUpdateOperations(CVConfiguration.class)
        .set("connectorId", obj.getConnectorId())
        .set("envId", obj.getEnvId())
        .set("serviceId", obj.getServiceId())
        .set("applicationId", obj.getApplicationId())
        .set("metrics", obj.getMetrics())
        .set("enabled24x7", obj.isEnabled24x7());
  }
}
