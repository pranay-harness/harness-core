package software.wings.graphql.datafetcher.ce.recommendation.dto;

import software.wings.graphql.schema.type.QLEnum;

@SuppressWarnings("squid:S115")
public enum QLWorkloadType implements QLEnum {
  DaemonSet,
  StatefulSet,
  Deployment,
  ReplicaSet,
  Job,
  CronJob;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
