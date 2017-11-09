//
//  SG_Handler.hpp
//  SG_tools_cpp
//
//  Created by Morgan Allen on 09/11/2017.
//  Copyright © 2017 Morgan Allen. All rights reserved.
//
#include <stdio.h>
#include <vector>
using namespace std;



const int
    VERSION_C3 = 0,
    VERSION_PH = 1,
    VERSION_ZE = 2,
    VERSION_EM = 3
;
const int
    SG_HEADER_SIZE = 80 ,
    SG_BITMAP_SIZE = 200,
    SG_RECORD_SIZE = 64 ,
    SG_INDEX_SIZE  = 600,
    SG_OPENING_SIZE = (
        SG_HEADER_SIZE + SG_INDEX_SIZE +
        (SG_BITMAP_SIZE * 100)
    )
;


struct SG_Header {
    
    int filesize;
    int version;
    int unknown1;
    int numRecords;
    int recordsUsed;
    int unknown2;
    int totalFilesize;
    int inner555Size;
    int outer555Size;
    int unknown3[11];
    
    short index[300];
};


struct Bitmap {
    
    char nameChars   [65];
    char commentChars[51];
    int width;
    int height;
    int numImages;
    int startIndex;
    int endIndex;
    char unknown[64];
};

struct BitmapDigest {
    Bitmap bitmap;
};


struct ImageRecord {
    
    int offset;
    int dataLength;
    int lengthNoComp;
    int unknown1;
    int inverseOffset;
    
    short width;
    short height;
    char unknown2[6];
    short numAnims;
    short unknown3;
    short spriteOffX;
    short spriteOffY;
    char unknown4[10];
    
    char canReverse;
    char unknown5;
    char imageType;
    char compressed;
    char externalData;
    char partCompressed;
    char unknown6[2];
    char bitmapID;
    char unknown7;
    char animSpeedID;
    char unknown8[5];
};

struct ImageRecordDigest {
    ImageRecord record;
};



struct File_SG {
    SG_Header header;
    BitmapDigest bitmaps[100];
    vector <ImageRecordDigest> records;
};



struct File_555 {
    //  ???
};








