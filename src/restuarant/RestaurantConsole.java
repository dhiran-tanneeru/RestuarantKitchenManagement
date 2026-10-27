package restuarant;
import java.io.IOException;
/** Compatibility entry: the former unrestricted console now always enters secure login. */
public class RestaurantConsole {
    public RestaurantConsole() throws IOException,ClassNotFoundException {}
    public void start() { Main.main(new String[0]); }
    public static void main(String[] args) { Main.main(args); }
}
