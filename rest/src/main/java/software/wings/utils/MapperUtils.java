package software.wings.utils;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

/**
 * Created by peeyushaggarwal on 6/1/16.
 */
public class MapperUtils {
  /**
   * Map object.
   *
   * @param from the from
   * @param to   the to
   */
  public static void mapObject(Object from, Object to) {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
    modelMapper.map(from, to);
  }
}
