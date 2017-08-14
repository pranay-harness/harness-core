package software.wings.yaml;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.introspector.Property;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.introspector.PropertyUtils;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.nodes.NodeTuple;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.nodes.Node;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.nodes.Tag;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.representer.Represent;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.representer.Representer;
import software.wings.beans.Application;

import java.util.Collection;

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
    /*
    if (javaBean instanceof Point && "location".equals(property.getName())) {
      return null;
    } else {
      return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
    }
    */

    /*
    // if javaBean doesn't have the @YamlSerialize annotation return null, otherwise do super
    YamlSerialize annos = property.getAnnotation(YamlSerialize.class);
    if (annos != null) {
      try {
        method.invoke(runner);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    */

    // System.out.println("****** property: (" + property + ", " + propertyValue + ")");

    if (propertyValue == null || propertyValue.equals("")
        || (propertyValue instanceof Collection<?> && ((Collection) propertyValue).size() == 0)) {
      return null;
    } else {
      return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
    }
  }
}