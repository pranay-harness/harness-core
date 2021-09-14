/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.cloudevents.aws.ecs.service.support.intfc;

import software.wings.beans.NameValuePair;

import java.util.List;

public interface AwsHelperResourceService {
  List<NameValuePair> getAwsRegions();
}
