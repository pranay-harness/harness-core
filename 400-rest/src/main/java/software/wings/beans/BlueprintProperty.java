/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import io.harness.data.validator.Trimmed;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
public class BlueprintProperty {
  @NotEmpty @Trimmed private String name;
  @NotNull private String value;
  private String valueType;
  private List<NameValuePair> fields;

  @Data
  @Builder
  public static final class Yaml {
    @NotEmpty @Trimmed private String name;
    @NotNull private String value;
    private String valueType;
    private List<NameValuePair.Yaml> fields;
  }
}
