package software.wings.utils;

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
import java.util.HashMap;
import java.util.Map;

class SerializationClasses {
  private static final Logger logger = LoggerFactory.getLogger(SerializationClasses.class);

  static Map<String, Integer> serializationClasses() {
    try {
      URL url = SerializationClasses.class.getClassLoader().getResource("serialization_classes");
      if (url != null) {
        Map<String, Integer> map = new HashMap<>();
        for (String s : Resources.readLines(url, Charsets.UTF_8)) {
          if (StringUtils.isNotBlank(s) && s.charAt(0) != '#') {
            String className = trim(substringBefore(s, ","));
            Integer id = Integer.valueOf(trim(substringAfter(s, ",")));
            if (map.containsKey(className)) {
              throw new IllegalStateException("Duplicate key: " + className);
            }
            map.put(className, id);
          }
        }
        return map;
      }
    } catch (IOException ex) {
      logger.error("Couldn't load serialization classes", ex);
      System.exit(1);
    }
    return null;
  }
}
