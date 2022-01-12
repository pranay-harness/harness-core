/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.analysis;

import static java.util.stream.Collectors.toMap;

import software.wings.stencils.DataProvider;

import java.util.Map;
import java.util.stream.Stream;

public class ElkValidationTypeProvider implements DataProvider {
  @Override
  public Map<String, String> getData(String appId, Map<String, String> params) {
    return Stream.of(ElkValidationType.values()).collect(toMap(ElkValidationType::name, ElkValidationType::getName));
  }
}
