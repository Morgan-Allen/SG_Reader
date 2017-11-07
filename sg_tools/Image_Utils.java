

package sg_tools;
import static sg_tools.SG_Handler.*;
import java.io.*;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import javax.imageio.*;
import java.util.ArrayList;
import java.util.HashSet;



public class Image_Utils {
  
  
  final static int
    TYPE_ISOMETRIC = 30
  ;
  
  
  static boolean replaceImageBytes(
    int offset, int originalLength, byte newBytes[],
    File_555 file, String outPath
  ) {
    try {
      
      //
      //  First, grab any bytes that come before the segment being replaced:
      byte below[] = new byte[offset];
      file.access.seek(0);
      file.access.read(below);
      
      //
      //  Then grab any bytes that come after:
      int totalBytes = (int) new File(file.fullpath).length();
      byte above[] = new byte[totalBytes - (offset + originalLength)];
      file.access.seek(offset + originalLength);
      file.access.read(above);
      
      //
      //  Then we create a new file that basically sandwiches the new data
      //  between these two segments.
      DataOutputStream OS = SG_Utils.outStream(outPath+file.filename, false);
      OS.write(below);
      OS.write(newBytes);
      OS.write(above);
      OS.close();
      
      //
      //  Having done this, we need to update the image-record in question, and
      //  any subsequent image-records within the 555 file.
      int bytesDiff = newBytes.length - originalLength;
      ArrayList <ImageRecord> changed = new ArrayList();
      HashSet <File_SG> toUpdate = new HashSet();
      
      for (ImageRecord record : file.referring) {
        if (record.offset == offset) {
          record.dataLength = newBytes.length;
          changed.add(record);
        }
        if (record.offset > offset) {
          record.offset += bytesDiff;
          changed.add(record);
        }
      }
      for (ImageRecord record : changed) {
        record.flagAsChanged = true;
        for (Bitmap b : record.belongs) toUpdate.add(b.file);
      }
      
      //
      //  Then we need to modify the associated entries in any SG2 files.
      for (File_SG updated : toUpdate) {
        try {
          totalBytes = (int) new File(updated.fullpath).length();
          byte copied[] = new byte[totalBytes];
          
          RandomAccessFile in = new RandomAccessFile(updated.fullpath, "r");
          in.read(copied);
          in.close();
          
          String newPath = outPath+updated.filename;
          RandomAccessFile out = new RandomAccessFile(newPath, "w");
          out.write(copied);
          
          int recordOffset = SG_OPENING_SIZE;
          for (ImageRecord record : updated.records) {
            if (record.flagAsChanged) {
              out.seek(recordOffset);
              file.handler.writeObjectFields(record, out);
            }
            recordOffset += SG_RECORD_SIZE;
          }
          
          out.close();
        }
        catch (IOException e) {
          throw e;
        }
        catch (Exception e) {
          say("Problem: "+e);
          e.printStackTrace();
        }
      }
      
      //
      //  Clean up and return:
      for (ImageRecord record : changed) {
        record.flagAsChanged = false;
      }
      return true;
    }
    catch (IOException e) {
      SG_Handler.say("Problem: "+e);
      e.printStackTrace();
    }
    return false;
  }
  
  
  
  /**  Utility methods for image extraction:
    */
  static class Bytes {
    byte data[];
    int used = -1;
    
    Bytes(int capacity) { data = new byte[capacity]; }
  }
  
  
  static BufferedImage extractImage(ImageRecord record) {
    //
    //  Basic sanity checks first...
    if (record.width == 0 || record.height == 0) {
      return null;
    }
    try {
      //
      //  First, ensure that the record is accessible and allocate space for
      //  extracting the data:
      //  TODO:  In later versions of this format, there may be extra
      //  transparency pixels.  Check this.
      RandomAccessFile access = record.file.access;
      if (access == null) return null;
      
      BufferedImage image = record.extracted = new BufferedImage(
        record.width, record.height, BufferedImage.TYPE_INT_ARGB
      );

      Bytes bytes = new Bytes(record.dataLength);
      access.seek(record.offset - (record.externalData ? 1 : 0));
      access.read(bytes.data);
      //
      //  Isometric images are actually stitched together from both a
      //  transparent upper and a diagonally-packed lower half, so they need
      //  special treatment:
      if (record.imageType == TYPE_ISOMETRIC) {
        readIsometricImage(bytes, record);
      }
      //
      //  Compression is used for any images with transparency, which means any
      //  other walker-sprites in practice:
      else if (record.compressed) {
        readSpriteImage(bytes, record);
      }
      //
      //  And finally, plain images are stored without compression:
      else {
        readPlainImage(bytes, record);
      }
      return image;
    }
    catch (IOException e) {
      SG_Handler.say("Problem: "+e);
      e.printStackTrace();
    }
    return null;
  }
  
  
  static void readIsometricImage(Bytes bytes, SG_Handler.ImageRecord r) {
    readIsometricBase(bytes, r);
    int done = r.lengthNoComp;
    readTransparentImage(bytes, done, r, r.dataLength - done);
  }
  
  
  static void readSpriteImage(Bytes bytes, SG_Handler.ImageRecord r) {
    readTransparentImage(bytes, 0, r, r.dataLength);
  }
  
  
  
