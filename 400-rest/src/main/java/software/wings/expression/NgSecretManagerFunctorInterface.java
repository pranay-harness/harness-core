package software.wings.expression;

// This functor is only to assure compatability between all  SecretManagerFunctors
public interface NgSecretManagerFunctorInterface {
  String FUNCTOR_NAME = "ngSecretManager";
  Object obtain(String secretIdentifier, int token);
}
