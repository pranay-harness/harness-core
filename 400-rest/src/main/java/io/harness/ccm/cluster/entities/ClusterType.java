package io.harness.ccm.cluster.entities;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(CE)
public class ClusterType {
  public static final String DIRECT_KUBERNETES = "DIRECT_KUBERNETES";
  public static final String AWS_ECS = "AWS_ECS";
  public static final String GCP_KUBERNETES = "GCP_KUBERNETES";
  public static final String AZURE_KUBERNETES = "AZURE_KUBERNETES";
}
