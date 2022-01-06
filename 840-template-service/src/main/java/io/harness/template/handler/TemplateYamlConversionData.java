package io.harness.template.handler;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.CDC)
@Data
@Builder
public class TemplateYamlConversionData {
  List<TemplateYamlConversionRecord> templateYamlConversionRecordList;
}
