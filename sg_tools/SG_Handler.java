

package sg_tools;
import java.io.*;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.awt.image.BufferedImage;




public class SG_Handler {
  
  
  /**  Data structures:
    */
  final static int
    VERSION_C3 = 0,
    VERSION_PH = 1,
    VERSION_ZE = 2,
    VERSION_EM = 3
  ;
  final static int
    SG_HEADER_SIZE = 80 ,
    SG_BITMAP_SIZE = 200,
    SG_RECORD_SIZE = 64 ,
    SG_INDEX_SIZE  = 600,
    SG_OPENING_SIZE = (
      SG_HEADER_SIZE + SG_INDEX_SIZE +
      (SG_BITMAP_SIZE * 100)
    )
  ;
  final static File_555 NOT_FOUND = new File_555();
  
  
  int version;
  boolean verbose;
  String basePath;
  
  ArrayList <File_SG > allSG  = new ArrayList();
  ArrayList <File_555> all555 = new ArrayList();
  
  Hashtable <String, ImageRecord> recordsLookup = new Hashtable();
  
  
  static class File_SG {
    
    //  80 bytes total here.
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
    short index[] = new short[300];
    
    Object BREAK;
    
    //  Extra fields for subsequent processing:
    SG_Handler handler;
    String filename;
    String fullpath;
    DataInputStream IS;
    
    Bitmap bitmaps[] = new Bitmap[100];
    ImageRecord records[];
    ArrayList <File_555> distinctRefs = new ArrayList();
  }
  
  
  static class File_555 {
    
    Object BREAK;
    
    //  No intrinsic data fields.  Extra fields for subsequent processing:
    SG_Handler handler;
    String filename;
    String fullpath;
    RandomAccessFile access;
    ArrayList <ImageRecord> referring = new ArrayList();
    ArrayList <File_SG> distinctRefs = new ArrayList();
  }
  
  
  static class Bitmap {
    
    //  200 bytes total here.
    byte nameChars   [] = new byte[65];
    byte commentChars[] = new byte[51];
    
    int width;
    int height;
    int numImages;
    int startIndex;
    int endIndex;
    
    byte remainder[] = new byte[64];
    
    Object BREAK;
    
    //  Extra fields for subsequent processing:
    File_SG file;
    String name;
    String comment;
    ArrayList <ImageRecord> records = new ArrayList();
  }
  
  
  static class ImageRecord {
    
    //  64 bytes total here.
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
    
    Object BREAK;
    
    //  Extra fields for subsequent processing:
    File_555 file;
    ArrayList <Bitmap> belongs = new ArrayList();
    BufferedImage extracted;
    String label;
    String lookupKey;
    boolean flagAsChanged = false;
  }
  
  
  
