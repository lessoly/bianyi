import analyse.Analyser;
import error.CompileError;
import out.OutToBinary;
import token.StringIter;
import token.Token;
import token.TokenType;
import token.Tokenizer;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class App {
    public static void main(String[] args) throws IOException, CompileError {

//        try {
        InputStream input;
        PrintStream output = new PrintStream("out.txt");
        PrintStream out;
        System.out.println("args[0]: " + args[0] + ", args[1]: " + args[1]);
        String inputFileName = args[0], outputFileName = args[1];
        input = new FileInputStream(inputFileName);
        out = new PrintStream(new FileOutputStream(outputFileName));


        Scanner scanner;
        scanner = new Scanner(input);
        while(scanner.hasNext()){
            System.out.println(scanner.nextLine());
        }
        System.out.println("------------start-----------");

        input = new FileInputStream(inputFileName);
        scanner = new Scanner(input);
        StringIter iter = new StringIter(scanner);
        Tokenizer tokenizer = tokenize(iter);

//        List<Token> tokens = new ArrayList<>();
//        try {
//          while (true) {
//            Token token = tokenizer.nextToken();
//            if (token.getTokenType().equals(TokenType.EOF)) {
//              break;
//            }
//            tokens.add(token);
//          }
//        } catch (Exception e) {
//          // 遇到错误不输出，直接退出
//          System.err.println(e);
//          System.exit(-1);
//          return;
//        }
//        for (Token token : tokens) {
//          output.println(token.toString());
//        }

        Analyser analyzer = new Analyser(tokenizer);
        analyzer.analyse();

        OutToBinary outPutBinary=new OutToBinary(analyzer.def_table, analyzer.getStartFunction());
        String bs = outPutBinary.generate();
//        byte[] temp=new byte[bs.size()];
//        for(int i=0;i<bs.size();i++)
//            temp[i]=bs.get(i);
//        out.write(temp);
        output.println(bs);
        out.println(bs);
    }

    private static Tokenizer tokenize(StringIter iter) {
        return new Tokenizer(iter);
    }


}
