package io.harness.delegate.task.citasks.cik8handler;

import java.io.ByteArrayOutputStream;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Value
@Data
@Builder
public class K8ExecCommandResponse {
  private ByteArrayOutputStream outputStream;
  private ExecCommandStatus execCommandStatus;
}
