//
//  SG_Handler.cpp
//  SG_tools_cpp
//
//  Created by Morgan Allen on 09/11/2017.
//  Copyright © 2017 Morgan Allen. All rights reserved.
//

#include "SG_Handler.hpp"
#include <iostream>
#include <fstream>
#include <sstream>





File_SG* readFile(string filename, bool report) {
    
    ifstream input;
    input.open(filename);
    int filesize = (int) input.tellg();
    bool good = input.good();
    
    if (report) {
        cout << "\nREAD FILE SIZE: " << filesize;
        cout << "\n  EXISTS? " << (good ? "true" : "false");
    }
    
    if (! good) return NULL;
    
    File_SG* file = new File_SG();
    SG_Header *header = &(file->header);
    
    input.read((char*) &(header->filesize     ), 4);
    input.read((char*) &(header->version      ), 4);
    input.read((char*) &(header->unknown1     ), 4);
    input.read((char*) &(header->numRecords   ), 4);
    input.read((char*) &(header->recordsUsed  ), 4);
    input.read((char*) &(header->unknown2     ), 4);
    input.read((char*) &(header->totalFilesize), 4);
    input.read((char*) &(header->inner555Size ), 4);
    input.read((char*) &(header->outer555Size ), 4);
    input.read((char*) &(header->unknown3     ), 11  * 4);
    input.read((char*) &(header->index        ), 300 * 2);
    
    if (report) {
        cout << "\nFinished reading file header: " << filename;
        cout << "\n  File size:     " << header->filesize   ;
        cout << "\n  File version:  " << header->version    ;
        cout << "\n  Total records: " << header->numRecords ;
        cout << "\n  Records used:  " << header->recordsUsed;
        cout << "\n\n";
    }
    
    for (int i = 0; i < SG_NUM_BITMAPS; i++) {
        Bitmap *map = &(file->bitmaps[i]);
        input.read((char*) &(map->nameChars   ), 65 * 1);
        input.read((char*) &(map->commentChars), 51 * 1);
        input.read((char*) &(map->width       ), 4);
        input.read((char*) &(map->height      ), 4);
        input.read((char*) &(map->numImages   ), 4);
        input.read((char*) &(map->startIndex  ), 4);
        input.read((char*) &(map->endIndex    ), 4);
        input.read((char*) &(map->unknown     ), 64 * 1);
        
        if (report) {
            cout << "\n\nRead bitmap: " << map->nameChars;
            cout << "\n  Comment: " << map->commentChars;
            cout << "\n  Width:   " << map->width;
            cout << "\n  Height:  " << map->height;
            cout << "\n  Images:  " << map->numImages;
        }
    }
    
    if (report) {
        cout << "\n\nReading image records...";
    }
    
    int numRecords = header->numRecords;
    for (int i = 0; i < numRecords; i++) {
        ImageRecord *record = new ImageRecord();
        
        input.read((char*) &(record->offset       ), 4);
        input.read((char*) &(record->dataLength   ), 4);
        input.read((char*) &(record->lengthNoComp ), 4);
        input.read((char*) &(record->unknown1     ), 4);
        input.read((char*) &(record->inverseOffset), 4);
        
        input.read((char*) &(record->width     ), 2);
        input.read((char*) &(record->height    ), 2);
        input.read((char*) &(record->unknown2  ), 6 * 1);
        input.read((char*) &(record->numAnims  ), 2);
        input.read((char*) &(record->unknown3  ), 2);
        input.read((char*) &(record->spriteOffX), 2);
        input.read((char*) &(record->spriteOffY), 2);
        input.read((char*) &(record->unknown4  ), 10 * 1);
        
        input.read((char*) &(record->canReverse    ), 1);
        input.read((char*) &(record->unknown5      ), 1);
        input.read((char*) &(record->imageType     ), 1);
        input.read((char*) &(record->compressed    ), 1);
        input.read((char*) &(record->externalData  ), 1);
        input.read((char*) &(record->partCompressed), 1);
        input.read((char*) &(record->unknown6      ), 2 * 1);
        input.read((char*) &(record->bitmapID      ), 1);
        input.read((char*) &(record->unknown7      ), 1);
        input.read((char*) &(record->animSpeedID   ), 1);
        input.read((char*) &(record->unknown8      ), 5 * 1);
        
        
        Bitmap* belongs = &(file->bitmaps[record->bitmapID]);
        
        int recordID = (int) belongs->records.size();
        stringstream label;
        label << string((char*) &(belongs->nameChars));
        label << "_" << recordID;
        record->label = label.str();
        
        if (report) {
            cout << "\n  " << record->label;
        }
        
        belongs->records.push_back(record);
        file->records.push_back(record);
    }
    
    cout << "\n  End of file? " << input.eof();
    cout << "\n";
    
    input.close();
    return file;
}