  /**  File parsing routines-
    */
  SG_Handler(int version, boolean verbose) {
    this.version = version;
    this.verbose = verbose;
  }
  
  
  void readAllFiles_SG(String basePath) throws Exception {
    
    this.basePath = basePath;
    File mainDir = new File(basePath);
    
    String suffix_SG = null;
    if (version == VERSION_C3) suffix_SG = ".sg2";
    else                       suffix_SG = ".sg3";
    
    for (File file : mainDir.listFiles()) {
      String filename = file.getName();
      if (filename.endsWith(suffix_SG)) {
        readFile_SG(filename);
      }
    }
    
    say("\nCompleted reading all SG files.  Report:");
    say("\n  Total "+suffix_SG+" Files:  "+allSG .size());
    for (File_SG file : allSG) {
      say("    "+file.filename);
      say("      Bitmaps: "+file.bitmaps.length);
      say("      Records: "+file.records.length);
      say("      Refers to:");
      for (File_555 refs : file.distinctRefs) {
        say("        "+refs.filename);
      }
    }
    say("\n  Total .555 Files: "+all555.size());
    for (File_555 file : all555) {
      say("    "+file.filename);
      say("      Records: "+file.referring.size());
      say("      Referenced by:");
      for (File_SG refs : file.distinctRefs) {
        say("        "+refs.filename);
      }
    }
  }
  
  
  File_SG readFile_SG(String filename) throws Exception {
    
    //
    //  First, read in the header information stored within the File_SG class:
    report("\nReading SG File: "+filename);
    
    File_SG file = new File_SG();
    file.filename = filename;
    file.fullpath = basePath+filename;
    file.IS = SG_Utils.inStream(file.fullpath, false);
    
    readObjectFields(file, file.IS);
    printObjectFields(file, "");
    
    //
    //  Once the header is processed, load up the bitmap entries:
    for (int i = 0; i < file.bitmaps.length; i++) {
      Bitmap b = file.bitmaps[i] = new Bitmap();
      readObjectFields(b, file.IS);
      
      b.file    = file;
      b.name    = new String(b.nameChars   , "UTF-8");
      b.comment = new String(b.commentChars, "UTF-8");
      
      report("\n  Loaded Bitmap...");
      printObjectFields(b, "    ");
    }
    
    //
    //  Once the bitmaps are processed, allocate and load the corresponding
    //  image-records:
    file.records = new ImageRecord[file.numRecords];
    for (int i = 0; i < file.numRecords; i++) {
      
      //
      //  First, load the raw record data...
      ImageRecord r = file.records[i] = new ImageRecord();
      readObjectFields(r, file.IS);
      report("\n  Loaded Image Record...");
      printObjectFields(r, "    ");
      
      //
      //  Then lookup the corresponding 555 File, assuming it exists.  (If it
      //  doesn't then we skip this record and move on)-
      Bitmap belongs = file.bitmaps[r.bitmapID];
      lookupFile_555(r, belongs);
      if (r.file == NOT_FOUND) continue;
      
      //
      //  Next, we compute a unique key for this record based on the 555 file
      //  it refers to and the data offset for image extraction...
      String lookupKey = noSuffix(r.file.filename)+"_"+r.offset;
      ImageRecord match = recordsLookup.get(lookupKey);
      
      //
      //  If such a record already exists, we will substitute that in the SG
      //  file's record-list instead, and discard the record we just read in-
      //  we don't want any duplicate entries.
      if (match != null) {
        r = file.records[i] = match;
        r.belongs.add(belongs);
      }
      
      //
      //  If this is a genuinely new image-record, give it a fresh label and
      //  hook up the internal reference graph to keep track:
      else {
        r.label     = noSuffix(belongs.name)+"_"+belongs.records.size();
        r.lookupKey = lookupKey;
        belongs.records.add(r);
        recordsLookup.put(lookupKey, r);
        r.file.referring.add(r);
        r.belongs.add(belongs);
        
        if (! r.file.distinctRefs.contains(file)) {
          r.file.distinctRefs.add(file);
        }
        if (! file.distinctRefs.contains(r.file)) {
          file.distinctRefs.add(r.file);
        }
      }
    }
    
    //
    //  Store the SG file for completeness and return-
    allSG.add(file);
    report("\nLoading complete.");
    return file;
  }
  
  
  void lookupFile_555(ImageRecord record, Bitmap bitmap) {
    
    //
    //  First, check to see if the corresponding lookup is already cached:
    if (record.file != null) return;
    
    //
    //  Otherwise, determine what file we need to look at in order to extract
    //  image data.  If that doesn't exist, return nothing-
    String lookup = null;
    
    if (record.externalData) {
      lookup = noSuffix(bitmap.name);
      if (lookup.isEmpty()) {
        record.file = NOT_FOUND;
        return;
      }
      lookup = lookup+".555";
      if (! fileExists(basePath+lookup)) {
        lookup = "555/"+lookup;
      }
    }
    else {
      lookup = noSuffix(bitmap.file.filename)+".555";
    }
    if (! fileExists(basePath+lookup)) {
      record.file = NOT_FOUND;
      return;
    }
    
    //
    //  If such a file exists, see if we've already allocated a corresponding
    //  object.  Failing that, create a new one.  Failing that, return nothing-
    File_555 file = null;
    for (File_555 f : all555) if (f.filename.equals(lookup)) {
      file = f;
      break;
    }
    if (file == null) try {
      file = new File_555();
      file.filename = lookup;
      file.fullpath = basePath+lookup;
      file.handler  = this;
      file.access   = new RandomAccessFile(file.fullpath, "r");
      all555.add(file);
    }
    catch (IOException e) {
      record.file = NOT_FOUND;
      return;
    }
    
    //
    //  If everything works out, record the file and return-
    record.file = file;
  }
  
  
  void closeAllFileAccess() throws Exception {
    for (File_555 file : all555) {
      file.access.close();
    }
  }
  
  
  static boolean fileExists(String fullpath) {
    return new File(fullpath).exists();
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
  
  
  
  /**  Reflection-based utilities for reading/writing object fields:
    */
  //  TODO:  Move these into the Utils class?
  
  int readInt(DataInput i) throws Exception {
    return
      (i.readByte() & 0xFF) << 0  |
      (i.readByte() & 0xFF) << 8  |
      (i.readByte() & 0xFF) << 16 |
      (i.readByte() & 0xFF) << 24
    ;
  }
  
  void writeInt(int val, DataOutput o) throws Exception {
    o.write((val >> 0 ) & 0xff);
    o.write((val >> 8 ) & 0xff);
    o.write((val >> 16) & 0xff);
    o.write((val >> 24) & 0xff);
  }
  
  short readShort(DataInput i) throws Exception {
    return (short) (
      (i.readByte() & 0xFF) << 0 |
      (i.readByte() & 0xFF) << 8
    );
  }
  
  void writeShort(short val, DataOutput o) throws Exception {
    o.write((val >> 0 ) & 0xff);
    o.write((val >> 8 ) & 0xff);
  }
  
  byte readChar(DataInput i) throws Exception {
    return i.readByte();
  }
  
  void writeChar(byte val, DataOutput o) throws Exception {
    o.write((int) val);
  }
  
  boolean readBoolean(DataInput i) throws Exception {
    return i.readByte() != 0;
  }
  
  void writeBoolean(boolean val, DataOutput o) throws Exception {
    o.write(val ? 1 : 0);
  }
  
  void readBytes(byte bytes[], DataInput i) throws Exception {
    i.readFully(bytes);
  }
  
  void writeBytes(byte bytes[], DataOutput o) throws Exception {
    o.write(bytes);
  }
  
  
  void readObjectFields(Object o, DataInputStream i) throws Exception {
    Field fields[] = o.getClass().getDeclaredFields();
    
    for (Field f : fields) {
      if (f.getName().equals("BREAK")) break;
      
      Class t = f.getType();
      if (t.isArray()) {
        Class  a     = t.getComponentType();
        Object array = f.get(o);
        if (array == null) continue;
        
        int length = Array.getLength(array);
        for (int n = 0; n < length; n++) {
          if (a == Integer.TYPE) Array.setInt    (array, n, readInt    (i));
          if (a == Short  .TYPE) Array.setShort  (array, n, readShort  (i));
          if (a == Byte   .TYPE) Array.setByte   (array, n, readChar   (i));
          if (a == Boolean.TYPE) Array.setBoolean(array, n, readBoolean(i));
        }
      }
      else if (t == Integer.TYPE) f.set(o, readInt    (i));
      else if (t == Short  .TYPE) f.set(o, readShort  (i));
      else if (t == Byte   .TYPE) f.set(o, readChar   (i));
      else if (t == Boolean.TYPE) f.set(o, readBoolean(i));
    }
  }
  
  
  void writeObjectFields(Object o, RandomAccessFile out) throws Exception {
    Field fields[] = o.getClass().getDeclaredFields();
    
    for (Field f : fields) {
      if (f.getName().equals("BREAK")) break;
      
      Class t = f.getType();
      if (t.isArray()) {
        Class  a     = t.getComponentType();
        Object array = f.get(o);
        if (array == null) continue;
        
        int length = Array.getLength(array);
        for (int n = 0; n < length; n++) {
          if (a == Integer.TYPE) writeInt    (Array.getInt    (array, n), out);
          if (a == Short  .TYPE) writeShort  (Array.getShort  (array, n), out);
          if (a == Byte   .TYPE) writeChar   (Array.getByte   (array, n), out);
          if (a == Boolean.TYPE) writeBoolean(Array.getBoolean(array, n), out);
        }
      }
      else if (t == Integer.TYPE) writeInt    (f.getInt    (o), out);
      else if (t == Short  .TYPE) writeShort  (f.getShort  (o), out);
      else if (t == Byte   .TYPE) writeChar   (f.getByte   (o), out);
      else if (t == Boolean.TYPE) writeBoolean(f.getBoolean(o), out);
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
  
  
  void report(String s) {
    if (! verbose) return;
    System.out.println(s);
  }
  
  
  static void say(String s) {
    System.out.println(s);
  }
  
}








