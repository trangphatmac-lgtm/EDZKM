import com.sun.source.tree.*;
import com.sun.source.util.*;
import javax.lang.model.element.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import javax.tools.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.io.*;

/** Symbol-bound, conservative name recovery. Run with a JDK, not a JRE.
 * No text substitution across unrelated symbols; no changes inside strings/comments.
 * Inferences are intentionally restricted to assignments, getters, argument roles,
 * array subscripts and declared reference types. Ambiguous primitives remain in the audit.
 */
public final class RecoverNames {
    static final Map<Element, VariableTree> declarations = new LinkedHashMap<>();
    static final Map<Element, TreePath> paths = new HashMap<>();
    static final Map<Element, String> recovered = new LinkedHashMap<>();
    static final Map<Element, String> evidence = new HashMap<>();
    static final Map<Element, Map<String, Integer>> suggestions = new HashMap<>();
    static final Map<CompilationUnitTree, String> sources = new LinkedHashMap<>();
    static final Map<CompilationUnitTree, List<Edit>> edits = new LinkedHashMap<>();
    static final Map<String, String> explicit = new LinkedHashMap<>();
    static final Map<Element, String> wireRoles = new LinkedHashMap<>();
    static Trees trees;
    static Types types;
    static Elements elements;
    static SourcePositions positions;
    static TypeMirror serializable;
    static Path output;
    static final class Edit {
        final int start, end; final String text;
        Edit(int start, int end, String text) { this.start=start; this.end=end; this.text=text; }
    }
    static String decap(String text) {
        if (text.isEmpty()) return text;
        if (text.matches("[A-Z][A-Z0-9_]+")) {
            StringBuilder value=new StringBuilder(); boolean upper=false;
            for(char c:text.toLowerCase(Locale.ROOT).toCharArray()) {
                if(c=='_') upper=true;
                else { value.append(upper?Character.toUpperCase(c):c); upper=false; }
            }
            return value.toString();
        }
        int prefix=0;
        while(prefix<text.length() && Character.isUpperCase(text.charAt(prefix))) prefix++;
        if(prefix>1) return text.substring(0,prefix-1).toLowerCase(Locale.ROOT)+text.substring(prefix-1);
        return Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }
    static boolean weak(String name) {
        return name.matches("(?:[a-zA-Z]{1,2}\\d*|string\\d*|strings\\d*|object\\d*|objects\\d*|bl\\d*|var\\d+|arg\\d+)");
    }
    static boolean valid(String name) {
        return name != null && !weak(name) && SourceVersion.isIdentifier(name)
            && !SourceVersion.isKeyword(name) && !name.matches("(?:this|super|class|arg)\\d*");
    }
    static String key(Element element) {
        return element.getEnclosingElement().toString() + "#" + element.getSimpleName();
    }
    static String variableKey(Element element) {
        Element method=element.getEnclosingElement();
        return method.getEnclosingElement()+"#"+method.getSimpleName()+"#"+element.getSimpleName();
    }
    static boolean serialized(Element element) {
        return element.getKind() == ElementKind.FIELD && !element.getModifiers().contains(Modifier.STATIC)
            && !element.getModifiers().contains(Modifier.TRANSIENT)
            && types.isAssignable(element.getEnclosingElement().asType(), serializable);
    }
    static boolean eligible(Element element) {
        if (!declarations.containsKey(element)) return false;
        if (element.getKind() == ElementKind.ENUM_CONSTANT) return false;
        if (serialized(element)) return false;
        if (element.getKind() == ElementKind.FIELD) return explicit.containsKey(key(element));
        if(variableKey(element).equals("com.zelix.klassmaster.classfile.insn.VerifierType#equals#bl1")) return false;
        if(explicit.containsKey(variableKey(element))) return true;
        return weak(element.getSimpleName().toString());
    }
    static String name(Element element) {
        if (element == null) return null;
        String value = recovered.getOrDefault(element, wireRoles.getOrDefault(element, element.getSimpleName().toString()));
        return valid(value) ? decap(value) : null;
    }
    static TreePath path(TreePath parent, Tree child) { return child == null ? null : new TreePath(parent, child); }
    static Element symbol(TreePath path) { return path == null ? null : trees.getElement(path); }
    static String property(String method) {
        for (String prefix : Arrays.asList("get", "is", "has", "set")) {
            if (method.startsWith(prefix) && method.length() > prefix.length()
                    && Character.isUpperCase(method.charAt(prefix.length()))) return decap(method.substring(prefix.length()));
        }
        return null;
    }
    static String expression(TreePath path) {
        if (path == null) return null;
        Tree tree=path.getLeaf();
        if (tree instanceof ParenthesizedTree) return expression(path(path, ((ParenthesizedTree)tree).getExpression()));
        if (tree instanceof TypeCastTree) return expression(path(path, ((TypeCastTree)tree).getExpression()));
        if (tree instanceof IdentifierTree || tree instanceof MemberSelectTree) return name(symbol(path));
        if (tree instanceof MethodInvocationTree) {
            MethodInvocationTree call=(MethodInvocationTree)tree;
            Element method=symbol(path(path, call.getMethodSelect()));
            if (method == null) return null;
            String methodName=method.getSimpleName().toString();
            String result=property(methodName);
            if (result != null && !methodName.startsWith("set")) return result;
            String owner=method.getEnclosingElement().toString();
            if(owner.equals("java.lang.System") && methodName.equals("getProperty") && !call.getArguments().isEmpty()
                    && call.getArguments().get(0) instanceof LiteralTree) {
                Object literal=((LiteralTree)call.getArguments().get(0)).getValue();
                if(literal instanceof String) {
                    StringBuilder propertyName=new StringBuilder(); boolean upper=false;
                    for(char c:((String)literal).toCharArray()) {
                        if(!Character.isJavaIdentifierPart(c)) upper=true;
                        else { propertyName.append(upper?Character.toUpperCase(c):c); upper=false; }
                    }
                    return propertyName.toString();
                }
            }
            if (methodName.equals("size") || methodName.equals("length")) {
                if (call.getMethodSelect() instanceof MemberSelectTree) {
                    Tree receiver=((MemberSelectTree)call.getMethodSelect()).getExpression();
                    String base=expression(path(path(path,call.getMethodSelect()),receiver));
                    return base == null ? null : base + (methodName.equals("size") ? "Size" : "Length");
                }
            }
        }
        if (tree instanceof ArrayAccessTree) {
            String array=expression(path(path,((ArrayAccessTree)tree).getExpression()));
            return array == null ? null : singular(array);
        }
        return null;
    }
    static String singular(String word) {
        if (word.endsWith("ies")) return word.substring(0,word.length()-3)+"y";
        if (word.endsWith("s") && !word.endsWith("ss")) return word.substring(0,word.length()-1);
        return word+"Element";
    }
    static void suggest(Element element, String candidate, int strength) {
        if (element == null || !eligible(element) || !valid(candidate)) return;
        suggestions.computeIfAbsent(element, e -> new LinkedHashMap<>()).merge(candidate,strength,Math::max);
    }
    static String typeName(TypeMirror type) {
        if (type.getKind() == TypeKind.ARRAY) {
            String part=typeName(((ArrayType)type).getComponentType());
            if (part == null) {
                part=((ArrayType)type).getComponentType().toString();
                if (part.equals("int")) part="integer";
                if (part.equals("char")) part="character";
            }
            return part+"Array";
        }
        if (type.getKind() != TypeKind.DECLARED) return null;
        String part=((DeclaredType)type).asElement().getSimpleName().toString();
        if (Arrays.asList("String","Object","Integer","Long","Boolean","Character","Short","Byte","Double","Float").contains(part)) return null;
        return decap(part);
    }
    static final class Infer extends TreePathScanner<Void, Void> {
        @Override public Void visitVariable(VariableTree tree, Void unused) {
            Element element=symbol(getCurrentPath());
            suggest(element,expression(path(getCurrentPath(),tree.getInitializer())),90);
            if (element != null && element.getKind() == ElementKind.EXCEPTION_PARAMETER)
                suggest(element,typeName(element.asType()),80);
            return super.visitVariable(tree,unused);
        }
        @Override public Void visitAssignment(AssignmentTree tree, Void unused) {
            TreePath left=path(getCurrentPath(),tree.getVariable());
            TreePath right=path(getCurrentPath(),tree.getExpression());
            suggest(symbol(right),expression(left),95);
            suggest(symbol(left),expression(right),85);
            return super.visitAssignment(tree,unused);
        }
        @Override public Void visitReturn(ReturnTree tree, Void unused) {
            TreePath enclosing=getCurrentPath();
            while(enclosing != null && !(enclosing.getLeaf() instanceof MethodTree)) enclosing=enclosing.getParentPath();
            if(enclosing != null) {
                MethodTree method=(MethodTree)enclosing.getLeaf();
                Element returned=symbol(path(getCurrentPath(),tree.getExpression()));
                if(returned!=null && returned.getKind()!=ElementKind.PARAMETER)
                    suggest(returned,property(method.getName().toString()),80);
            }
            return super.visitReturn(tree,unused);
        }
        void arguments(List<? extends ExpressionTree> arguments, Element method) {
            if (!(method instanceof ExecutableElement)) return;
            List<? extends VariableElement> parameters=((ExecutableElement)method).getParameters();
            for(int i=0;i<Math.min(parameters.size(),arguments.size());i++) {
                TreePath argument=path(getCurrentPath(),arguments.get(i));
                String parameterName=name(parameters.get(i));
                if(parameterName==null) parameterName=libraryParameter((ExecutableElement)method,i);
                suggest(symbol(argument),parameterName,70);
                // Only propagate from call sites when the argument has a semantic name.
                suggest(parameters.get(i),expression(argument),60);
            }
        }
        String libraryParameter(ExecutableElement method, int index) {
            String owner=method.getEnclosingElement().toString(), methodName=method.getSimpleName().toString();
            if(owner.equals("java.lang.String")) {
                if(methodName.equals("substring") || methodName.equals("subSequence")) return index==0?"beginIndex":"endIndex";
                if(methodName.equals("charAt") || methodName.equals("codePointAt")) return "characterIndex";
                if(methodName.equals("indexOf") || methodName.equals("lastIndexOf"))
                    return index==1?"fromIndex":(method.getParameters().get(0).asType().getKind().isPrimitive()?"searchCharacter":"searchText");
                if(methodName.equals("startsWith")) return index==0?"prefix":"prefixOffset";
                if(methodName.equals("endsWith")) return "suffix";
            }
            if(owner.equals("java.lang.System") && methodName.equals("arraycopy"))
                return new String[]{"sourceArray","sourceIndex","destinationArray","destinationIndex","copyLength"}[index];
            // The declaring JDK interface/class establishes these collection roles.
            if(owner.startsWith("java.util.") && (owner.endsWith("Map") || owner.endsWith("Hashtable"))) {
                if(methodName.equals("get") || methodName.equals("containsKey") || methodName.equals("remove")) return index==0?"key":null;
                if(methodName.equals("put")) return index==0?"key":"value";
            }
            if(owner.startsWith("java.util.") && (owner.endsWith("List") || owner.endsWith("Vector"))) {
                if((methodName.equals("get") || methodName.equals("set") || methodName.equals("remove") || methodName.equals("elementAt"))
                        && index==0 && method.getParameters().get(0).asType().getKind()==TypeKind.INT) return "elementIndex";
            }
            return null;
        }
        @Override public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
            Element method=symbol(path(getCurrentPath(),tree.getMethodSelect()));
            arguments(tree.getArguments(),method);
            if(method != null && tree.getArguments().size()==1 && method.getSimpleName().toString().startsWith("set"))
                suggest(symbol(path(getCurrentPath(),tree.getArguments().get(0))),property(method.getSimpleName().toString()),75);
            return super.visitMethodInvocation(tree,unused);
        }
        @Override public Void visitNewClass(NewClassTree tree, Void unused) {
            arguments(tree.getArguments(),symbol(getCurrentPath()));
            return super.visitNewClass(tree,unused);
        }
        @Override public Void visitArrayAccess(ArrayAccessTree tree, Void unused) {
            String array=expression(path(getCurrentPath(),tree.getExpression()));
            suggest(symbol(path(getCurrentPath(),tree.getIndex())),array == null ? null : singular(array)+"Index",75);
            return super.visitArrayAccess(tree,unused);
        }
        @Override public Void visitMethod(MethodTree tree, Void unused) {
            if(tree.getParameters().size()==1) {
                String method=tree.getName().toString();
                Element parameter=symbol(path(getCurrentPath(),tree.getParameters().get(0)));
                if(method.startsWith("set")) suggest(parameter,property(method),95);
                if(method.equals("equals") || method.equals("compareTo")) suggest(parameter,"other",100);
                if(method.matches("(?:from|for)(?:Value|Ordinal|Code|Descriptor|Name)"))
                    suggest(parameter,decap(method.substring(method.startsWith("from")?4:3)),95);
            }
            return super.visitMethod(tree,unused);
        }
    }
    static void addEdit(CompilationUnitTree unit, long start, long end, String oldName, String newName) {
        String source=sources.get(unit);
        if(start < 0 || end < start || end > source.length() || !source.substring((int)start,(int)end).equals(oldName))
            throw new IllegalStateException("Unexpected source token: "+unit.getSourceFile()+":"+start+" "+oldName);
        edits.computeIfAbsent(unit, u -> new ArrayList<>()).add(new Edit((int)start,(int)end,newName));
    }
    static final class Rewrite extends TreePathScanner<Void, Void> {
        CompilationUnitTree unit;
        @Override public Void visitCompilationUnit(CompilationUnitTree tree, Void unused) {
            unit=tree; return super.visitCompilationUnit(tree,unused);
        }
        void reference(Tree tree, String oldName) {
            String newName=recovered.get(symbol(getCurrentPath()));
            if(newName != null) {
                long end=positions.getEndPosition(unit,tree);
                addEdit(unit,end-oldName.length(),end,oldName,newName);
            }
        }
        @Override public Void visitIdentifier(IdentifierTree tree, Void unused) {
            reference(tree,tree.getName().toString()); return super.visitIdentifier(tree,unused);
        }
        @Override public Void visitMemberSelect(MemberSelectTree tree, Void unused) {
            reference(tree,tree.getIdentifier().toString()); return super.visitMemberSelect(tree,unused);
        }
        @Override public Void visitMemberReference(MemberReferenceTree tree, Void unused) {
            reference(tree,tree.getName().toString()); return super.visitMemberReference(tree,unused);
        }
        @Override public Void visitMethod(MethodTree tree, Void unused) {
            String replacement=recovered.get(symbol(getCurrentPath()));
            if(replacement != null) {
                String oldName=tree.getName().toString();
                int start=(int)positions.getEndPosition(unit,tree.getReturnType());
                int end=(int)(tree.getBody()==null ? positions.getEndPosition(unit,tree) : positions.getStartPosition(unit,tree.getBody()));
                Matcher match=Pattern.compile("\\b"+Pattern.quote(oldName)+"\\s*\\(").matcher(sources.get(unit).substring(start,end));
                if(!match.find()) throw new IllegalStateException("Method declaration not found: "+oldName);
                addEdit(unit,start+match.start(),start+match.start()+oldName.length(),oldName,replacement);
            }
            return super.visitMethod(tree,unused);
        }
        @Override public Void visitVariable(VariableTree tree, Void unused) {
            String replacement=recovered.get(symbol(getCurrentPath()));
            if(replacement != null) {
                String oldName=tree.getName().toString();
                int start=(int)positions.getStartPosition(unit,tree);
                int end=(int)(tree.getInitializer()==null ? positions.getEndPosition(unit,tree)
                    : positions.getStartPosition(unit,tree.getInitializer()));
                String prefix=sources.get(unit).substring(start,end);
                Matcher match=Pattern.compile("(?<![\\w$])"+Pattern.quote(oldName)+"(?![\\w$])").matcher(prefix);
                int found=-1;
                while(match.find()) found=match.start();
                if(found<0) throw new IllegalStateException("Declaration not found: "+oldName);
                addEdit(unit,start+found,start+found+oldName.length(),oldName,replacement);
            }
            return super.visitVariable(tree,unused);
        }
    }
    public static void main(String[] args) throws Exception {
        if(args.length<2) throw new IllegalArgumentException("RecoverNames <source-root> <report-dir> [--apply] [field-mapping.tsv]");
        Path root=Paths.get(args[0]); output=Paths.get(args[1]); Files.createDirectories(output);
        boolean apply=Arrays.asList(args).contains("--apply");
        for(String arg:args) if(arg.endsWith(".tsv")) {
            for(String line:Files.readAllLines(Paths.get(arg))) if(!line.isEmpty() && !line.startsWith("#")) {
                String[] entry=line.split("\\t"); explicit.put(entry[0],entry[1]);
            }
        }
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics=new DiagnosticCollector<>();
        try(StandardJavaFileManager files=compiler.getStandardFileManager(diagnostics,null,StandardCharsets.UTF_8)) {
            List<File> javaFiles=new ArrayList<>();
            try(java.util.stream.Stream<Path> stream=Files.walk(root)) {
                stream.filter(p->p.toString().endsWith(".java")).sorted().forEach(p->javaFiles.add(p.toFile()));
            }
            JavacTask task=(JavacTask)compiler.getTask(null,files,diagnostics,
                Arrays.asList("--release","8","-proc:none","-classpath","lib/ant-1.10.11.jar"+File.pathSeparator+"lib/stubs-wtk.jar"),null,
                files.getJavaFileObjectsFromFiles(javaFiles));
            List<CompilationUnitTree> units=new ArrayList<>();
            for(CompilationUnitTree unit:task.parse()) { units.add(unit); sources.put(unit,unit.getSourceFile().getCharContent(true).toString()); }
            task.analyze();
            long errors=diagnostics.getDiagnostics().stream().filter(d->d.getKind()==Diagnostic.Kind.ERROR).count();
            if(errors>0) {
                for(Diagnostic<?> d:diagnostics.getDiagnostics()) if(d.getKind()==Diagnostic.Kind.ERROR) System.err.println(d);
                throw new IllegalStateException("Refusing edits: "+errors+" attribution errors");
            }
            trees=Trees.instance(task); positions=trees.getSourcePositions(); types=task.getTypes(); elements=task.getElements();
            serializable=elements.getTypeElement("java.io.Serializable").asType();
            for(CompilationUnitTree unit:units) new TreePathScanner<Void,Void>() {
                @Override public Void visitVariable(VariableTree tree, Void unused) {
                    Element element=symbol(getCurrentPath());
                    if(element != null && positions.getStartPosition(unit,tree)>=0) {
                        declarations.put(element,tree); paths.put(element,getCurrentPath());
                    }
                    return super.visitVariable(tree,unused);
                }
            }.scan(unit,null);
            for(Element element:declarations.keySet()) {
                String mapped=explicit.get(key(element));
                if(mapped != null && !serialized(element)) { recovered.put(element,mapped); evidence.put(element,"reviewed field mapping"); }
                if(mapped != null && serialized(element)) wireRoles.put(element,mapped);
                if(element.getKind()!=ElementKind.FIELD && explicit.containsKey(variableKey(element))) {
                    recovered.put(element,explicit.get(variableKey(element))); evidence.put(element,"reviewed variable mapping");
                }
            }
            for(CompilationUnitTree unit:units) new TreePathScanner<Void,Void>() {
                @Override public Void visitMethod(MethodTree tree, Void unused) {
                    String role=property(tree.getName().toString());
                    if(valid(role) && tree.getParameters().isEmpty() && tree.getBody()!=null && tree.getBody().getStatements().size()==1
                            && tree.getBody().getStatements().get(0) instanceof ReturnTree) {
                        ReturnTree result=(ReturnTree)tree.getBody().getStatements().get(0);
                        Element field=symbol(TreePath.getPath(unit,result.getExpression()));
                        if(field!=null && serialized(field) && weak(field.getSimpleName().toString())) wireRoles.putIfAbsent(field,role);
                    }
                    return super.visitMethod(tree,unused);
                }
            }.scan(unit,null);
            for(CompilationUnitTree unit:units) new TreePathScanner<Void,Void>() {
                @Override public Void visitMethod(MethodTree tree, Void unused) {
                    Element element=symbol(getCurrentPath());
                    if(element != null && explicit.containsKey(key(element))) {
                        recovered.put(element,explicit.get(key(element))); paths.put(element,getCurrentPath());
                        evidence.put(element,"reviewed method mapping");
                    }
                    return super.visitMethod(tree,unused);
                }
            }.scan(unit,null);
            // Resolve only unambiguous strongest evidence; iterate to propagate names across calls.
            for(int round=0;round<5;round++) {
                suggestions.clear();
                for(CompilationUnitTree unit:units) new Infer().scan(unit,null);
                for(Element element:declarations.keySet()) {
                    if(!eligible(element) || recovered.containsKey(element)) continue;
                    Map<String,Integer> votes=suggestions.getOrDefault(element,Collections.emptyMap());
                    int best=votes.values().stream().mapToInt(Integer::intValue).max().orElse(0);
                    List<String> winners=new ArrayList<>();
                    votes.forEach((candidate,strength)->{if(strength==best) winners.add(candidate);});
                    if(winners.size()==1 && best>=70) { recovered.put(element,winners.get(0)); evidence.put(element,"data flow strength "+best); }
                }
            }
            for(Element element:declarations.keySet()) {
                if(eligible(element) && !recovered.containsKey(element)) {
                    String type=typeName(element.asType());
                    if(valid(type)) { recovered.put(element,type); evidence.put(element,"declared type"); }
                }
            }
            // Reserve every original identifier in a compilation unit, including inherited members
            // referenced there, to avoid shadowing or changing overload/member resolution.
            Map<CompilationUnitTree,Set<String>> reserved=new HashMap<>();
            for(CompilationUnitTree unit:units) {
                Set<String> names=new HashSet<>(); reserved.put(unit,names);
                for(Map.Entry<Element,String> member:recovered.entrySet())
                    if(member.getKey().getKind()==ElementKind.FIELD || member.getKey().getKind()==ElementKind.METHOD) names.add(member.getValue());
                Matcher tokens=Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*").matcher(sources.get(unit));
                while(tokens.find()) names.add(tokens.group());
            }
            Map<Element,Set<String>> scopes=new HashMap<>();
            for(Element element:new ArrayList<>(recovered.keySet())) {
                if(element.getKind()==ElementKind.FIELD || element.getKind()==ElementKind.METHOD) continue;
                CompilationUnitTree unit=paths.get(element).getCompilationUnit();
                Element scope=element.getEnclosingElement();
                Set<String> used=scopes.computeIfAbsent(scope,k->new HashSet<>(reserved.get(unit)));
                String base=recovered.get(element), candidate=base;
                if(used.contains(candidate)) {
                    candidate=base+(element.getKind()==ElementKind.PARAMETER ? "Argument" : "Value");
                    int suffix=2;
                    while(used.contains(candidate)) candidate=base+(element.getKind()==ElementKind.PARAMETER ? "Argument" : "Value")+suffix++;
                }
                used.add(candidate); recovered.put(element,candidate);
            }
            List<String> mapping=new ArrayList<>(),remaining=new ArrayList<>();
            mapping.add("file\tline\tkind\towner\told\tnew\tevidence");
            remaining.add("file\tline\tkind\towner\tname\ttype\treason");
            for(Element element:declarations.keySet()) {
                TreePath location=paths.get(element); CompilationUnitTree unit=location.getCompilationUnit();
                long line=unit.getLineMap().getLineNumber(positions.getStartPosition(unit,location.getLeaf()));
                String prefix=root.toAbsolutePath().normalize().relativize(Paths.get(unit.getSourceFile().toUri()))+"\t"+line+"\t"+element.getKind()+"\t"+element.getEnclosingElement()+"\t"+element.getSimpleName();
                if(recovered.containsKey(element)) mapping.add(prefix+"\t"+recovered.get(element)+"\t"+evidence.get(element));
                else if(weak(element.getSimpleName().toString())) remaining.add(prefix+"\t"+element.asType()+"\t"+(serialized(element)?"serialized field: preserve wire name":"insufficient unambiguous semantic evidence"));
            }
            Files.write(output.resolve("name-mapping.tsv"),mapping,StandardCharsets.UTF_8);
            Files.write(output.resolve("unresolved-names.tsv"),remaining,StandardCharsets.UTF_8);
            List<String> members=new ArrayList<>(), wire=new ArrayList<>();
            for(Map.Entry<Element,String> entry:recovered.entrySet())
                if(entry.getKey().getKind()==ElementKind.FIELD || entry.getKey().getKind()==ElementKind.METHOD)
                    members.add(key(entry.getKey())+"\t"+entry.getValue());
            for(Map.Entry<Element,String> entry:wireRoles.entrySet()) wire.add(key(entry.getKey())+"\t"+entry.getValue());
            Files.write(output.resolve("renamed-members.tsv"),members,StandardCharsets.UTF_8);
            Files.write(output.resolve("serialized-field-roles.tsv"),wire,StandardCharsets.UTF_8);
            if(apply) {
                for(CompilationUnitTree unit:units) new Rewrite().scan(unit,null);
                for(Map.Entry<CompilationUnitTree,List<Edit>> entry:edits.entrySet()) {
                    entry.getValue().sort(Comparator.comparingInt((Edit edit)->edit.start).reversed());
                    StringBuilder source=new StringBuilder(sources.get(entry.getKey()));
                    int boundary=source.length();
                    for(Edit edit:entry.getValue()) {
                        if(edit.end>boundary) throw new IllegalStateException("Overlapping edits");
                        source.replace(edit.start,edit.end,edit.text); boundary=edit.start;
                    }
                    Files.write(Paths.get(entry.getKey().getSourceFile().toUri()),source.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            System.out.println("Parsed "+units.size()+" files, "+declarations.size()+" variables; recovered "+recovered.size()+" names; unresolved "+(remaining.size()-1)+". Applied: "+apply);
        }
    }
}
