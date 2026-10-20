
package restuarant;

import java.io.*;

public class Main {

    public static void main(String[] args) {

        try {
            RestaurantConsole console = new RestaurantConsole();
            console.start();

        } 
        catch (IOException e) {
            System.err.println( "Could not access restaurant data: " + e.getMessage());

        } 
        catch (ClassNotFoundException e) {
            System.err.println( "Could not restore saved classes: " + e.getMessage());
        }
    }
}

