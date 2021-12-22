package symbol;

public enum SymbolType {
    Global, Param, Local, Function, Iteral, Var,/*编译变量*/
    ;

    @Override
    public String toString() {
        switch (this) {
            case Param:
                return "函数入参";
            case Local:
                return "局部变量";
            case Global:
                return "全局变量";
            case Function:
                return "函数名";
            case Iteral:
                return "常量值";
            case Var:
                return "编译变量";
            default:
                return "unknown symbol";
        }
    }
}