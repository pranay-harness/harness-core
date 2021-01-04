package software.wings.expression;

// This functor is only to assure compatability between all  SecretManagerFunctors
public interface SecretManagerFunctorInterface {
  String FUNCTOR_NAME = "secretManager";
  Object obtain(String secretName, int token);
}
