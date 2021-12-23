package instruction;

import function.Function;
import symbol.SymbolEntry;
import symbol.SymbolType;
import token.TokenType;

import java.util.List;
import java.util.Objects;

public class Instruction {
    private Operation opt;
    // 操作数1
    String op1;
    boolean is_num1 = false;
    // 操作数2
    String op2;
    boolean is_num2 = false;
    // 结果保存在
    String save_at;
    // int double void
    TokenType type;
    Function function;
    List<SymbolEntry> param_list;

    public Instruction(Operation opt) {
        this.opt = opt;
        this.op1 = null;
        this.op2 = null;
    }

    public Instruction(Operation opt, TokenType type) {
        this.opt = opt;
        this.type = type;
    }

    public Instruction(Operation opt, String op1) {
        this.opt = opt;
        this.op1 = op1;
    }

    public Instruction(Operation opt, String op1, String op2) {
        this.opt = opt;
        this.op1 = op1;
        this.op2 = op2;
    }

    public Instruction(Operation opt, String op1, String op2, String save_at) {
        this.opt = opt;
        this.op1 = op1;
        this.op2 = op2;
        this.save_at = save_at;
    }

    public Instruction(Operation opt, TokenType type, String op1, String op2) {
        this.opt = opt;
        this.op1 = op1;
        this.op2 = op2;
        this.type = type;
    }

    public Instruction(Operation opt, TokenType type, String op1, String op2, String save_at) {
        this.opt = opt;
        this.op1 = op1;
        this.op2 = op2;
        this.type = type;
        this.save_at = save_at;
    }

    public Instruction(Operation opt, TokenType type, String op1) {
        this.opt = opt;
        this.op1 = op1;
        this.type = type;
    }

    public void setFunction(Function func){
        this.function = func;
    }
    public void setIsNum1(){
        is_num1 = true;
    }

    public void setIsNum2(){
        is_num2 = true;
    }

    public void setSaveAt(String save_at) {this.save_at = save_at;}

    public Instruction() {
        this.opt = Operation.nop;
        this.op1 = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Instruction that = (Instruction) o;
        return opt == that.opt && Objects.equals(op1, that.op1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(opt, op1);
    }

    public Operation getOpt() {
        return opt;
    }

    public void setOpt(Operation opt) {
        this.opt = opt;
    }

    public int getValueSize() { return this.opt.getSize(); }

    public String getOp1() {
        return op1;
    }

    public String getOp2() {
        return op2;
    }

    public TokenType getType(){ return this.type; }

    public void  setType(TokenType type) { this.type = type ;}

    public void setOp1(String op1) {
        this.op1 = op1;
    }
    public void setOp2(String op2) {
        this.op2 = op2;
    }

    @Override
    public String toString() {

        switch (this.opt) {
            case nop:
                return String.format("%s %s", this.opt, this.op1);
            case pop:
            case dup:
            case load8:
            case load16:
            case load32:
            case load:
                return String.format("%s = %s i32, i32* %s", this.save_at, this.opt, this.op1);
            case store8:
            case store32:
            case store:
                return String.format("%s %s %s, %s %s, align 4", this.opt, this.op2, this.op1, "i32*", this.save_at);
            case alloc:
            case free:
            case add:
            case sub:
            case mul:
            case sdiv:
                return String.format("%s = %s i32 %s, %s", this.save_at, this.opt, this.op1, this.op2);
            case add_f:
            case mul_f:
            case div_f:
            case div_u:
            case shl:
            case shr:
            case and:
                return String.format("%s = and %s %s, %s", this.save_at, this.type, this.op1, this.op2);
            case or:
                return String.format("%s = or %s %s, %s", this.save_at, this.type, this.op1, this.op2);
            case xor:
            case not:
            case icmp_sle:
                return String.format("%s = icmp sle i32 %s, %s", this.save_at, this.op1, this.op2);
            case icmp_slt:
                return String.format("%s = icmp slt i32 %s, %s", this.save_at, this.op1, this.op2);
            case icmp_ne:
                return String.format("%s = icmp ne i32 %s, %s", this.save_at, this.op1, this.op2);
            case icmp_eq:
                return String.format("%s = icmp eq i32 %s, %s", this.save_at, this.op1, this.op2);
            case icmp_sge:
                return String.format("%s = icmp sge i32 %s, %s", this.save_at, this.op1, this.op2);
            case icmp_sgt:
                return String.format("%s = icmp sgt i32 %s, %s", this.save_at, this.op1, this.op2);
            case cmp_u:
            case cmp_f:
            case neg_i:
            case neg_f:
            case itof:
            case ftoi:
            case shrl:
            case slt:
            case sgt:
                if(this.type == TokenType.INT_KW){
                    return String.format("%s %s %s %s", this.opt, "i32", this.op1, this.op2);
                }
                else{
                    return String.format("%s %s", this.opt, this.op1);
                }
            case ret:
                if(this.type == TokenType.INT_KW){
                    return String.format("%s %s %s", this.opt, "i32", this.op1);
                }
                else if(this.type == TokenType.VOID_KW){
                    return String.format("%s %s", this.opt, "void");
                }
                else{
                    return String.format("%s", this.opt);
                }
            case push:
            case popn:
            case loca:
            case alloca:
                return String.format("%s = %s i32, align 4", this.save_at, this.opt);
            case globa:
            case stackalloc:
            case br:
                return String.format("%s label %s", this.opt, this.op1);
            case br_false:
            case br_i1:
                return String.format("br i1 %s, label %s, label %s", this.save_at, this.op1, this.op2);
            case call:
                if(this.type == TokenType.INT_KW){
                    return String.format("%s = %s %s @%s(%s)", this.save_at, this.opt, "i32", this.function.getName(), getParamToString());
                }
                else{
                    return String.format("%s void @%s(%s)", this.opt, this.function.getName(), getParamToString());
                }
            case callname:
//                return String.format("call %s @%s(i32 %s)", this.op1);
            case label:
                return String.format("%s:", this.op1);
            default:
                return "nop";
        }
    }

    public void setParamList(List<SymbolEntry> param_list){
        this.param_list = param_list;
    }

    public String getParamToString() {
        StringBuilder s = new StringBuilder();
        int i = 0;
        if(param_list == null){
            return "";
        }
        for(SymbolEntry e: this.param_list){
            if(i != 0){
                s.append(",");
            }
            switch (e.getTokenType()){
                case INT_KW:
                    if(e.getType() == SymbolType.Var){
                        s.append(String.format("i32 %s", e.getName()));
                    }
//                    else if(e.getType() == SymbolType.Iteral){
//                        s.append(String.format("i32 %%a%s", e.getName()));
//                    }
//                    else if(e.getType() == SymbolType.Local){
//                        s.append(String.format("i32* %%a%s", e.getId()));
//                    }
                    else{
                        s.append(String.format("i32 %%a%s", e.getId()));
                    }
                    break;
                case INT_POINTER:
                    if(e.getType() == SymbolType.Var){
                        s.append(String.format("i32* %s", e.getName()));
                    }
//                    else if(e.getType() == SymbolType.Iteral){
//                        s.append(String.format("i32* %%a%s", e.getName()));
//                    }
                    else{
                        s.append(String.format("i32* %%a%s", e.getId()));
                    }
                    break;
                case NUMBER_LITERAL:
                    s.append(String.format("i32 %s", e.getValue()));
                    break;
            }
            i++;
        }
        return s.toString();
    }
}