  /**  Utilities for reading/writing plain images-
    */
  static void readPlainImage(Bytes bytes, SG_Handler.ImageRecord r) {
    processPlainImage(bytes, r, false);
  }
  
  
  static void writePlainImage(SG_Handler.ImageRecord r, Bytes store) {
    processPlainImage(store, r, true);
  }
  
  
  static void processPlainImage(
    Bytes bytes, SG_Handler.ImageRecord r, boolean write
  ) {
    BufferedImage store = r.extracted;
    for (int x, y = 0, i = 0; y < store.getHeight(); y++) {
      for (x = 0; x < store.getWidth(); x++, i += 2) {
        if (write) {
          int ARGB = store.getRGB(x, y);
          ARGBtoBytes(ARGB, bytes, i);
        }
        else {
          int ARGB = bytesToARGB(bytes, i);
          store.setRGB(x, y, ARGB);
        }
      }
    }
  }
  
  
  
  /**  Utilities for reading/writing transparency-
    */
  static void readTransparentImage(
    Bytes bytes, int offset, SG_Handler.ImageRecord r, int length
  ) {
    BufferedImage store = r.extracted;
    int i = offset;
    int x = 0, y = 0, j;
    int width = store.getWidth();
    
    while (i < offset + length) {
      int c = bytes.data[i++] & 0xff;
      if (c == 255) {
        //  The next byte is the number of pixels to skip
        x += bytes.data[i++] & 0xff;
        while (x >= width) {
          x -= width;
          y++;
        }
      }
      else {
        //  `c' is the number of image data bytes
        for (j = 0; j < c; j++, i += 2) {
          int ARGB = bytesToARGB(bytes, i);
          store.setRGB(x, y, ARGB);
          x++;
          while (x >= width) {
            x -= width;
            y++;
          }
        }
      }
    }
  }
  
  
  static void writeTransparentImage(
    SG_Handler.ImageRecord r, int length, Bytes store, int offset
  ) {
    //  TODO:  You'll need a variable-length byte-buffer instead of an array,
    //  if a fresh image of variable size is being encoded.
    
    //  TODO:  You'll also need to omit any pixels accounted for by an
    //  isometric base!
    
    //  TODO:  And you need to make sure a run of length > 255 is never stored.
    
    BufferedImage img = r.extracted;
    boolean inGap = false;
    int index          =  0;
    int gapSize        =  0;
    int fillStartIndex = -1;
    
    for (int x = 0; x < img.getWidth(); x++) {
      for (int y = 0; y < img.getHeight(); y++) {
        int pix = img.getRGB(x, y);
        
        if ((pix & 0xff000000) == 0) {
          if (! inGap) {
            if (fillStartIndex >= 0) {
              store.data[fillStartIndex] = (byte) gapSize;
            }
            gapSize = 0;
            inGap = true;
          }
          gapSize += 1;
        }
        else {
          if (inGap) {
            inGap = false;
            store.data[index++] = (byte) 0xff;
            store.data[index++] = (byte) gapSize;
            fillStartIndex = index++;
            gapSize = 0;
          }
          ARGBtoBytes(pix, store, index);
          index += 2;
          gapSize += 1;
        }
      }
    }
  }
  
  
  
  /**  Utilities for reading/writing isometric pixels-
    */
  final static int
    TILE_WIDE      = 58  ,
    TILE_HIGH      = 30  ,
    TILE_BYTES     = 1800,
    BIG_TILE_WIDE  = 78  ,
    BIG_TILE_HIGH  = 40  ,
    BIG_TILE_BYTES = 3200
  ;
  
  static void readIsometricBase(Bytes bytes, SG_Handler.ImageRecord r) {
    processIsometricBase(bytes, r, false);
  }
  
  static void writeIsometricBase(SG_Handler.ImageRecord r, Bytes store) {
    processIsometricBase(store, r, true);
  }
  
