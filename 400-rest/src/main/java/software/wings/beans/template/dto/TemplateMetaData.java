package software.wings.beans.template.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@OwnedBy(PL)
@TargetModule(HarnessModule._360_CG_MANAGER)
public class TemplateMetaData {
  private String appId;
  private String uuid;
  private String name;
  private String folderName;
}
