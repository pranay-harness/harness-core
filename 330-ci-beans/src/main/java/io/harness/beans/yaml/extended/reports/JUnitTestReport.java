package io.harness.beans.yaml.extended.reports;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.beans.SwaggerConstants.STRING_LIST_CLASSPATH;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.serializer.RunTimeInputHandler;
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
@OwnedBy(CI)
public class JUnitTestReport implements UnitTestReportSpec {
  @YamlSchemaTypes(value = {string})
  @ApiModelProperty(dataType = STRING_LIST_CLASSPATH)
  ParameterField<List<String>> paths;

  public List<String> resolve(String identifier, String stepType) {
    return RunTimeInputHandler.resolveListParameter("paths", stepType, identifier, paths, false);
  }
}