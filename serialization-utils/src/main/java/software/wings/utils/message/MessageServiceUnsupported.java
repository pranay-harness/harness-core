package software.wings.utils.message;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;

public class MessageServiceUnsupported implements MessageService {
  private static final String MESSAGE = "Filesystem messaging not supported.";

  @Override
  public void writeMessage(String message, String... params) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public void writeMessageToChannel(
      MessengerType targetType, String targetProcessId, String message, String... params) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public Message readMessage(long timeout) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public Message readMessageFromChannel(MessengerType sourceType, String sourceProcessId, long timeout) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public Runnable getMessageCheckingRunnable(long readTimeout, Consumer<Message> messageHandler) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public Runnable getMessageCheckingRunnableForChannel(
      MessengerType sourceType, String sourceProcessId, long readTimeout, Consumer<Message> messageHandler) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public Message waitForMessage(String messageName, long timeout) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public List<Message> waitForMessages(String messageName, long timeout, long minWaitTime) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public Message waitForMessageOnChannel(
      MessengerType sourceType, String sourceProcessId, String messageName, long timeout) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public List<Message> waitForMessagesOnChannel(
      MessengerType sourceType, String sourceProcessId, String messageName, long timeout, long minWaitTime) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public List<String> listChannels(MessengerType type) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public void closeChannel(MessengerType type, String id) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public void putData(String name, String key, Object value) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public void putAllData(String name, Map<String, Object> dataToWrite) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public <T> T getData(String name, String key, Class<T> valueClass) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public Map<String, Object> getAllData(String name) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public List<String> listDataNames(@Nullable String prefix) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public void removeData(String name, String key) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public void closeData(String name) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public void logAllMessages(MessengerType sourceType, String sourceProcessId) {
    throw new UnsupportedOperationException(MESSAGE);
  }
}
