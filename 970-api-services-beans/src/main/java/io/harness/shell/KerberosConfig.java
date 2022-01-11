/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.shell;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import java.beans.Transient;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by Aaditi Joag on 10/16/18.
 */
@JsonTypeName("KERBEROS_CONFIG")
@Data
@Builder
public class KerberosConfig {
  @Attributes(title = "Principal", required = true) @NotEmpty private String principal;
  @Attributes(title = "Generate TGT") private boolean generateTGT;
  @Attributes(title = "Realm", required = true) @NotEmpty private String realm;
  @Attributes(title = "KeyTab File Path") private String keyTabFilePath;

  @JsonIgnore
  @Transient
  public String getPrincipalWithRealm() {
    return realm != null && realm.length() != 0 ? principal + "@" + realm : principal;
  }
}
