package software.wings.beans;

import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class AccountYaml extends BaseEntityYaml {
  private List<NameValuePairYaml> defaults = new ArrayList<>();

  @lombok.Builder
  public AccountYaml(String type, String harnessApiVersion, List<NameValuePairYaml> defaults) {
    super(type, harnessApiVersion);
    this.defaults = defaults;
  }
}
