import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import java.nio.file.*;
import java.util.*;

/** Proves the naming pass did not change executable class contents, modulo member names
 * listed in the actual applied manifest and source/debug metadata. Explicit serialVersionUID
 * constants are checked separately by SerializationContracts. Requires Gradle's ASM jars.
 */
public final class VerifyRenaming {
    public static void main(String[] args) throws Exception {
        Path before=Paths.get(args[0]), after=Paths.get(args[1]);
        Map<String,String> reverse=new HashMap<>();
        for(String line:Files.readAllLines(Paths.get(args[2]))) {
            if(line.isEmpty() || line.startsWith("#")) continue;
            String[] parts=line.split("\t"); int separator=parts[0].lastIndexOf('#');
            String owner=parts[0].substring(0,separator).replace('.','/');
            reverse.put(owner+"#"+parts[1],parts[0].substring(separator+1));
        }
        Map<String,String> parents=new HashMap<>();
        List<Path> classFiles=new ArrayList<>();
        try(java.util.stream.Stream<Path> files=Files.walk(before)) {
            files.filter(p->p.toString().endsWith(".class")).sorted().forEach(classFiles::add);
        }
        for(Path file:classFiles) {
            ClassReader reader=new ClassReader(Files.readAllBytes(file));
            parents.put(reader.getClassName(),reader.getSuperName());
        }
        Remapper remapper=new Remapper() {
            String member(String owner,String name) {
                while(owner!=null) {
                    String old=reverse.get(owner+"#"+name);
                    if(old!=null) return old;
                    owner=parents.get(owner);
                }
                return name;
            }
            @Override public String mapFieldName(String owner,String name,String descriptor) { return member(owner,name); }
            @Override public String mapMethodName(String owner,String name,String descriptor) { return member(owner,name); }
        };
        List<String> differences=new ArrayList<>();
        for(Path file:classFiles) {
            Path relative=before.relativize(file), changed=after.resolve(relative);
            if(!Files.exists(changed) || !Arrays.equals(normalize(Files.readAllBytes(file),null),normalize(Files.readAllBytes(changed),remapper)))
                differences.add(relative.toString());
        }
        try(java.util.stream.Stream<Path> files=Files.walk(after)) {
            files.filter(p->p.toString().endsWith(".class")).filter(p->!Files.exists(before.resolve(after.relativize(p))))
                .forEach(p->differences.add("ADDED "+after.relativize(p)));
        }
        if(!differences.isEmpty()) throw new AssertionError("Executable differences after reversing names: "+differences);
        System.out.println("Naming pass: all "+classFiles.size()+" classes have identical executable contents after reversing member names and excluding debug metadata / separately checked serialVersionUID constants.");
    }
    static byte[] normalize(byte[] bytes, Remapper names) {
        ClassWriter writer=new ClassWriter(0);
        ClassVisitor withoutSerialUid=new ClassVisitor(Opcodes.ASM9,writer) {
            @Override public FieldVisitor visitField(int access,String name,String descriptor,String signature,Object value) {
                if(name.equals("serialVersionUID") && descriptor.equals("J")
                        && access==(Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC|Opcodes.ACC_FINAL) && value instanceof Long) return null;
                return super.visitField(access,name,descriptor,signature,value);
            }
        };
        ClassVisitor visitor=names==null?withoutSerialUid:new ClassRemapper(withoutSerialUid,names);
        new ClassReader(bytes).accept(visitor,ClassReader.SKIP_DEBUG);
        return writer.toByteArray();
    }
}
