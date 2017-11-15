/**
 *
 */

package software.wings.beans;

/**
 * Some of the bean objects are not entities in mongo db. They are used as embedded objects.
 * In other words, they don't extend Base or UuidAware.
 *
 * @author rktummala on 10/28/17
 */
public interface ObjectType {
  String PHASE = "PHASE";
  String PHASE_STEP = "PHASE_STEP";
  String STEP = "STEP";
  String NAME_VALUE_PAIR = "NAME_VALUE_PAIR";
  String TEMPLATE_EXPRESSION = "TEMPLATE_EXPRESSION";
  String NOTIFICATION_RULE = "NOTIFICATION_RULE";

  String VARIABLE = "VARIABLE";
  String FAILURE_STRATEGY = "FAILURE_STRATEGY";
  String NOTIFICATION_GROUP = "NOTIFICATION_GROUP";
  String PIPELINE_STAGE = "PIPELINE_STAGE";
  String COMMAND_UNIT = "COMMAND_UNIT";
}
