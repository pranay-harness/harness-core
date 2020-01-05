package software.wings.graphql.scalar;

import graphql.language.IntValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@UtilityClass
@Slf4j
public class NumberScalar {
  public static final GraphQLScalarType type =
      GraphQLScalarType.newScalar()
          .name("Number")
          .description(
              "This represents either an int or a long or a double. Will be tried to map to one of these in the same order")
          .coercing(new Coercing<Number, Number>() {
            @Override
            public Number serialize(Object dataFetcherResult) throws CoercingSerializeException {
              if (dataFetcherResult instanceof Number) {
                return (Number) dataFetcherResult;
              }
              throw new CoercingSerializeException("Cannot convert the data " + dataFetcherResult + " to a number");
            }

            @Override
            public Number parseValue(Object input) throws CoercingParseValueException {
              Number val = getValue(input);
              if (val == null) {
                throw new CoercingParseValueException("Cannot parse input" + input);
              }
              return val;
            }

            private Number getValue(@NotNull Object input) {
              if (input instanceof Integer) {
                return (Integer) input;
              } else if (input instanceof Long) {
                return (Long) input;
              } else if (input instanceof Double) {
                return (Double) input;
              } else if (input instanceof IntValue) {
                return ((IntValue) input).getValue().longValue();
              } else {
                Number val = null;
                try {
                  val = Integer.parseInt(input.toString());
                  return val;
                } catch (NumberFormatException e) {
                  logger.debug("Cannot parse [{}] to integer", input, e);
                }
                try {
                  val = Long.parseLong(input.toString());
                  return val;
                } catch (NumberFormatException e) {
                  logger.debug("Cannot parse [{}] to long", input, e);
                }
                try {
                  val = Double.parseDouble(input.toString());
                  return val;
                } catch (NumberFormatException e) {
                  logger.debug("Cannot parse [{}] to integer", input, e);
                }
                return val;
              }
            }

            @Override
            public Number parseLiteral(Object input) throws CoercingParseLiteralException {
              Number val = getValue(input);
              if (val == null) {
                throw new CoercingParseLiteralException("Cannot parse input" + input);
              }
              return val;
            }
          })
          .build();
}
