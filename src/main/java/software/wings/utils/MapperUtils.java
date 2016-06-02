package software.wings.utils;

import org.modelmapper.ModelMapper;

/**
 * Created by peeyushaggarwal on 6/1/16.
 */
public class MapperUtils {
  private static final ModelMapper modelMapper = new ModelMapper();

  public static void mapObject(Object from, Object to) {
    modelMapper.map(from, to);
  }
}
