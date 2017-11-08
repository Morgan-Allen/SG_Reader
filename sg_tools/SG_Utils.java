

package sg_tools;
import static sg_tools.SG_Handler.*;
import static sg_tools.Image_Utils.*;
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
  static void testImagePacking() {
    
    final String testImageIDs[] = {
      "empire_panels_3",
      "Carts_692",
      "Housng1a_42",
    };
    int dispX = 200;
    
    try {
      say("\nTesting image un/packing...");
      SG_Handler handler = new SG_Handler(VERSION_C3, false);
      handler.basePath = "Caesar 3/";
      File_SG file = handler.readFile_SG("C3_North.sg2");
      
      for (String ID : testImageIDs) {
        
        ImageRecord record = recordWithLabel(file, ID);
        if (record == null) continue;
        
        Bytes bytesIn = extractRawBytes(record);
        BufferedImage image = imageFromBytes(bytesIn, record);
        if (image == null) return;
        
        Bytes bytesOut = bytesFromImage(record, image);
        boolean result = checkPackResult(bytesIn, bytesOut);
        if (result) {
          System.out.println("Packed correctly- "+ID);
        }
        else {
          System.out.println("Did not pack correctly! "+ID);
        }
        
        Image_Utils.saveImage(image, ID+".png");
        Image_Utils.displayImage(image, dispX += 100, 50);
      }
      
      handler.closeAllFileAccess();
    }
    catch(Exception e) {
      System.out.print("Problem: "+e);
      e.printStackTrace();
    }
  }
  
  
  static boolean checkPackResult(Bytes in, Bytes out) {
    if (in.used != out.used) {
      System.out.println("Different lengths: "+in.used+" vs. "+out.used);
      return false;
    }
    for (int i = in.used; i-- > 0;) {
      int VI = in .data[i] & 0xff;
      int VO = out.data[i] & 0xff;
      if (VI != VO) {
        System.out.println("Differ at index: "+i+", "+VI+" vs. "+VO);
        return false;
      }
    }
    return true;
  }
  
  
  public static void main(String args[]) {
    testImagePacking();
    
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



