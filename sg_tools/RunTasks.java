

package sg_tools;
import static sg_tools.SG_Handler.*;



public class RunTasks {
  
  final static String
    C3_DIR_PATH = "C:/Program Files (x86)/GOG Galaxy/Games/Caesar 3/"
  ;
  
  public static void main(String args[]) {
    SG_UtilsIO.replaceSingleImage(
      C3_DIR_PATH, "C3.sg2", VERSION_C3,
      "Govt_9", "custom_images/custom_forum.png", "output_c3/"
    );
    
    /*
    try {
      SG_Handler handler = new SG_Handler(VERSION_C3, false);
      handler.readAllFiles_SG("Caesar 3/");
    }
    catch(Exception e) {
      System.out.print("Problem: "+e);
      e.printStackTrace();
    }
    //*/
    
    /*
    unpackSG(
      "Caesar 3/", "C3_North.sg2", VERSION_C3, "output_sg_north/"
    );
    //*/
  }
}
