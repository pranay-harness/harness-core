package io.harness.delegate.message;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Message {
  String message;
  List<String> params;
  MessengerType fromType;
  String fromProcess;
  long timestamp;
}
