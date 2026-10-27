package restuarant;
public class Main {
    public static void main(String[] args) {
        try { new LoginDemo().start(); }
        catch(Exception ex) { System.err.println("Application stopped: "+ex.getMessage()+". Existing save files were not reset.");System.exit(1); }
    }
}
