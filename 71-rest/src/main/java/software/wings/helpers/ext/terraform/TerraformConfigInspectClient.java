package software.wings.helpers.ext.terraform;

import java.util.Collection;

/*
 ** Created by Yogesh on 05/07/2019
 */
public interface TerraformConfigInspectClient {
  enum BLOCK_TYPE { VARIABLES, MANAGED_RESOURCES, DIAGNOSTICS, DETAIL, SEVERITY, POS, FILENAME, LINE, SUMMARY }
  enum ERROR_TYPE { ERROR, WARNING }
  Collection<String> parseFieldsUnderBlock(String Directory, String category);
}
