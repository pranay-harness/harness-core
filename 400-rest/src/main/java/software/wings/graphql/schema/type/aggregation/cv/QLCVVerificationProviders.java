package software.wings.graphql.schema.type.aggregation.cv;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
public enum QLCVVerificationProviders implements QLEnum {
  APP_DYNAMICS,
  NEW_RELIC,
  DATA_DOG,
  SPLUNK,
  SUMO,
  ELK,
  APM_VERIFICATION,
  LOG_VERIFICATION,
  CLOUD_WATCH,
  PROMETHEUS,
  STACKDRIVER,
  STACKDRIVER_LOG,
  DYNA_TRACE,
  BUG_SNAG,
  DATA_DOG_LOG;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
