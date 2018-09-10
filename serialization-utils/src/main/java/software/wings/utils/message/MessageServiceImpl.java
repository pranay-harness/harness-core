package software.wings.utils.message;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.filefilter.FileFileFilter.FILE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.utils.JsonUtils;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.annotation.Nullable;

/**
 * Message service for inter-process communication via the filesystem
 *
 * Created by brett on 10/26/17
 */
public class MessageServiceImpl implements MessageService {
  private static final Logger logger = LoggerFactory.getLogger(MessageServiceImpl.class);

  private static final String ROOT = "msg/";
  private static final String IO = "io/";
  private static final String DATA = "data/";
  @VisibleForTesting static final String IN = "IN";
  @VisibleForTesting static final String OUT = "OUT";
  @VisibleForTesting static final String PRIMARY_DELIMITER = "|-|";
  @VisibleForTesting static final String SECONDARY_DELIMITER = "::";

  private final Clock clock;
  private final MessengerType messengerType;
  private final String processId;

  private final TimeLimiter timeLimiter = new SimpleTimeLimiter();
  private final Map<File, Long> messageTimestamps = new HashMap<>();
  private final Map<File, BlockingQueue<Message>> messageQueues = new HashMap<>();

  public MessageServiceImpl(Clock clock, MessengerType messengerType, String processId) {
    this.clock = clock;
    this.messengerType = messengerType;
    this.processId = processId;
  }

  @Override
  public void writeMessage(String message, String... params) {
    writeMessageToChannel(messengerType, processId, message, params);
  }

  @Override
  public void writeMessageToChannel(
      MessengerType targetType, String targetProcessId, String message, String... params) {
    boolean isOutput = messengerType == targetType && processId.equals(targetProcessId);
    String output = isOutput ? "Writing message" : "Sending message to " + targetType + " " + targetProcessId;
    String paramStr = params != null ? Joiner.on(", ").join(params) : "";
    logger.info("{}: {}({})", output, message, paramStr);
    try {
      File channel = getMessageChannel(targetType, targetProcessId);
      List<String> messageContent = new ArrayList<>();
      messageContent.add(isOutput ? OUT : IN);
      messageContent.add("" + clock.millis());
      messageContent.add(messengerType.name());
      messageContent.add(processId);
      messageContent.add(message);
      if (params != null) {
        messageContent.add(Joiner.on(SECONDARY_DELIMITER).join(params));
      }
      try {
        if (acquireLock(channel)) {
          FileUtils.touch(channel);
          FileUtils.writeLines(channel, singletonList(Joiner.on(PRIMARY_DELIMITER).join(messageContent)), true);
        } else {
          logger.error("Failed to acquire lock {}", channel.getPath());
        }
      } finally {
        if (!releaseLock(channel)) {
          logger.error("Failed to release lock {}", channel.getPath());
        }
      }
    } catch (Exception e) {
      logger.error("Error writing message to channel: {}({})", message, params, e);
    }
  }

  @Override
  public Message readMessage(long timeout) {
    return readMessageFromChannel(messengerType, processId, timeout);
  }