  static void processIsometricBase(
    Bytes bytes, SG_Handler.ImageRecord r, boolean write
  ) {
    
    BufferedImage store = r.extracted;
    int wide      = store.getWidth();
    int high      = (wide + 2) / 2;
    boolean big   = r.file.handler.version == SG_Handler.VERSION_EM;
    int tileWide  = big ? BIG_TILE_WIDE  : TILE_WIDE ;
    int tileHigh  = big ? BIG_TILE_HIGH  : TILE_HIGH ;
    int tileBytes = big ? BIG_TILE_BYTES : TILE_BYTES;
    int tileSpan  = high / tileHigh;
    
    if ((wide + 2) * high != r.lengthNoComp) {
      SG_Handler.say("Isometric data size did not match: "+r.label);
      return;
    }

    int maxY    = (tileSpan * 2) - 1;
    int offsetY = store.getHeight() - high;
    int offsetX = 0;
    int index   = 0;
    
    for (int y = 0; y < maxY; y++) {
      boolean lower = y < tileSpan;
      int nextY = y + 1;
      int maxX  = lower ? nextY : ((tileSpan * 2) - nextY);
      offsetX   = lower ? (tileSpan - nextY) : (nextY - tileSpan);
      offsetX   *= tileHigh;
      
      for (int x = 0; x < maxX; x++) {
        processIsometricTile(
          bytes, index * tileBytes, r,
          offsetX, offsetY,
          tileWide, tileHigh,
          write
        );
        index += 1;
        offsetX += tileWide + 2;
      }
      offsetY += tileHigh / 2;
    }
  }
  
  
  static void processIsometricTile(
    Bytes bytes, int offset, SG_Handler.ImageRecord r,
    int offX, int offY, int tileWide, int tileHigh, boolean write
  ) {
    BufferedImage store = r.extracted;
    
    for (int y = 0, i = 0; y < tileHigh; y++) {
      int startX = (2 * y) - tileHigh;
      if (startX < 0) { startX *= -1; startX -= 2; }
      
      for (int x = startX; x < tileWide - startX; x++, i += 2) {
        if (write) {
          int ARGB = store.getRGB(offX + x, offY + y);
          ARGBtoBytes(ARGB, bytes, offset + i);
        }
        else {
          int ARGB = bytesToARGB(bytes, offset + i);
          store.setRGB(offX + x, offY + y, ARGB);
        }
      }
    }
  }
  
  
  
  /**  Reading and writing individual pixel values:
    */
  final static int
    BITS_5  = (1 << 5) - 1,
    MASK_R5 = BITS_5 << 10,
    MASK_G5 = BITS_5 << 5 ,
    MASK_B5 = BITS_5 << 0 ,
    BITS_8  = 0xff,
    MASK_R8 = BITS_8 << 16,
    MASK_G8 = BITS_8 << 8 ,
    MASK_B8 = BITS_8 << 0 
  ;
  
  
  static int bytesToARGB(Bytes bytes, int offset) {
    if (offset + 1 >= bytes.data.length) return 0;
    
    byte raw[] = bytes.data;
    int color = (raw[offset] & 0xff) | ((raw[offset + 1] & 0xff) << 8);
    int RGBA  = 0xff000000;
    RGBA |= (color & MASK_R5) << 9;
    RGBA |= (color & MASK_G5) << 6;
    RGBA |= (color & MASK_B5) << 3;
    
    return RGBA;
  }
  
  
  static void ARGBtoBytes(int ARGB, Bytes store, int offset) {
    if (offset + 1 >= store.data.length) return;
    
    int stored = 0;
    stored |= ((ARGB & MASK_R8) >> 9) & MASK_R5;
    stored |= ((ARGB & MASK_G8) >> 6) & MASK_G5;
    stored |= ((ARGB & MASK_B8) >> 3) & MASK_B5;
    
    store.data[offset    ] = (byte) ((stored >> 0) & 0xff);
    store.data[offset + 1] = (byte) ((stored >> 8) & 0xff);
    store.used = offset + 2;
  }
  
  
  
  /**  Dealing with the extracted images:
    */
  static void displayImage(final BufferedImage image) {
    JFrame frame = new JFrame();
    JPanel pane = new JPanel() {
      final static long serialVersionUID = 0;
      
      public void paintComponent(Graphics g) {
        g.drawImage(image, 10, 10, null);
      }
    };
    frame.add(pane);
    
    int wide = image.getWidth() + 20, high = image.getHeight() + 20;
    if (wide < 100) wide = 100;
    if (high < 100) high = 100;
    frame.getContentPane().setPreferredSize(new Dimension(wide, high));
    frame.pack();
    
    frame.setVisible(true);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }
  
  
  static void saveImage(BufferedImage image, String outPath) {
    try {
      File outputFile = new File(outPath);
      ImageIO.write(image, "png", outputFile);
    }
    catch (Exception e) {
      SG_Handler.say("Could not save: "+outPath+", problem: "+e);
      return;
    }
  }
  
}










