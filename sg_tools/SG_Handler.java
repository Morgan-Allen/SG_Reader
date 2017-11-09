

package sg_tools;
import java.io.*;
import java.lang.reflect.*;
import java.util.ArrayList;
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
  final static File_555
    NO_555 = new File_555()
  ;
  
  
  int version;
  boolean verbose;
  String basePath;
  
  ArrayList <File_SG > allSG  = new ArrayList();
  ArrayList <File_555> all555 = new ArrayList();
  
  
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
    File_555 lookup555;
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
    
    File_555 lookup555;
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
    
    byte canReverse;
    byte unknown5;
    byte imageType;
    byte compressed;
    byte externalData;
    byte partCompressed;
    byte unknown6[] = new byte[2];
    byte bitmapID;
    byte unknown7;
    byte animSpeedID;
    byte unknown8[] = new byte[5];
    
    Object BREAK;
    
    //  Extra fields for subsequent processing:
    File_555 file;
    Bitmap belongs;
    BufferedImage extracted;
    String label;
    boolean flagAsChanged = false;
  }
  
  
  
  /**  File parsing routines-
    */
  SG_Handler(int version, String basePath, boolean verbose) {
    this.version  = version ;
    this.basePath = basePath;
    this.verbose  = verbose ;
  }
  
  
  void readAllFilesInBaseDir() throws Exception {
    
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
    file.handler  = this;
    file.filename = filename;
    file.fullpath = basePath+filename;
    file.IS = SG_UtilsIO.inStream(file.fullpath, false);
    
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
      
      ImageRecord r = file.records[i] = new ImageRecord();
      readObjectFields(r, file.IS);
      
      report("\n  Loaded Image Record...");
      printObjectFields(r, "    ");
      
      r.belongs = file.bitmaps[r.bitmapID];
      r.label   = noSuffix(r.belongs.name)+"_"+r.belongs.records.size();
      r.belongs.records.add(r);
      
      r.file = lookupFile_555(r, r.belongs);
      if (r.file == null) continue;
      
      r.file.referring.add(r);
      
      if (! r.file.distinctRefs.contains(file)) {
        r.file.distinctRefs.add(file);
      }
      if (! file.distinctRefs.contains(r.file)) {
        file.distinctRefs.add(r.file);
      }
    }
    
    //
    //  Store the SG file for completeness and return-
    allSG.add(file);
    report("\nLoading complete.");
    return file;
  }
  
  
  File_555 lookupFile_555(ImageRecord record, Bitmap bitmap) {
    
    //
    //  To start, check to see if we've already looked up the 555 file for
    //  the parent bitmap or SG file:
    boolean external = record.externalData != 0;
    File_555 lookup = external ?
      bitmap.     lookup555 :
      bitmap.file.lookup555
    ;
    if (lookup == NO_555) {
      return null;
    }
    if (lookup != null) {
      return lookup;
    }
    
    //
    //  Failing that, check to see if a corresponding file exists in either
    //  the same directory or the /555/ folder-
    String baseName = noSuffix(external ? bitmap.name : bitmap.file.filename);
    baseName += ".555";
    
    if (! fileExists(basePath+baseName)) {
      baseName = "555/"+baseName;
    }
    if (! fileExists(basePath+baseName)) {
      if (external) bitmap.     lookup555 = NO_555;
      else          bitmap.file.lookup555 = NO_555;
      return null;
    }
    
    //
    //  If such a file exists, see if we've already allocated a corresponding
    //  object.  Failing that, create a new one.
    File_555 file = null;
    for (File_555 f : all555) if (f.filename.equals(baseName)) {
      file = f;
      break;
    }
    if (file == null) try {
      file = new File_555();
      file.filename = baseName;
      file.fullpath = basePath+baseName;
      file.handler  = this;
      file.access   = new RandomAccessFile(file.fullpath, "r");
      all555.add(file);
    }
    catch (IOException e) {
      say("Problem: "+e);
      file = NO_555;
    }
    
    //
    //  Cache for later reference, and return-
    if (external) bitmap.     lookup555 = file;
    else          bitmap.file.lookup555 = file;
    if (file == NO_555) return null;
    return file;
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
  
  
  
  /**  Methods for writing or modifying SG files:
    */
  void writeFile_SG(File_SG file, String outputPath) throws Exception {
    report("\nWriting SG File: "+outputPath);
    DataOutputStream OS = SG_UtilsIO.outStream(outputPath, false);
    writeFile_SG(file, OS);
    OS.flush();
    OS.close();
  }
  

  void writeFile_SG(File_SG file, DataOutput OS) throws Exception {
    writeObjectFields(file, OS);
    for (Bitmap      b : file.bitmaps) writeObjectFields(b, OS);
    for (ImageRecord r : file.records) writeObjectFields(r, OS);
  }
  
  
  void copyAndModifyFile_SG(
    File_SG file, ArrayList <ImageRecord> changed, String outputDir
  ) throws Exception {
    
    for (ImageRecord record : changed) record.flagAsChanged = true;
    
    int totalBytes = (int) new File(file.fullpath).length();
    byte copied[] = new byte[totalBytes];
    int changedHere = 0;
    say("\n  Updating "+file.filename);
    
    RandomAccessFile in = new RandomAccessFile(file.fullpath, "r");
    in.read(copied);
    in.close();
    
    String newPath = outputDir+file.filename;
    RandomAccessFile out = new RandomAccessFile(newPath, "rw");
    out.write(copied);
    
    int recordOffset = SG_OPENING_SIZE;
    for (ImageRecord record : file.records) {
      if (record.flagAsChanged) {
        out.seek(recordOffset);
        file.handler.writeObjectFields(record, out);
        changedHere += 1;
      }
      recordOffset += SG_RECORD_SIZE;
    }
    
    say("  Total records updated: "+changedHere+"/"+file.numRecords);
    
    for (ImageRecord record : changed) record.flagAsChanged = false;
    out.close();
  }
  
  
  
  /**  Reflection-based utilities for reading/writing object fields:
    */
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
    o.write((int) (val & 0xff));
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
  
  
  void readObjectFields(Object o, DataInput i) throws Exception {
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
  
  
  void writeObjectFields(Object o, DataOutput out) throws Exception {
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
      
      if (name.equals("BREAK")) break;
      
      out.append(indent);
      out.append(name);
      out.append(":");
      
      for (int p = maxNameLength + 1 - name.length(); p-- > 0;) {
        out.append(' ');
      }
      if (value == null) {
        out.append("<none>");
      }
      else if (type.isArray()) {
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