  @Override
  public Message readMessageFromChannel(MessengerType sourceType, String sourceProcessId, long timeout) {
    boolean isInput = messengerType == sourceType && processId.equals(sourceProcessId);
    try {
      File channel = getMessageChannel(sourceType, sourceProcessId);
      long lastReadTimestamp = Optional.ofNullable(messageTimestamps.get(channel)).orElse(0L);
      if (!channel.exists()) {
        FileUtils.touch(channel);
      }
      return timeLimiter.callWithTimeout(() -> {
        while (true) {
          LineIterator reader = FileUtils.lineIterator(channel);
          while (reader.hasNext()) {
            String line = reader.nextLine();
            if (StringUtils.startsWith(line, (isInput ? IN : OUT) + PRIMARY_DELIMITER)) {
              List<String> components = Splitter.on(PRIMARY_DELIMITER).splitToList(line);
              long timestamp = Long.parseLong(components.get(1));
              if (timestamp > lastReadTimestamp) {
                MessengerType fromType = MessengerType.valueOf(components.get(2));
                String fromProcess = components.get(3);
                String messageName = components.get(4);
                List<String> msgParams = new ArrayList<>();
                if (components.size() == 6) {
                  String params = components.get(5);
                  if (!params.contains(SECONDARY_DELIMITER)) {
                    msgParams.add(params);
                  } else {
                    msgParams.addAll(Splitter.on(SECONDARY_DELIMITER).splitToList(params));
                  }
                }
                Message message = Message.builder()
                                      .message(messageName)
                                      .params(msgParams)
                                      .timestamp(timestamp)
                                      .fromType(fromType)
                                      .fromProcess(fromProcess)
                                      .build();
                if (logger.isDebugEnabled()) {
                  String input =
                      isInput ? "Read message" : "Retrieved message from " + sourceType + " " + sourceProcessId;
                  logger.debug("{}: {}", input, message);
                }
                messageTimestamps.put(channel, timestamp);
                reader.close();
                return message;
              }
            }
          }
          reader.close();
          Thread.sleep(200L);
        }
      }, timeout, TimeUnit.MILLISECONDS, true);
    } catch (UncheckedTimeoutException e) {
      logger.debug("Timed out reading message from channel {} {}", sourceType, sourceProcessId);
    } catch (Exception e) {
      logger.error("Error reading message from channel {} {}", sourceType, sourceProcessId, e);
    }
    return null;
  }

  @Override
  public Runnable getMessageCheckingRunnable(long readTimeout, Consumer<Message> messageHandler) {
    return getMessageCheckingRunnableForChannel(messengerType, processId, readTimeout, messageHandler);
  }

