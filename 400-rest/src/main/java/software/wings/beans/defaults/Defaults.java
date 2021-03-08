package software.wings.beans.defaults;

import software.wings.beans.Base;
import software.wings.beans.NameValuePair;
import software.wings.beans.NameValuePairYaml;
import software.wings.yaml.BaseEntityYaml;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author rktummala on 1/15/18
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class Defaults extends Base {
  private String accountId;
  private List<NameValuePair> nameValuePairList;

  @Builder
  public Defaults(String accountId, List<NameValuePair> nameValuePairList) {
    this.accountId = accountId;
    this.nameValuePairList = nameValuePairList;
  }
}
