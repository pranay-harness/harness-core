package software.wings.graphql.scalar;

import static graphql.Assert.assertShouldNeverHappen;
import static io.harness.time.EpochUtils.PST_ZONE_ID;

import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.EnumValue;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.NullValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.language.VariableReference;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created this GraphQLScalar for DateTime scaclar.
 * At present adding support for String/Long/Int conversion to date.
 */
public final class GraphQLScalars {
  public static final GraphQLScalarType DATE_TIME =
      GraphQLScalarType.newScalar()
          .name("DateTime")
          .description("DateTime Scalar")
          .coercing(new Coercing() {
            @Override
            public Object serialize(Object obj) throws CoercingSerializeException {
              Object parsedObject = parseLiteral(obj);

              if (parsedObject instanceof ZonedDateTime) {
                return ((ZonedDateTime) parsedObject).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
              }

              return obj;
            }

            @Override
            public Object parseValue(Object obj) throws CoercingParseValueException {
              return serialize(obj);
            }

            @Override
            public Object parseLiteral(Object obj) throws CoercingParseLiteralException {
              if (obj instanceof StringValue) {
                return ZonedDateTime.parse(((StringValue) obj).getValue());
              } else if (obj instanceof IntValue) {
                return Instant.ofEpochMilli(((IntValue) obj).getValue().longValue()).atZone(ZoneId.of(PST_ZONE_ID));
              } else if (obj instanceof Long) {
                return Instant.ofEpochMilli(((Long) obj).longValue()).atZone(ZoneId.of(PST_ZONE_ID));
              } else {
                return obj;
              }
            }
          })
          .build();

  public static final GraphQLScalarType OBJECT =
      GraphQLScalarType.newScalar()
          .name("Object")
          .description("Object Scalar")
          .coercing(new Coercing<Object, Object>() {
            public Object serialize(Object input) throws CoercingSerializeException {
              return input;
            }

            public Object parseValue(Object input) throws CoercingParseValueException {
              return input;
            }

            public Object parseLiteral(Object input) throws CoercingParseLiteralException {
              return this.parseLiteral(input, Collections.emptyMap());
            }

            public Object parseLiteral(Object input, Map<String, Object> variables)
                throws CoercingParseLiteralException {
              if (!(input instanceof Value)) {
                throw new CoercingParseLiteralException("Expected AST type 'StringValue' ");
              } else if (input instanceof NullValue) {
                return null;
              } else if (input instanceof FloatValue) {
                return ((FloatValue) input).getValue();
              } else if (input instanceof StringValue) {
                return ((StringValue) input).getValue();
              } else if (input instanceof IntValue) {
                return ((IntValue) input).getValue();
              } else if (input instanceof BooleanValue) {
                return ((BooleanValue) input).isValue();
              } else if (input instanceof EnumValue) {
                return ((EnumValue) input).getName();
              } else if (input instanceof VariableReference) {
                String varName = ((VariableReference) input).getName();
                return variables.get(varName);
              } else {
                if (input instanceof ArrayValue) {
                  List values = ((ArrayValue) input).getValues();
                  return values.stream().map(v -> this.parseLiteral(v, variables)).collect(Collectors.toList());
                } else if (input instanceof ObjectValue) {
                  List<ObjectField> values = ((ObjectValue) input).getObjectFields();
                  Map<String, Object> parsedValues = new LinkedHashMap();
                  values.forEach(fld -> {
                    Object parsedValue = this.parseLiteral(fld.getValue(), variables);
                    parsedValues.put(fld.getName(), parsedValue);
                  });
                  return parsedValues;
                } else {
                  return assertShouldNeverHappen("We have covered all Value types", new Object[0]);
                }
              }
            }
          })
          .build();
}
