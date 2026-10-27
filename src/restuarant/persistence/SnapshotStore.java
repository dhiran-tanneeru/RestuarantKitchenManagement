package restuarant.persistence;
import java.io.*;
import java.nio.file.*;
/** One atomic snapshot contains accounts and operations. Legacy files are never overwritten. */
public final class SnapshotStore {
    private SnapshotStore() {}
    public static void save(Path path, RestaurantState state) throws IOException {
        synchronized(state) {
            Files.createDirectories(path.toAbsolutePath().getParent());
            Path tmp=Files.createTempFile(path.toAbsolutePath().getParent(),"review1-",".tmp");
            try {
                try(FileOutputStream file=new FileOutputStream(tmp.toFile()); ObjectOutputStream out=new ObjectOutputStream(file)) {
                    out.writeObject(state); out.flush(); file.getFD().sync();
                }
                if(Files.exists(path)) Files.copy(path,path.resolveSibling(path.getFileName()+".bak"),StandardCopyOption.REPLACE_EXISTING);
                try { Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
                catch(AtomicMoveNotSupportedException ex) { Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(tmp); }
        }
    }
    public static RestaurantState load(Path path) throws IOException,ClassNotFoundException {
        try(ObjectInputStream in=new SafeInput(Files.newInputStream(path))) {
            Object value=in.readObject();
            if(!(value instanceof RestaurantState)) throw new IOException("Unexpected snapshot type");
            RestaurantState state=(RestaurantState)value;
            if(state.getStaff()==null) throw new IOException("Snapshot has no staff registry");
            state.integrate(state.getStaff()); return state;
        }
    }
    public static class SafeInput extends ObjectInputStream {
        public SafeInput(InputStream in) throws IOException { super(in); }
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException,ClassNotFoundException {
            String n=desc.getName(), component=n;
            while(component.startsWith("[")) component=component.substring(1);
            if(component.startsWith("L") && component.endsWith(";")) component=component.substring(1,component.length()-1);
            boolean primitive=component.length()==1 && "BCDFIJSZ".contains(component);
            if(!(primitive || component.startsWith("restuarant.") || component.startsWith("java.lang.") || component.startsWith("java.util.") || component.startsWith("java.math.") || component.startsWith("java.time."))) throw new InvalidClassException("Unexpected class",n);
            return super.resolveClass(desc);
        }
        protected Class<?> resolveProxyClass(String[] names) throws IOException { throw new InvalidClassException("Proxies are not supported"); }
    }
}
