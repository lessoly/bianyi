package analyse;

import error.*;
import instruction.Instruction;
import function.BreakAndContinue;
import function.ExprStack;
import function.Function;
import instruction.Operation;
import symbol.Definition;
import symbol.SymbolEntry;
import symbol.SymbolType;
import token.Token;
import token.TokenType;
import token.Tokenizer;
import util.Format;
import vo.Pos;

import java.util.*;
public final class Analyser {

    Tokenizer tokenizer;
    List<Instruction> global_instructions;
    int global_slot;

    /** 当前偷看的 token */
    Token peekedToken = null;

    /** 符号表 */
    public Definition def_table;

    // 表达式栈
    ExprStack expr_stack;

    // block标签号，每次函数结束后清空
    int block_index;
    // 编译变量标号，从1号开始
    int var_index = 1;


    // 暂存函数的相关内容
    Function function;
    List<Instruction> function_body;
    List<SymbolEntry> param_table;
    List<SymbolEntry> local_table;
    List<SymbolEntry> var_table;
    TokenType return_type;
    private boolean onAssign;
    private boolean onCond = false;
    private int while_level;
    //continue和break指令的集合
    List<BreakAndContinue> continue_instruction = new ArrayList<BreakAndContinue>();
    List<BreakAndContinue> break_instruction = new ArrayList<>();

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.global_instructions = new ArrayList<>();
    }

    public void analyse() throws CompileError {
        analyseCompUnit();
    }

    /**
     * 查看下一个 Token
     *
     * @throws TokenizeError
     * @return
     */
    private Token peek() throws TokenizeError {
        if (peekedToken == null) {
            peekedToken = tokenizer.nextToken();
        }
        return peekedToken;
    }

    /**
     * 获取下一个 Token
     *
     * @throws TokenizeError
     */
    private Token next() throws TokenizeError {
        if (peekedToken != null) {
            Token token = peekedToken;
            peekedToken = null;
            return token;
        } else {
            return tokenizer.nextToken();
        }
    }

    private boolean check(TokenType... tt) throws TokenizeError {
        TokenType token = peek().getTokenType();
        while(token == TokenType.COMMENT){
            next();
            token = peek().getTokenType();
        }
        for(TokenType t: tt){
            if(token == t){
                return true;
            }
        }
        return false;
    }

    private Token expect(TokenType... tt) throws CompileError {
        TokenType token = peek().getTokenType();
        while(token == TokenType.COMMENT){
            next();
            token = peek().getTokenType();
        }
        for (TokenType t : tt) {
            if (token == t) {
                return next();
            }
        }
        throw new ExpectedTokenError(Format.generateList(tt), peek());
    }

    /**
     * 设置符号为已赋值
     *
     * @param name   符号名称
     * @param curPos 当前位置（报错用）
     * @throws AnalyzeError 如果未定义则抛异常
     */
    private void initializeSymbol(String name, Pos curPos) throws AnalyzeError {
        SymbolEntry sym = this.def_table.getSymbol(name);
        if (sym == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else if(sym.isConstant()) {
            throw new AnalyzeError(ErrorCode.AssignToConstant, curPos);
        }else if(sym.isFunction()) {
            throw new AnalyzeError(ErrorCode.AssignToFunction, curPos);
        }else{
            sym.setInitialized(true);
        }
    }

    private Instruction getLocalOrParamAddress(Token token) throws AnalyzeError {
        SymbolEntry sym = this.def_table.getSymbol(token.getValueString());
        if(sym == null)
            throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
        if(sym.getType() == SymbolType.Function)
            throw new AnalyzeError(ErrorCode.FunctionHasNoAddr, token.getStartPos());
        else if(sym.getType() == SymbolType.Param){
            // load
            return new Instruction(Operation.load, String.valueOf(sym.getId()));
        }else if(sym.getType() == SymbolType.Local) {
            return new Instruction(Operation.load, String.valueOf(sym.getId()));
        }else{
            return new Instruction(Operation.load, String.valueOf(sym.getId()));
        }
    }

    private void addInstruction(Instruction ins){
        if(this.def_table.getLevel() != 0){
            this.function_body.add(ins);
        }
        else{
            this.global_instructions.add(ins);
        }
    }

    private void addAllInstruction(List<Instruction> ins){
        if(this.def_table.getLevel() != 0){
            this.function_body.addAll(ins);
        }
        else{
            this.global_instructions.addAll(ins);
        }
    }

    private void functionAddParam(TokenType tt, String name, Pos pos, boolean is_const) throws AnalyzeError {
        SymbolEntry se = this.def_table.addSymbol(this.param_table.size(), name, SymbolType.Param, tt, true, is_const, pos, null, 1);
        this.param_table.add(se);
    }

    private SymbolEntry functionAddLocal(TokenType tt, String name,Boolean is_init, Boolean is_const, Pos pos, int level) throws AnalyzeError {
        SymbolEntry se = this.def_table.addSymbol(var_index++, name, SymbolType.Local, tt, is_init, is_const, pos, null, level);
        this.local_table.add(se);
        return se;
    }

    public Function getStartFunction() throws AnalyzeError {
        return this.def_table.generate(this.global_instructions);
    }

    private void analyseCompUnit() throws CompileError {
        this.def_table = new Definition();
        this.expr_stack = new ExprStack();
        this.global_instructions = new ArrayList<>();
        while(!check(TokenType.EOF)){
            if(check(TokenType.INT_KW, TokenType.VOID_KW)){
                var_index = 1;
                block_index = 0;
                analyseFunction();
            }
//            else if(check(TokenType.LET_KW)||check(TokenType.CONST_KW)){
//                analyseDeclStmt(0);
//            }
            else{
                throw new ExpectedTokenError(
                        Format.generateList(TokenType.INT_KW, TokenType.CONST_KW, TokenType.FN_KW), peek());
            }
        }
        this.def_table.instruction = this.global_instructions;
        expect(TokenType.EOF);
    }

    private void analyseFunction() throws CompileError{
        Token return_tt = expect(TokenType.VOID_KW, TokenType.INT_KW);
        this.return_type = return_tt.getTokenType();
        Token nameToken = expect(TokenType.IDENT);
        this.function = null;
        this.function_body = new ArrayList<>();
        this.param_table = new ArrayList<>();
        this.local_table = new ArrayList<>();
        this.def_table.level = 1;
        this.onAssign = false;

        Function func = this.def_table.addFunction(nameToken.getValueString(), this.return_type, nameToken.getStartPos());
        this.function = func;
        expect(TokenType.L_PAREN);
//        if(!check(TokenType.R_PAREN))
//            analyseFunctionParamList();
//        func.setParams(this.param_table);
//        func.setParamSlot(this.param_slot);
        expect(TokenType.R_PAREN);
        Block(this.return_type, 1);
        if(this.function_body.size() == 0 || this.function_body.get(this.function_body.size() - 1).getOpt() != Operation.ret){
//            this.function_body.add(new Instruction(Operation.ret, TokenType.VOID_KW));
            if(func.getReturnType() != TokenType.VOID_KW){
                throw new AnalyzeError(ErrorCode.ReturnTypeWrong, peekedToken.getStartPos());
            }
        }
        func.setFunctionBody(this.function_body);
        func.setLocals(this.local_table);
    }

    // return_type
    private void Block(TokenType return_type, int level) throws CompileError{
        this.def_table.level = level;
        expect(TokenType.L_BRACE);
        while(!check(TokenType.R_BRACE) && !check(TokenType.EOF)){
            BlockItem(return_type, level);
        }
        expect(TokenType.R_BRACE);
        // 只能清符号表，不能清函数local表
        this.def_table.levelDown();
    }

    private void BlockItem(TokenType return_type, int level) throws CompileError{
        if(check(TokenType.INT_KW) || check((TokenType.CONST_KW))){
            Decl(level);
        }
        else if(check(TokenType.IDENT)){
            analyseExpr();
        }
        else if(check(TokenType.IF_KW)){
            analyseIfStmt(level, false);
        }
//        else if(check(TokenType.WHILE_KW)){
//            analyseWhileStmt(level);
//        } else if(check(TokenType.BREAK_KW)){
//            analyseBreakStmt();
//        } else if(check(TokenType.CONTINUE_KW)){
//            analyseContinueStmt();
//        }
        else if(check(TokenType.RETURN_KW)){
            analyseReturnStmt();
        }
        else if(check(TokenType.L_BRACE)){
            Block(return_type, level + 1);
        }
        else if(check(TokenType.SEMICOLON)){
            expect(TokenType.SEMICOLON);
            expr_stack.reset();
        }
        else{
            throw new ExpectedTokenError(Format.generateList(TokenType.INT_KW, TokenType.CONST_KW, TokenType.IDENT,
                    TokenType.RETURN_KW, TokenType.L_BRACE, TokenType.SEMICOLON, TokenType.IF_KW), next());
        }
    }

    private void analyseExprStmt() throws CompileError{
        SymbolEntry type = analyseExpr();
        this.addAllInstruction(expr_stack.addAndReset(type.getTokenType(), var_index));
        this.var_index = this.expr_stack.getVarIndex();
        expect(TokenType.SEMICOLON);
        expr_stack.reset();
    }

    private SymbolEntry analyseExpr() throws CompileError{
        SymbolEntry type;
        boolean is_sign = false;
        if(check(TokenType.MINUS)){
            expect(TokenType.MINUS);
            // 放入0和减号
            expr_stack.pushNum(new SymbolEntry(SymbolType.Iteral, TokenType.INT_KW, 0));
            this.addAllInstruction(expr_stack.addOptAndGenerateInstruction(TokenType.MINUS, TokenType.INT_KW, var_index));
            var_index = expr_stack.getVarIndex();
//            expr_stack.pushOpt(TokenType.MINUS);
            type = analyseExpr();
            is_sign = true;
//            if(type.getTokenType() == TokenType.INT_KW)
//                this.addInstruction(new Instruction(Operation.neg_i));
        }
        else if(check(TokenType.PLUS)){
            expect(TokenType.PLUS);
            expr_stack.pushNum(new SymbolEntry(SymbolType.Iteral, TokenType.INT_KW, 0));
            this.addAllInstruction(expr_stack.addOptAndGenerateInstruction(TokenType.PLUS, TokenType.INT_KW, var_index));
            var_index = expr_stack.getVarIndex();
//            expr_stack.pushOpt(TokenType.PLUS);
            type = analyseExpr();
            is_sign = true;
        }
        else if(check(TokenType.NEGATE) && onCond){
            expect(TokenType.NEGATE);
            expr_stack.pushNum(new SymbolEntry(SymbolType.Iteral, TokenType.INT_KW, 0));
            this.addAllInstruction(expr_stack.addOptAndGenerateInstruction(TokenType.MINUS, TokenType.INT_KW, var_index));
            var_index = expr_stack.getVarIndex();
//            expr_stack.pushOpt(TokenType.MINUS);
            type = analyseExpr();
            is_sign = true;
        }
        else if(check(TokenType.L_PAREN)){
            type = analyseGroupExpr();
        }
        else if(check(TokenType.IDENT)){
            Token nameToken = expect(TokenType.IDENT);

            if(check(TokenType.L_PAREN)) {
                type = analyseCallExpr(nameToken);
            }
            else if(check(TokenType.ASSIGN)) {
                SymbolEntry se = this.def_table.getSymbol(nameToken.getValueString());
                if(se == null){
                    throw new AnalyzeError(ErrorCode.NotDeclared, peek().getStartPos());
                }
                type = analyseAssignExpr(se, nameToken);
            }
            else{
                type = analyseIdentExpr(nameToken);
            }
        }
        else if(check(TokenType.NUMBER_LITERAL, TokenType.DOUBLE_LITERAL, TokenType.STRING_LITERAL, TokenType.CHAR_LITERAL)){
            type = analyseLiteralExpr();
        }
        else {
            throw new ExpectedTokenError(Format.generateList(TokenType.MINUS, TokenType.L_PAREN, TokenType.IDENT, TokenType.L_PAREN, TokenType.ASSIGN),
                    next());
        }

        while(check(TokenType.AND, TokenType.OR, TokenType.MOD, TokenType.MUL, TokenType.DIV, TokenType.EQ,
                TokenType.NEQ, TokenType.LT, TokenType.GT, TokenType.LE, TokenType.GE, TokenType.AS_KW)){
            if(check(TokenType.AND, TokenType.OR, TokenType.MOD, TokenType.MUL, TokenType.DIV, TokenType.EQ,
                    TokenType.NEQ, TokenType.LT, TokenType.GT, TokenType.LE, TokenType.GE)) {
                type = analyseOperatorExpr(type.getTokenType());
            }
        }
        while(check(TokenType.PLUS, TokenType.MINUS)){
            if(check(TokenType.PLUS, TokenType.MINUS)){
                if(is_sign){
                    type = analyseExpr();
                }
                else{
                    type = analyseOperatorExpr(type.getTokenType());
                }
            }
        }
        return type;
    }

    private SymbolEntry analyseLiteralExpr() throws CompileError{
        Token token = expect(TokenType.NUMBER_LITERAL, TokenType.STRING_LITERAL, TokenType.CHAR_LITERAL);
        TokenType tt = token.getTokenType();
        if(tt == TokenType.NUMBER_LITERAL/* || tt == TokenType.CHAR_LITERAL*/){
            // 直接push进栈
            int num;
            num = (int) token.getValue();
            SymbolEntry se = new SymbolEntry(SymbolType.Iteral, TokenType.NUMBER_LITERAL, token.getValue());
            expr_stack.pushNum(se);
//            this.addInstruction(new Instruction(Operation.push, (long)num));
            return new SymbolEntry(SymbolType.Iteral, TokenType.INT_KW, num);
        }
//        else if(tt == TokenType.STRING_LITERAL){
//            // 新建全局变量， 变量名是该字符串，变量值也是该字符串
//            int global_index = this.def_table.addGlobal(token.getValueString(),
//                    TokenType.STRING_LITERAL, true, true, token.getStartPos(), token.getValueString());
//            this.addInstruction(new Instruction(Operation.push, (long)global_index));
//            return TokenType.STRING_LITERAL;
//        }
//        else if(tt == TokenType.DOUBLE_LITERAL){
//            String binary = Long.toBinaryString(Double.doubleToRawLongBits((Double) token.getValue()));
//            this.addInstruction(new Instruction(Operation.push, Format.StringToLong(binary)));
//            return TokenType.DOUBLE_KW;
//        }
        else{
            throw new ExpectedTokenError(Format.generateList(TokenType.NUMBER_LITERAL),
                    next());
        }
    }

//    private TokenType analyseAsExpr(SymbolEntry tt) throws CompileError{
//        expect(TokenType.AS_KW);
//        TokenType as_tt = expect(TokenType.VOID_KW, TokenType.INT_KW, TokenType.DOUBLE_KW).getTokenType();
//        if(tt.getTokenType() == TokenType.INT_KW && as_tt == TokenType.DOUBLE_KW){
//            // int to double
//            this.addInstruction(new Instruction(Operation.itof));
//        }
//        else if(tt.getTokenType() == TokenType.DOUBLE_KW && as_tt == TokenType.INT_KW){
//            // double to int
//            this.addInstruction(new Instruction(Operation.ftoi));
//        }
//        else if(tt.getTokenType() != as_tt){
//            throw new AnalyzeError(ErrorCode.AsTypeWrong, peek().getStartPos());
//        }
////        while(check(TokenType.AS_KW)){
////            as_tt = analyseAsExpr(as_tt);
////        }
//        return as_tt;
//    }

    private SymbolEntry analyseOperatorExpr(TokenType tt) throws CompileError{
        Token token = expect(TokenType.AND, TokenType.OR, TokenType.MOD, TokenType.PLUS, TokenType.MINUS, TokenType.MUL, TokenType.DIV, TokenType.EQ,
                TokenType.NEQ, TokenType.LT, TokenType.GT, TokenType.LE, TokenType.GE);
        this.addAllInstruction(expr_stack.addOptAndGenerateInstruction(token.getTokenType(), tt, var_index));
        var_index = expr_stack.getVarIndex();
//        expr_stack.pushOpt(token.getTokenType());
        SymbolEntry next_tt = analyseExpr();
        if(tt != next_tt.getTokenType() || (tt != TokenType.INT_KW && tt != TokenType.DOUBLE_KW)){
            throw new AnalyzeError(ErrorCode.ExprTypeWrong, peek().getStartPos());
        }
        return next_tt;
    }

    private SymbolEntry analyseIdentExpr(Token token) throws CompileError{
        SymbolEntry se = this.def_table.getSymbol(token.getValueString());
        if(se == null){
            throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
        }
        if(!this.def_table.getSymbol(token.getValueString()).isInitialized()){
            throw new AnalyzeError(ErrorCode.NotInitialized, token.getStartPos());
        }
//        this.addInstruction(getLocalOrParamAddress(token));
        expr_stack.pushNum(se);
//        this.addInstruction(new Instruction(Operation.load, se.getTokenType(), "%a" + se.getName(), null, "%a" + var_index++));
        return se;
    }

    private SymbolEntry analyseAssignExpr(SymbolEntry se, Token token) throws CompileError{
        expect(TokenType.ASSIGN);
        if(this.onAssign){
            throw new AnalyzeError(ErrorCode.AssignFaild, peek().getStartPos());
        }
        if (se.isConstant())
            throw new AnalyzeError(ErrorCode.AssignToConstant, peek().getStartPos());

//        this.addInstruction(getLocalOrParamAddress(token));
//        SymbolEntry sym = this.def_table.getSymbol(token.getValueString());
//        if(sym == null)
//            throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
        // 获取值的类型
        SymbolEntry type = analyseExpr();
//        TokenType assigned = se.getTokenType();
        // 判断值的类型是否和se相同
//        if(type.getTokenType() != assigned || (assigned != TokenType.INT_KW && assigned != TokenType.DOUBLE_KW)){
//            throw new AnalyzeError(ErrorCode.AssignTypeWrong, peek().getStartPos());
//        }
        this.addAllInstruction(expr_stack.addAndReset(type.getTokenType(), var_index));
        this.var_index = this.expr_stack.getVarIndex();
        System.out.println("ini:: " + token.getValueString());
        initializeSymbol(token.getValueString(), token.getStartPos());
        if(this.expr_stack.getTopNum().getType() == SymbolType.Local){
            this.addAllInstruction(expr_stack.beforeGetTopNum(var_index, TokenType.INT_KW));
            this.addInstruction(new Instruction(Operation.store, this.expr_stack.getTopNum().getTokenType(), this.expr_stack.getTopNum().getName(), "i32", "%a" + se.getId()));
            var_index = expr_stack.getVarIndex();
        }
        else{
            this.addInstruction(new Instruction(Operation.store, this.expr_stack.getTopNum().getTokenType(), this.expr_stack.getTopNum().getName(), "i32", "%a" + se.getId()));
        }
        return new SymbolEntry(SymbolType.Var, TokenType.VOID_KW, null);
    }

    private SymbolEntry analyseCallExpr(Token token) throws CompileError{
        // 返回函数的返回值
        Function func = this.def_table.getFunction(token.getValueString());
        // 分配return的slot
        Instruction is;
        if(func.getReturnNum() != 0){
//            this.addInstruction(new Instruction(Operation.stackalloc, String.valueOf(func.getReturnSlot())));
            SymbolEntry se = new SymbolEntry(SymbolType.Var, func.getReturnType(), "%a" + var_index);
            expr_stack.pushNum(se);
            is = new Instruction(Operation.call, func.getReturnType(), String.valueOf(func.getId()), null, "%a" + var_index++);
        }
        else{
            is = new Instruction(Operation.call, String.valueOf(func.getId()));
        }
        is.setFunction(func);
        expect(TokenType.L_PAREN);
//        this.expr_stack.operation_stack.push(TokenType.L_PAREN);
        if(!check(TokenType.R_PAREN)) {
            // 准备参数，分配空间并放入参数
            List<SymbolEntry> param_list = analyseCallParamList(func.getParams());
            is.setParamList(param_list);
        }
        else{
            if(func.getParamNum() != 0){
                throw new AnalyzeError(ErrorCode.ParamNumWrong, this.peekedToken.getStartPos());
            }
        }
        expect(TokenType.R_PAREN);
//        System.out.println("top  " + this.expr_stack.operation_stack.peek());

//        if(func.isSTDFunction()){
//            this.addInstruction(new Instruction(Operation.callname, String.valueOf(func.getId())));
//        }
//        else{
        this.addInstruction(is);
//        }
        return new SymbolEntry(SymbolType.Function, func.getReturnType(), null);
    }
//
    private List<SymbolEntry> analyseCallParamList(List<SymbolEntry> param_list) throws CompileError{
        int param_num = 1, i = 0;
        this.expr_stack.reset();
        SymbolEntry type = analyseExpr();
        List<SymbolEntry> res_list = new ArrayList<>();
        if(param_list.get(i++).getTokenType() != type.getTokenType()){
            throw new AnalyzeError(ErrorCode.ExprTypeWrong, peek().getStartPos());
        }
//        while (!this.expr_stack.operation_stack.empty() && this.expr_stack.operation_stack.peek() != TokenType.L_PAREN) {
        this.addAllInstruction(expr_stack.addAndReset(type.getTokenType(), var_index));
        this.var_index = expr_stack.getVarIndex();
        this.addAllInstruction(expr_stack.beforeGetTopNum(var_index, TokenType.INT_KW));
        res_list.add(expr_stack.getTopNum());
        var_index = expr_stack.getVarIndex();
//        }
        while(check(TokenType.COMMA)){
            expect(TokenType.COMMA);
            this.expr_stack.reset();
            type = analyseExpr();
            if(param_list.get(i++).getTokenType() != type.getTokenType()){
                throw new AnalyzeError(ErrorCode.ExprTypeWrong, peek().getStartPos());
            }
//            while (!this.expr_stack.operation_stack.empty() && this.expr_stack.operation_stack.peek() != TokenType.L_PAREN) {
//                this.addAllInstruction(this.expr_stack.generateInstruction(this.expr_stack.operation_stack.pop(), type.getTokenType()));
//            }
            this.addAllInstruction(expr_stack.addAndReset(type.getTokenType(), var_index));
            this.var_index = expr_stack.getVarIndex();
            this.addAllInstruction(expr_stack.beforeGetTopNum(var_index, TokenType.INT_KW));
            res_list.add(expr_stack.getTopNum());
            var_index = expr_stack.getVarIndex();
            param_num++;
        }
        if(param_num != param_list.size()){
            System.out.println("当前参数个数：" + param_num + " ，期望参数个数：" + param_list.size());
            throw new AnalyzeError(ErrorCode.ParamNumWrong, this.peekedToken.getStartPos());
        }
        return res_list;
    }

    private SymbolEntry analyseGroupExpr() throws CompileError{
        expect(TokenType.L_PAREN);
        this.addAllInstruction(expr_stack.addOptAndGenerateInstruction(TokenType.L_PAREN, TokenType.INT_KW, var_index));
        this.var_index = expr_stack.getVarIndex();
        SymbolEntry type = analyseExpr();
        expect(TokenType.R_PAREN);
        this.addAllInstruction(expr_stack.addOptAndGenerateInstruction(TokenType.R_PAREN, type.getTokenType(), var_index));
        this.var_index = expr_stack.getVarIndex();
//        this.addAllInstruction(expr_stack.addAndReset(type.getTokenType(), var_index));
//        this.var_index = this.expr_stack.getVarIndex();
        return type;
    }

    private void analyseReturnStmt() throws CompileError{
        expect(TokenType.RETURN_KW);
        if(!check(TokenType.SEMICOLON)){
            // 有返回值
            if(this.return_type == TokenType.VOID_KW){
                throw new AnalyzeError(ErrorCode.ReturnTypeWrong, peekedToken.getStartPos());
            }
            // 返回值off是0
            // alloca原为arga，第二个参数为arg的标号，返回值id为0
//            this.addInstruction(new Instruction(Operation.alloca, TokenType.INT_KW, null,null, "%a" + var_index));
//            var_index++;
//            int save_index = var_index;
            // 1. %var_index = alloca i32, align 4
//            var_index++;
            SymbolEntry type = analyseExpr();
            if(type.getTokenType() != this.return_type){
                throw new AnalyzeError(ErrorCode.ReturnTypeWrong, peek().getStartPos());
            }
            this.addAllInstruction(expr_stack.addAndReset(type.getTokenType(), var_index));
            this.var_index = this.expr_stack.getVarIndex();
            SymbolEntry se;
//            Instruction is = new Instruction(Operation.store, return_type, String.valueOf(se.getName()), "0", "%a" + String.valueOf(save_index));
//            if(se.getType() == SymbolType.Iteral){
//                is.setIsNum1();
//            }
//            this.addInstruction(is);
            if(return_type != TokenType.VOID_KW) {
//            this.addInstruction(new Instruction(Operation.load, return_type, String.valueOf(0), String.valueOf(0), String.valueOf(var_index)));
                this.addAllInstruction(expr_stack.beforeGetTopNum(var_index, TokenType.INT_KW));
                se = expr_stack.getTopNum();
                var_index = expr_stack.getVarIndex();
                if(se.isIteral())
                    this.addInstruction(new Instruction(Operation.ret, return_type, String.valueOf(se.getValue())));
                else if(se.isVar()){
                    this.addInstruction(new Instruction(Operation.ret, return_type, String.valueOf(se.getName())));
                }
                else{
                    this.addInstruction(new Instruction(Operation.ret, return_type, "%a" + String.valueOf(se.getId())));
                }
            }
        }
        else if(this.return_type != TokenType.VOID_KW){
            throw new AnalyzeError(ErrorCode.ReturnTypeWrong, peek().getStartPos());
        }
        expect(TokenType.SEMICOLON);
        expr_stack.reset();
    }

    private int analyseIfStmt(int level, boolean in_else_if) throws CompileError{
        expect(TokenType.IF_KW);
        onCond = true;
        int if_label = var_index;
        if(in_else_if){
            this.addInstruction(new Instruction(Operation.label, "a" + String.valueOf(var_index++)));
        }
        SymbolEntry type = analyseExpr();
        onCond = false;
        this.addAllInstruction(expr_stack.addAndReset(type.getTokenType(), var_index));
        this.var_index = this.expr_stack.getVarIndex();

        this.addAllInstruction(expr_stack.beforeGetTopNum(var_index, TokenType.I1));
        SymbolEntry se = this.expr_stack.getTopNum();
        var_index = expr_stack.getVarIndex();

        Instruction br_i1 = new Instruction(Operation.br_i1);
        if(se.getType() == SymbolType.Iteral){
            br_i1.setSaveAt(String.valueOf(se.getValue()));
        }
        else{
            br_i1.setSaveAt(se.getName());
        }
        if(!in_else_if) {
            if_label = var_index;
        }
        br_i1.setOp1("%a" + var_index);
        this.addInstruction(br_i1);
        this.addInstruction(new Instruction(Operation.label, "a" + String.valueOf(var_index++)));
        List<Instruction> br_list = new ArrayList<>();
        br_list.add(new Instruction(Operation.br, "%a" + String.valueOf(0)));

        //分析if中的语句
        if(check(TokenType.L_BRACE) ){
            Block(null, level + 1);
        }
        else{
            this.def_table.level = level + 1;
            BlockItem(return_type, level + 1);
            this.def_table.levelDown();
            while(check(TokenType.SEMICOLON)){
                expect(TokenType.SEMICOLON);
            }
            expr_stack.reset();
        }
        this.addInstruction(br_list.get(0));
        Instruction br;
        int bri1_label2;
        while(check(TokenType.ELSE_KW)){
            expect(TokenType.ELSE_KW);
            if(check(TokenType.IF_KW)){
                // else if，递归调用if分析
                bri1_label2 = analyseIfStmt(level, true);
                br = new Instruction(Operation.br);
                br_list.add(br);
                this.addInstruction(br);
            }
            else{
                // else 语句
                bri1_label2 = var_index;
                addInstruction(new Instruction(Operation.label, "a" + String.valueOf(var_index++)));
                if(check(TokenType.L_BRACE) ){
                    Block(null, level + 1);
                }
                else{
                    this.def_table.level = level + 1;
                    BlockItem(return_type, level + 1);
                    this.def_table.levelDown();
                    while(check(TokenType.SEMICOLON)){
                        expect(TokenType.SEMICOLON);
                    }
                    expr_stack.reset();
                }
                br = new Instruction(Operation.br);
                br_list.add(br);
                this.addInstruction(br);
            }
            if(br_i1.getOp2() == null){
                br_i1.setOp2("%a" + bri1_label2);
            }
        }
        int end_label = var_index;
        addInstruction(new Instruction(Operation.label, "a" + String.valueOf(var_index++)));
        for(Instruction b: br_list) {
            b.setOp1("%a" + end_label);
        }
        if(br_i1.getOp2() == null){
            br_i1.setOp2("%a" + end_label);
        }
        return if_label;
    }

    private void analyseFunctionParamList() throws CompileError{
        analyseFunctionParam();
        while(check(TokenType.COMMA)){
            expect(TokenType.COMMA);
            analyseFunctionParam();
        }

    }

    private void analyseFunctionParam() throws CompileError{
        boolean is_const = false;
        if(check(TokenType.CONST_KW)){
            expect(TokenType.CONST_KW);
            is_const = true;
        }
        Token nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        Token type = expect(TokenType.VOID_KW, TokenType.INT_KW, TokenType.DOUBLE_KW);
        functionAddParam(type.getTokenType(), nameToken.getValueString(), nameToken.getStartPos(), is_const);
    }

    // level == 0是全局
    private void Decl(int level) throws CompileError{
        if(check(TokenType.INT_KW)){
            VarDecl(level);
        } else if (check(TokenType.CONST_KW)){
            ConstDecl(level);
        }
    }

    // level == 0是全局
    private void ConstDecl(int level) throws CompileError{
        expect(TokenType.CONST_KW);
//        expect(TokenType.COLON);
        Token type = expect(TokenType.INT_KW);
        Token nameToken = expect(TokenType.IDENT);
        if(this.def_table.getSymbol(nameToken.getValueString()) != null){
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, nameToken.getStartPos());
        }
        expect(TokenType.ASSIGN);
        this.onAssign = true;
        SymbolEntry se;
        if(level == 0){// 全局
            int global_id = this.def_table.addGlobal(nameToken.getValueString(), type.getTokenType(), true, true, nameToken.getStartPos(), null);
            se = def_table.getGlobal(nameToken.getValueString());
            this.global_instructions.add(new Instruction(Operation.globa, String.valueOf(global_id)));
        }
        else{
            se = functionAddLocal(type.getTokenType(),nameToken.getValueString(), true, true, nameToken.getStartPos(), level);
            this.addInstruction(new Instruction(Operation.alloca, TokenType.INT_KW, null,null, "%a" + String.valueOf(se.getId())));
        }
        SymbolEntry tt = analyseExpr();
        if(tt.getTokenType() != type.getTokenType()){
            throw new AnalyzeError(ErrorCode.ExprTypeWrong, peek().getStartPos());
        }
        this.addAllInstruction(this.expr_stack.addAndReset(tt.getTokenType(), var_index));
        this.var_index = this.expr_stack.getVarIndex();
        this.addAllInstruction(expr_stack.beforeGetTopNum(var_index, TokenType.INT_KW));
        this.addInstruction(new Instruction(Operation.store, this.expr_stack.getTopNum().getTokenType(), this.expr_stack.getTopNum().getName(), "i32", "%a" + se.getId()));
        var_index = expr_stack.getVarIndex();
        this.onAssign = false;
        expect(TokenType.SEMICOLON);
        expr_stack.reset();
    }

    private void VarDecl(int level) throws CompileError{
        expect(TokenType.INT_KW);
        VarDef(level, TokenType.INT_KW);
        while(check(TokenType.COMMA)) {
            expect(TokenType.COMMA);
            VarDef(level, TokenType.INT_KW);
        }
        expect(TokenType.SEMICOLON);
        expr_stack.reset();
    }

    private void VarDef(int level, TokenType type) throws CompileError {
        Token nameToken = expect(TokenType.IDENT);
        SymbolEntry se;
        if(check(TokenType.ASSIGN)){
            expect(TokenType.ASSIGN);
            this.onAssign = true;
            if(level == 0){
                // 全局变量
                int global_id = this.def_table.addGlobal(nameToken.getValueString(), type, true, false, nameToken.getStartPos(), null);
                se = def_table.getGlobal(nameToken.getValueString());
                this.global_instructions.add(new Instruction(Operation.globa, String.valueOf(global_id)));
            }
            else{
                // 局部变量
                se = functionAddLocal(type, nameToken.getValueString(), true, false, nameToken.getStartPos(), level);
                this.addInstruction(new Instruction(Operation.alloca, TokenType.INT_KW, null,null, "%a" + String.valueOf(se.getId())));
            }
            SymbolEntry tt = analyseExpr();
            if(tt.getTokenType() != type){
                System.out.println(tt + "  " + type);
                throw new AnalyzeError(ErrorCode.ExprTypeWrong, peek().getStartPos());
            }
            this.addAllInstruction(this.expr_stack.addAndReset(tt.getTokenType(), var_index));
            this.var_index = this.expr_stack.getVarIndex();
            this.addAllInstruction(expr_stack.beforeGetTopNum(var_index, TokenType.INT_KW));
            this.addInstruction(new Instruction(Operation.store, this.expr_stack.getTopNum().getTokenType(), this.expr_stack.getTopNum().getName(), "i32", "%a" + se.getId()));
            var_index = expr_stack.getVarIndex();
            this.onAssign = false;
        }
        else{
            if(level == 0){
                // 全局变量
                this.def_table.addGlobal(nameToken.getValueString(), type, true, false, nameToken.getStartPos(), null);
            }
            else{
                // 局部变量
                se = functionAddLocal(type,nameToken.getValueString(), false, false, nameToken.getStartPos(), level);
                this.addInstruction(new Instruction(Operation.alloca, TokenType.INT_KW, null,null, "%a" + String.valueOf(se.getId())));
            }
        }
    }
    //    private void analyseContinueStmt() throws CompileError{
