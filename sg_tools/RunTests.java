
package sg_tools;
import static sg_tools.SG_UtilsIO   .*;
import static sg_tools.SG_Utils555.*;
import static sg_tools.SG_Handler .*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;



public class RunTests {
  
  /**  Testing methods-
    */
  static void testImagePacking(
    String basePath, String outputDir, String fileSG, int version,
    String... testImageIDs
  ) {
    try {
      //
      //  Have to test basic byte un/packing first (note that the 16th bit of
      //  the short for a pixel is always filled....)
      Bytes testIn  = new Bytes(4);
      Bytes testOut = new Bytes(4);
      for (int i = 4; i-- > 0;) testIn.data[i] = (byte) (Math.random() * 256);
      testIn.data[1] |= 1 << 7;
      testIn.data[3] |= 1 << 7;
      testIn.used = testOut.used = 4;
      int pixels[] = {
        bytesToARGB(testIn, 0),
        bytesToARGB(testIn, 2)
      };
      ARGBtoBytes(pixels[0], testOut, 0);
      ARGBtoBytes(pixels[1], testOut, 2);
      
      say("\nTesting byte-level conversions...");
      String inB = "  In: ", outB = "  Out:";
      for (int i = 0; i < 32; i++) {
        if (i % 8 == 0) { inB += " "; outB += " "; }
        inB  += bitAt(i % 8, testIn .data[i / 8] & 0xff);
        outB += bitAt(i % 8, testOut.data[i / 8] & 0xff);
      }
      say(inB );
      say(outB);
      
      boolean basicOK = checkPackingSame(testIn, testOut);
      if (basicOK) {
        say("  Bytes packed correctly.");
      }
      else {
        say("  Bytes not packed correctly!");
      }
      
      //
      //  Then, we test the full range of test images supplied:
      int dispX = 200;
      File dirFile = new File(outputDir);
      if (! dirFile.exists()) dirFile.mkdirs();
      
      say("\nTesting image un/packing...");
      SG_Handler handler = new SG_Handler(version, false);
      handler.basePath = basePath;
      File_SG file = handler.readFile_SG(fileSG);
      
      for (String ID : testImageIDs) {
        
        ImageRecord record = recordWithLabel(file, ID);
        if (record == null) continue;
        
        String sizeDesc  = "  Size: "+record.width+" x "+record.height;
        String bytesDesc = "  Bytes: "+record.dataLength;
        say("\n  Image: "+ID+sizeDesc+bytesDesc);
        
        Bytes bytesIn = extractRawBytes(record);
        BufferedImage loaded = imageFromBytes(bytesIn, record);
        if (loaded == null) return;
        
        Bytes bytesOut = bytesFromImage(record, loaded);
        saveImage(loaded, outputDir+ID+"_loaded.png");
        displayImage(loaded, dispX, 50);
        
        BufferedImage packed = imageFromBytes(bytesOut, record);
        saveImage(packed, outputDir+ID+"_packed.png");
        displayImage(packed, dispX, 200);
        
        dispX += 100;
        
        boolean imgSame = checkImagesSame(loaded, packed);
        if (imgSame) {
          say("  Displayed images are identical.");
        }
        else {
          say("  Displayed images do not match.");
        }
        
        boolean packSame = checkPackingSame(bytesIn, bytesOut);
        if (packSame) {
          say("  Bytes packed identically.");
        }
        else {
          say("  Did not pack bytes identically.");
        }
      }
      
      handler.closeAllFileAccess();
    }
    catch(Exception e) {
      System.out.print("Problem: "+e);
      e.printStackTrace();
    }
  }
  
  
  static int bitAt(int index, int number) {
    return ((number & (1 << index)) == 0) ? 0 : 1;
  }
  
  
  static boolean checkImagesSame(BufferedImage in, BufferedImage out) {
    boolean allOK = true;
    
    if (in.getWidth() != out.getWidth()) {
      say("Different width: "+in.getWidth()+" -> "+out.getWidth());
      allOK = false;
    }
    if (in.getHeight() != out.getHeight()) {
      say("Different height: "+in.getHeight()+" -> "+out.getHeight());
      allOK = false;
    }
    int maxWide = Math.min(in.getWidth (), out.getWidth ());
    int maxHigh = Math.min(in.getHeight(), out.getHeight());
    
    pixLoop: for (int y = 0; y < maxHigh; y++) {
      for (int x = 0; x < maxWide; x++) {
        int VI = in .getRGB(x, y);
        int VO = out.getRGB(x, y);
        if (VI != VO) {
          Color inC  = new Color(VI);
          Color outC = new Color(VO);
          say("  Differ at point: "+x+"|"+y);
          say("    In value:  "+VI+" "+inC );
          say("    Out value: "+VO+" "+outC);
          allOK = false;
          break pixLoop;
        }
      }
    }
    return allOK;
  }
  
  
  static boolean checkPackingSame(Bytes in, Bytes out) {
    boolean allOK = true;
    
    if (in.used != out.used) {
      say("  Different lengths: "+in.used+" -> "+out.used);
      allOK = false;
    }
    
    int maxIndex = Math.min(in.used, out.used);
    for (int i = 0; i <  maxIndex; i++) {
      int VI = in .data[i] & 0xff;
      int VO = out.data[i] & 0xff;
      if (VI != VO) {
        say("  Differ at index: "+i+", "+VI+" -> "+VO);
        allOK = false;
        break;
      }
    }
    return allOK;
  }
  
  
  static void testImageSubstitution(
    String basePath, String fileSG, int version,
    String recordID, String savePath, String outputDir,
    String... testFilenames
  ) {
    say("\nTesting file-record substitution...");
    
    BufferedImage extract = unpackSingleImage(
      basePath, fileSG, version,
      recordID
    );
    saveImage(extract, savePath);
    replaceSingleImage(
      basePath, fileSG, version,
      recordID, savePath, outputDir
    );
    
    //  Now verify that the SG files in question are identical-
    for (String filename : testFilenames) {
      boolean same = testFilesSame(basePath, outputDir, filename);
      if (same) {
        say("  "+filename+" identical in output directory.");
      }
      else {
        say("  "+filename+" is not identical.");
      }
    }
  }
  
  
  static boolean testFilesSame(
    String basePath, String outputDir, String filename
  ) {
    try {
      File original = new File(basePath +filename);
      File rewrite  = new File(outputDir+filename);
      
      Bytes bytesO = new Bytes((int) original.length());
      Bytes bytesR = new Bytes((int) rewrite .length());
      
      DataInputStream inO = inStream(basePath +filename, false);
      DataInputStream inR = inStream(outputDir+filename, false);
      
      bytesO.used = inO.read(bytesO.data);
      bytesR.used = inR.read(bytesR.data);
      
      inO.close();
      inR.close();
      
      return checkPackingSame(bytesO, bytesR);
    }
    catch (Exception e) {
      return false;
    }
  }
  
  
  
  /**  Basic test-suite...
    */
  public static void main(String args[]) {
    
    final String testImageIDs[] = {
      "empire_panels_3",
      "Carts_692",
      "Govt_0",
      "Govt_9",
      "Housng1a_42",
      "Housng1a_47",
    };
    testImagePacking(
      "Caesar 3/", "output_test/", "C3.sg2", VERSION_C3, testImageIDs
    );
    
    testImageSubstitution(
      "Caesar 3/", "C3.sg2", VERSION_C3,
      "Housng1a_42", "output_test/temp_house_42.png", "output_test/",
      "C3.sg2",
      "C3.555"
    );
  }
}


