package io.harness.batch.processing.cloudevents.aws.ecs.service.support.intfc;

import com.amazonaws.services.ec2.model.Instance;
import software.wings.beans.AwsCrossAccountAttributes;

import java.util.List;
import java.util.Set;

public interface AwsEC2HelperService {
  List<Instance> listEc2Instances(
      AwsCrossAccountAttributes awsCrossAccountAttributes, Set<String> instanceIds, String region);
}
