/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

import static java.util.stream.Collectors.toMap;

import software.wings.stencils.DataProvider;

import com.google.inject.Singleton;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Created by sriram_parthasarathy on 10/5/17.
 */
@Singleton
public class ElkConnectorProvider implements DataProvider {
  @Override
  public Map<String, String> getData(String appId, Map<String, String> params) {
    return Stream.of(ElkConnector.values()).collect(toMap(ElkConnector::name, ElkConnector::getName));
  }
}
