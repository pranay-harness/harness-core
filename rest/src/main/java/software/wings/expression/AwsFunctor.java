package software.wings.expression;

import com.amazonaws.services.ec2.model.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;

public class AwsFunctor {
  @SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC")
  public class TagsFunctor {
    public String find(Collection<Tag> tags, String key) {
      return tags.stream()
          .filter(tag -> tag.getKey().equals(key))
          .map(Tag::getValue)
          .findFirst()
          .orElse(StringUtils.EMPTY);
    }
  }

  public final TagsFunctor tags = new TagsFunctor();
}
