/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.integer;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.math.BigDecimal;
import lombok.Data;

@OwnedBy(CDP)
@Data
@JsonTypeName("Count")
@RecasterAlias("io.harness.cdng.k8s.CountInstanceSelection")
public class CountInstanceSelection implements InstanceSelectionBase {
  @YamlSchemaTypes({string, integer}) ParameterField<String> count;
  @Override
  public K8sInstanceUnitType getType() {
    return K8sInstanceUnitType.Count;
  }

  @Override
  public Integer getInstances() {
    if (ParameterField.isNull(this.count)) {
      return null;
    }
    try {
      return new BigDecimal(count.getValue()).intValueExact();
    } catch (Exception exception) {
      throw new InvalidRequestException(
          String.format("Count value: [%s] is not an integer", count.getValue()), exception);
    }
  }
}
