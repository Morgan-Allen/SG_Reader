

import java.io.*;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.awt.image.BufferedImage;




public class File_SG {
  
  
  /**  Data structures:
    */
  
  //  80 bytes total here.
  static class Header {
    File_SG file;
    
    int filesize;
    int version;
    int unknown1;
    int numRecords;
    int recordsUsed;
    int unknown2;
    
    int totalFilesize;
    int inner555Size;
    int outer555Size;
    
    int remainder[] = new int[11];
    
    short  index  [] = new short [300];
    Bitmap bitmaps[] = new Bitmap[100];
    ImageRecord records[];
  }
  
  //  200 bytes total here.
  static class Bitmap {
    File_SG file;
    
    byte nameChars   [] = new byte[65];
    byte commentChars[] = new byte[51];
    String name;
    String comment;
    
    int width;
    int height;
    int numImages;
    int startIndex;
    int endIndex;
    
    byte remainder[] = new byte[64];
    
    ArrayList <ImageRecord> records = new ArrayList <ImageRecord> ();
    public String toString() { return name; }
  }
  
  //  64 bytes total here.
  static class ImageRecord {
    File_SG file;
    BufferedImage extracted;
    
    Bitmap belongs;
    String label;
    
    int offset;
    int dataLength;
    int lengthNoComp;
    int unknown1;
    int inverseOffset;
    
    short width;
    short height;
    byte unknown2[] = new byte[6];
    short numAnims;
    short unknown3;
    short spriteOffX;
    short spriteOffY;
    byte unknown4[] = new byte[10];
    
