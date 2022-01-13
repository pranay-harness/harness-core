/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.defaults;

import software.wings.beans.Base;
import software.wings.beans.NameValuePair;
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

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends BaseEntityYaml {
    private List<NameValuePair.Yaml> defaults;

    @Builder
    public Yaml(String type, String harnessApiVersion, List<NameValuePair.Yaml> defaults) {
      super(type, harnessApiVersion);
      this.defaults = defaults;
    }
  }
}
