

package sg_tools;
import static sg_tools.SG_Handler.*;
import static sg_tools.Image_Utils.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;




public class SG_Utils {
  
  
  /**  File IO factory methods-
    */
  static DataInputStream inStream(
    String filename, boolean nullIfError
  ) throws IOException {
    try {
      FileInputStream FS = new FileInputStream(filename);
      BufferedInputStream BS = new BufferedInputStream(FS);
      return new DataInputStream(BS);
    }
    catch (IOException e) {
      if (nullIfError) return null;
      else throw e;
    }
  }
  
  
  static DataOutputStream outStream(
    String filename, boolean nullIfError
  ) throws IOException {
    try {
      FileOutputStream FS = new FileOutputStream(filename);
      BufferedOutputStream BS = new BufferedOutputStream(FS);
      return new DataOutputStream(BS);
    }
    catch (IOException e) {
      if (nullIfError) return null;
      else throw e;
    }
  }
  
  
  static FileWriter writerOutStream(
    String filename, boolean nullIfError
  ) throws IOException {
    try {
      return new FileWriter(filename);
    }
    catch (IOException e) {
      if (nullIfError) return null;
      else throw e;
    }
  }
  
  
  
  /**  Utility methods for accessing records:
    */
  static Bitmap bitmapWithLabel(File_SG file, String label) {
    for (Bitmap b : file.bitmaps) if (b.name.equals(label)) {
      return b;
    }
    return null;
  }
  
  
  static ImageRecord recordWithLabel(File_SG file, String label) {
    for (ImageRecord r : file.records) if (r.label.equals(label)) {
      return r;
    }
    return null;
  }
  
  
  
  /**  Main usage routines:
    */
  static void unpackSG(
    String basePath, String fileSG, int version, String outputPath
  ) {
    try {
      //
      //  First, read the main SG file and create a blank directory for
      //  subsequent output-
      say("\nReading main .SG file: "+basePath+fileSG);
      
      String baseFileName = noSuffix(fileSG);
      SG_Handler handler = new SG_Handler(version, false);
      handler.basePath = basePath;
      File_SG file = handler.readFile_SG(fileSG);
      
      File outDir = new File(outputPath);
      if (  outDir.exists()) wipeDirectory(outDir);
      if (! outDir.exists()) outDir.mkdirs();
      
      //
      //  Then write the main records-index:
      String outPath = outputPath+baseFileName+".xml";
      FileWriter FW = writerOutStream(outPath, false);
      
      say("  Writing header and bitmaps to "+outPath);
      
      XML asXML = handler.objectAsXML(file);
      asXML.writeToFile(FW, "");
      
      for (Bitmap writes : file.bitmaps) {
        asXML = handler.objectAsXML(writes);
        asXML.writeToFile(FW, "");
      }
      
      FW.flush();
      FW.close();
      
      //
      //  Now write sub-directories for each of the image-records in a bitmap:
      for (Bitmap map : file.bitmaps) {
        if (map.name.isEmpty()) continue;
        
        String baseMapName = noSuffix(map.name);
        File mapDir = new File(outputPath+baseMapName);
        if (! mapDir.exists()) mapDir.mkdirs();
        
        String mapFileName = outputPath+baseMapName+"/records.xml";
        FileWriter MW = writerOutStream(mapFileName, true);
        if (MW == null) continue;
        
        say("  Writing image records: "+mapFileName);
        
        for (Object writes : map.records) {
          asXML = handler.objectAsXML(writes);
          asXML.writeToFile(MW, "");
        }
        
        MW.flush();
        MW.close();
        
        for (ImageRecord record : map.records) {
          BufferedImage image = Image_Utils.extractImage(record);
          if (image == null) continue;
          String imgName = record.label+".png";
          Image_Utils.saveImage(image, outputPath+baseMapName+"/"+imgName);
        }
      }
      
      handler.closeAllFileAccess();
    }
    catch(Exception e) {
      System.out.print("Problem: "+e);
      e.printStackTrace();
    }
  }
  
  
  static void unpackSingleImage(
    String basePath, String fileSG, int version,
    String recordID, String outputPath
  ) {
    try {
      say("\nReading main .SG file: "+basePath+fileSG);
      SG_Handler handler = new SG_Handler(version, false);
      handler.basePath = basePath;
      File_SG file = handler.readFile_SG(fileSG);
      
      ImageRecord record = recordWithLabel(file, recordID);
      if (record == null) return;
      
      BufferedImage image = Image_Utils.extractImage(record);
      handler.closeAllFileAccess();
      
      if (image == null) return;
      Image_Utils.saveImage(image, outputPath);
      Image_Utils.displayImage(image);
    }
    catch(Exception e) {
      System.out.print("Problem: "+e);
      e.printStackTrace();
    }
  }
  
  
  
  /**  Main execution method-
    */
  static void testImagePacking(String... testImageIDs) {
    
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
      String outDir = "output_test/";
      File dirFile = new File(outDir);
      if (! dirFile.exists()) dirFile.mkdirs();
      
      say("\nTesting image un/packing...");
      SG_Handler handler = new SG_Handler(VERSION_C3, false);
      handler.basePath = "Caesar 3/";
      File_SG file = handler.readFile_SG("C3_North.sg2");
      
      for (String ID : testImageIDs) {
        
        ImageRecord record = recordWithLabel(file, ID);
        if (record == null) continue;
        say("\n  File: "+ID+"  Size: "+record.width+" x "+record.height);
        
        Bytes bytesIn = extractRawBytes(record);
        BufferedImage loaded = imageFromBytes(bytesIn, record);
        if (loaded == null) return;
        
        Bytes bytesOut = bytesFromImage(record, loaded);
        saveImage(loaded, outDir+ID+"_loaded.png");
        displayImage(loaded, dispX, 50);
        
        BufferedImage packed = imageFromBytes(bytesOut, record);
        saveImage(packed, outDir+ID+"_packed.png");
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
          say("  Differ at point: "+x+"|"+y+", "+inC+" -> "+outC);
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
  
  
  public static void main(String args[]) {
    final String testImageIDs[] = {
      "empire_panels_3",
      "Carts_692",
      "Govt_0",
      "Govt_9",
      "Housng1a_42",
      "Housng1a_47",
    };
    //Image_Utils.packVerbose = true;
    testImagePacking(testImageIDs);
    
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



