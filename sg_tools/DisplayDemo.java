

package sg_tools;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;



public class DisplayDemo {

  
  final static int
    TYPE_TERRAIN  = 0,
    TYPE_BUILDING = 1,
    TYPE_WALKER   = 2,
    TILE_WIDE = 60,
    TILE_HIGH = 30;
  
  
  static class Sprite {
    
    int type = TYPE_BUILDING;
    Image images[];
    float animProgress;
    
    int size;
    float x, y;
    Point path[];
    float walkProgress;
    int facing;
    
    int sx, sy, depth;
    
    
    static Image[] loadImages(String path, String... IDs) {
      Image images[] = new Image[IDs.length];
      for (int i = IDs.length; i-- > 0;) try {
        images[i] = ImageIO.read(new File(path+IDs[i]));
      }
      catch (IOException e) {}
      return images;
    }
    
    static Sprite with(
      int size, String path, String... IDs
    ) {
      Sprite s = new Sprite();
      s.size   = size;
      s.images = loadImages(path, IDs);
      return s;
    }
    
    static Sprite with(
      int size, String path, int minID, int maxID, String suffix,
      int... excepted
    ) {
      ArrayList <String> IDs = new ArrayList();
      loop: for (int i = minID; i < maxID; i++) {
        for (int e : excepted) if (e == i) continue loop;
        IDs.add(i+suffix);
      }
      return with(size, path, IDs.toArray(new String[0]));
    }
  }
  
  
  static class Stage {
    
    ArrayList <Sprite> terrain = new ArrayList();
    ArrayList <Sprite> sprites = new ArrayList();
    int viewX = 0, viewY = 0;
    
    Sprite addTerrain(Sprite s, int x, int y) {
      s.x = x;
      s.y = y;
      terrain.add(s);
      return s;
    }
    
