package io.harness.beans.yaml.extended.reports;

import static io.harness.common.SwaggerConstants.STRING_LIST_CLASSPATH;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.list;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class JUnitTestReport implements UnitTestReportSpec {
  @YamlSchemaTypes(value = {list, string}, defaultType = list)
  @ApiModelProperty(dataType = STRING_LIST_CLASSPATH)
  ParameterField<List<String>> paths;
}