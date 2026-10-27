
package restuarant.auth;

import java.io.*;
import java.nio.file.*;

public class AccountStore {

    private static final Path FILE =
        Paths.get("data", "staff-accounts.dat");

    public static boolean exists() {
        return Files.exists(FILE);
    }

    public static void save(StaffService staff)
            throws IOException {

        if (staff == null) {
            throw new IllegalArgumentException(
                "Staff service is required"
            );
        }

        Files.createDirectories(FILE.getParent());

        Path temp = FILE.resolveSibling(
            "staff-accounts.dat.tmp"
        );

        try (ObjectOutputStream out =
                new ObjectOutputStream(
                    Files.newOutputStream(temp))) {

            out.writeObject(staff);
        }

        try {
            Files.move(
                temp,
                FILE,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(
                temp,
                FILE,
                StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    public static StaffService load()
            throws IOException, ClassNotFoundException {

        if (!Files.isRegularFile(FILE)) {
            throw new FileNotFoundException(
                "Account file not found"
            );
        }

        try (ObjectInputStream in =
                new restuarant.persistence.SnapshotStore.SafeInput(
                    Files.newInputStream(FILE))) {

            Object obj = in.readObject();

            if (!(obj instanceof StaffService)) {
                throw new IOException(
                    "Invalid account file"
                );
            }

            return (StaffService) obj;
        }
    }
}
