package software.wings.utils;

import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.trim;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.stream.Collectors;

public class SerializationClasses {
  private static final Logger logger = LoggerFactory.getLogger(SerializationClasses.class);

  public static Map<String, Integer> serializationClasses() {
    try {
      URL url = SerializationClasses.class.getClassLoader().getResource("serialization_classes");
      if (url != null) {
        return Resources.readLines(url, Charsets.UTF_8)
            .stream()
            .filter(StringUtils::isNotBlank)
            .filter(s -> !StringUtils.startsWith(s, "#"))
            .collect(Collectors.toMap(
                s -> trim(substringBefore(s, ",")), s -> Integer.valueOf(trim(substringAfter(s, ",")))));
      }
    } catch (IOException ex) {
      logger.error("Couldn't load serialization classes", ex);
    }
    return emptyMap();
  }
}