    Sprite addSprite(Sprite s, int x, int y) {
      s.x = x;
      s.y = y;
      sprites.add(s);
      return s;
    }
    
    
    void updateWalkers() {
      for (Sprite s : sprites) if (s.type == TYPE_WALKER) {
        
        float pathLen = 0, segProg = -1;
        Point l = s.path[0], n = l;
        
        for (int i = 1; i < s.path.length; i++) {
          float dist = (float) s.path[i].distance(s.path[i - 1]);
          pathLen += dist;
          if (pathLen > s.walkProgress && segProg == -1) {
            l = s.path[i - 1];
            n = s.path[i    ];
            if (dist == 0) segProg = 0;
            else segProg = 1 - ((pathLen - s.walkProgress) / dist);
          }
        }
        if (pathLen < 1) pathLen = 1;
        
        float x = l.x, y = l.y;
        x += (float) ((n.x - l.x) * segProg);
        y += (float) ((n.y - l.y) * segProg);
        s.x = x;
        s.y = y;
        
        float angle = (float) Math.atan2(n.x - l.x, n.y - l.y);
        angle = (float) Math.toDegrees(angle) * 8f / 360;
        if (angle < 0) angle += 8;
        s.facing = (int) angle;
        
        s.walkProgress += 0.03f;
        while (s.walkProgress >= pathLen) s.walkProgress -= pathLen;
        
        s.animProgress += 0.05f;
        while(s.animProgress >= 1) s.animProgress--;
      }
    }
    
    
    void translatePoint(
      float sx, float sy, int wide, int high, Point store
    ) {
      int x = 0, y = 0;
      
      //  X is going down and right, Y is going up and right:
      sx -= viewX;
      sy -= viewY;
      x += sx * TILE_WIDE / 2;
      x += sy * TILE_WIDE / 2;
      y += sy * TILE_HIGH / 2;
      y -= sx * TILE_HIGH / 2;
      x += wide / 2;
      y += high / 2;
      
      y = high - (1 + y);
      store.setLocation(x, y);
    }
    
    
    void renderTo(Graphics2D g, int wide, int high) {
      Point store = new Point();
      
      for (Sprite s : terrain) {
        Image img = s.images[0];
        translatePoint(s.x, s.y, wide, high, store);
        store.y -= TILE_HIGH * 0.5f;
        g.drawImage(img, store.x, store.y, null);
      }
      
      for (Sprite s : sprites) {
        Image img = s.images[0];
        translatePoint(s.x, s.y, wide, high, store);
        int depth = store.y;
        
        int baseW = img.getWidth(null);
        store.y -= img.getHeight(null);
        store.y += TILE_HIGH * 0.5f * baseW / TILE_WIDE;
        
        if (s.type == TYPE_WALKER) {
          store.x += (TILE_WIDE - baseW) / 2;
          store.y += TILE_HIGH * 0.2f * baseW / TILE_WIDE;
        }
        
        s.sx    = store.x;
        s.sy    = store.y;
        s.depth = depth  ;
      }
      
      Collections.sort(sprites, new Comparator <Sprite> () {
        public int compare(Sprite a, Sprite b) {
          if (a == b) return 0;
          return a.depth > b.depth ? 1 : -1;
        }
      });
      
      for (Sprite s : sprites) {
        int numFrames = s.images.length / 8;
        int index = 8 * (int) (s.animProgress * numFrames);
        index += s.facing;
        
        Image img = s.images[index];
        if (s.type == TYPE_WALKER) {
          g.drawImage(img, s.sx + 5, s.sy + 10, 15, 20, null);
        }
        else {
          g.drawImage(img, s.sx, s.sy, null);
        }
      }
      
      translatePoint(0, 0, wide, high, store);
      g.setColor(Color.RED);
      g.drawRect(store.x, store.y, 2, 2);
    }
  }
  
  
  static void setupRendering(
    final Stage stage, int x, int y, int wide, int high
  ) {
    final JFrame frame = new JFrame();
    final JPanel pane = new JPanel() {
      final static long serialVersionUID = 0;
      
      public void paintComponent(Graphics g) {
        stage.updateWalkers();
        stage.renderTo((Graphics2D) g, getWidth(), getHeight());
      }
    };
    frame.add(pane);
    
    if (wide < 100) wide = 100;
    if (high < 100) high = 100;
    frame.getContentPane().setPreferredSize(new Dimension(wide, high));
    frame.pack();
    
    frame.setLocation(x, y);
    frame.setVisible(true);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    
    final Timer timer = new Timer(40, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        pane.repaint();
      }
    });
    timer.start();
  }
  
  
  public static void main(String args[]) {
    Stage stage = new Stage();
    
    Sprite ground = Sprite.with(
      1, "output_sg_north/Land1a/Land1a_",
      61, 119, ".png"
    );
    Sprite trees = Sprite.with(
      1, "output_sg_north/Land1a/Land1a_",
      9, 16, ".png", 12, 13
    );
    Sprite roads = Sprite.with(
      1, "output_sg_north/Land2a/Land2a_",
      43, 62, ".png"
    );
    int roadIDs[][] = {
      { 0, 0, 0, 0, 0, 0, 0, 0, 0 },
      { 0, 5, 1, 1, 1, 6, 0, 0, 0 },
      { 0, 2, 0, 0, 0, 2, 0, 0, 0 },
      { 0, 2, 0, 0, 0, 2, 0, 0, 0 },
      { 0, 2, 0, 0, 0, 2, 0, 0, 0 },
      { 0, 8, 1, 1, 1, 7, 0, 0, 0 },
      { 0, 0, 0, 0, 0, 0, 0, 0, 0 },
      { 0, 0, 0, 0, 0, 0, 0, 0, 0 },
      { 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    };
    
    for (int x = 9, index; x-- > 0;) for (int y = 9; y-- > 0;) {
      
      int roadID = roadIDs[x][y];
      if (roadID > 0) {
        Sprite r = new Sprite();
        r.size = 1;
        r.type = TYPE_TERRAIN;
        r.images = new Image[] { roads.images[roadID - 1] };
        stage.addTerrain(r, x, y);
        continue;
      }
      
      Sprite g = new Sprite();
      g.size = 1;
      g.type = TYPE_TERRAIN;
      index = (int) (Math.random() * ground.images.length);
      g.images = new Image[] { ground.images[index] };
      stage.addTerrain(g, x, y);
      
      if (x > 0 && x < 8 && y > 0 && y < 8) continue;
      if (Math.random() > 0.5f) continue;
      
      Sprite t = new Sprite();
      t.size = 1;
      t.type = TYPE_TERRAIN;
      index = (int) (Math.random() * trees.images.length);
      t.images = new Image[] { trees.images[index] };
      stage.addSprite(t, x, y);
    }
    
    stage.addSprite(Sprite.with(
      2, "output_sg_north/Housng1a/Housng1a_",
      "32.png"
    ), 1, 6);
    stage.addSprite(Sprite.with(
      2, "output_sg_north/Housng1a/Housng1a_",
      "33.png"
    ), 3, 6);
    stage.addSprite(Sprite.with(
      2, "output_sg_north/Housng1a/Housng1a_",
      "36.png"
    ), 6, 1);
    stage.addSprite(Sprite.with(
      3, "output_sg_north/Security/Security_",
      "45.png"
    ), 2, 2);
    stage.viewX = 4;
    stage.viewY = 4;
    
    Sprite walker = stage.addSprite(Sprite.with(
      1, "output_sg_north/Citizen01/Citizen01_",
      0, 96, ".png"
    ), 1, 1);
    walker.type = TYPE_WALKER;
    walker.path = new Point[] {
      new Point(1, 1),
      new Point(1, 5),
      new Point(5, 5),
      new Point(5, 1),
      new Point(1, 1)
    };
    
    Dimension screenS = Toolkit.getDefaultToolkit().getScreenSize();
    int w = screenS.width, h = screenS.height;
    setupRendering(stage, w / 4, h / 4, w / 2, h / 2);
  }
  
}


