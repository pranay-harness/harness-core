package software.wings.utils.message;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Message service for inter-process communication via the filesystem
 */
public interface MessageService {
  void writeMessage(String message, String... params);

  Message readMessage(long timeout);

  void sendMessage(MessengerType receiverType, String receiverProcessId, String message, String... params);

  Message retrieveMessage(MessengerType senderType, String senderProcessId, long timeout);

  List<String> listChannels(MessengerType type);

  void closeChannel(MessengerType type, String id);

  void putData(String name, String key, Object value);

  void putAllData(String name, Map<String, Object> dataToWrite);

  Object getData(String name, String key);

  Map<String, Object> getAllData(String name);

  List<String> listDataNames(@Nullable String prefix);

  void removeData(String name, String key);

  void closeData(String name);
}
