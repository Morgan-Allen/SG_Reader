package sg_tools;
/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
import java.io.*;
import java.util.ArrayList;



/**  In essence, an easier-going XML file/node, which constructs a hierarchy of
  *  tags, attributes, arguments and content from a given .xml file.
  */
public class XML {
  
  private static boolean
    verbose      = false,
    extraVerbose = false;
  
  void say(String s) {
    System.out.println(s);
  }
  
  
  final static XML NULL_NODE = new XML().compile(0);
  
  
  protected XML
    parent,
    children[];
  protected int
    indexAsChild = -1;
  protected String
    tag,
    content,
    attributes[],
    values[];
  
  public String tag()       { return tag == null ? "" : tag; }
  public String content()   { return content == null ? "" : content; }
  public int numChildren()  { return children.length; }
  public int indexAsChild() { return indexAsChild; }
  public XML child(int n)   { return children[n]; }
  public XML[] children()   { return children; }
  public XML parent()       { return parent; }
  
  
  
  /**  Returns the first child matching the given tag.
    */
  public XML child(String tag) {
    for (XML child : children) if (child.tag.equals(tag)) return child;
    return NULL_NODE;
  }

  /**  Returns an array of all children matching the given tag.
    */
  public XML[] allChildrenMatching(String tag) {
    ArrayList <XML> matches = new ArrayList <XML> ();
    for (XML child : children)
      if (child.tag.equals(tag)) matches.add(child);
    return (matches.size() > 0) ?
      (XML[]) matches.toArray(new XML[0]) :
      new XML[0];
  }
  
  /**  Returns the first child whose named attribute matches the given value.
    */
  public XML matchChildValue(String att, String value) {
    for (XML child : childList)
      if (child.value(att).equals(value))
        return child;
    return NULL_NODE;
  }
  
  
  /**  Returns this node's value for the given attribute (if present- null
    *  otherwise.)
    */
  public String value(String label) {
    for (int n = values.length; n-- > 0;)
      if (attributes[n].equals(label)) return values[n];
    return null;
  }
  
  public boolean getBool(String label) {
    final String val = value(label);
    return (val == null) ? false : Boolean.parseBoolean(val);
  }
  
  public float getFloat(String label) {
    final String val = value(label);
    return (val == null) ? 1 : Float.parseFloat(val);
  }
  
  public int getInt(String label) {
    return (int) getFloat(label);
  }
  
  
  
  /**  Construction methods-
    */
  public static XML node(String tag, Object... args) {
    XML node = new XML();
    node.tag = tag;
    
    for (int i = 0; i < args.length;) {
      String att = args[i++].toString();
      String val = args[i++].toString();
      node.set(att, val);
    }
    return node;
  }
  
  public void setTag(String tag) {
    this.tag = tag;
  }
  
  public void setContent(String content) {
    this.content = content;
  }
  
  public void set(String att, String value) {
    attributeList.add(att  );
    valueList    .add(value);
  }
  
  public void addChild(XML node) {
    childList.add(node);
  }
  
  
  public static void writeXML(XML root, String outFile) {
    root.compile(0);
    try {
      final File baseFile = new File(outFile);
      final FileWriter FW = new FileWriter(baseFile);
      root.writeToFile(FW, "");
      FW.flush();
      FW.close();
    }
    catch (Exception e) {}
  }
  
  
  public void writeToFile(FileWriter writer, String indent) throws Exception {
    compile(0);
    
    StringBuffer out = new StringBuffer();
    out.append("\n"+indent+"<"+tag);
    
    int maxAttLen = -1;
    for (String att : attributes) {
      if (att.length() > maxAttLen) maxAttLen = att.length();
    }
    for (int i = 0; i < attributes.length; i++) {
      String att = attributes[i], val = values[i];
      out.append("\n"+indent+"  "+att);
      int pad = 1 + maxAttLen - att.length();
      while (pad-- > 0) out.append(' ');
      out.append("= \""+val+"\"");
    }
    
    out.append("\n"+indent+">\n");
    
    if (content != null) {
      out.append(indent+"  ");
      out.append(content);
    }
    writer.write(out.toString());
    
    for (XML child : children) {
      child.writeToFile(writer, indent+"  ");
    }
    
    out.delete(0, out.length());
    boolean anyKids = content != null || children.length > 0;
    if (anyKids) out.append("\n");
    out.append(indent+"</"+tag+">");
    writer.write(out.toString());
    
  }
  
  
  
  /**  Returns the XML node constructed from the file with the given name.
    */
  public static XML load(String fileName) {
    final XML xml = new XML(fileName);
    return xml;
  }
  
  
  //  Temporary member lists, to be discarded once setup is complete-
  private ArrayList <XML>
    childList = new ArrayList <XML> ();
  private ArrayList <String>
    attributeList = new ArrayList <String> (),
    valueList     = new ArrayList <String> ();
  
