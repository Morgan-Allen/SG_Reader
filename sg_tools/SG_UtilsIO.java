

package sg_tools;
import static sg_tools.SG_Handler.*;
import static sg_tools.SG_Utils555.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.ImageIO;




public class SG_UtilsIO {
  
  
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
          BufferedImage image = SG_Utils555.extractImage(record);
          if (image == null) continue;
          String imgName = record.label+".png";
          SG_Utils555.saveImage(image, outputPath+baseMapName+"/"+imgName);
        }
      }
      
      handler.closeAllFileAccess();
    }
    catch(Exception e) {
      System.out.print("Problem: "+e);
      e.printStackTrace();
    }
  }
  
  
  static BufferedImage unpackSingleImage(
    String basePath, String fileSG, int version, String recordID
  ) {
    try {
      say("\nReading main .SG file: "+basePath+fileSG);
      SG_Handler handler = new SG_Handler(version, false);
      handler.basePath = basePath;
      File_SG file = handler.readFile_SG(fileSG);
      
      ImageRecord record = recordWithLabel(file, recordID);
      if (record == null) return null;
      
      BufferedImage image = extractImage(record);
      handler.closeAllFileAccess();
      if (image == null) return null;
      
      return image;
    }
    catch(Exception e) {
      System.out.print("Problem: "+e);
      e.printStackTrace();
      return null;
    }
  }
  
  
  static void replaceSingleImage(
    String basePath, String fileSG, int version,
    String recordID, String newImagePath, String outputDir
  ) {
    try {
      say("\nReplacing image record: "+recordID+" in "+basePath+fileSG);
      
      SG_Handler handler = new SG_Handler(version, false);
      handler.basePath = basePath;
      File_SG file = handler.readFile_SG(fileSG);
      
      ImageRecord record = recordWithLabel(file, recordID);
      if (record == null) return;
      
      //  TODO:  Automate this bit.
      int offset = record.offset, length = record.dataLength;
      if (record.externalData) offset -= 1;
      BufferedImage image = ImageIO.read(new File(newImagePath));
      if (image == null) return;
      
      Bytes asBytes = bytesFromImage(
        record, image
      );
      replaceImageBytes(
        offset, length, asBytes.data, record.file, outputDir
      );
    }
    catch(Exception e) {
      System.out.print("Problem: "+e);
      e.printStackTrace();
    }
  }
}



