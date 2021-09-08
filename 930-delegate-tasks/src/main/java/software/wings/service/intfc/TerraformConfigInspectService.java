package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;

@OwnedBy(CDP)
public interface TerraformConfigInspectService {
  List<String> parseFieldsUnderCategory(String directory, String category, boolean useLatestVersion);
}
