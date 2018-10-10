package software.wings.service.intfc.verification;

import org.mongodb.morphia.query.UpdateOperations;
import software.wings.verification.CVConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;

/**
 * @author Vaibhav Tulsyan
 * 11/Oct/2018
 */
public interface NewRelicCVConfigurationService {
  UpdateOperations<CVConfiguration> getUpdateOperations(NewRelicCVServiceConfiguration obj);
}
