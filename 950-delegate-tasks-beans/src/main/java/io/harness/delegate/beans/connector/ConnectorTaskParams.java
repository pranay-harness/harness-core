/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */


package io.harness.delegate.beans.connector;

import java.util.Set;
import lombok.Data;
import lombok.experimental.SuperBuilder;
@Data
@SuperBuilder
public class ConnectorTaskParams {
  protected Set<String> delegateSelectors;
}
