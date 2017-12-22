package software.wings.service.intfc.yaml.sync;

import software.wings.beans.Base;
import software.wings.beans.RestResponse;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.YamlProcessingException;
import software.wings.yaml.BaseYaml;
import software.wings.yaml.YamlPayload;

import java.util.List;

/**
 * @author rktummala on 10/17/17
 */
public interface YamlService<Y extends BaseYaml, B extends Base> {
  /**
   * Process all the yaml changes, convert to beans and save the changes in mongodb.
   * @param changeList yaml change list
   * @return
   * @throws HarnessException if yaml is not well formed or if any error in processing
   */
  List<ChangeContext> processChangeSet(List<Change> changeList) throws YamlProcessingException;

  /**
   *
   * @param yamlPayload
   * @param accountId
   * @return
   */
  RestResponse<B> update(YamlPayload yamlPayload, String accountId);

  RestResponse<Y> getYaml(String accountId, String yamlFilePath);
}
