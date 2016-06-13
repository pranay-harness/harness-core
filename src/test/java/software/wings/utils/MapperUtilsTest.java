package software.wings.utils;

import com.google.common.collect.Maps;

import org.junit.Test;
import software.wings.sm.states.EmailState;

import java.util.Map;

/**
 * Created by peeyushaggarwal on 6/13/16.
 */
public class MapperUtilsTest {
  @Test
  public void mapObject() throws Exception {
    Map<String, Object> map = Maps.newLinkedHashMap();
    map.put("toAddress", "a@b.com");
    map.put("subject", "test");
    map.put("body", "test");

    EmailState emailState = new EmailState("id");
    System.out.println(emailState.isIgnoreDeliveryFailure());
    MapperUtils.mapObject(map, emailState);
  }
}
