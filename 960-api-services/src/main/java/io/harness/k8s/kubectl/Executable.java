/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.k8s.kubectl;

import java.io.OutputStream;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;

interface Executable {
  ProcessResult execute(String directory, OutputStream output, OutputStream error, boolean printCommand)
      throws Exception;
  StartedProcess executeInBackground(String directory, OutputStream output, OutputStream error) throws Exception;
  String command();
}
