

package sg_tools;
import static sg_tools.SG_Handler.*;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;



public class RunTasks {
  
  final static String
    C3_DIR_PATH = "C:/Program Files (x86)/GOG Galaxy/Games/Caesar 3/"
  ;
  
  public static void main(String args[]) {
    
    //SG_Utils555.packVerbose = true;
    
    SG_UtilsIO.replaceSingleImage(
      C3_DIR_PATH, "C3.sg2", VERSION_C3,
      "Govt_9", "custom_images/custom_forum.png", "output_c3/"
    );
    
    BufferedImage original = SG_Utils555.loadImage(
      "custom_images/custom_forum.png"
    );
    SG_Utils555.displayImage(original, 500, 50);
    
    BufferedImage packed = SG_UtilsIO.unpackSingleImage(
      "output_c3/", "C3.sg2", VERSION_C3, "Govt_9"
    );
    SG_Utils555.displayImage(packed, 600, 50);
    
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
