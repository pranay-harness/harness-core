package io.harness.batch.processing.cloudevents.aws.ecs.service.support.intfc;

import software.wings.beans.AwsCrossAccountAttributes;

import java.util.List;

public interface AwsECSHelperService {
  List<String> listECSClusters(String region, AwsCrossAccountAttributes awsCrossAccountAttributes);
}
