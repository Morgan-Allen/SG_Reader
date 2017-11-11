//
//  SG_Handler.cpp
//  SG_tools_cpp
//
//  Created by Morgan Allen on 09/11/2017.
//  Copyright © 2017 Morgan Allen. All rights reserved.
//

#include "SG_Handler.hpp"
#include <string>
#include <iostream>
#include <fstream>





File_SG* readFile(string filename, bool report) {
    
    
    File_SG* file = new File_SG();
    
    ifstream input;
    input.open(filename);
    
    int filesize = (int) input.tellg();
    bool good = input.good();
    
    if (report) {
        cout << "\nREAD FILE SIZE: " << filesize;
        cout << "\n  EXISTS? " << (good ? "true" : "false");
    }
    
    if (! good) return NULL;
    
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
    
    input.read((char*) &(header->unknown3), 11  * 4);
    input.read((char*) &(header->index   ), 300 * 2);
    
    if (report) {
        cout << "\nFinished reading file header: " << filename;
        cout << "\n  File size:     " << header->filesize  ;
        cout << "\n  File version:  " << header->version   ;
        cout << "\n  Total records: " << header->numRecords;
        cout << "\n\n";
    }
    
    for (int i = 0; i < SG_NUM_BITMAPS; i++) {
        Bitmap *map = &(file->bitmaps[i]);
        input.read((char*) &(map->nameChars   ), 65 * 1);
        input.read((char*) &(map->commentChars), 51 * 1);
        input.read((char*) &(map->width     ), 4);
        input.read((char*) &(map->height    ), 4);
        input.read((char*) &(map->numImages ), 4);
        input.read((char*) &(map->startIndex), 4);
        input.read((char*) &(map->endIndex  ), 4);
        input.read((char*) &(map->unknown), 64 * 1);
        
        if (report) {
            cout << "\n\nRead bitmap: " << map->nameChars;
            cout << "\n  Comment: " << map->commentChars;
            cout << "\n  Width:   " << map->width;
            cout << "\n  Height:  " << map->height;
            cout << "\n  Images:  " << map->numImages;
        }
    }
    
    
    
    cout << "\n  End of file? " << input.eof();
    
    cout << "\n";
    
    input.close();
    return file;
}








