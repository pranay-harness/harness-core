import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Created by rishi on 1/23/17.
 */
public class Test {
  public static void main(String[] args) throws Exception {
    char[] chars = new char[] {'\u0012'};
    String str = new String(chars);
    byte[] bytes = str.getBytes();
    System.out.println(Arrays.toString(bytes));
    System.out.println(Charset.defaultCharset());
  }
}
