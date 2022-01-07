/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import software.wings.beans.command.CommandUnitType;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CommandCategory {
  private Type type;
  private String displayName;
  private List<CommandUnit> commandUnits;

  @Value
  @Builder
  public static class CommandUnit {
    private String name;
    private String uuid;
    private CommandUnitType type;
  }

  public enum Type {
    COMMANDS("Command"),
    COPY("Copy"),
    SCRIPTS("Scripts"),
    VERIFICATIONS("Verifications");

    Type(String displayName) {
      this.displayName = displayName;
    }

    private String displayName;

    public String getDisplayName() {
      return displayName;
    }
  }
}
