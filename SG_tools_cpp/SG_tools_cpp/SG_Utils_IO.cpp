//
//  SG_Utils_IO.cpp
//  SG_tools_cpp
//
//  Created by Morgan Allen on 12/11/2017.
//  Copyright © 2017 Morgan Allen. All rights reserved.
//

#include "SG_Utils_IO.hpp"
#include <fstream>



/*
 char buf[PATH_MAX + 1];
 realpath(".", buf);
 cout << "\nWORKING DIRECTORY: " << buf;
 
 cout << "\nREADING FILES IN BASE DIRECTORY...";
 DIR* dirp = opendir(basePath.c_str());
 dirent *dp;
 while ((dp = readdir(dirp)) != NULL) {
 int len = dp->d_namlen;
 string name(dp->d_name, len);
 cout << "\n  " << name;
 }
 closedir(dirp);
//*/



bool replaceImageBytes(
    int offset, int originalLength, byte newBytes[], int newLength,
    File_555* file555, string basePath, string outPath
) {
    printf("\n\nReplacing image bytes...");
    printf(
        "\n  Will replace bytes between %i and %i",
        offset, (offset + originalLength)
    );
    
    string path555 = basePath + file555->filename;
    
    ifstream input;
    input.open(path555, ifstream:: ate);
    int totalLength = (int) input.tellg();
    int belowLength = offset;
    int aboveLength = totalLength - (offset + originalLength);
    
    //
    //  First, grab any bytes that come before the segment being replaced:
    Bytes *belowBytes = initBytes(belowLength);
    input.seekg(0);
    input.read((char*) belowBytes->data, belowLength);
    
    //
    //  Then grab any bytes that come after:
    Bytes* aboveBytes = initBytes(aboveLength);
    input.seekg(offset + originalLength);
    input.read((char*) aboveBytes->data, aboveLength);
    
    input.close();
    
    //
    //  Then we create a new file that basically sandwiches the new data
    //  between these two segments.
    ofstream output;
    output.open(outPath + file555->filename);
    output.write((char*) belowBytes->data, belowLength);
    output.write((char*) newBytes        , newLength  );
    output.write((char*) aboveBytes->data, aboveLength);
    
    deleteBytes(belowBytes);
    deleteBytes(aboveBytes);
    output.close();
    
    //
    //  Having done this, we need to update the image-record in question, and
    //  any subsequent image-records within the 555 file.
    int bytesDiff = newLength - originalLength;
    vector <ImageRecord*> changed;
    vector <File_SG*> toUpdate;
    
    for (ImageRecord* record : file555->records) {
        if (record->offset == offset) {
            record->dataLength = newLength;
            changed.push_back(record);
        }
        if (record->offset > offset) {
            record->offset += bytesDiff;
            changed.push_back(record);
        }
    }
    for (ImageRecord* record : changed) {
        bool newFile = true;
        for (File_SG* f : toUpdate) if (f == record->file) newFile = false;
        if (newFile) toUpdate.push_back(record->file);
    }
    
    printf("\n  Records modified: %i", (int) changed .size());
    printf("\n  Files to update:  %i", (int) toUpdate.size());
    
    //
    //  Then we need to modify the associated entries in any SG files-
    for (File_SG* updated : toUpdate) {
        copyAndModifyFile_SG(updated, changed, outPath);
    }
    
    return true;
}



void replaceSingleImage(
    string basePath, string fileSG, int version,
    string recordID, string newBMPpath, string outputDir
) {
    //printf("\nReplacing image record: "+recordID+" in "+basePath+fileSG);
    
    File_SG* file = lookupFile_SG(basePath, fileSG, false);
    
    ImageRecord* record = recordWithID(recordID, file);
    if (record == NULL) return;
    
    int offset = record->offset, length = record->dataLength;
    if (record->externalData != 0) offset -= 1;
    
    SDL_Surface *image = SDL_LoadBMP(newBMPpath.c_str());
    if (image == NULL) return;
    
    Bytes* asBytes = bytesFromImage(record, image);
    replaceImageBytes(
        offset, length, asBytes->data, asBytes->used,
        record->file555, basePath, outputDir
    );
}






















