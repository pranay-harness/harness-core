package software.wings.utils;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 6/1/16.
 */
public class MapperUtils {
  private static final ModelMapper modelMapper = new ModelMapper();
  static {
    modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
  }

  /**
   * Map object.
   *
   * @param from the from
   * @param to   the to
   */
  public static void mapObject(Object from, Object to) {
    modelMapper.map(from, to);
  }
}
