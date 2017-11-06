

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
  static DataInputStream inStream(String filename) {
    try {
      FileInputStream FS = new FileInputStream(filename);
      BufferedInputStream BS = new BufferedInputStream(FS);
      return new DataInputStream(BS);
    }
    catch (Exception e) { return null; }
  }
  
  
  static RandomAccessFile randomInStream(String filename) {
    try { return new RandomAccessFile(filename, "r"); }
    catch (Exception e) { return null; }
  }
  
  
  
  /**  File parsing routines-
    */
  String filename;
  DataInputStream IS;
  boolean verbose;
  Header header;
  
  
  void readFile(String filename, boolean verbose) {
    this.filename = filename;
    this.verbose  = verbose ;
    say("\nReading SG File: "+filename);
    
    try {
      IS = inStream(filename);
      
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
      
      say("");
      say("  File size: "+h.filesize   );
      say("  Version:   "+h.version    );
      say("  # Records: "+h.numRecords );
      say("  # Used:    "+h.recordsUsed);
      say("  Total file size: "+h.totalFilesize);
      say("  Inner 555 size:  "+h.inner555Size );
      say("  Outer 555 size:  "+h.outer555Size );
      say("");
      
      for (int i = 0; i < h.index.length; i++) {
        h.index[i] = readShort();
      }
      
      for (int i = 0; i < h.bitmaps.length; i++) {
        Bitmap b = h.bitmaps[i] = new Bitmap();
        slurpObjectFields(b);

        b.file    = this;
        b.name    = new String(b.nameChars   , "UTF-8");
        b.comment = new String(b.commentChars, "UTF-8");
        
        say("\n  Loaded Bitmap...");
        printObjectFields(b, "    ");
      }
      
      h.records = new ImageRecord[h.numRecords];
      for (int i = 0; i < h.numRecords; i++) {
        ImageRecord r = h.records[i] = new ImageRecord();
        slurpObjectFields(r);
        
        r.file    = this;
        r.belongs = h.bitmaps[r.bitmapID];
        r.label   = r.belongs.name+" #"+r.belongs.records.size();
        r.belongs.records.add(r);
        
        say("\n  Loaded Image Record...");
        printObjectFields(r, "    ");
      }
    }
    catch (Exception e) {
      say("Problem: "+e);
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
  
  
  void say(String s) {
    if (! verbose) return;
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
      
      for(int p = maxNameLength + 1 - name.length(); p-- > 0;) {
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
    
    say(out.toString());
  }
  
  
  XML objectAsXML(Object o) throws Exception {
    XML node = XML.node(o.getClass().getName());
    for (Field f : o.getClass().getDeclaredFields()) {
      Class  t = f.getType();
      Object v = f.get(o);
      String att = f.getName(), val = v == null ? "None" : v.toString();
      node.set(att, val);
    }
    return node;
  }
  
  
  
  /**  Main execution method-
    */
  
  public static void main(String args[]) {
    
    File_SG mainFile = new File_SG();
    mainFile.readFile("C3 Files/C3_North.sg2", false);
    
    //  Okay, here's the plan:
    //  You create a folder for each SG file.
    //  You create a master XML that lists the header and each of the bitmaps.
    //  You create sub-folders for each bitmap, with an XML index for all the
    //  image-records.
    //  You disgorge all the images into that sub-folder.
    
    //  Then... if the user wants to modify something, they make a copy of that
    //  folder, tweak the relevant images or attributes, and then recompile the
    //  S3 starting from the root XML in the root folder.
    
    
    //  Supplemental info you might want to store:
    //    -Exact filenames/locations for external 555 files
    //    -Descriptive labels for exported building/walker/anim sprites
    //  ...Where do I keep those?  TODO- figure it out.
    
    
    try {
      File baseFile = new File("test_xml.xml");
      FileWriter FW = new FileWriter(baseFile);
      
      for (Bitmap writes : mainFile.header.bitmaps) {
        XML asXML = mainFile.objectAsXML(writes);
        asXML.writeToFile(FW, "");
        mainFile.say("Bitmap label: "+writes.name);
      }
      
      FW.flush();
      FW.close();
      
      baseFile = new File("test_records.xml");
      FW = new FileWriter(baseFile);
      
      Bitmap commerce = mainFile.bitmapWithLabel("Commerce.bmp                                                     ");
      for (Object writes : commerce.records) {
        XML asXML = mainFile.objectAsXML(writes);
        asXML.writeToFile(FW, "");
      }
      
      FW.flush();
      FW.close();
      
      //  ...This should be enough to start with.  Okay then.
      
      RandomAccessFile fileAccess = File_SG.randomInStream(
        "C3 Files/C3_North.555"
      );
      ImageRecord record = mainFile.recordWithLabel("Commerce.bmp                                                      #157");
      BufferedImage image = File_555.extractImage(record, fileAccess);
      fileAccess.close();
      
      File_555.displayImage(image);
    }
    catch (Exception e) {
      System.out.print("Problem: "+e);
      e.printStackTrace();
    }
  }
  
}








