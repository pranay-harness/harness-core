/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.generator.constants;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class InfraDefinitionGeneratorConstants {
  public static final String GCP_CLUSTER = "us-central1-a/harness-test";
  public static final String AWS_EC2_USER = "ec2-user";
  public static final String AZURE_SUBSCRIPTION_ID = "20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0";
  public static final String AZURE_RESOURCE_GROUP = "rathna-rg";
  public static final String AZURE_DEPLOY_HOST = "vm1-test-rathna.centralus.cloudapp.azure.com";
  public static final List<String> SSH_DEPLOY_HOST =
      ImmutableList.of("host0", "host1", "host2", "host3", "host4", "host5", "host6", "host7", "host8", "host9",
          "host10", "host11", "host12", "host13", "host14", "host15", "host16", "host17", "host18", "host19");
  public static final String AZURE_FUNCTIONAL_TEST_RESOURCE_GROUP = "harness-functional-test";
  public static final String AZURE_FUNCTIONAL_TEST_APP_SERVICE_RESOURCE_GROUP = "harness-functional-test-app-service";
  public static final String AZURE_VMSS_VM_USERNAME = "testUserHarness";
  public static final String AZURE_VMSS_BASE_SCALE_SET_NAME = "baseFunTestScaleSet";
  public static final String AZURE_VMSS_BASIC_INFRA_DEFINITION_NAME = "Azure_VMSS_Basic_InfraDef";
  public static final String AZURE_VMSS_BLUE_GREEN_INFRA_DEFINITION_NAME = "Azure_VMSS_Blue_Green_InfraDef";
  public static final String AZURE_WEB_APP_BLUE_GREEN_INFRA_DEFINITION_NAME = "Azure_Web_App_Blue_Green_InfraDef";
  public static final String AZURE_WEB_APP_BLUE_GREEN_ROLLBACK_INFRA_DEFINITION_NAME =
      "Azure_Web_App_Blue_Green_Rollback_InfraDef";
  public static final String AZURE_WEB_APP_CANARY_INFRA_DEFINITION_NAME = "Azure_Web_App_Canary_InfraDef";
  public static final String AZURE_WEB_APP_API_INFRA_DEFINITION_NAME = "Azure_Web_App_API_InfraDef";
  public static final String AZURE_VMSS_API_INFRA_DEFINITION_NAME = "Azure_VMSS_API_InfraDef";
  public static final String AZURE_VMSS_SUBSCRIPTION_QA_NAME = "Harness-QA";
  public static final String AZURE_VMSS_BASE_SCALE_SET_LOAD_BALANCER_NAME = "baseFunTestScaleSetLoadBalancer";
  public static final String AZURE_VMSS_BLUE_GREEN_BALANCER_NAME = "blueGreenStandardLoadBalancer";
  public static final String AZURE_VMSS_BLUE_GREEN_STAGE_BP_NAME = "stageBP";
  public static final String AZURE_VMSS_BLUE_GREEN_PROD_BP_NAME = "prodBP";
}
