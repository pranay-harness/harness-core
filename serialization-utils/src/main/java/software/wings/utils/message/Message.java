package software.wings.utils.message;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Message {
  String message;
  List<String> params;
  MessengerType fromType;
  String fromProcess;
  long timestamp;
}
