package instruction;

public enum Operation {
//    ILL, LIT, LOD, STO, ADD, SUB, MUL, DIV, WRT,
    nop(0x00, 0), push(0x01, 8), pop(0x02, 0), popn(0x03, 4), dup(0x04, 0),
    loca(0x0a, 4), alloca(0x0b, 4), globa(0x0c, 4), load8(0x10, 0), load16(0x11, 0),
    load32(0x12, 0), load(0x13, 0), store8(0x14, 0), store16(0x15, 0), store32(0x16, 0), store(0x17, 0),
    alloc(0x18, 0), free(0x19, 0), stackalloc(0x1a, 4), add(0x20, 0), sub(0x21, 0),
    mul(0x22, 0), sdiv(0x23, 0), add_f(0x24, 0), sub_f(0x25, 0), mul_f(0x26, 0),
    div_f(0x27, 0), div_u(0x28, 0), shl(0x29, 0), shr(0x2a, 0), and(0x2b, 0),
    or(0x2c, 0), xor(0x2d, 0), not(0x2e, 0), cmp_u(0x31, 0), cmp_f(0x32, 0),
    neg_i(0x34, 0), neg_f(0x35, 0), itof(0x36, 0), ftoi(0x37, 0), shrl(0x38, 0),
    slt(0x39, 0), sgt(0x3a, 0), br(0x41, 4), br_false(0x42, 4), br_i1(0x43, 4),
    call(0x48, 4), ret(0x49, 0), callname(0x4a, 4), label(0xff, 0),
    icmp_sle(0x30, 0), icmp_slt(0x30, 0), icmp_eq(100, 0), icmp_ne(101, 0),
    icmp_sge(0, 0), icmp_sgt(0, 0);

    private Integer size;

    Operation(int code, int size) {
        this.size = size;
    }
    public int getSize() { return this.size; }
}
