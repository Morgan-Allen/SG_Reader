

import java.io.*;
import java.lang.reflect.*;
import java.util.ArrayList;




public class SG_Reader {
  
  
  /**  Data structures:
    */
  
  //  80 bytes total here.
  class Header {
    
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
  class Bitmap {
    
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
  class ImageRecord {
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
  
  
  /**  File parsing routines-
    */
  DataInputStream IS;
  boolean verbose;
  
  
  void readFile(String filename, boolean verbose) {
    this.verbose = verbose;
    say("\nReading SG File: "+filename);
    
    try {
      File reads = new File(filename);
      FileInputStream FS = new FileInputStream(reads);
      BufferedInputStream BS = new BufferedInputStream(FS);
      IS = new DataInputStream(BS);
      
      Header h = new Header();
      h.filesize    = readInt();
      h.version     = readInt();
      h.unknown1    = readInt();
      h.numRecords  = readInt();
      h.recordsUsed = readInt();
      h.unknown2    = readInt();
      
      say("");
      say("  File size: "+h.filesize   );
      say("  Version:   "+h.version    );
      say("  # Records: "+h.numRecords );
      say("  # Used:    "+h.recordsUsed);
      
      h.totalFilesize = readInt();
      h.inner555Size  = readInt();
      h.outer555Size  = readInt();
      for (int i = 0; i < h.remainder.length; i++) {
        h.remainder[i] = readInt();
      }
      
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
        
        b.name    = new String(b.nameChars   , "UTF-8");
        b.comment = new String(b.commentChars, "UTF-8");
        
        say("\n  Loaded Bitmap...");
        printObjectFields(b, "    ");
      }
      
      h.records = new ImageRecord[h.numRecords];
      for (int i = 0; i < h.numRecords; i++) {
        ImageRecord r = h.records[i] = new ImageRecord();
        slurpObjectFields(r);
        
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
  
  
  
  /**  Including for debug/printout purposes:
    */
  void say(String s) {
    if (! verbose) return;
    System.out.println(s);
  }
  
  
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
  
  
  void writeObjectFieldsToXML(Object o, XML node) {
    for (Field f : o.getClass().getDeclaredFields()) {
      
    }
  }
  
  
  
  /**  Main execution method-
    */
  public static void main(String args[]) {
    XML testX = XML.node("text");
    testX.setContent("Here's some sample text!");
    testX.set("type", "info");
    testX.set("format", "latin_alphabet");
    
    XML.writeXML(testX, "test_xml.xml");
    
    //SG_Reader reader = new SG_Reader();
    //reader.readFile("C3 Files/C3_North.sg2", true);
  }
  
}
















