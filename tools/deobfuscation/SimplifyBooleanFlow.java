import com.sun.source.tree.*;
import com.sun.source.util.*;
import javax.lang.model.type.TypeKind;
import javax.tools.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Replaces boolean-only ternaries with equivalent short-circuit expressions.
 * Operands retain their order and number of evaluations. Reference-typed conditional
 * results and boxed conditions in identity rewrites are deliberately excluded.
 */
public final class SimplifyBooleanFlow {
    static final class Edit {
        final int start,end; final String replacement;
        Edit(int start,int end,String replacement) {this.start=start;this.end=end;this.replacement=replacement;}
    }
    static Boolean literal(ExpressionTree expression) {
        if(expression instanceof LiteralTree && ((LiteralTree)expression).getValue() instanceof Boolean)
            return (Boolean)((LiteralTree)expression).getValue();
        return null;
    }
    public static void main(String[] args) throws Exception {
        Path root=Paths.get(args[0]).toAbsolutePath().normalize(); List<String> audit=new ArrayList<>();
        audit.add("file\tline\tbefore\tafter");
        int total=0;
        for(int pass=0;pass<20;pass++) {
            JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
            DiagnosticCollector<JavaFileObject> errors=new DiagnosticCollector<>();
            int changed=0;
            try(StandardJavaFileManager manager=compiler.getStandardFileManager(errors,null,StandardCharsets.UTF_8)) {
                List<File> files=new ArrayList<>();
                try(java.util.stream.Stream<Path> stream=Files.walk(root)) {stream.filter(p->p.toString().endsWith(".java")).sorted().forEach(p->files.add(p.toFile()));}
                JavacTask task=(JavacTask)compiler.getTask(null,manager,errors,Arrays.asList("--release","8","-proc:none","-classpath",
                    "lib/ant-1.10.11.jar"+File.pathSeparator+"lib/stubs-wtk.jar"),null,manager.getJavaFileObjectsFromFiles(files));
                List<CompilationUnitTree> units=new ArrayList<>(); for(CompilationUnitTree unit:task.parse())units.add(unit);
                task.analyze();
                if(errors.getDiagnostics().stream().anyMatch(d->d.getKind()==Diagnostic.Kind.ERROR)) throw new AssertionError(errors.getDiagnostics());
                Trees trees=Trees.instance(task); SourcePositions positions=trees.getSourcePositions();
                for(CompilationUnitTree unit:units) {
                    String source=unit.getSourceFile().getCharContent(true).toString(); List<Edit> edits=new ArrayList<>();
                    new TreePathScanner<Void,Void>() {
                        String text(Tree tree) {return source.substring((int)positions.getStartPosition(unit,tree),(int)positions.getEndPosition(unit,tree));}
                        @Override public Void visitConditionalExpression(ConditionalExpressionTree tree,Void unused) {
                            if(trees.getTypeMirror(getCurrentPath()).getKind()!=TypeKind.BOOLEAN) return super.visitConditionalExpression(tree,unused);
                            Boolean yes=literal(tree.getTrueExpression()),no=literal(tree.getFalseExpression());
                            String condition=text(tree.getCondition()), replacement=null;
                            if(yes!=null && no!=null && yes!=no) {
                                if(!yes) replacement="!("+condition+")";
                                else if(trees.getTypeMirror(new TreePath(getCurrentPath(),tree.getCondition())).getKind()==TypeKind.BOOLEAN)
                                    replacement="("+condition+")";
                            } else if(yes!=null && no==null) {
                                replacement=yes?"(("+condition+") || ("+text(tree.getFalseExpression())+"))"
                                    :"(!("+condition+") && ("+text(tree.getFalseExpression())+"))";
                            } else if(no!=null && yes==null) {
                                replacement=no?"(!("+condition+") || ("+text(tree.getTrueExpression())+"))"
                                    :"(("+condition+") && ("+text(tree.getTrueExpression())+"))";
                            }
                            if(replacement==null) return super.visitConditionalExpression(tree,unused);
                            int start=(int)positions.getStartPosition(unit,tree),end=(int)positions.getEndPosition(unit,tree);
                            edits.add(new Edit(start,end,replacement));
                            audit.add(root.relativize(Paths.get(unit.getSourceFile().toUri()))+"\t"+unit.getLineMap().getLineNumber(start)
                                +"\t"+source.substring(start,end).replace('\n',' ').replace('\t',' ')+"\t"+replacement.replace('\n',' ').replace('\t',' '));
                            return null; // Nested replacements are made on the next attributed pass.
                        }
                    }.scan(unit,null);
                    if(!edits.isEmpty()) {
                        changed+=edits.size(); edits.sort(Comparator.comparingInt((Edit e)->e.start).reversed());
                        StringBuilder result=new StringBuilder(source);
                        for(Edit edit:edits)result.replace(edit.start,edit.end,edit.replacement);
                        Files.write(Paths.get(unit.getSourceFile().toUri()),result.toString().getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
            total+=changed;
            if(changed==0) {
                Files.write(Paths.get(args[1]),audit,StandardCharsets.UTF_8);
                System.out.println("Simplified "+total+" boolean expressions without changing operand evaluation order.");
                return;
            }
        }
        throw new AssertionError("Simplification did not converge");
    }
}