  @Override
  public Runnable getMessageCheckingRunnableForChannel(
      MessengerType sourceType, String sourceProcessId, long readTimeout, Consumer<Message> messageHandler) {
    try {
      messageQueues.putIfAbsent(getMessageChannel(sourceType, sourceProcessId), new LinkedBlockingQueue<>());
    } catch (IOException e) {
      logger.error("Couldn't get message channel for {} {}", sourceType, sourceProcessId);
    }
    return () -> {
      try {
        Message message = readMessageFromChannel(sourceType, sourceProcessId, readTimeout);
        if (message != null) {
          if (messageHandler != null) {
            messageHandler.accept(message);
          }
          try {
            messageQueues.get(getMessageChannel(sourceType, sourceProcessId)).put(message);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      } catch (Exception e) {
        logger.error("Error while checking for message from channel {} {}", sourceType, sourceProcessId, e);
      }
    };
  }

  @Override
  public Message waitForMessage(String messageName, long timeout) {
    return waitForMessageOnChannel(messengerType, processId, messageName, timeout);
  }

  @Override
  public List<Message> waitForMessages(String messageName, long timeout, long minWaitTime) {
    return waitForMessagesOnChannel(messengerType, processId, messageName, timeout, minWaitTime);
  }

  @Override
  public Message waitForMessageOnChannel(
      MessengerType sourceType, String sourceProcessId, String messageName, long timeout) {
    try {
      BlockingQueue<Message> queue = messageQueues.get(getMessageChannel(sourceType, sourceProcessId));
      if (queue == null) {
        RuntimeException ex = new RuntimeException(
            "To wait for a message you must first schedule the runnable returned by getMessageCheckingRunnable[ForChannel] at regular intervals.");
        logger.error(ex.getMessage(), ex);
        throw ex;
      }
      return timeLimiter.callWithTimeout(() -> {
        Message message = null;
        while (message == null || !messageName.equals(message.getMessage())) {
          try {
            message = queue.take();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          Thread.sleep(200L);
        }
        return message;
      }, timeout, TimeUnit.MILLISECONDS, true);
    } catch (UncheckedTimeoutException e) {
      logger.debug("Timed out waiting for message {} from channel {} {}", messageName, sourceType, sourceProcessId);
    } catch (Exception e) {
      logger.error(
          "Error while waiting for message {} from channel {} {}", messageName, sourceType, sourceProcessId, e);
    }
    return null;
  }

  @Override
  public List<Message> waitForMessagesOnChannel(
      MessengerType sourceType, String sourceProcessId, String messageName, long timeout, long minWaitTime) {
    try {
      BlockingQueue<Message> queue = messageQueues.get(getMessageChannel(sourceType, sourceProcessId));
      if (queue == null) {
        RuntimeException ex = new RuntimeException(
            "To wait for a message you must first schedule the runnable returned by getMessageCheckingRunnable[ForChannel] at regular intervals.");
        logger.error(ex.getMessage(), ex);
        throw ex;
      }
      return timeLimiter.callWithTimeout(() -> {
        List<Message> messages = new ArrayList<>();
        while (messages.isEmpty()) {
          try {
            timeLimiter.callWithTimeout(() -> {
              while (true) {
                try {
                  Message message = queue.take();
                  if (messageName.equals(message.getMessage())) {
                    messages.add(message);
                  }
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
                Thread.sleep(200L);
              }
            }, minWaitTime, TimeUnit.MILLISECONDS, true);
          } catch (UncheckedTimeoutException e) {
            // Do nothing
          }
          Thread.sleep(200L);
        }
        return messages;
      }, timeout, TimeUnit.MILLISECONDS, true);
    } catch (UncheckedTimeoutException e) {
      logger.debug("Timed out waiting for message {} from channel {} {}", messageName, sourceType, sourceProcessId);
    } catch (Exception e) {
      logger.error(
          "Error while waiting for message {} from channel {} {}", messageName, sourceType, sourceProcessId, e);
    }
    return null;
  }

  @Override
  public List<String> listChannels(MessengerType type) {
    File channelDirectory = new File(ROOT + IO + type.name().toLowerCase() + "/");
    try {
      FileUtils.forceMkdir(channelDirectory);
    } catch (Exception e) {
      logger.error(format("Error creating channel directory: %s", channelDirectory.getAbsolutePath()), e);
      return null;
    }
    return FileUtils.listFiles(channelDirectory, FILE, null).stream().map(File::getName).collect(toList());
  }

  @Override
  public void closeChannel(MessengerType type, String id) {
    logger.debug("Closing channel {} {}", type, id);
    try {
      File channel = getMessageChannel(type, id);
      messageTimestamps.remove(channel);
      messageQueues.remove(channel);
      if (channel.exists()) {
        FileUtils.forceDelete(channel);
      }
    } catch (Exception e) {
      logger.error("Error closing channel {} {}", type, id, e);
    }
  }

  @Override
  public void clearChannel(MessengerType type, String id) {
    logger.debug("Clearing channel {} {}", type, id);
    try {
      File channel = getMessageChannel(type, id);
      if (channel.exists()) {
        FileUtils.write(channel, "", UTF_8);
      }
    } catch (Exception e) {
      logger.error("Error clearing channel {} {}", type, id, e);
    }
  }

  @Override
  public void putData(String name, String key, Object value) {
    Map<String, Object> data = new HashMap<>();
    data.put(key, value);
    putAllData(name, data);
  }

  @Override
  public void putAllData(String name, Map<String, Object> dataToWrite) {
    logger.debug("Writing data to {}: {}", name, dataToWrite);
    try {
      File file = getDataFile(name);
      try {
        if (acquireLock(file)) {
          Map<String, Object> data = getDataMap(file);
          data.putAll(dataToWrite);
          if (file.exists()) {
            FileUtils.forceDelete(file);
          }
          FileUtils.touch(file);
          FileUtils.write(file, JsonUtils.asPrettyJson(data), UTF_8);
        } else {
          logger.error("Failed to acquire lock {}", file.getPath());
        }
      } finally {
        if (!releaseLock(file)) {
          logger.error("Failed to release lock {}", file.getPath());
        }
      }
    } catch (IOException e) {
      logger.error("Error writing data to {}. Couldn't store {}", name, dataToWrite);
    }
  }

  @Override
  @SuppressWarnings({"unchecked"})
  public <T> T getData(String name, String key, Class<T> valueClass) {
    Map<String, Object> allData = getAllData(name);
    if (allData == null) {
      logger.error("Error reading data from {}. Couldn't get {}", name, key);
      return null;
    }
    Object value = allData.get(key);
    if (value == null) {
      return null;
    }
    if (!valueClass.isAssignableFrom(value.getClass())) {
      logger.error("Value is not an instance of {}: {}", valueClass.getName(), value);
      return null;
    }
    logger.debug("Value read from {}: {} = {}", name, key, value);
    return (T) value;
  }

  @Override
  public Map<String, Object> getAllData(String name) {
    try {
      return getDataMap(getDataFile(name));
    } catch (Exception e) {
      logger.error("Error reading data from {}", name);
      return null;
    }
  }

  @Override
  public List<String> listDataNames(@Nullable String prefix) {
    File dataDirectory = new File(ROOT + DATA);
    try {
      FileUtils.forceMkdir(dataDirectory);
    } catch (Exception e) {
      logger.error(format("Error creating data directory: %s", dataDirectory.getAbsolutePath()), e);
      return null;
    }
    return FileUtils
        .listFiles(dataDirectory,
            new AndFileFilter(asList(FILE, new PrefixFileFilter(prefix == null ? "" : prefix),
                new NotFileFilter(new SuffixFileFilter(".lock")))),
            null)
        .stream()
        .map(File::getName)
        .collect(toList());
  }

  @Override
  public void removeData(String name, String key) {
    logger.debug("Removing data from {}: {}", name, key);
    try {
      File file = getDataFile(name);
      try {
        if (acquireLock(file)) {
          Map data = getDataMap(file);
          data.remove(key);
          if (file.exists()) {
            FileUtils.forceDelete(file);
          }
          FileUtils.touch(file);
          FileUtils.write(file, JsonUtils.asPrettyJson(data), UTF_8);
        } else {
          logger.error("Failed to acquire lock {}", file.getPath());
        }
      } finally {
        if (!releaseLock(file)) {
          logger.error("Failed to release lock {}", file.getPath());
        }
      }
    } catch (IOException e) {
      logger.error("Error removing data from {}. Couldn't remove {}", name, key);
    }
  }

  @Override
  public void closeData(String name) {
    logger.debug("Closing data: {}", name);
    try {
      File file = getDataFile(name);
      if (file.exists()) {
        FileUtils.forceDelete(file);
      }
    } catch (Exception e) {
      logger.error(format("Error closing data: %s", name), e);
    }
  }

  @Override
  public void logAllMessages(MessengerType sourceType, String sourceProcessId) {
    try {
      File channel = getMessageChannel(sourceType, sourceProcessId);
      if (channel.exists()) {
        LineIterator reader = FileUtils.lineIterator(channel);
        while (reader.hasNext()) {
          logger.error(reader.nextLine());
        }
      }
    } catch (IOException e) {
      logger.error("Couldn't read channel for {} {}.", sourceType, sourceProcessId, e);
    }
  }

  private File getMessageChannel(MessengerType type, String id) throws IOException {
    File channel = new File(ROOT + IO + type.name().toLowerCase() + "/" + id);
    FileUtils.forceMkdirParent(channel);
    return channel;
  }

  private File getDataFile(String name) throws IOException {
    File file = new File(ROOT + DATA + name);
    FileUtils.forceMkdirParent(file);
    return file;
  }

  @SuppressWarnings({"unchecked"})
  private Map<String, Object> getDataMap(File file) {
    if (file.exists()) {
      try {
        return JsonUtils.asObject(FileUtils.readFileToString(file, UTF_8), HashMap.class);
      } catch (Exception e) {
        logger.error(format("Couldn't read map from %s. Returning empty data map", file.getName()), e);
      }
    }
    return new HashMap<>();
  }

  private boolean acquireLock(File file) {
    File lockFile = new File(file.getPath() + ".lock");
    final long finishAt = clock.millis() + TimeUnit.SECONDS.toMillis(5);
    boolean wasInterrupted = false;
    try {
      while (lockFile.exists()) {
        final long remaining = finishAt - clock.millis();
        if (remaining < 0) {
          break;
        }
        try {
          Thread.sleep(Math.min(100, remaining));
        } catch (InterruptedException e) {
          wasInterrupted = true;
          return false;
        }
      }
      FileUtils.touch(lockFile);
      return true;
    } catch (Exception e) {
      return false;
    } finally {
      if (wasInterrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private boolean releaseLock(File file) {
    File lockFile = new File(file.getPath() + ".lock");
    try {
      if (lockFile.exists()) {
        FileUtils.forceDelete(lockFile);
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
