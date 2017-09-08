package software.wings.yaml;

import com.google.common.collect.Iterables;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.introspector.Property;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.nodes.NodeTuple;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.nodes.Tag;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.representer.Representer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class YamlRepresenter extends Representer {
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
  public NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
    Class aClass = javaBean.getClass();
    // changed this because otherwise it was not getting fields of parent classes
    // Field[] fields = aClass.getDeclaredFields();
    // this isn't a solution either because it only gets PUBLIC fields up the hierarchy
    // Field[] fields = aClass.getFields();

    /*
    THE PROBLEM:
    getFields() - All the public fields up the entire class hierarchy.
    getDeclaredFields() - All the fields, regardless of their accessibility but only for the current class, not any base
    classes that the current class might be inheriting from.

    THE SOLUTION:
    To get all the fields up the hierarchy, we need something like this "getFieldsUpTo" method
    */

    // this seems to work
    Field[] fields = Iterables.toArray(getFieldsUpTo(aClass, GenericYaml.class), Field.class);

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

  // FOUND THIS METHOD HERE:
  // https://stackoverflow.com/questions/16966629/what-is-the-difference-between-getfields-and-getdeclaredfields-in-java-reflectio
  public static Iterable<Field> getFieldsUpTo(@Nonnull Class<?> startClass, @Nullable Class<?> exclusiveParent) {
    List<Field> currentClassFields = new ArrayList<>(Arrays.asList(startClass.getDeclaredFields()));
    Class<?> parentClass = startClass.getSuperclass();

    if (parentClass != null && (exclusiveParent == null || !(parentClass.equals(exclusiveParent)))) {
      List<Field> parentClassFields = (List<Field>) getFieldsUpTo(parentClass, exclusiveParent);
      currentClassFields.addAll(parentClassFields);
    }

    return currentClassFields;
  }
}