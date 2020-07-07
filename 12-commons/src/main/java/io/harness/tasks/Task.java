package io.harness.tasks;

public interface Task {
  String getUuid();

  // Will not be needed in the NG world but for now our delegate service use this
  String getWaitId();
}
