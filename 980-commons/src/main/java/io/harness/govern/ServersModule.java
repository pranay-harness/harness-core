/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.govern;

import com.google.inject.Injector;
import java.io.Closeable;
import java.util.List;

public interface ServersModule {
  List<Closeable> servers(Injector injector);
}
