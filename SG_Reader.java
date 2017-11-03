
import java.io.*;




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
  }
  
  //  4 + 16 + 8 + 12 + 24
  
  //  64 bytes total here.
  class ImageRecord {
    int offset;
    int dataLength;
    int lengthRawData;
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
  
  
  void readFile(String filename) {
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
      
      for (int i = 0; i < h.index.length; i++) {
        h.index[i] = readShort();
      }
      for (int i = 0; i < h.bitmaps.length; i++) {
        h.bitmaps[i] = readBitmap();
      }
    }
    catch (Exception e) {
      say("Problem: "+e);
    }
  }
  
  Bitmap readBitmap() throws Exception {
    
    Bitmap b = new Bitmap();
    readBytes(b.nameChars   );
    readBytes(b.commentChars);
    b.name       = new String(b.nameChars, "UTF-8");
    b.comment    = new String(b.commentChars, "UTF-8");
    b.width      = readInt();
    b.height     = readInt();
    b.numImages  = readInt();
    b.startIndex = readInt();
    b.endIndex   = readInt();
    readBytes(b.remainder);
    
    say("\n  Reading Bitmap...");
    say("    Name:     "+b.name      );
    say("    Comment:  "+b.comment   );
    say("    Width:    "+b.width     );
    say("    Height:   "+b.height    );
    say("    # Images: "+b.numImages );
    say("    @ Start:  "+b.startIndex);
    say("    @ End:    "+b.endIndex  );
    return b;
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
  
  void readBytes(byte bytes[]) throws Exception {
    IS.read(bytes);
  }
  
  void say(String s) {
    System.out.println(s);
  }
  
  
  
  /**  Main execution method-
    */
  public static void main(String args[]) {
    SG_Reader reader = new SG_Reader();
    reader.readFile("C3 Files/C3_North.sg2");
  }
  
}








