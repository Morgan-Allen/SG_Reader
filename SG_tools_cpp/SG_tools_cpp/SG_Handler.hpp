//
//  SG_Handler.hpp
//  SG_tools_cpp
//
//  Created by Morgan Allen on 09/11/2017.
//  Copyright © 2017 Morgan Allen. All rights reserved.
//
#ifndef sg_handler
#define sg_handler
#include <stdio.h>
#include <vector>
#include <string>
#include <SDL.h>
using namespace std;



typedef unsigned int   uint  ;
typedef unsigned char  byte  ;
typedef unsigned short ushort;


const int
    VERSION_C3 = 0,
    VERSION_PH = 1,
    VERSION_ZE = 2,
    VERSION_EM = 3
;
const int
    SG_HEADER_SIZE = 80 ,
    SG_INDEX_SIZE  = 600,
    SG_NUM_BITMAPS = 100,
    SG_BITMAP_SIZE = 200,
    SG_OPENING_SIZE = (
        SG_HEADER_SIZE + SG_INDEX_SIZE +
        (SG_BITMAP_SIZE * SG_NUM_BITMAPS)
    ),
    SG_RECORD_SIZE = 64
;

struct SG_Header;
struct Bitmap;
struct ImageRecord;
struct File_SG;
struct File_555;


struct SG_Header {
    
    uint filesize;
    uint version;
    uint unknown1;
    uint numRecords;
    uint recordsUsed;
    uint unknown2;
    uint totalFilesize;
    uint inner555Size;
    uint outer555Size;
    uint unknown3[11];
    
    ushort index[300];
    
    File_SG* file;
};


struct Bitmap {
    
    byte nameChars   [65];
    byte commentChars[51];
    uint width;
    uint height;
    uint numImages;
    uint startIndex;
    uint endIndex;
    byte unknown[64];
    
    vector <ImageRecord*> records;
    File_SG* file;
    string name;
    string comment;
    string namePaired555;
};


struct ImageRecord {
    
    uint offset;
    uint dataLength;
    uint lengthNoComp;
    uint unknown1;
    uint inverseOffset;
    
    ushort width;
    ushort height;
    byte unknown2[6];
    ushort numAnims;
    ushort unknown3;
    ushort spriteOffX;
    ushort spriteOffY;
    byte unknown4[10];
    
    byte canReverse;
    byte unknown5;
    byte imageType;
    byte compressed;
    byte externalData;
    byte partCompressed;
    byte unknown6[2];
    byte bitmapID;
    byte unknown7;
    byte animSpeedID;
    byte unknown8[5];
    
    File_SG* file;
    Bitmap* belongs;
    int offsetInsideSG;
    string label;
    
    File_555* file555;
    SDL_Surface *imageData;
};


struct File_SG {
    SG_Header header;
    Bitmap bitmaps[SG_NUM_BITMAPS];
    vector <ImageRecord*> records;
    vector <File_555*> refers;
    
    string filename;
    string fullpath;
    string namePaired555;
};


struct File_555 {
    vector <ImageRecord*> records;
    vector <File_SG*> referredBy;
    
    string filename;
    string fullpath;
};



ImageRecord* recordWithID(string label, File_SG* file);

File_SG* lookupFile_SG(string basePath, string filename, bool report);

void writeFile_SG(File_SG* file, string filepath, bool report);

void copyAndModifyFile_SG(File_SG* file, vector <ImageRecord*> changed, string outputDir);


#endif













