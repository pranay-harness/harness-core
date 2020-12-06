package software.wings.service.impl;

import software.wings.helpers.ext.terraform.TerraformConfigInspectClient;
import software.wings.service.intfc.TerraformConfigInspectService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class TerraformConfigInspectServiceImpl implements TerraformConfigInspectService {
  @Inject private TerraformConfigInspectClient terraformConfigInspectClient;

  @Override
  public List<String> parseFieldsUnderCategory(String directory, String category) {
    return new ArrayList<>(terraformConfigInspectClient.parseFieldsUnderBlock(directory, category));
  }
}
