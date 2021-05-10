package io.harness.audit.mapper;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.YamlDiffRecordDTO;
import io.harness.audit.entities.YamlDiffRecord;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class YamlDiffRecordMapper {
  public static YamlDiffRecord fromDTO(YamlDiffRecordDTO yamlDiffRecordDTO) {
    return YamlDiffRecord.builder()
        .oldYaml(yamlDiffRecordDTO.getOldYaml())
        .newYaml(yamlDiffRecordDTO.getNewYaml())
        .build();
  }

  public static YamlDiffRecordDTO toDTO(YamlDiffRecord yamlDiffRecord) {
    return YamlDiffRecordDTO.builder()
        .oldYaml(yamlDiffRecord.getOldYaml())
        .newYaml(yamlDiffRecord.getNewYaml())
        .build();
  }
}
