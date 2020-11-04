package inputset;

public interface InputSet {
  enum Type { Manual, Webhook }

  InputSet.Type getType();
}
