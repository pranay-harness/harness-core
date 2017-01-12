package software.wings.utils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import com.fasterxml.jackson.core.type.TypeReference;
import com.hazelcast.nio.serialization.ByteArraySerializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * Created by peeyushaggarwal on 8/29/16.
 *
 * @param <T> the type parameter
 */
public class HazelcastJacksonSerializer<T> implements ByteArraySerializer<T> {
  @Override
  public byte[] write(T object) throws IOException {
    return JsonUtils
        .asJson(Maps.newHashMap(ImmutableMap.builder().put("data", object).build()), JsonUtils.mapperForCloning)
        .getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public T read(byte[] buffer) throws IOException {
    return JsonUtils
        .asObject(new String(buffer, StandardCharsets.UTF_8), new TypeReference<HashMap<String, T>>() {},
            JsonUtils.mapperForCloning)
        .get("data");
  }

  @Override
  public int getTypeId() {
    return 0;
  }

  @Override
  public void destroy() {}
}
