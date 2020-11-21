package io.harness.batch.processing.cloudevents.aws.ecs.service.support.intfc;

import software.wings.beans.NameValuePair;

import java.util.List;

public interface AwsHelperResourceService {
  List<NameValuePair> getAwsRegions();
}
