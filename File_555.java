

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
      //
      //  Isometric images are actually stitched together from both a
      //  transparent upper and a diagonally-packed lower half, so they need
      //  special treatment:
      if (record.imageType == TYPE_ISOMETRIC) {
        processIsometricImage(rawData, record);
      }
      //
      //  Compression is used for any images with transparency, which means any
      //  other walker-sprites in practice:
      else if (record.compressed) {
        processSpriteImage(rawData, record);
      }
      //
      //  And finally, plain images are stored without compression:
      else {
        processPlainImage(rawData, record);
      }
      return image;
    }
    catch (IOException e) {}
    return null;
  }
  
  
  static void processIsometricImage(byte rawData[], File_SG.ImageRecord r) {
    processIsometricBase(rawData, r);
    int done = r.lengthNoComp;
    processTransparentImage(rawData, done, r, r.dataLength - done);
  }
  
  
  static void processSpriteImage(byte rawData[], File_SG.ImageRecord r) {
    processTransparentImage(rawData, 0, r, r.dataLength);
  }
  
  
  static void processPlainImage(byte rawData[], File_SG.ImageRecord r) {
    BufferedImage store = r.extracted;
    for (int x, y = 0, i = 0; y < store.getHeight(); y++) {
      for (x = 0; x < store.getWidth(); x++, i += 2) {
        int ARGB = bytesToARGB(rawData, i);
        store.setRGB(x, y, ARGB);
      }
    }
  }
  
  
  static void processTransparentImage(
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
  
  
  final static int
    TILE_WIDE      = 58  ,
    TILE_HIGH      = 30  ,
    TILE_BYTES     = 1800,
    BIG_TILE_WIDE  = 78  ,
    BIG_TILE_HIGH  = 40  ,
    BIG_TILE_BYTES = 3200
  ;
  
  static void processIsometricBase(byte rawData[], File_SG.ImageRecord r) {
    
    BufferedImage store = r.extracted;
    int wide        = store.getWidth();
    int high        = (wide + 2) / 2;
    boolean bigTile = (high % BIG_TILE_HIGH) == 0;
    int tileWide    = bigTile ? BIG_TILE_WIDE  : TILE_WIDE ;
    int tileHigh    = bigTile ? BIG_TILE_HIGH  : TILE_HIGH ;
    int tileBytes   = bigTile ? BIG_TILE_BYTES : TILE_BYTES;
    int tilesAcross = high / tileHigh;
    
    if ((wide + 2) * high != r.lengthNoComp) {
      //  TODO:  Raise the alarm!
      return;
    }

    int maxY    = (tilesAcross * 2) - 1;
    int offsetY = store.getHeight() - high;
    int offsetX = 0;
    int index   = 0;
    
    for (int y = 0; y < maxY; y++) {
      boolean lower = y < tilesAcross;
      
      int maxX = lower ? (y + 1) : (tilesAcross * 2) - (y + 1);
      offsetX  = lower ? (tilesAcross - (y + 1)) : ((y + 1) - tilesAcross);
      offsetX  *= tileHigh;
      
      for (int x = 0; x < maxX; x++) {
        processIsometricTile(
          rawData, index * tileBytes, r,
          offsetX, offsetY,
          tileWide, tileHigh
        );
        index += 1;
        offsetX += tileWide + 2;
      }
      offsetY += tileHigh / 2;
    }
  }
  
  
  static void processIsometricTile(
    byte rawData[], int offset, File_SG.ImageRecord r,
    int offX, int offY, int tileWide, int tileHigh
  ) {
    BufferedImage store = r.extracted;
    int half_height = tileHigh / 2;
    int x, y, i = 0;
    
    for (y = 0; y < half_height; y++) {
      int start = tileHigh - (2 * (y + 1));
      int end   = tileWide - start;
      for (x = start; x < end; x++, i += 2) {
        int ARGB = bytesToARGB(rawData, offset + i);
        store.setRGB(offX + x, offY + y, ARGB);
      }
    }
    for (y = half_height; y < tileHigh; y++) {
      int start = (2 * y) - tileHigh;
      int end   = tileWide - start;
      for (x = start; x < end; x++, i += 2) {
        int ARGB = bytesToARGB(rawData, offset + i);
        store.setRGB(offX + x, offY + y, ARGB);
      }
    }
  }
  
  
  static int bytesToARGB(byte rawData[], int offset) {
    if (offset + 1 >= rawData.length) return 0;
    
    int color = (rawData[offset] & 0xff) | ((rawData[offset + 1] & 0xff) << 8);
    int RGBA  = 0xff000000;
    
    // Red: bits 11-15, should go to bits 17-24
    RGBA |= ((color & 0x7c00) << 9) | ((color & 0x7000) << 4);
    
    // Green: bits 6-10, should go to bits 9-16
    RGBA |= ((color & 0x3e0) << 6) | ((color & 0x300));
    
    // Blue: bits 1-5, should go to bits 1-8
    RGBA |= ((color & 0x1f) << 3) | ((color & 0x1c) >> 2);
    
    return RGBA;
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










