package function;

import instruction.Instruction;
import instruction.Operation;
import symbol.SymbolEntry;
import symbol.SymbolType;
import token.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ExprStack {
    public Stack<TokenType> operation_stack =new Stack<>();
    //  + - * / ( )
    // 操作数有：number_iteral、int_kw
    public Stack<SymbolEntry> num_stack = new Stack<>();

    int[][] priority ={
            {1,  1,-1,-1,-1,-1, 1,    1, 1, 1, 1, 1, 1,  1, 1, -1},
            {1,  1,-1,-1,-1,-1, 1,    1, 1, 1, 1, 1, 1,  1, 1, -1},
            {1,  1, 1, 1, 1,-1, 1,    1, 1, 1, 1, 1, 1,  1, 1, -1},
            {1,  1, 1, 1, 1,-1, 1,    1, 1, 1, 1, 1, 1,  1, 1, -1},
            {1,  1, 1, 1, 1,-1, 1,    1, 1, 1, 1, 1, 1,  1, 1, -1},
            {-1,-1,-1,-1,-1,-1,100,  -1,-1,-1,-1,-1,-1,  1, 1, -1},
            {-1,-1,-1,-1,-1, 0, 0,   -1,-1,-1,-1,-1,-1,  1, 1, -1},

            {-1,-1,-1,-1,-1,-1, 1,    1, 1, 1, 1, 1, 1,  1, 1, -1},
            {-1,-1,-1,-1,-1,-1, 1,    1, 1, 1, 1, 1, 1,  1, 1, -1},
            {-1,-1,-1,-1,-1,-1, 1,    1, 1, 1, 1, 1, 1,  1, 1, -1},
            {-1,-1,-1,-1,-1,-1, 1,    1, 1, 1, 1, 1, 1,  1, 1, -1},
            {-1,-1,-1,-1,-1,-1, 1,    1, 1, 1, 1, 1, 1,  1, 1, -1},
            {-1,-1,-1,-1,-1,-1, 1,    1, 1, 1, 1, 1, 1,  1, 1, -1},

            {-1,-1,-1,-1,-1,-1, 1,    -1,-1,-1,-1,-1,-1,  1, 1, -1},
            {-1,-1,-1,-1,-1,-1, 1,    -1,-1,-1,-1,-1,-1,  -1, 1, -1},

            { 1, 1, 1, 1, 1,-1, 1,    1, 1, 1, 1, 1, 1,   1, 1, -1}

    };

    int index_to;
    int index_from;


//    // 当前计算结果的类型
//    TokenType type;

    private int getIndex(TokenType tokenType){
        if(tokenType == TokenType.PLUS){
            return 0;
        } else if(tokenType == TokenType.MINUS){
            return 1;
        } else if(tokenType == TokenType.MUL){
            return 2;
        } else if(tokenType == TokenType.DIV){
            return 3;
        } else if(tokenType == TokenType.MOD){
            return 4;
        } else if(tokenType == TokenType.L_PAREN){
            return 5;
        } else if(tokenType == TokenType.R_PAREN){
            return 6;
        } else if(tokenType== TokenType.LT){
            return 7;
        } else if(tokenType== TokenType.GT){
            return 8;
        } else if(tokenType== TokenType.LE){
            return 9;
        } else if(tokenType== TokenType.GE){
            return 10;
        } else if(tokenType== TokenType.EQ){
            return 11;
        } else if(tokenType== TokenType.NEQ){
            return 12;
        } else if(tokenType== TokenType.AND){
            return 13;
        } else if(tokenType== TokenType.OR){
            return 14;
        }
        return -1;
    }

    /**
     * 调用结束之后需要index++
     * @param top
     * @param op1
     * @param op2
     * @return
     */
    public List<Instruction> generateInstruction(TokenType top, SymbolEntry op1, SymbolEntry op2){
        // 如果se是常数，则直接算，如果是变量，则用新的se去load它，用这个se进行计算
        List<Instruction> res_ins = new ArrayList<>();
        String op1_num, op2_num;
        if(op1.isIteral()){
            op1_num = String.valueOf(op1.getValue());
        }
        else if(op1.isVar()){
            op1_num = op1.getName();
        }
        else{
            // 是命名变量，需要load进编译变量再计算
            SymbolEntry res = new SymbolEntry(SymbolType.Var, TokenType.INT_KW, "%a" + index_to++);
            op1_num = String.valueOf(res.getName());
            res_ins.add(new Instruction(Operation.load, TokenType.INT_KW, "%a" + op1.getId(), null, res.getName()));
        }
        if(op2.isIteral()){
            op2_num = String.valueOf(op2.getValue());
        }
        else if(op2.isVar()){
            op2_num = op2.getName();
        }
        else{
            SymbolEntry res = new SymbolEntry(SymbolType.Var, TokenType.INT_KW, "%a" + index_to++);
            op2_num = String.valueOf(res.getName());
            res_ins.add(new Instruction(Operation.load, TokenType.INT_KW, "%a" + op2.getId(), null, res.getName()));
        }
        // 结果存在一个编译变量里
        SymbolEntry res = new SymbolEntry(SymbolType.Var, TokenType.INT_KW, "%a" + index_to++);
        switch (top) {
            case LT:
                res.setTokenType(TokenType.I1);
                res_ins.add(new Instruction(Operation.icmp_slt, TokenType.I1, op1_num, op2_num, res.getName()));
                break;
            case LE:
                res.setTokenType(TokenType.I1);
                res_ins.add(new Instruction(Operation.icmp_sle, TokenType.I1, op1_num, op2_num, res.getName()));
                break;
            case GT:
                res.setTokenType(TokenType.I1);
                res_ins.add(new Instruction(Operation.icmp_sgt, TokenType.I1, op1_num, op2_num, res.getName()));
                break;
            case GE:
                res.setTokenType(TokenType.I1);
                res_ins.add(new Instruction(Operation.icmp_sge, TokenType.I1, op1_num, op2_num, res.getName()));
                break;
            case PLUS:
                res_ins.add(new Instruction(Operation.add, TokenType.INT_KW, op1_num, op2_num, res.getName()));
                break;
            case MINUS:
                res_ins.add(new Instruction(Operation.sub, TokenType.INT_KW, op1_num, op2_num, res.getName()));
                break;
            case MUL:
                res_ins.add(new Instruction(Operation.mul, TokenType.INT_KW, op1_num, op2_num, res.getName()));
                break;
            case DIV:
                res_ins.add(new Instruction(Operation.sdiv, TokenType.INT_KW, op1_num, op2_num, res.getName()));
                break;
            case MOD:
                // a/b,结果在res里，用这个结果*b
                res_ins.add(new Instruction(Operation.sdiv, TokenType.INT_KW, op1_num, op2_num, res.getName()));
                // res1 * b，结果在res2里
                SymbolEntry res2 = new SymbolEntry(SymbolType.Var, TokenType.INT_KW, "%a" + index_to++);
                res_ins.add(new Instruction(Operation.mul, TokenType.INT_KW, res.getName(), op2_num, res2.getName()));
                // a - res2, 结果在res3里
                res = new SymbolEntry(SymbolType.Var, TokenType.INT_KW, "%a" + index_to++);
                res_ins.add(new Instruction(Operation.sub, TokenType.INT_KW, op1_num, res2.getName(), res.getName()));
                break;
            case EQ:
                res.setTokenType(TokenType.I1);
                res_ins.add(new Instruction(Operation.icmp_eq, TokenType.I1, op1_num, op2_num, res.getName()));
                break;
            case NEQ:
                res.setTokenType(TokenType.I1);
                res_ins.add(new Instruction(Operation.icmp_ne, TokenType.I1, op1_num, op2_num, res.getName()));
                break;
            case AND:
                res.setTokenType(TokenType.I1);
                res_ins.add(new Instruction(Operation.and, TokenType.I1, op1_num, op2_num, res.getName()));
                break;
            case OR:
                res.setTokenType(TokenType.I1);
                res_ins.add(new Instruction(Operation.or, TokenType.I1, op1_num, op2_num, res.getName()));
                break;
        }
        // 把计算结果存入
        num_stack.add(res);
        // todo:修改analyse中的index
        return res_ins;
    }


    /**
     *
     * @param tt
     * @param type 表示int和double
     * @return
     */
    public List<Instruction> addOptAndGenerateInstruction(TokenType tt, TokenType type, int index){
        this.index_to = index;
        List<Instruction> instructions=new ArrayList<>();
        if (operation_stack.empty()){
            operation_stack.push(tt);
            return instructions;
        }

        TokenType top= operation_stack.peek();
        while (priority[getIndex(top)][getIndex(tt)] > 0){
            operation_stack.pop();
            if (top == TokenType.L_PAREN)
                break;
            SymbolEntry op2 = num_stack.pop(), op1 = num_stack.pop();
            instructions.addAll(generateInstruction(top, op1, op2));
            if(operation_stack.empty())
                break;
            top = operation_stack.peek();
        }
        if(tt != TokenType.R_PAREN)
            operation_stack.push(tt);
        return instructions;
    }

    public List<Instruction> addAndReset(TokenType type, int index){
        index_to = index;
        List<Instruction> instructions=new ArrayList<>();
        while(!operation_stack.empty() && !num_stack.empty()){
            SymbolEntry op2 = num_stack.pop(), op1 = num_stack.pop();
            instructions.addAll(generateInstruction(operation_stack.pop(), op1, op2));
        }
        return instructions;
    }

//    public void pushOp(TokenType type){
//        operation_stack.push(type);
//    }

    public void pushNum(SymbolEntry se) {
        num_stack.push(se);
    }

    public void pushOpt(TokenType se) {
        operation_stack.push(se);
    }

    public int getVarIndex(){
        return index_to;
    }

    public SymbolEntry getTopNum(){
        return num_stack.peek();
    }

    public List<Instruction> beforeGetTopNum(int index, TokenType tt) {
        index_to = index;
        List<Instruction> res_ins = new ArrayList<>();
        SymbolEntry se = num_stack.peek();
        if(se.getType() == SymbolType.Local){
            num_stack.pop();
            SymbolEntry res = new SymbolEntry(SymbolType.Var, TokenType.INT_KW, "%a" + index_to++);
            res_ins.add(new Instruction(Operation.load, TokenType.INT_KW, "%a" + se.getId(), null, res.getName()));
            num_stack.add(res);
        }
        if(tt == TokenType.I1 && se.getTokenType() == TokenType.INT_KW){
            num_stack.pop();
            SymbolEntry res = new SymbolEntry(SymbolType.Var, TokenType.I1, "%a" + index_to++);
            if(se.getType() == SymbolType.Local)
                res_ins.add(new Instruction(Operation.icmp_eq, TokenType.I1, "%a" + se.getId(), "0", res.getName()));
            else{
                res_ins.add(new Instruction(Operation.icmp_eq, TokenType.I1, se.getName(), "0", res.getName()));
            }
            num_stack.add(res);
        }
        return res_ins;
    }

    public void reset(){
        this.num_stack = new Stack<>();
        this.operation_stack = new Stack<>();
    }
}
