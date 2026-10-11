import java.awt.Font;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

/** Whole-class differential checks for script parsing, error collection and UI text wrapping. */
public final class FrontendEquivalence {
    static final String PARSER="com.zelix.klassmaster.script.parser.";
    static int checks;
    static URLClassLoader loader(String classes) throws Exception {
        return new URLClassLoader(new URL[]{Paths.get(classes).toUri().toURL(),Paths.get("src/main/resources").toUri().toURL(),
                Paths.get("lib/ant-1.10.11.jar").toUri().toURL(),Paths.get("lib/stubs-wtk.jar").toUri().toURL()},ClassLoader.getPlatformClassLoader());
    }
    static Object call(Object target,String name,Class<?>[] types,Object...args) throws Exception {
        try { return target.getClass().getMethod(name,types).invoke(target,args); }
        catch(InvocationTargetException error) {
            Throwable cause=error.getCause();
            return cause.getClass().getName()+":"+(cause instanceof NullPointerException?"":cause.getMessage());
        }
    }
    static Object field(Object target,String name) throws Exception { return target.getClass().getField(name).get(target); }
    static void field(Object target,String name,Object value) throws Exception { target.getClass().getField(name).set(target,value); }
    static void same(String name,Object before,Object after) {
        checks++;
        if(!Objects.equals(before,after)) throw new AssertionError(name+"\nbefore="+before+"\nafter="+after);
    }
    static String tree(Object node) throws Exception {
        if(node==null || node instanceof String) return String.valueOf(node);
        int count=(Integer)call(node,"jjtGetNumChildren",new Class<?>[0]);
        StringBuilder text=new StringBuilder(node.getClass().getName()).append('(');
        for(int i=0;i<count;i++) text.append(tree(call(node,"jjtGetChild",new Class<?>[]{int.class},i))).append(',');
        return text.append(')').toString();
    }
    static String parse(ClassLoader loader,String source) throws Exception {
        Object parser=loader.loadClass(PARSER+"ZkmScriptParser").getConstructor(Reader.class).newInstance(new StringReader(source));
        Object result=call(parser,"Input",new Class<?>[0]);
        return tree(result)+"\nlookahead="+Arrays.toString((int[])field(parser,"jj_la1"));
    }
    static String wrap(ClassLoader loader,String text,int width,boolean previouslyWrapped) throws Exception {
        Class<?> canvas=loader.loadClass("com.zelix.klassmaster.ui.component.MultiLineLabelCanvas");
        Object instance=canvas.getConstructor(Font.class).newInstance(new Font(Font.MONOSPACED,Font.PLAIN,12));
        field(instance,"text",text);field(instance,"wrapWidth",width);field(instance,"linesWrapped",previouslyWrapped);
        Object result=call(instance,"wrapLines",new Class<?>[0]);
        return result+"|"+field(instance,"lines")+"|"+field(instance,"linesWrapped");
    }
    static String errorToken(ClassLoader loader,int savedLength,int token,int position,int variant) throws Exception {
        Object parser=loader.loadClass(PARSER+"ZkmScriptParser").getConstructor(Reader.class).newInstance(new StringReader(""));
        field(parser,"jj_endpos",savedLength);
        int[] last=new int[100];for(int i=0;i<100;i++)last[i]=i%7;
        field(parser,"jj_lasttokens",last);
        List<int[]> entries=new ArrayList<>();
        if(variant>0) entries.add(new int[]{99});
        if(variant>1) entries.add(Arrays.copyOf(last,Math.max(0,Math.min(100,savedLength))));
        field(parser,"jj_expentries",entries);
        Object result=call(parser,"jj_add_error_token",new Class<?>[]{int.class,int.class},token,position);
        return result+"|"+field(parser,"jj_endpos")+"|"+Arrays.toString((int[])field(parser,"jj_lasttokens"))
                +"|"+Arrays.toString((int[])field(parser,"jj_expentry"))+"|"+Arrays.deepToString(entries.toArray());
    }
    static String diagnostic(ClassLoader loader,int statement,int[][] sequences) throws Exception {
        Class<?> tokenClass=loader.loadClass(PARSER+"ZkmScriptToken");
        Object current=tokenClass.getConstructor().newInstance(), previous=current;
        field(current,"image","previous");
        for(int i=0;i<8;i++) {
            Object token=tokenClass.getConstructor().newInstance();field(token,"kind",i==7?0:40);
            field(token,"image","token"+i);field(token,"endLine",3);field(token,"endColumn",i+1);
            field(previous,"next",token);previous=token;
        }
        String[] images=new String[260];for(int i=0;i<images.length;i++)images[i]="TOKEN_"+i;
        Object exception=loader.loadClass(PARSER+"ZkmScriptParseException")
                .getConstructor(tokenClass,int[][].class,String[].class,int.class).newInstance(current,sequences,images,statement);
        return String.valueOf(call(exception,"getMessage",new Class<?>[0]));
    }
    static String verifierEquality(ClassLoader loader,String leftDescriptor,String rightDescriptor,
                                   boolean leftInitialized,boolean rightInitialized,boolean strict,int otherKind) throws Exception {
        String prefix="com.zelix.klassmaster.classfile.insn.";
        Class<?> frame=loader.loadClass(prefix+"StackFrameState");
        Field flag=frame.getDeclaredField("strictMergeEnabled"); flag.setAccessible(true); flag.setBoolean(null,strict);
        Object left=loader.loadClass(prefix+(leftInitialized?"VerifierType":"UninitializedThisValueType"))
                .getConstructor(String.class).newInstance(leftDescriptor);
        Object right=otherKind==0?null:otherKind==1?"wrong type":loader.loadClass(prefix+(rightInitialized?"VerifierType":"UninitializedThisValueType"))
                .getConstructor(String.class).newInstance(rightDescriptor);
        return String.valueOf(call(left,"equals",new Class<?>[]{Object.class},right));
    }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless","true");
        try(URLClassLoader before=loader(args[0]);URLClassLoader after=loader(args[1])) {
            List<String> scripts=new ArrayList<>(Arrays.asList("", "open \"example.jar\";", "open \"a.jar\" \"b.jar\";",
                    "open \"a.jar\" -\"b.jar\" +\"c.jar\";", "open \"a.jar\" expectedInitialSHA256=\"abc\";",
                    "ignoreMissingReferences *;", "ignoreMissingReferences * and *;", "ignoreMissingReferences ;",
                    "open ;", "open \"abc\" { * };", "open \"abc\" {", "obfuscate;", "trim;", "classpath \"a.jar\";"));
            for(String source:new ArrayList<>(scripts))for(int end=0;end<source.length();end++)scripts.add(source.substring(0,end));
            String[] rules={"*", "*.*", "public *", "!public *", "package *", "(public *)", "!(public *)", "*.*.*",
                    "* *", "* *(*)", "* *(int, java.lang.String)", "* *()", "@A *", "@A(*) *", "module-info",
                    "* extends *", "* implements *", "* extends A implements B, C", "* +", "* ^", "* and *", "* or *",
                    "public final *", "!abstract *", "* public *", "* private int field", "* native void method()"};
            for(String command:new String[]{"exclude", "unexclude", "trimExclude", "trimUnexclude", "ignoreMissingReferences", "fixedClasses"})
                for(String rule:rules) scripts.add(command+" "+rule+";");
            for(String filter:new String[]{"", "\"*.class\"", "!\"*.xml\"", "\"a\" and \"b\"", "\"a\" or !\"b\"", "\"a\" and", "and \"a\""})
                scripts.add("open \"a.jar\" {"+filter+"} \"b.jar\";");
            for(String source:scripts)same("script "+source,parse(before,source),parse(after,source));
            String[] texts={"",null,"a","a b","\n","\r\n"," a  b ","abc\ndef\rghi","longword x y","汉字测试 文本"};
            for(String text:texts)for(int width:new int[]{-1,0,1,7,12,30,250})for(boolean flag:new boolean[]{false,true})
                same("wrap "+text+" width="+width,wrap(before,text,width,flag),wrap(after,text,width,flag));
            for(int saved:new int[]{-1,0,1,2,9,99,100,101})for(int position:new int[]{-1,0,1,2,10,99,100,101})for(int variant=0;variant<3;variant++)
                same("error token "+saved+"/"+position,errorToken(before,saved,43,position,variant),errorToken(after,saved,43,position,variant));
            int[][][] sequences={ {}, {{}}, {{0}}, {{37}}, {{37,0},{38,0},{99,1,0}}, {{40,0},{90},{-1}}, {{260}}, {null} };
            for(int statement=0;statement<260;statement++)for(int[][] expected:sequences)
                same("diagnostic "+statement,diagnostic(before,statement,expected),diagnostic(after,statement,expected));
            for(String left:new String[]{null,"I","J","La;",""})for(String right:new String[]{null,"I","J","La;",""})
                for(boolean strict:new boolean[]{false,true})for(boolean leftInit:new boolean[]{false,true})
                    for(boolean rightInit:new boolean[]{false,true})for(int otherKind=0;otherKind<3;otherKind++)
                        same("verifier equality",verifierEquality(before,left,right,leftInit,rightInit,strict,otherKind),
                                verifierEquality(after,left,right,leftInit,rightInit,strict,otherKind));
        }
        System.out.println("Frontend equivalence: "+checks+" comparisons passed.");
    }
}
