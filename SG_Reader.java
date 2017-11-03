
import java.io.*;




public class SG_Reader {
  
  
  /**  Data structures:
    */
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
  
  class Bitmap {
    
    char nameChars   [] = new char[65];
    char commentChars[] = new char[51];
    String name;
    String comment;
    
    int width;
    int height;
    int numImages;
    int startIndex;
    int endIndex;
    
    byte remainder[] = new byte[74];
  }
  
  class ImageRecord {
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








