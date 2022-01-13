/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.infra;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Splitter;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ProvisionerAware {
  Map<String, String> getExpressions();
  void setExpressions(Map<String, String> expressions);
  @JsonIgnore Set<String> getSupportedExpressions();
  void applyExpressions(Map<String, Object> resolvedExpressions, String appId, String envId, String infraDefinitionId);

  default List<String> getList(Object input) {
    if (input instanceof String) {
      return Splitter.on(",").splitToList((String) input);
    } else if (input instanceof List) {
      return ((List<?>) input).stream().map(obj -> (String) obj).collect(toList());
    } else {
      throw new InvalidRequestException(
          format("Comma-separated String or List<String> type expected. Found [%s]", input.getClass()),
          WingsException.USER);
    }
  }
}
