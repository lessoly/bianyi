package symbol;

import error.AnalyzeError;
import error.ErrorCode;
import function.Function;
import instruction.Instruction;
import instruction.Operation;
import token.TokenType;
import vo.Pos;

import java.util.*;

public class Definition {
    public List<SymbolEntry> global_list = new ArrayList<SymbolEntry>();

    public Map<String, Function> function_list = new HashMap<String, Function>();
    public Map<String, Function> std_function_list = new HashMap<String, Function>();

    // 全局变量加函数的符号表
    public Map<Integer, SymbolEntry> symbol_list = new HashMap<>();

    int nextOffset = 0;

    int symbol_list_index = 0;

    public List<Instruction> instruction;

    // 当前level
    public int level = 0;

    public Definition() throws AnalyzeError {
        this.addFunction("_start", TokenType.VOID_KW, new Pos(-1, -1));
    }

    public Boolean isSTDFunction(String name){
        String[] std_function = {"getch", "getint", "getdouble", "putch", "putint", "putdouble", "putchar",
            "putstr", "putln"};
        for(String n: std_function){
            if(n.equals(name)){
                return true;
            }
        }
        return false;
    }

    // 调用时需保证函数从未被添加
    public Function addSTDFunction(String name) throws AnalyzeError {
        if(!isSTDFunction(name)){
            throw new AnalyzeError(ErrorCode.NotSTDFunction, new Pos(-1, -1));
        }
        Function func;
        switch (name) {
            case "getch":
                func = this.addFunction("getch", TokenType.INT_KW, new Pos(-1, -1));
                func.setFunctionBody(new ArrayList<>(Collections.singletonList(new Instruction())));
                return func;
            case "getint":
                func = this.addFunction("getint", TokenType.INT_KW, new Pos(-1, -1));
                func.setFunctionBody(new ArrayList<>(Collections.singletonList(new Instruction())));
                return func;
            case "getarray":
                func = this.addFunction("getarray", TokenType.INT_KW, new Pos(-1, -1));
                func.setFunctionBody(new ArrayList<>(Collections.singletonList(new Instruction())));
                func.setParams(new ArrayList<>(Collections.singletonList(
                        new SymbolEntry(-1, SymbolType.Param, null, TokenType.INT_POINTER, false, false,
                                null, -1))));
                return func;
            case "putint":
                func = this.addFunction("putint", TokenType.VOID_KW, new Pos(-1, -1));
                func.setFunctionBody(new ArrayList<>(Collections.singletonList(new Instruction())));
                func.setReturnType(TokenType.VOID_KW);
                func.setParams(new ArrayList<>(Collections.singletonList(
                    new SymbolEntry(-1, SymbolType.Param, null, TokenType.INT_KW, false, false,
                        null, -1))));
                return func;
            case "putarray":
                func = this.addFunction("putarray", TokenType.VOID_KW, new Pos(-1, -1));
                func.setFunctionBody(new ArrayList<>(Collections.singletonList(new Instruction())));
                func.setReturnType(TokenType.VOID_KW);
                func.setParams(Arrays.asList(
                    new SymbolEntry(-1, SymbolType.Param, null, TokenType.INT_KW, false, false,
                        null, -1),
                        new SymbolEntry(-1, SymbolType.Param, null, TokenType.INT_POINTER, false, false,
                                null, -1)));
                return func;
            case "putch":
                func = this.addFunction("putch", TokenType.VOID_KW, new Pos(-1, -1));
                func.setFunctionBody(new ArrayList<>(Collections.singletonList(new Instruction())));
                func.setReturnType(TokenType.VOID_KW);
                func.setParams(new ArrayList<>(Collections.singletonList(
                    new SymbolEntry(-1, SymbolType.Param, null, TokenType.INT_KW, false, false,
                        null, -1))));
                return func;
//            case "putln":
//                func = this.addFunction("putln", TokenType.DOUBLE_KW, new Pos(-1, -1));
//                func.setFunctionBody(new ArrayList<>(Collections.singletonList(new Instruction())));
//                func.setReturnType(TokenType.VOID_KW);
//                func.setParamSlot(0);
//                return func;
            default:
                throw new AnalyzeError(ErrorCode.NotDeclared, new Pos(-2, -2));
        }
    }

    public List<SymbolEntry> getGlobalList() { return this.global_list; }

    public int getGlobalListCount(){ return this.global_list.size(); }

    public Map<String, Function> getFunctionList() {
        Map<String, Function> resMap = new HashMap<>();
        resMap.putAll(function_list);
        resMap.putAll(std_function_list);
        return resMap;
    }