  /**  Constructs a new XML node from the given text file.
   */
  private XML(String xmlF) {
    final boolean report = extraVerbose;
    if (report) say("\nLoading XML from path: "+xmlF);
    
    try {
      XML current = this;
      boolean
        readsTag = false,  //reading an opening tag.
        readsAtt = false,  //reading a tag or attribute name.
        readsVal = false,  //reading an attribute value.
        readsCon = false;  //reading content between open and closing tags.
      int
        cRead = 0,  //index for  start of content reading.
        aRead = 0,  //attribute reading...
        vRead = 0,  //value reading...
        index,      //current index in file.
        length;     //total length of file.
      
      final File baseFile = new File(xmlF);
      final FileInputStream fR = new FileInputStream(baseFile);
      byte chars[] = new byte[length = (int) baseFile.length()];
      char read;
      fR.read(chars);
      
      for (index = 0; index < length; index++) {
        read = (char) chars[index];
        if (Character.isWhitespace((char) read)) read = ' ';
        if (report) say(" "+read);
        //
        //  If you're reading a tag or value:
        if (readsTag) {
          //
          //  If you're reading an attribute value:
          if (readsVal) {
            if (read == '"') {
              String value = readS(chars, vRead, index);
              current.valueList.add(value);
              if (report) say("  Adding value: "+value);
              readsVal = false;
            }
            continue;
          }
          //  If you're reading an attribute or name tag:
          if (readsAtt) {
            switch(read) {
              case('='):
              case('>'):
              case(' '):
                if (current.tag == null) {
                  current.tag = readS(chars, aRead, index);
                  if (report) say("  Setting tag: "+current.tag);
                }
                else {
                  String attribute = readS(chars, aRead, index);
                  current.attributeList.add(attribute);
                  if (report) say(
                    "  Adding attribute: "+attribute+
                    " (No. "+current.attributeList.size()+")"
                  );
                }
                readsAtt = false;
                break;
            }
            if (readsAtt) continue;
          }
          //  Otherwise:
          switch(read) {
            
            case('"'):
              readsVal = true;
              vRead = index + 1;
              break;
            
            case('>'):
              if (chars[index - 1] == '/') {
                //this is a closed tag, so the xml block ends here.
                readsTag = false;
                if (report) say(
                  "  Closed tag ("+current.tag+"). Going back to parent-"
                );
                current = current.parent;
              }
              readsCon = readsTag;
              if (readsCon) cRead = index + 1;
              //this was an opening tag, so new content should be read.
              readsTag = false;
              break;
            
            case('='):
            case('/'):
            case(' '):
              //ignore these characters.
              break;
            
            default:
              //anything else would begin an attribute in a tag.
              //say("\nopening tag: ");
              readsAtt = true;
              aRead = index;
              break;
          }
          
          if (readsTag) continue;
        }
        
        if (read == '<') {
          //  An opening/closing tag begins...
          readsTag = true;
          
          if (readsCon) {
            if (report) say("  Adding content.");
            current.content = readS(chars, cRead, index);
            readsCon = false;
          }
          
          if (chars[index + 1] == '/') {
            //...this is a closing tag, so the xml block ends here.
            readsTag = false;
            if (report)  say(
              "  End xml block ("+current.tag+"). Going back to parent-"
            );
            current = current.parent;
          }
          else {
            if (report) say("  New xml block:");
            //a new xml block starts here.
            readsVal = readsAtt = false;
            XML xml = new XML();
            xml.indexAsChild = current.childList.size();
            xml.parent = current;
            current.childList.add(xml);
            current = xml;
          }
        }
      }
      if (report) say("\n___xxxBEGIN XML COMPILATIONxxx___");
      compile(0);
      fR.close();
      if (report) say("___xxxEND OF XML COMPILATIONxxx___\n");
    }
    catch(IOException e) {
      say("" + e);
    }
  }
  
  //  Simple helper method for reading a String between start and end indices:
  final private String readS(final byte chars[], final int s, final int e) {
    return new String(chars, s, e - s);
  }
	
  
  private XML() {}
  
  
  
  /**  Transforms the temporary member lists into proper arrays.
    */
  final private XML compile(int depth) {
    if (children != null) return this;
    
    children   = childList    .toArray(new XML   [0]);
    attributes = attributeList.toArray(new String[0]);
    values     = valueList    .toArray(new String[0]);
    
    if (children == null) {
      children = new XML[0];
    }
    if ((attributes == null) || (values == null)) {
      attributes = values = new String[0];
    }
    if (verbose) {
      final byte iB[] = new byte[depth * 2];
      for (int n = depth * 2; n-- > 0;) iB[n] = ' ';
      final String iS = new String(iB);
      
      say("\n"+iS+"Node Tag = \""+tag+"\"");
      for (int n = 0; n < values.length; n++) {
        say(iS+"  "+attributes[n]+" = \""+values[n]+"\"");
      }
      if (children.length == 0) say(iS+"  Content: "+content);
      else say(iS+"  Children: ");
    }
    for (XML child : children) child.compile(depth + 1);
    return this;
  }
  
  
  
  /**  Simple testing routine-
    */
  public static void main(String args[]) {

    XML testX = XML.node("text");
    testX.set("type", "info");
    testX.set("format", "latin_alphabet");
    testX.setContent("Here's some sample text!");
    
    XML kid = XML.node("passage");
    kid.set("format", "numeric");
    kid.setContent("1010101010001111");
    testX.addChild(kid);
    
    XML.writeXML(testX, "test_xml.xml");
  }
  
}









