/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.number;
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
@JsonTypeName("Percentage")
@RecasterAlias("io.harness.cdng.k8s.PercentageInstanceSelection")
public class PercentageInstanceSelection implements InstanceSelectionBase {
  @YamlSchemaTypes({string, number}) ParameterField<String> percentage;
  @Override
  public K8sInstanceUnitType getType() {
    return K8sInstanceUnitType.Percentage;
  }

  @Override
  public Integer getInstances() {
    if (ParameterField.isNull(percentage)) {
      return null;
    }
    try {
      return new BigDecimal(percentage.getValue()).intValueExact();
    } catch (Exception exception) {
      throw new InvalidRequestException(
          String.format("Percentage value: [%s] is not an integer", percentage.getValue()), exception);
    }
  }
}
