package software.wings.utils.message;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.lang.StringUtils;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Message service for inter-process communication via the filesystem
 */
public class MessageServiceImpl implements MessageService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

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

  private TimeLimiter timeLimiter = new SimpleTimeLimiter();
  private Map<File, Long> timestamps = new HashMap<>();

  public MessageServiceImpl(Clock clock, MessengerType messengerType, String processId) {
    this.clock = clock;
    this.messengerType = messengerType;
    this.processId = processId;
  }

  @Override
  public void writeMessage(String message, String... params) {
    sendMessage(messengerType, processId, message, params);
  }

  @Override
  public Message readMessage(long timeout) {
    return retrieveMessage(messengerType, processId, timeout);
  }

  @Override
  public void sendMessage(MessengerType targetType, String targetProcessId, String message, String... params) {
    boolean isOutput = messengerType == targetType && processId.equals(targetProcessId);
    logger.info("{}: {}({})", isOutput ? "Writing message" : "Sending message to " + targetType + " " + targetProcessId,
        message, params != null ? Joiner.on(", ").join(params) : "");
    try {
      File file = getMessageFile(targetType, targetProcessId);
      List<String> messageContent = new ArrayList<>();
      messageContent.add(isOutput ? OUT : IN);
      messageContent.add(Long.valueOf(clock.millis()).toString());
      messageContent.add(messengerType.name());
      messageContent.add(processId);
      messageContent.add(message);
      if (params != null) {
        messageContent.add(Joiner.on(SECONDARY_DELIMITER).join(params));
      }
      try {
        if (acquireLock(file)) {
          FileUtils.touch(file);
          FileUtils.writeLines(file, singletonList(Joiner.on(PRIMARY_DELIMITER).join(messageContent)), true);
          logger.info("Message {}", isOutput ? "written" : "sent");
        } else {
          logger.error("Failed to acquire lock {}", file.getPath());
        }
      } finally {
        if (!releaseLock(file)) {
          logger.error("Failed to release lock {}", file.getPath());
        }
      }
    } catch (Exception e) {
      logger.error("Error sending message: {}({})", message, params, e);
    }
  }

  @Override
  public Message retrieveMessage(MessengerType sourceType, String sourceProcessId, long timeout) {
    boolean isInput = messengerType == sourceType && processId.equals(sourceProcessId);
    try {
      File file = getMessageFile(sourceType, sourceProcessId);
      long lastReadTimestamp = Optional.ofNullable(timestamps.get(file)).orElse(0L);
      if (!file.exists()) {
        FileUtils.touch(file);
      }
      LineIterator reader = FileUtils.lineIterator(file);
      return timeLimiter.callWithTimeout(() -> {
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
              logger.info("{}: {}",
                  isInput ? "Read message" : "Retrieved message from " + sourceType + " " + sourceProcessId, message);
              timestamps.put(file, timestamp);
              reader.close();
              return message;
            }
          }
        }
        reader.close();
        return null;
      }, timeout, TimeUnit.MILLISECONDS, true);
    } catch (Exception e) {
      logger.error("Error retrieving message from {} {}", sourceType, sourceProcessId, e);
    }
    return null;
  }

  @Override
  public void closeChannel(MessengerType type, String id) {
    logger.info("Closing channel for {} {}", type, id);
    try {
      File file = getMessageFile(type, id);
      timestamps.remove(file);
      if (file.exists()) {
        FileUtils.forceDelete(file);
      }
      logger.info("Channel closed for {} {}", type, id);
    } catch (IOException e) {
      logger.error("Error closing channel for {} {}", type, id, e);
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
    logger.info("Writing data to {}: {}", name, dataToWrite);
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
          logger.info("Data written");
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
  public Object getData(String name, String key) {
    Map<String, Object> allData = getAllData(name);
    if (allData == null) {
      logger.error("Error reading data from {}. Couldn't get {}", name, key);
      return null;
    }
    Object value = allData.get(key);
    logger.info("Value read from {}: {} = {}", name, key, value);
    return value;
  }

  @Override
  public Map<String, Object> getAllData(String name) {
    logger.info("Reading data from {}", name);
    try {
      return getDataMap(getDataFile(name));
    } catch (IOException e) {
      logger.error("Error reading data from {}", name);
      return null;
    }
  }

  @Override
  public List<String> listDataNames(@Nullable String prefix) {
    File dataDirectory = new File(ROOT + DATA);
    try {
      FileUtils.forceMkdir(dataDirectory);
    } catch (IOException e) {
      logger.error("Error creating data directory: {}", dataDirectory.getAbsolutePath(), e);
      return null;
    }
    return FileUtils.listFiles(dataDirectory, new PrefixFileFilter(prefix == null ? "" : prefix), null)
        .stream()
        .map(File::getName)
        .collect(Collectors.toList());
  }

  @Override
  public void removeData(String name, String key) {
    logger.info("Removing data to {}: {}", name, key);
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
          logger.info("Data written");
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
    logger.info("Closing data: {}", name);
    try {
      File file = getDataFile(name);
      if (file.exists()) {
        FileUtils.forceDelete(file);
      }
      logger.info("Data closed: {}", name);
    } catch (IOException e) {
      logger.error("Error closing data: {}", name, e);
    }
  }

  private File getMessageFile(MessengerType type, String id) throws IOException {
    File file = new File(ROOT + IO + type.name().toLowerCase() + "/" + id);
    FileUtils.forceMkdirParent(file);
    return file;
  }

  private File getDataFile(String name) throws IOException {
    File file = new File(ROOT + DATA + name);
    FileUtils.forceMkdirParent(file);
    return file;
  }

  private Map getDataMap(File file) throws IOException {
    if (file.exists()) {
      return JsonUtils.asObject(FileUtils.readFileToString(file, UTF_8), HashMap.class);
    }
    return new HashMap<>();
  }

  private boolean acquireLock(File file) {
    File lockFile = new File(file.getPath() + ".lock");
    final long finishAt = clock.millis() + 5000L;
    boolean wasInterrupted = false;
    try {
      while (lockFile.exists()) {
        try {
          final long remaining = finishAt - Clock.systemUTC().millis();
          if (remaining < 0) {
            break;
          }
          Thread.sleep(Math.min(100, remaining));
        } catch (final InterruptedException ignore) {
          wasInterrupted = true;
        } catch (final Exception ex) {
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
    } catch (IOException e) {
      return false;
    }
  }
}
