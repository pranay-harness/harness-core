package io.harness.pms.pipeline.service.yamlschema.cache;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PIPELINE)
@Data
@Builder
public class YamlSchemaDetailsWrapperValue {
  List<YamlSchemaDetailsValue> yamlSchemaWithDetailsList;
}
