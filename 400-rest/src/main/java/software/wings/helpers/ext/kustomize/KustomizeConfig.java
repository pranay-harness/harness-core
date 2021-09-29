package software.wings.helpers.ext.kustomize;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.validator.Trimmed;

import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TargetModule(HarnessModule._970_API_SERVICES_BEANS)
public class KustomizeConfig {
  @Trimmed private String pluginRootDir;
  @Builder.Default @Trimmed private String kustomizeDirPath = EMPTY;

  @Nullable
  public static KustomizeConfig cloneFrom(@Nullable KustomizeConfig config) {
    if (config == null) {
      return null;
    }
    return KustomizeConfig.builder()
        .pluginRootDir(config.getPluginRootDir())
        .kustomizeDirPath(config.getKustomizeDirPath())
        .build();
  }
}
