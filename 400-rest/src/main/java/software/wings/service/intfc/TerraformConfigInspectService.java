package software.wings.service.intfc;

import java.util.List;

public interface TerraformConfigInspectService {
  List<String> parseFieldsUnderCategory(String directory, String category);
}
