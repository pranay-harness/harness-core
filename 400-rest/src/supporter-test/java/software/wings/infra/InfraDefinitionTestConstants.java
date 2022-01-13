/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.infra;

import java.util.Arrays;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class InfraDefinitionTestConstants {
  public static final String REGION = "region";

  public static final List<String> CLASSIC_LOAD_BALANCERS = Arrays.asList("LoadBalancer1", "LoadBalancer2");

  public static final List<String> STAGE_CLASSIC_LOAD_BALANCERS = Arrays.asList("LoadBalancer1", "LoadBalancer2");

  public static final List<String> TARGET_GROUP_ARNS = Arrays.asList("ARN1", "ARN2");

  public static final List<String> STAGE_TARGET_GROUP_ARNS = Arrays.asList("ARN1", "ARN2");

  public static final List<String> SUBNET_IDS = Arrays.asList("subnet-0581525f93ec3267f", "subnet-0581525f93ec3267g");

  public static final List<String> SECURITY_GROUP_IDS = Arrays.asList("sg-01f9750e6902eaf14", "sg-01f9750e6902eaf15");

  public static final String HOSTNAME_CONVENTION = "hostNameConvention";

  public static final String RELEASE_NAME = "releaseName";

  public static final String INFRA_DEFINITION_ID = "infraDefinitionId";

  public static final String INFRA_DEFINITION_NAME = "infraDefinitionName";

  public static final String INFRA_PROVISIONER = "infraProvisioner";

  public static final String INFRA_PROVISIONER_ID = "infraProvisionerId";

  public static final String RESOURCE_CONSTRAINT_NAME = "Queuing";
}
