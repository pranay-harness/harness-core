package software.wings.infra;

import java.util.Arrays;
import java.util.List;

public interface InfraDefinitionTestConstants {
  String REGION = "region";

  List<String> CLASSIC_LOAD_BALANCERS = Arrays.asList("LoadBalancer1", "LoadBalancer2");

  List<String> STAGE_CLASSIC_LOAD_BALANCERS = Arrays.asList("LoadBalancer1", "LoadBalancer2");

  List<String> TARGET_GROUP_ARNS = Arrays.asList("ARN1", "ARN2");

  List<String> STAGE_TARGET_GROUP_ARNS = Arrays.asList("ARN1", "ARN2");

  List<String> SUBNET_IDS = Arrays.asList("subnet-0581525f93ec3267f", "subnet-0581525f93ec3267g");

  List<String> SECURITY_GROUP_IDS = Arrays.asList("sg-01f9750e6902eaf14", "sg-01f9750e6902eaf15");

  String HOSTNAME_CONVENTION = "hostNameConvention";

  String RELEASE_NAME = "releaseName";

  String INFRA_DEFINITION_ID = "infraDefinitionId";

  String INFRA_PROVISIONER = "infraProvisioner";

  String INFRA_PROVISIONER_ID = "infraProvisionerId";
}