    boolean canReverse;
    byte unknown5;
    byte imageType;
    boolean compressed;
    boolean externalData;
    boolean partCompressed;
    byte unknown6[] = new byte[2];
    byte bitmapID;
    byte unknown7;
    byte animSpeedID;
    byte unknown8[] = new byte[5];
  }
  
  
  /**  File IO/routines-
    */
  static DataInputStream inStream(
    String filename, boolean nullIfError
  ) throws Exception {
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
  
  
  static RandomAccessFile randomInStream(
    String filename, boolean nullIfError
  ) throws Exception {
    try {
      return new RandomAccessFile(filename, "r");
    }
    catch (IOException e) {
      if (nullIfError) return null;
      else throw e;
    }
  }
  
  
  static FileWriter writerOutStream(
    String filename, boolean nullIfError
  ) throws Exception {
    try {
      return new FileWriter(filename);
    }
    catch (IOException e) {
      if (nullIfError) return null;
      else throw e;
    }
  }
  
  
  
  /**  File parsing routines-
    */
  String filename;
  DataInputStream IS;
  boolean verbose;
  Header header;
  
  
  void readFile(String filename, boolean verbose) throws Exception {
    this.filename = filename;
    this.verbose  = verbose ;
    report("\nReading SG File: "+filename);
    
    IS = inStream(filename, false);
    
    Header h = this.header = new Header();
    h.file          = this;
    h.filesize      = readInt();
    h.version       = readInt();
    h.unknown1      = readInt();
    h.numRecords    = readInt();
    h.recordsUsed   = readInt();
    h.unknown2      = readInt();
    h.totalFilesize = readInt();
    h.inner555Size  = readInt();
    h.outer555Size  = readInt();
    for (int i = 0; i < h.remainder.length; i++) {
      h.remainder[i] = readInt();
    }
    
    report("");
    report("  File size: "+h.filesize   );
    report("  Version:   "+h.version    );
    report("  # Records: "+h.numRecords );
    report("  # Used:    "+h.recordsUsed);
    report("  Total file size: "+h.totalFilesize);
    report("  Inner 555 size:  "+h.inner555Size );
    report("  Outer 555 size:  "+h.outer555Size );
    report("");
    
    for (int i = 0; i < h.index.length; i++) {
      h.index[i] = readShort();
    }
    
    for (int i = 0; i < h.bitmaps.length; i++) {
      Bitmap b = h.bitmaps[i] = new Bitmap();
      slurpObjectFields(b);

      b.file    = this;
      b.name    = new String(b.nameChars   , "UTF-8");
      b.comment = new String(b.commentChars, "UTF-8");
      
      report("\n  Loaded Bitmap...");
      printObjectFields(b, "    ");
    }
    
    h.records = new ImageRecord[h.numRecords];
    for (int i = 0; i < h.numRecords; i++) {
      ImageRecord r = h.records[i] = new ImageRecord();
      slurpObjectFields(r);
      
      r.file    = this;
      r.belongs = h.bitmaps[r.bitmapID];
      r.label   = noSuffix(r.belongs.name)+"_"+r.belongs.records.size();
      r.belongs.records.add(r);
      
      report("\n  Loaded Image Record...");
      printObjectFields(r, "    ");
    }
  }
  
  
  
  /**  Assorted utility methods-
    */
  int read() throws Exception {
    return IS.read();
  }
  
  int readInt() throws Exception {
    return
      (read() & 0xFF) << 0  |
      (read() & 0xFF) << 8  |
      (read() & 0xFF) << 16 |
      (read() & 0xFF) << 24
    ;
  }
  
  short readShort() throws Exception {
    return (short) (
      (read() & 0xFF) << 0 |
      (read() & 0xFF) << 8
    );
  }
  
  byte readChar() throws Exception {
    return (byte) read();
  }
  
  boolean readBoolean() throws Exception {
    return read() != 0;
  }
  
  byte[] readBytes(byte bytes[]) throws Exception {
    IS.read(bytes);
    return bytes;
  }
  
  
  void report(String s) {
    if (! verbose) return;
    System.out.println(s);
  }
  
  
  static void say(String s) {
    System.out.println(s);
  }
  
  
  
  /**  Utility methods for accessing records:
    */
  Bitmap bitmapWithLabel(String label) {
    for (Bitmap b : header.bitmaps) if (b.name.equals(label)) {
      return b;
    }
    return null;
  }
  
  
  ImageRecord recordWithLabel(String label) {
    for (ImageRecord r : header.records) if (r.label.equals(label)) {
      return r;
    }
    return null;
  }
  

  
  /**  Reflection-based utilities for reading/writing object fields:
    */
  void slurpObjectFields(Object o) throws Exception {
    Field fields[] = o.getClass().getDeclaredFields();
    
    for (Field f : fields) {
      Class t = f.getType();
      if (t.isArray()) {
        Class  a      = t.getComponentType();
        Object array  = f.get(o);
        int    length = Array.getLength(array);
        
        for (int i = 0; i < length; i++) {
          if (a == Integer.TYPE) Array.setInt    (array, i, readInt    ());
          if (a == Short  .TYPE) Array.setShort  (array, i, readShort  ());
          if (a == Byte   .TYPE) Array.setByte   (array, i, readChar   ());
          if (a == Boolean.TYPE) Array.setBoolean(array, i, readBoolean());
        }
        continue;
      }
      if (t == Integer.TYPE) f.set(o, readInt    ());
      if (t == Short  .TYPE) f.set(o, readShort  ());
      if (t == Byte   .TYPE) f.set(o, readChar   ());
      if (t == Boolean.TYPE) f.set(o, readBoolean());
    }
  }
  
  
  void printObjectFields(Object o, String indent) throws Exception {
    if (! verbose) return;
    
    //  TODO:  Move this into the XML routines.
    
    ArrayList <Field> fields = new ArrayList <Field> ();
    int maxNameLength = -1;
    
    for (Field f : o.getClass().getDeclaredFields()) {
      Class t = f.getType();
      String name = f.getName();
      if (name.startsWith("unknown")) continue;
      if (name.startsWith("this"   )) continue;
      int nameLen = name.length();
      if (maxNameLength < nameLen) maxNameLength = nameLen;
      fields.add(f);
    }
    
    StringBuffer out = new StringBuffer();
    for (Field f : fields) {
      Class  type  = f.getType();
      String name  = f.getName();
      Object value = f.get(o);
      
      out.append(indent);
      out.append(name);
      out.append(":");
      
      for (int p = maxNameLength + 1 - name.length(); p-- > 0;) {
        out.append(' ');
      }
      if (type.isArray()) {
        out.append("["+type.getComponentType());
        out.append(" x"+Array.getLength(value)+"]");
      }
      else {
        out.append(value.toString());
      }
      out.append("\n");
    }
    
    report(out.toString());
  }
  
  
  XML objectAsXML(Object o) throws Exception {
    XML node = XML.node(o.getClass().getName());
    for (Field f : o.getClass().getDeclaredFields()) {
      Class  t   = f.getType();
      Object v   = f.get(o);
      String att = f.getName(), val = v == null ? "None" : v.toString();
      node.set(att, val);
    }
    return node;
  }
  
  
  
  static void unpackSG(String basePath, String fileSG, String outputPath) {
    try {
      
      //
      //  First, read the main SG file and create a blank directory for
      //  subsequent output-
      say("\nReading main .SG file: "+basePath+fileSG);
      
      String baseFileName = noSuffix(fileSG);
      File_SG mainFile = new File_SG();
      mainFile.readFile(basePath+fileSG, false);
      
      File outDir = new File(outputPath);
      if (  outDir.exists()) wipeDirectory(outDir);
      if (! outDir.exists()) outDir.mkdirs();
      
      //
      //  Then write the main records-index:
      String outPath = outputPath+baseFileName+".xml";
      FileWriter FW = writerOutStream(outPath, false);
      
      say("  Writing header and bitmaps to "+outPath);
      
      XML asXML = mainFile.objectAsXML(mainFile.header);
      asXML.writeToFile(FW, "");
      
      for (Bitmap writes : mainFile.header.bitmaps) {
        asXML = mainFile.objectAsXML(writes);
        asXML.writeToFile(FW, "");
      }
      
      FW.flush();
      FW.close();
      
      //
      //  Prepare for random file access:
      RandomAccessFile fileAccess = File_SG.randomInStream(
        basePath+baseFileName+".555", false
      );
      
      //
      //  Now write sub-directories for each of the image-records in a bitmap:
      for (Bitmap map : mainFile.header.bitmaps) {
        if (map.name.isEmpty()) continue;
        
        String baseMapName = noSuffix(map.name);
        File mapDir = new File(outputPath+baseMapName);
        if (! mapDir.exists()) mapDir.mkdirs();
        
        String mapFileName = outputPath+baseMapName+"/records.xml";
        FileWriter MW = writerOutStream(mapFileName, true);
        if (MW == null) continue;
        
        say("  Writing image records: "+mapFileName);
        
        for (Object writes : map.records) {
          asXML = mainFile.objectAsXML(writes);
          asXML.writeToFile(MW, "");
        }
        
        MW.flush();
        MW.close();
        
        for (ImageRecord record : map.records) {
          BufferedImage image = File_555.extractImage(record, fileAccess);
          if (image == null) continue;
          String imgName = record.label+".png";
          File_555.saveImage(image, outputPath+baseMapName+"/"+imgName);
        }
      }
      fileAccess.close();
    }
    catch(Exception e) {
      System.out.print("Problem: "+e);
      e.printStackTrace();
    }
  }
  
  
  static String noSuffix(String filename) {
    int dotIndex = filename.indexOf(".");
    if (dotIndex == -1) return filename;
    return filename.substring(0, dotIndex);
  }
  
  
  static void wipeDirectory(File dir) {
    for (File file: dir.listFiles()) {
      if (file.isDirectory()) wipeDirectory(file);
      file.delete();
    }
  }
  
  
  
  /**  Main execution method-
    */
  public static void main(String args[]) {
    File_SG.unpackSG("C3 Files/", "C3_North.sg2", "output_sg_north/");
    
    //  Then... if the user wants to modify something, they make a copy of that
    //  folder, tweak the relevant images or attributes, and then recompile the
    //  S3 starting from the root XML in the root folder.
    
    //  Supplemental info you might want to store:
    //    -Exact filenames/locations for external 555 files
    //    -Descriptive labels for exported building/walker/anim sprites
    //  ...Where do I keep those?  TODO- figure it out.
  }
  
}








