import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Records Java serialization UIDs and wire fields using an isolated class loader. */
public final class SerializationContracts {
    public static void main(String[] args) throws Exception {
        Path classes=Paths.get(args[0]);
        URL[] classpath={classes.toUri().toURL(),Paths.get("lib/ant-1.10.11.jar").toUri().toURL(),Paths.get("lib/stubs-wtk.jar").toUri().toURL()};
        List<String> contracts=new ArrayList<>(), failures=new ArrayList<>();
        try(URLClassLoader loader=new URLClassLoader(classpath,ClassLoader.getPlatformClassLoader());
            java.util.stream.Stream<Path> paths=Files.walk(classes)) {
            for(Path file:(Iterable<Path>)paths.filter(p->p.toString().endsWith(".class")).sorted()::iterator) {
                String name=classes.relativize(file).toString().replace(File.separatorChar,'.').replaceAll("\\.class$","");
                try {
                    Class<?> type=Class.forName(name,false,loader);
                    if(!Serializable.class.isAssignableFrom(type)) continue;
                    ObjectStreamClass descriptor=ObjectStreamClass.lookup(type);
                    StringJoiner fields=new StringJoiner(",");
                    for(ObjectStreamField field:descriptor.getFields()) fields.add(field.getName()+":"+field.getTypeCode()+":"+field.getTypeString());
                    contracts.add(name+"\t"+descriptor.getSerialVersionUID()+"\t"+fields);
                } catch(Throwable error) { failures.add(name+"\t"+error); }
            }
        }
        Files.write(Paths.get(args[1]),contracts,StandardCharsets.UTF_8);
        if(!failures.isEmpty()) throw new AssertionError("Unable to inspect classes: "+failures);
        System.out.println("Recorded "+contracts.size()+" serialization contracts.");
    }
}
