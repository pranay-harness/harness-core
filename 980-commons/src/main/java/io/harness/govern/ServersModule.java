package io.harness.govern;

import com.google.inject.Injector;
import java.io.Closeable;
import java.util.List;

public interface ServersModule {
  List<Closeable> servers(Injector injector);
}
