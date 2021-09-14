/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
