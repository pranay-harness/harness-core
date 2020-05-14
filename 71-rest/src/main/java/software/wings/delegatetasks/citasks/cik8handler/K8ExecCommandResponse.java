package software.wings.delegatetasks.citasks.cik8handler;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.io.ByteArrayOutputStream;

@Value
@Data
@Builder
public class K8ExecCommandResponse {
  private ByteArrayOutputStream outputStream;
  private ExecCommandStatus execCommandStatus;
}
