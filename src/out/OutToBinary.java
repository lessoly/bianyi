package out;

import function.Function;
import instruction.Instruction;
import instruction.Operation;
import symbol.Definition;
import token.TokenType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OutToBinary {
    private Function start;
    Definition def_table;
    //  private List<Byte> output;
    private StringBuilder temp_output;
    private StringBuilder output = new StringBuilder();

    //DataOutputStream out = new DataOutputStream(new FileOutputStream(new File("src/out.txt")));

    int magic=0x72303b3e;
    int version=0x00000001;

    public OutToBinary(Definition def_table, Function start){
        this.start = start;
        this.def_table = def_table;
        temp_output = new StringBuilder();
    }

    public String generate() throws IOException {
        //globals.count
//    output.addAll(int2bytes(4, def_table.getGlobalListCount()));
        System.out.println("globals.count: " + def_table.getGlobalListCount());

        System.out.println("-----------------输出global数组----------------");
        int i = 0;
//        for(SymbolEntry g : def_table.getGlobalList()){
//            System.out.println(i++ + "     -----------");
//            //is_const
//            List<Byte> is_const = int2bytes(1, g.isConstant().compareTo(false));
//            output.addAll(is_const);
//            System.out.println("index: " + g.getId());
//            System.out.println("is_const: " + g.isConstant().compareTo(false));
//
//            List<Byte> global_value_count;
//            List<Byte> global_value;
//
//            if (g.getValue() == null) {
//                global_value_count = int2bytes(4, 8);
//                global_value = long2bytes(8,0);
//            }
//            else {
//                global_value = String2bytes(g.getValue().toString());
//                global_value_count = int2bytes(4, global_value.size());
//            }
//
//            output.addAll(global_value_count);
//            output.addAll(global_value);
//        }

        //functions.count
        List<Byte> functionsCount=int2bytes(4, this.def_table.getFunctionListCount());
//        output.addAll(functionsCount);
        System.out.println("function count: " + this.def_table.getFunctionListCount());

        System.out.println("-----------------输出function数组----------------");
//        generateFunction(start);
        i = 1;
        List<Function> func_list = new ArrayList<>();
        System.out.println("-----------------函数定义----------------");
        for (String name: def_table.getFunctionList().keySet()) {
            if(!name.equals("_start")){
                Function func = def_table.getFunctionList().get(name);
                if(!func.isSTDFunction()){
                    func_list.add(func);
                }
                if(!func.getName().equals("main")) {
                    temp_output.append("declare ");
                    temp_output.append(func.getReturnType() == TokenType.INT_KW ? "i32 " : "void ");
                    temp_output.append("@").append(func.getName());
                    temp_output.append("(").append(func.getParamToString()).append(")").append("\n");
                }
            }
        }
        System.out.println(temp_output);
        output.append(temp_output);
        temp_output.delete( 0, temp_output.length() );

        Collections.sort(func_list);
        for(Function f: func_list){
            System.out.println(i++ + "个函数     -------------");
            if(!f.isSTDFunction())
                generateFunction(f);
        }


        return output.toString();
    }

    private void generateStart(Function start_func){

    }

    private void generateFunction(Function function){
        temp_output.append("define dso_local ");
        temp_output.append(function.getReturnType() == TokenType.INT_KW ? "i32 " : "void ");
        //name
//        List<Byte> name = int2bytes(4, function.getId());
        temp_output.append("@").append(function.getName());
//        System.out.println("function name: " + function.getName());
        //out.writeBytes(name.toString());

        //retSlot
//        List<Byte> retSlots = int2bytes(4, function.getReturnSlot());
//        temp_output.addAll(retSlots);
//        System.out.println("function return slot: " + function.getReturnSlot());
//        //out.writeBytes(retSlots.toString());

        //paramsSlots
//        List<Byte> paramsSlots=int2bytes(4, function.getParamSlot());
        temp_output.append(function.getParamNameToString());
        temp_output.append("{\n");
//        System.out.println("function param slot: " + function.getParamSlot());

        //locSlots
//        List<Byte> locSlots=int2bytes(4, function.getLocalSlot());
//        temp_output.addAll(locSlots);
//        System.out.println("function local slot: " + function.getLocalSlot());

        List<Instruction> ins = function.getFunctionBody();

        //bodyCount
//        List<Byte> bodyCount=int2bytes(4, ins.size());
//        temp_output.addAll(bodyCount);
//        System.out.println("function body count: " + ins.size());

        //body
        int cc = 1;
        for(Instruction i : ins){
            //type
//            List<Byte> type = int2bytes(1, i.getCode());
            if(i.getOpt() != Operation.label){
                temp_output.append("    ").append(i.toString()).append("\n");
                System.out.println("    " + i.toString());
            }
            else{
                temp_output.append(i.toString()).append("\n");
                System.out.println(i.toString());
            }
//            temp_output.append("    " + i.getOpt()).append(" ").append(i.getType()).append(" ").append(i.getOp1()).append(" ").append(i.getOp2()).append("\n");
        }
        temp_output.append("}\n\n");
//        System.out.println(temp_output);
        output.append(temp_output);
        temp_output.delete( 0, temp_output.length() );
    }

    private List<Byte> Char2bytes(char value) {
        List<Byte>  AB=new ArrayList<>();
        AB.add((byte)(value&0xff));
        return AB;
    }

    private List<Byte> String2bytes(String valueString) {
        List<Byte>  AB=new ArrayList<>();
        for (int i=0;i<valueString.length();i++){
            char ch=valueString.charAt(i);
            AB.add((byte)(ch&0xff));
        }
        return AB;
    }

    private List<Byte> long2bytes(int length, long value) {
        ArrayList<Byte> bytes = new ArrayList<>();
        int start = 8 * (length-1);
        for(int i = 0 ; i < length; i++){
            bytes.add((byte) (( value >> ( start - i * 8 )) & 0xFF ));
        }
        return bytes;
    }

    /*
     * length 长度
     * target 值
     */
    private ArrayList<Byte> int2bytes(int length, int target){
        ArrayList<Byte> bytes = new ArrayList<>();
        int start = 8 * (length-1);
        for(int i = 0 ; i < length; i++){
            bytes.add((byte) (( target >> ( start - i * 8 )) & 0xFF ));
        }
        return bytes;
    }
}
