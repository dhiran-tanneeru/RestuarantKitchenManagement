
package restuarant.persistence;

import java.io.*;
import java.nio.file.*;

public class FileManager {

    private static final Path SAVE_FILE = Paths.get("data", "restaurant.dat");

    public static void save(RestaurantState state) throws IOException {

        if (state == null) {
            throw new IllegalArgumentException( "Cannot save null restaurant state");
        }

        Path folder = SAVE_FILE.getParent();

        if (folder != null) { 
            Files.createDirectories(folder);
        }

        Path temporaryFile = SAVE_FILE.resolveSibling("restaurant.dat.tmp");

        try (ObjectOutputStream output = new ObjectOutputStream( new BufferedOutputStream( Files.newOutputStream(temporaryFile)))) {
            output.writeObject(state);
        }

        try {
            Files.move( temporaryFile, SAVE_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        } catch (AtomicMoveNotSupportedException e) {

            Files.move( temporaryFile, SAVE_FILE, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static boolean saveExists() {
        return Files.isRegularFile(SAVE_FILE);
    }

    public static RestaurantState load() throws IOException, ClassNotFoundException {

        if (!saveExists()) {
            throw new FileNotFoundException( "No restaurant save file found");
        }

        try (ObjectInputStream input = new ObjectInputStream( new BufferedInputStream( Files.newInputStream(SAVE_FILE)))) {

            Object data = input.readObject();

            if (!(data instanceof RestaurantState)) {
                throw new IOException( "Invalid restaurant save format");
            }

            return (RestaurantState) data;
        }
    }
}

