package io.harness.generator.constants;

import com.google.common.collect.ImmutableList;

import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class InfraDefinitionGeneratorConstants {
  public static final String GCP_CLUSTER = "us-central1-a/harness-test";
  public static final String AZURE_SUBSCRIPTION_ID = "20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0";
  public static final String AZURE_RESOURCE_GROUP = "rathna-rg";
  public static final String AZURE_DEPLOY_HOST = "vm1-test-rathna.centralus.cloudapp.azure.com";
  public static final List<String> SSH_DEPLOY_HOST =
      ImmutableList.of("host0", "host1", "host2", "host3", "host4", "host5", "host6", "host7", "host8", "host9",
          "host10", "host11", "host12", "host13", "host14", "host15", "host16", "host17", "host18", "host19");
}
