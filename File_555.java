

import java.io.*;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import javax.imageio.*;



public class File_555 {
  
  
  final static int
    TYPE_ISOMETRIC = 30
  ;
  
  
  static BufferedImage extractImage(
    File_SG.ImageRecord record,
    RandomAccessFile fileAccess
  ) {
    //
    //  TODO:  Allow for loading from alternative 555 files!
    if (record.externalData) return null;
    if (record.width == 0 || record.height == 0) return null;
    
    try {
      BufferedImage image = record.extracted = new BufferedImage(
        record.width, record.height,
        BufferedImage.TYPE_INT_ARGB
      );
      //
      //  TODO:  In the case of later versions of this format, there may be
      //  extra transparency pixels.  Investigate.
      byte rawData[] = new byte[record.dataLength];
      fileAccess.seek(record.offset);
      fileAccess.read(rawData);
      //File_SG.say("\nBytes read: "+bytesRead+"/"+record.dataLength);
      //
      //  Isometric images are actually stitched together from both a
      //  transparent upper and a diagonally-packed lower half, so they need
      //  special treatment:
      if (record.imageType == TYPE_ISOMETRIC) {
        readIsometricImage(rawData, record);
      }
      //
      //  Compression is used for any images with transparency, which means any
      //  other walker-sprites in practice:
      else if (record.compressed) {
        readSpriteImage(rawData, record);
      }
      //
      //  And finally, plain images are stored without compression:
      else {
        readPlainImage(rawData, record);
      }
      return image;
    }
    catch (IOException e) {}
    return null;
  }
  
  
  static void readIsometricImage(byte rawData[], File_SG.ImageRecord r) {
    readIsometricBase(rawData, r);
    int done = r.lengthNoComp;
    readTransparentImage(rawData, done, r, r.dataLength - done);
  }
  
  
  static void readSpriteImage(byte rawData[], File_SG.ImageRecord r) {
    readTransparentImage(rawData, 0, r, r.dataLength);
  }
  
  
  
  /**  Utilities for reading/writing plain images-
    */
  static void readPlainImage(byte rawData[], File_SG.ImageRecord r) {
    processPlainImage(rawData, r, false);
  }
  
  
  static void writePlainImage(File_SG.ImageRecord r, byte storeData[]) {
    processPlainImage(storeData, r, true);
  }
  
  
  static void processPlainImage(
    byte rawData[], File_SG.ImageRecord r, boolean write
  ) {
    BufferedImage store = r.extracted;
    for (int x, y = 0, i = 0; y < store.getHeight(); y++) {
      for (x = 0; x < store.getWidth(); x++, i += 2) {
        if (write) {
          int ARGB = store.getRGB(x, y);
          ARGBtoBytes(ARGB, rawData, i);
        }
        else {
          int ARGB = bytesToARGB(rawData, i);
          store.setRGB(x, y, ARGB);
        }
      }
    }
  }
  
  
  
  /**  Utilities for reading/writing transparency-
    */
  static void readTransparentImage(
    byte rawData[], int offset, File_SG.ImageRecord r, int length
  ) {
    BufferedImage store = r.extracted;
    int i = offset;
    int x = 0, y = 0, j;
    int width = store.getWidth();
    
    while (i < offset + length) {
      int c = rawData[i++] & 0xff;
      if (c == 255) {
        //  The next byte is the number of pixels to skip
        x += rawData[i++] & 0xff;
        while (x >= width) {
          x -= width;
          y++;
        }
      }
      else {
        //  `c' is the number of image data bytes
        for (j = 0; j < c; j++, i += 2) {
          int ARGB = bytesToARGB(rawData, i);
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
    File_SG.ImageRecord r, int length, byte storeData[], int offset
  ) {
    //  TODO:  You'll need a variable-length byte-buffer instead of an array,
    //  if a fresh image of variable size is being encoded.
    
    //  TODO:  You'll also need to omit any pixels accounted for by an
    //  isometric base!
    
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
              storeData[fillStartIndex] = (byte) gapSize;
            }
            gapSize = 0;
            inGap = true;
          }
          gapSize += 1;
        }
        else {
          if (inGap) {
            inGap = false;
            storeData[index++] = (byte) 0xff;
            storeData[index++] = (byte) gapSize;
            fillStartIndex = index++;
            gapSize = 0;
          }
          ARGBtoBytes(pix, storeData, index);
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
  
  static void readIsometricBase(byte rawData[], File_SG.ImageRecord r) {
    processIsometricBase(rawData, r, false);
  }
  
  static void writeIsometricBase(File_SG.ImageRecord r, byte storeData[]) {
    processIsometricBase(storeData, r, true);
  }
  
  static void processIsometricBase(
    byte rawData[], File_SG.ImageRecord r, boolean write
  ) {
    
    BufferedImage store = r.extracted;
    int wide      = store.getWidth();
    int high      = (wide + 2) / 2;
    boolean big   = r.file.version == File_SG.VERSION_EM;
    int tileWide  = big ? BIG_TILE_WIDE  : TILE_WIDE ;
    int tileHigh  = big ? BIG_TILE_HIGH  : TILE_HIGH ;
    int tileBytes = big ? BIG_TILE_BYTES : TILE_BYTES;
    int tileSpan  = high / tileHigh;
    
    if ((wide + 2) * high != r.lengthNoComp) {
      File_SG.say("Isometric data size did not match: "+r.label);
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
          rawData, index * tileBytes, r,
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
    byte rawData[], int offset, File_SG.ImageRecord r,
    int offX, int offY, int tileWide, int tileHigh, boolean write
  ) {
    BufferedImage store = r.extracted;
    
    for (int y = 0, i = 0; y < tileHigh; y++) {
      int startX = (2 * y) - tileHigh;
      if (startX < 0) { startX *= -1; startX -= 2; }
      
      for (int x = startX; x < tileWide - startX; x++, i += 2) {
        if (write) {
          int ARGB = store.getRGB(offX + x, offY + y);
          ARGBtoBytes(ARGB, rawData, offset + i);
        }
        else {
          int ARGB = bytesToARGB(rawData, offset + i);
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
  
  
  static int bytesToARGB(byte rawData[], int offset) {
    if (offset + 1 >= rawData.length) return 0;
    
    int color = (rawData[offset] & 0xff) | ((rawData[offset + 1] & 0xff) << 8);
    int RGBA  = 0xff000000;
    
    RGBA |= (color & MASK_R5) << (6 + 3);
    RGBA |= (color & MASK_G5) << (3 + 3);
    RGBA |= (color & MASK_B5) << (0 + 3);
    
    return RGBA;
  }
  
  
  static void ARGBtoBytes(int ARGB, byte storeData[], int offset) {
    
    int stored = 0;
    stored |= ((ARGB & MASK_R8) >> 6) & MASK_R5;
    stored |= ((ARGB & MASK_G8) >> 3) & MASK_G5;
    stored |= ((ARGB & MASK_B8) >> 0) & MASK_B5;
    
    storeData[offset    ] = (byte) ((stored >> 0) & 0xff);
    storeData[offset + 1] = (byte) ((stored >> 8) & 0xff);
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
      File_SG.say("Could not save: "+outPath+", problem: "+e);
      return;
    }
  }
  
}










