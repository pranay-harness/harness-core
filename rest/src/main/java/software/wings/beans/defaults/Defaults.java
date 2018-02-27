package software.wings.beans.defaults;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.Base;
import software.wings.beans.NameValuePair;
import software.wings.yaml.BaseEntityYaml;

import java.util.List;

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

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static class Yaml extends BaseEntityYaml {
    private List<NameValuePair.Yaml> defaults;

    @Builder
    public Yaml(String type, String harnessApiVersion, List<NameValuePair.Yaml> defaults) {
      super(type, harnessApiVersion);
      this.defaults = defaults;
    }
  }
}