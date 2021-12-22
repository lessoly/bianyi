package symbol;

import token.TokenType;

public class SymbolEntry {
    // 全局变量、函数、参数、局部变量
    SymbolType type;
    // 程序变量的话，为%原名，编译变量为%int
    String name;
    // 变量的tt是变量的类型，函数的tt是函数的返回值类型
    TokenType tt;
    // 是否初始化
    boolean is_initialized;
    // 是否是常量
    boolean is_const;
    Object value;
    int level;
    int id;

    public SymbolEntry(int id, SymbolType type, String name, TokenType tt, boolean is_initialized, boolean is_const, Object value, int level) {
        this.id = id;   // 为全局变量的id，函数在全局变量中对应的id，局部变量在函数表中对应的id，参数在函数表中对应的id，根据ST不同
        this.type = type;
        this.name = name;
        this.tt = tt;
        this.is_initialized = is_initialized;
        this.is_const = is_const;
        this.value = value;
        this.level = level;
    }

    /**
     * 用于num_ilteral
     * @param type SE.Iteral
     * @param tt
     * @param value
      */
    public SymbolEntry(SymbolType type, TokenType tt, Object value) {
        this.type = type;
        this.tt = tt;
        this.value = value;
    }

    /**
     * 用于编译变量
     * @param type SE.Var
     * @param tt TT.int_kw
     * @param name %int
     */
    public SymbolEntry(SymbolType type, TokenType tt, String name) {
        this.type = type;
        this.tt = tt;
        this.name = name;
    }

    public int getId() { return this.id;}

    public String getName() {
        if(type == SymbolType.Iteral){
            return String.valueOf(value);
        }
        return name;
    }

    public void setName(String name) { this.name = name; }

    public Object getValue() { return value; }

    public void setValue(Object value) { this.value = value; }

    public int getLevel() { return level; }

    public void setLevel(int level) { this.level = level; }

    /**
     * @return the isInitialized
     */
    public boolean isInitialized() {
        return is_initialized;
    }

    public void setType(SymbolType type) {
        this.type = type;
    }

    public void setTokenType(TokenType tt) { this.tt = tt; }

    public SymbolType getType() { return this.type; }

    public TokenType getTokenType() { return this.tt; }

    public Boolean isConstant() {
        return this.is_const;
    }

    public Boolean isFunction() {
        return this.type == SymbolType.Function;
    }

    // 判断是不是编译变量，如果不是，就需要load
    public Boolean isVar() {
        return type == SymbolType.Var;
    }

    public Boolean isIteral(){
        return type == SymbolType.Iteral;
    }

    /**
     * @param isInitialized the isInitialized to set
     */
    public void setInitialized(boolean isInitialized) {
        this.is_initialized = isInitialized;
    }

    public void setId(int id) { this.id = id; }
}
