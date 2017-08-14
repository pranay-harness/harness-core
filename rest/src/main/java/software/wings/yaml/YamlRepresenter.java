package software.wings.yaml;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.introspector.FieldProperty;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.introspector.Property;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.introspector.PropertyUtils;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.nodes.NodeTuple;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.nodes.Node;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.nodes.Tag;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.representer.Represent;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.representer.Representer;
import software.wings.beans.Application;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class YamlRepresenter extends Representer {
  public YamlRepresenter() {
    // this.representers.put(Dice.class, new RepresentDice());
    // this.representers.put(Application.class, new RepresentApplication());
  }

  /*
  private class RepresentDice implements Represent {
    public Node representData(Object data) {
      Dice dice = (Dice) data;
      String value = dice.getA() + "d" + dice.getB();
      return representScalar(new Tag("!dice"), value);
    }
  }
*/

  /*
  private class RepresentApplication implements Represent {
    public Node representData(Object data) {
      Application dice = (Application) data;
      String value = dice.getA() + "d" + dice.getB();
      return representScalar(new Tag("!dice"), value);
    }
  }
*/

  @Override
  protected NodeTuple representJavaBeanProperty(
      Object javaBean, Property property, Object propertyValue, Tag customTag) {
    Class aClass = javaBean.getClass();
    Field[] fields = aClass.getDeclaredFields();
    Set<String> yamlSerializableFields = new HashSet<>();

    for (Field field : fields) {
      YamlSerialize annos = field.getAnnotation(YamlSerialize.class);
      if (annos != null) {
        yamlSerializableFields.add(field.getName());
      }
    }

    /*
    // this is how we would filter out empty values: null, empty strings, arrys/lists of size 0
    if (propertyValue == null || propertyValue.equals("") || (propertyValue instanceof Collection<?> && ((Collection)
    propertyValue).size() == 0)) { return null; } else {
      // if javaBean has the @YamlSerialize annotation do super, otherwise return null
      if (yamlSerializableFields.contains(property.getName())) {
        return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
      }

      return null;
    }
    */

    if (yamlSerializableFields.contains(property.getName())) {
      return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
    }

    return null;
  }
}