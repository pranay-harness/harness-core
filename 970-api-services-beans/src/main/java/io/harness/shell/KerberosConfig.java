/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
