package io.harness.cvng.core.services.api.demo;

import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.core.entities.changeSource.ChangeSource;

import java.util.List;

public interface ChangeSourceDemoDataGenerator<T extends ChangeSource> {
  List<ChangeEventDTO> generate(T changeSource);
}
