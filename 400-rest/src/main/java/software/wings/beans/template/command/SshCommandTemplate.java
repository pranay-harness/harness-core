/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.template.command;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.ReferencedTemplate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

@OwnedBy(CDC)
@JsonTypeName("SSH")
@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
public class SshCommandTemplate implements BaseTemplate {
  private CommandType commandType;
  @JsonInclude(NON_NULL) private transient List<AbstractCommandUnit.Yaml> commands;
  @Wither private List<CommandUnit> commandUnits;
  @Wither @JsonIgnore private List<ReferencedTemplate> referencedTemplateList;
}
