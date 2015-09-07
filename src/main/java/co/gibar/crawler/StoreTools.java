package co.gibar.crawler;

import java.io.*;

/**
 * Created by abola on 2015/9/4.
 */
public class StoreTools {

    // http://www.codeproject.com/Tips/315892/A-quick-and-easy-way-to-direct-Java-System-out-to

    public static Boolean startStdoutTo(String path){
        try {
            new File( path.substring(0, path.lastIndexOf("/"))  ) . mkdir();

            System.setOut(new PrintStream(new FileOutputStream(path, true)));
            return true;
        }catch(Exception ex){
            return false;
        }
    }

    public static void resetStdout(){
        System.out.flush();
        try {
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
}
