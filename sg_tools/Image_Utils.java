

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
  
  
  /**  Some initial constants and utility containers:
    */
  final static int
    TYPE_ISOMETRIC = 30
  ;
  static boolean packVerbose = false;
  
  
  static class Bytes {
    byte data[];
    int used = -1;
    
    Bytes(int capacity) { data = new byte[capacity]; }
  }
  
  
  
  /**  Handling storage of new images within .555 files:
    */
  static Bytes bytesFromImage(ImageRecord record, BufferedImage image) {
    //
    //  If it hasn't been set already, store the image now:
    record.extracted = image;
    //
    //  We allocate a buffer with some (hopefully) excess capacity for storing
    //  the compressed image-
    int capacity = record.width * record.height * 4;
    Bytes store = new Bytes(capacity);
    //
    //  Isometric images are actually stitched together from both a
    //  transparent upper and a diagonally-packed lower half, so they need
    //  special treatment:
    if (record.imageType == TYPE_ISOMETRIC) {
      //
      //  We create a copy of the image, since we will need to wipe certain
      //  pixels as we go before creating transparency:
      ColorModel     cm      = record.extracted.getColorModel();
      boolean        alphaPM = cm.isAlphaPremultiplied();
      WritableRaster raster  = record.extracted.copyData(null);
      record.extracted = new BufferedImage(cm, raster, alphaPM, null);
      
      isometricBasePass(store, record, true, true);
      int done = store.used;
      writeTransparentImage(record, record.dataLength - done, store, done);
      
      //  TODO:  You'll need to create a new ImageRecord here, potentially with
      //  new values for dataLength and lengthNoComp.
      record.lengthNoComp = done;
      record.dataLength   = store.used;
    }
    //
    //  Compression is used for any images with transparency, which means any
    //  other walker-sprites in practice:
    else if (record.compressed) {
      writeTransparentImage(record, record.dataLength, store, 0);
    }
    //
    //  And finally, plain images are stored without compression:
    else {
      plainImagePass(store, record, true);
    }
    //
    //  Finally, we trim the buffer down to size and return-
    byte clipped[] = new byte[store.used];
    System.arraycopy(store.data, 0, clipped, 0, store.used);
    store.data = clipped;
    return store;
  }
  
  
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
      say("Problem: "+e);
      e.printStackTrace();
    }
    return false;
  }
  
  
  
  /**  Utility methods for image extraction:
    */
  static Bytes extractRawBytes(
    ImageRecord record
  ) throws IOException {
    RandomAccessFile access = record.file.access;
    if (access == null) return null;
    //
    //  First, ensure that the record is accessible and allocate space for
    //  extracting the data:
    //  TODO:  In later versions of this format, there may be extra
    //  transparency pixels.  Check this.
    Bytes bytes = new Bytes(record.dataLength);
    access.seek(record.offset - (record.externalData ? 1 : 0));
    access.read(bytes.data);
    bytes.used = record.dataLength;
    return bytes;
  }
  
  
  static BufferedImage imageFromBytes(
    Bytes bytes,
    ImageRecord record
  ) throws IOException {
    //
    //  Allocate the image first:
    BufferedImage image = record.extracted = new BufferedImage(
      record.width, record.height, BufferedImage.TYPE_INT_ARGB
    );
    //
    //  Isometric images are actually stitched together from both a
    //  transparent upper and a diagonally-packed lower half, so they need
    //  special treatment:
    if (record.imageType == TYPE_ISOMETRIC) {
      isometricBasePass(bytes, record, false, false);
      int done = record.lengthNoComp;
      readTransparentImage(bytes, done, record, record.dataLength - done);
    }
    //
    //  Compression is used for any images with transparency, which means any
    //  other walker-sprites in practice:
    else if (record.compressed) {
      readTransparentImage(bytes, 0, record, record.dataLength);
    }
    //
    //  And finally, plain images are stored without compression:
    else {
      plainImagePass(bytes, record, false);
    }
    return image;
  }
  
  
  static BufferedImage extractImage(ImageRecord record) {
    //
    //  Basic sanity checks first...
    if (record.width == 0 || record.height == 0) {
      return null;
    }
    try {
      Bytes bytes = extractRawBytes(record);
      return imageFromBytes(bytes, record);
    }
    catch (IOException e) {
      say("Problem: "+e);
      e.printStackTrace();
    }
    return null;
  }
  
  
  
  /**  Utilities for reading/writing plain images-
    */
  static void plainImagePass(
    Bytes bytes, ImageRecord r, boolean write
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
    Bytes bytes, int offset, ImageRecord r, int length
  ) {
    boolean report = packVerbose;
    if (report) say("\nReading transparent image pixels...");
    
    BufferedImage store = r.extracted;
    int maxRead = offset + length;
    if (maxRead > bytes.used) maxRead = bytes.used;
    
    int i = offset;
    int x = 0, y = 0, j;
    int width = store.getWidth();
    
    while (i < maxRead) {
      int c = bytes.data[i++] & 0xff;
      if (c == 255) {
        //  The next byte is the number of pixels to skip
        int skip = bytes.data[i++] & 0xff;
        if (report) say("    Gap  x"+skip+", index: "+(i - 2));
        
        x += skip;
        while (x >= width) {
          x -= width;
          y++;
        }
      }
      else {
        //  `c' is the number of image data bytes
        if (report) say("    Fill x"+c+", index: "+(i - 1));
        
        for (j = 0; j < c && i < maxRead; j++, i += 2) {
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
    ImageRecord r, int length, Bytes store, int offset
  ) {
    boolean report = packVerbose;
    if (report) say("\nWriting transparent image bytes...");
    
    BufferedImage img = r.extracted;
    int high = img.getHeight(), wide = img.getWidth();
    int maxX = wide - 1, maxIndex = offset + length;
    
    final int MAX_GAP = 255, MAX_FILL = 16;
    int index = offset;
    int fillBuffer[] = new int[MAX_FILL];
    int fillCount = 0, gapCount = 0;
    
    for (int y = 0; y < high; y++) {
      int pixel = 0, next = img.getRGB(0, y);
      
      for (int x = 0; x < wide; x++) {
        boolean atEnd = x == maxX;
        pixel = next;
        next  = atEnd ? -1 : img.getRGB(x + 1, y);
        boolean empty     = (pixel & 0xff000000) == 0;
        boolean nextEmpty = (next  & 0xff000000) == 0;
        
        if (empty) {
          gapCount++;
          
          if (gapCount == MAX_GAP || atEnd || ! nextEmpty) {
            if (report) {
              say("    Encoding gap of length "+gapCount+"/"+MAX_GAP);
              say("      X: "+x+"/"+wide+"  Y: "+y+"/"+high+" index: "+index);
            }
            store.data[index++] = (byte) 0xff;
            store.data[index++] = (byte) gapCount;
            store.used = index;
            gapCount = 0;
          }
        }
        else {
          fillBuffer[fillCount++] = pixel;
          
          if (fillCount == MAX_FILL || atEnd || nextEmpty) {
            if (report) {
              say("\n    Pixel-fill ended with size "+fillCount+"/"+MAX_FILL);
              say("      X: "+x+"/"+wide+"  Y: "+y+"/"+high+" index: "+index);
            }
            store.data[index++] = (byte) fillCount;
            for (int i = 0; i < fillCount; i++) {
              ARGBtoBytes(fillBuffer[i], store, index);
              index += 2;
            }
            fillCount = 0;
          }
        }
        
        if (index >= maxIndex) break;
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
  
  
  static void isometricBasePass(
    Bytes bytes, ImageRecord r, boolean write, boolean wipe
  ) {
    
    BufferedImage store = r.extracted;
    int wide      = store.getWidth();
    int high      = (wide + 2) / 2;
    boolean big   = r.file.handler.version == VERSION_EM;
    int tileWide  = big ? BIG_TILE_WIDE  : TILE_WIDE ;
    int tileHigh  = big ? BIG_TILE_HIGH  : TILE_HIGH ;
    int tileBytes = big ? BIG_TILE_BYTES : TILE_BYTES;
    int tileSpan  = high / tileHigh;
    
    if ((wide + 2) * high != r.lengthNoComp) {
      say("Isometric data size did not match: "+r.label);
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
        isometricTilePass(
          bytes, index * tileBytes, r,
          offsetX, offsetY,
          tileWide, tileHigh,
          write, wipe
        );
        index += 1;
        offsetX += tileWide + 2;
      }
      offsetY += tileHigh / 2;
    }
  }
  
  
  static void isometricTilePass(
    Bytes bytes, int offset, ImageRecord r,
    int offX, int offY, int tileWide, int tileHigh,
    boolean write, boolean wipe
  ) {
    BufferedImage store = r.extracted;
    
    for (int y = 0, i = 0; y < tileHigh; y++) {
      int startX = (2 * y) - tileHigh;
      if (startX < 0) { startX *= -1; startX -= 2; }
      
      for (int x = startX; x < tileWide - startX; x++, i += 2) {
        if (write) {
          int ARGB = store.getRGB(offX + x, offY + y);
          ARGBtoBytes(ARGB, bytes, offset + i);
          if (wipe) store.setRGB(offX + x, offY + y, 0);
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
    
    int stored = 1 << 15;
    stored |= ((ARGB & MASK_R8) >> 9) & MASK_R5;
    stored |= ((ARGB & MASK_G8) >> 6) & MASK_G5;
    stored |= ((ARGB & MASK_B8) >> 3) & MASK_B5;
    
    store.data[offset    ] = (byte) (stored >> 0);
    store.data[offset + 1] = (byte) (stored >> 8);
    store.used = offset + 2;
  }
  
  
  
  /**  Dealing with the extracted images:
    */
  static void displayImage(final BufferedImage image) {
    displayImage(image, 50, 50);
  }
  
  
  static void displayImage(final BufferedImage image, int atX, int atY) {
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
    
    frame.setLocation(atX, atY);
    frame.setVisible(true);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }
  
  
  static void saveImage(BufferedImage image, String outPath) {
    try {
      File outputFile = new File(outPath);
      ImageIO.write(image, "png", outputFile);
    }
    catch (Exception e) {
      say("Could not save: "+outPath+", problem: "+e);
      return;
    }
  }
  
}