//        expect(TokenType.CONTINUE_KW);
//        expect(TokenType.SEMICOLON);
//        if(this.while_level == 0){
//            throw new AnalyzeError(ErrorCode.OutWhile, peek().getStartPos());
//        }
//        Instruction instruction = new Instruction(Operation.br);
//        this.continue_instruction.add(new BreakAndContinue(instruction, this.function_body.size() + 1, this.while_level));
//        this.function_body.add(instruction);
//    }
//
//    private void analyseBreakStmt() throws CompileError{
//        expect(TokenType.BREAK_KW);
//        if(this.while_level == 0){
//            throw new AnalyzeError(ErrorCode.OutWhile, peek().getStartPos());
//        }
//        Instruction instruction = new Instruction(Operation.br);
//        this.break_instruction.add(new BreakAndContinue(instruction, this.function_body.size() + 1, this.while_level));
//        this.function_body.add(instruction);
//
//        expect(TokenType.SEMICOLON);
//        expr_stack.reset();
//    }

//    private void analyseWhileStmt(int level) throws CompileError{
//        // 函数里的第一个while level为1
//        expect(TokenType.WHILE_KW);
//        this.addInstruction(new Instruction(Operation.br, 0));
//
//        // start，记录开始计算while条件的指令位置
//        int start = this.function_body.size();
//        SymbolEntry type = analyseExpr();
//        this.addAllInstruction(expr_stack.addAndReset(type.getTokenType()));
//
//        if(type.getTokenType() != TokenType.INT_KW && type.getTokenType() != TokenType.DOUBLE_KW){
//            throw new AnalyzeError(ErrorCode.ExprTypeWrong, peek().getStartPos());
//        }
//
//        // br_true，如果是真的话跳过br指令，如果是假的话跳到br指令跳出循环
//        this.addInstruction(new Instruction(Operation.br_true, 1));
//
//        //br，跳出循环体，参数待填
//        Instruction br = new Instruction(Operation.br);
//        this.addInstruction(br);
//
//        // 记录while循环体开始处
//        int index = this.function_body.size();
//
//        this.while_level++;
//        Block(null, level + 1);
//        if(this.while_level > 0){
//            this.while_level--;
//        }
//
//        // br_start，跳到while条件判断处，参数待填
//        Instruction br_start = new Instruction(Operation.br);
//        this.addInstruction(br_start);
//        br_start.setOp1(start - this.function_body.size());
//
//        int end_index = this.function_body.size();
//        br.setOp1(end_index - index);
//
//        if(break_instruction.size()!=0){
//            for(BreakAndContinue b: break_instruction){
//                if(b.getWhileLevel() == this.while_level + 1)
//                    b.getInstruction().setOp1(end_index - b.getLocation());
//            }
//        }
//
//        if(continue_instruction.size() != 0){
//            for(BreakAndContinue c: continue_instruction){
//                if(c.getWhileLevel() == this.while_level + 1)
//                    c.getInstruction().setOp1(end_index - c.getLocation() - 1);
//            }
//        }
//
//        // 重新初始化
//        if(this.while_level == 0){
//            this.continue_instruction = new ArrayList<>();
//            this.break_instruction = new ArrayList<>();
//        }
//    }
}
