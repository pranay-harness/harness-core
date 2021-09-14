/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm.states;

import com.github.reinert.jjschema.Attributes;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by sgurubelli on 8/31/17.
 */
@Data
@NoArgsConstructor
public class ParameterEntry {
  @Attributes(title = "Name") String key;
  @Attributes(title = "Value") String value;
}
