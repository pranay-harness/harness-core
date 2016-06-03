import org.bson.types.ObjectId;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Arrays;

/**
 *
 */

/**
 * @author Rishi
 *
 */
public class TestUUID {
  @Test
  public void test() {
    ObjectId ob = new ObjectId();
    System.out.println(ob.toString());
    System.out.println(ob.toHexString());

    System.out.println(Arrays.toString(ob.toByteArray()));
    BigInteger bi = new BigInteger(ob.toString(), 16);
    System.out.println(bi.toString(62));
  }
}
