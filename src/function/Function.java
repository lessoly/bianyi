package function;

import instruction.Instruction;
import symbol.SymbolEntry;
import token.TokenType;
import vo.Pos;

import java.util.ArrayList;
import java.util.List;

public class Function implements Comparable<Function>{
    // 函数体
    List<Instruction> function_body;

    // 参数
    List<SymbolEntry> param_table;
    // 局部变量
    List<SymbolEntry> local_table;
    // 临时变量（编译过程产生的）
    List<SymbolEntry> var_table;
    // 函数名
    String name;
    // 返回值类型
    TokenType return_type;
    // 定义位置
    Pos pos;

    SymbolEntry se;

    public Function(SymbolEntry se, String name, Pos pos, TokenType return_type) {
        this.name = name;//main 这种
        this.pos = pos;
//    this.function_body = new ArrayList<>();
    this.param_table = new ArrayList<>();
//    this.local_table = new ArrayList<>();
        this.return_type = return_type;
        this.se = se;
    }


    public void setFunctionBody(List<Instruction> function_body) { this.function_body = function_body; }

    public int getReturnSlot() {
        if(this.return_type == TokenType.INT_KW || this.return_type == TokenType.DOUBLE_KW){
            return 1;
        }
        return 0;
    }

    public List<SymbolEntry> getParams() { return this.param_table; }

    public String getParamToString() {
        StringBuilder s = new StringBuilder();
        int i = 0;
        if(this.param_table == null){
            return "";
        }
        for(SymbolEntry e: this.param_table){
            if(i != 0){
                s.append(",");
            }
            switch (e.getTokenType()){
                case INT_KW:
                    s.append("i32");
                    break;
                case INT_POINTER:
                    s.append("i32*");
                    break;
            }
            i++;
        }
        return s.toString();
    }

    public String getParamNameToString() {
        StringBuilder s = new StringBuilder();
        s.append("(");
        int i = 0;
        if(this.param_table == null){
            return "";
        }
        for(SymbolEntry e: this.param_table){
            if(i != 0){
                s.append(",");
            }
            switch (e.getTokenType()){
                case INT_KW:
                    s.append("i32 ");
                    break;
                case INT_POINTER:
                    s.append("i32* ");
                    break;
            }
            s.append("%").append(e.getName());
            i++;
        }
        s.append(")");
        return s.toString();
    }

    public void setParams(List<SymbolEntry> params) {
        this.param_table = params;
    }

    public void setLocals(List<SymbolEntry> locals) {
        this.local_table = locals;
    }

    public int getId() { return se.getId(); }

    public void setId(int id) { this.se.setId(id); }

    public List<Instruction>  getFunctionBody(){ return this.function_body; }

    public List<SymbolEntry> getSymbolTable() {
        List<SymbolEntry> list = new ArrayList<>(this.param_table);
        list.addAll(this.local_table);
        list.addAll(this.var_table);
        return list;
    }

    public TokenType getReturnType() { return this.return_type; }

    public String getName() { return name; }

    public Pos getPos() { return pos; }

    public int getParamNum() { return this.param_table.size(); }

//  public int getVarNmum() { return this.let_table.size(); }

    public List<SymbolEntry> getVarTable() {
        return var_table;
    }

    public void setVarTable(List<SymbolEntry> var_table) {
        this.var_table = var_table;
    }

    public int getReturnNum() {
        if (this.return_type!= TokenType.VOID_KW)
            return 1;
        else return 0;
    }

    public void setPos(Pos pos) {
        this.pos = pos;
    }

    public void setReturnType(TokenType return_type) {
        this.return_type = return_type;
    }

    public Boolean isSTDFunction(){
        String[] std_function = {"getint", "getarray", "getch", "putint", "putch", "putarray"};
        for(String n: std_function) {
            if (this.se.getName().equals(n)){
                return true;
            }
        }
        return false;
    }

    @Override
    public int compareTo(Function arg0) {
        return this.getId() - arg0.getId();
    }
}