    public int getFunctionListCount(){
        int count = 0;
        for(String name: this.function_list.keySet()){
            if(!isSTDFunction(name)){
                count++;
            }
        }
        return count;
    }
    // value是他的值
    // 返回的是global的id
    public int addGlobal(String name, TokenType tt, boolean is_ini, boolean is_const, Pos pos, Object value) throws AnalyzeError {
        System.out.println("add global: " + name + "\t\t index: " + this.global_list.size());
        if(getSymbol(name) != null){
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, pos);
        }
        SymbolEntry se = addSymbol(this.global_list.size(), name, SymbolType.Global, tt, is_ini, is_const, pos, value, 0);
        this.global_list.add(se);
        return se.getId();
    }

    public SymbolEntry getGlobal(String name) throws AnalyzeError {
        for(SymbolEntry se: global_list){
            if(se.getName().equals(name)){
                return se;
            }
        }
        throw new AnalyzeError(ErrorCode.NoSuchGlobal, new Pos(-1, -1));
    }

    public Function addFunction(String name, TokenType return_tt, Pos pos) throws AnalyzeError {
        System.out.println("add function: " + name + "\t\t index: " + this.global_list.size());
        if(this.function_list.get(name) != null || getSymbol(name, level) != null || this.std_function_list.get(name) != null){
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, pos);
        }
        Function func;
        SymbolEntry se = this.addSymbol(this.global_list.size(), name, SymbolType.Function, return_tt, true, true, pos, name, 0);
        func = new Function(se, name, pos, return_tt);
        if(isSTDFunction(name)){
            this.std_function_list.put(name, func);
        }
        else{
            this.function_list.put(name, func);
        }
        this.global_list.add(se);
        return func;
    }

    // todo: 局部变量和全局可以重名，和函数可以重名吗？不可以
    public SymbolEntry addSymbol(int id, String name, SymbolType type, TokenType tt, boolean is_init, boolean is_const, Pos curPos, Object value, int level) throws AnalyzeError {
//        System.out.println("add symbol " + name + "\t\t index: " + this.symbol_list_index + "\t\t type: " + tt + "\t\t level: " + level);
        SymbolEntry se = getSymbol(name, level);
        // 同级存在重复定义
        if(se != null && !isSTDFunction(name)){
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        }
        if(isSTDFunction(name) && se != null){
            return se;
        }
        // todo:name加%
        se = new SymbolEntry(id, type, name, tt, is_init, is_const, value, level);
        this.symbol_list.put(this.symbol_list_index++, se);
//        System.out.println(this.symbol_list.size());
        return se;
    }

    public SymbolEntry getSymbol(String name){
        // todo: 从level最高的开始查， hashmap排序
//        System.out.println("get symbol: " + name + " now_level: " + this.level);
        for(int i = this.level; i >= 0; i--){
            for(Integer a :this.symbol_list.keySet()){
                SymbolEntry sym = this.symbol_list.get(a);
//                System.out.println("check name: " +sym.getName());
                if(sym.level == i && sym.getName().equals(name)){
                    return sym;
                }
            }
        }
        return null;
    }

    public SymbolEntry getSymbol(String name, int level){
//        System.out.println("get symbol: " + name + " level: " + this.level);
        for(Integer a :this.symbol_list.keySet()){
            SymbolEntry sym = this.symbol_list.get(a);
            if(sym.getLevel() == level && sym.getName().equals(name)){
                return sym;
            }
        }
        return null;
    }

    // 获取函数或加载库函数
    public Function getFunction(String name) throws AnalyzeError {
        Function func = this.function_list.get(name);
        if(func != null){
            return func;
        }
        else if(isSTDFunction(name)){
            func = this.std_function_list.get(name);
            if(func == null)
                func = addSTDFunction(name);
        }
        return func;
    }

    public Function generate(List<Instruction> global_ins) throws AnalyzeError {
        Function main_func = getFunction("main");
        if(main_func == null){
            throw new AnalyzeError(ErrorCode.CantFindMain, new Pos(0,0));
        }
        Function start_func = getFunction("_start");
        List<Instruction> instructions = new ArrayList<>(global_ins);
//        instructions.add(new Instruction(Operation.stackalloc, (long)main_func.getReturnSlot()));
//        instructions.add(new Instruction(Operation.call, (long)getFunctionIndex(main_func)));
        start_func.setFunctionBody(instructions);
        return start_func;
    }

    public void levelDown(){
        HashMap<Integer, SymbolEntry> res = new HashMap<>();
        for(Integer i: this.symbol_list.keySet()){
            if(this.symbol_list.get(i).getLevel() == this.level){
                res.put(i, this.symbol_list.get(i));
                System.out.println("remove symbol " + this.symbol_list.get(i).getName() + "\t\t type: " + this.symbol_list.get(i).getType() + "\t\t level: " + this.level);
            }
        }
        for(Integer i : res.keySet()){
            this.symbol_list.remove(i);
        }
        this.level--;
    }

    public int getLevel() { return this.level; }

    public void setLevel(int level) { this.level = level;}

    public int getFunctionIndex(Function func) {
        List<Function> func_list = new ArrayList<>();
        for (String name: getFunctionList().keySet()) {
            if(!this.isSTDFunction(name)){
                func_list.add(getFunctionList().get(name));
            }
        }
        Collections.sort(func_list);
        int i = 0;
        for(Function f: func_list){
            if(f.equals(func)){
                return i;
            }
            i++;
        }
        return -1;
    }
}
