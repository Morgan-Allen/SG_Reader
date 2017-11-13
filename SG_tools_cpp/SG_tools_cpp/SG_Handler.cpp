//
//  SG_Handler.cpp
//  SG_tools_cpp
//
//  Created by Morgan Allen on 09/11/2017.
//  Copyright © 2017 Morgan Allen. All rights reserved.
//

#include "SG_Handler.hpp"
#include "SG_Utils_555.hpp"
#include <iostream>
#include <fstream>
#include <sstream>



vector <File_SG *> allFile_SGs ;
vector <File_555*> allFile_555s;


bool fileExists(string path) {
    ifstream input;
    input.open(path, ifstream::ate);
    int filesize = (int) input.tellg();
    bool good = input.good() && filesize > 0;
    input.close();
    return good;
}

string noSuffix(string filename) {
    size_t dotIndex = filename.find(".");
    if (dotIndex == string::npos) return filename;
    return filename.substr(0, dotIndex);
}

string nameOf555For(string filename, string basePath) {
    string name = noSuffix(filename) + ".555";
    if (! fileExists(basePath + name)) {
        name = "555/" + name;
        if (! fileExists(basePath + name)) {
            name = "";
        }
    }
    return name;
}

File_555* lookup555For(ImageRecord* record, string basePath) {
    string name = record->externalData ?
        record->belongs->namePaired555 :
        record->file   ->namePaired555
    ;
    if (name.size() == 0) return NULL;
    
    for (File_555* file : allFile_555s) {
        if (file->filename.compare(name) == 0) return file;
    }
    
    File_555* file = new File_555();
    file->filename = name;
    file->fullpath = basePath + name;
    
    allFile_555s.push_back(file);
    return file;
}

void recordReference(File_SG* fileSG, File_555* file555) {
    
    bool match555 = false;
    for (File_555* f : fileSG->refers) if (f == file555) match555 = true;
    if (! match555) {
        fileSG->refers.push_back(file555);
    }
    
    bool matchSG  = false;
    for (File_SG* f : file555->referredBy) if (f == fileSG) matchSG = true;
    if (! matchSG) {
        file555->referredBy.push_back(fileSG);
    }
}

ImageRecord* recordWithID(string label, File_SG* file) {
    for (int i = 0; i < file->header.numRecords; i++) {
        ImageRecord* r = file->records[i];
        if (r->label.compare(label) == 0) return r;
    }
    return NULL;
}



File_SG* lookupFile_SG(string basePath, string filename, bool report) {
    
    string fullpath = basePath + filename;
    for (File_SG* f : allFile_SGs) if (f->fullpath.compare(fullpath) == 0) {
        return f;
    }
    
    if (! fileExists(fullpath)) return NULL;
    
    File_SG* file = new File_SG();
    file->filename = filename;
    file->fullpath = basePath + filename;
    file->namePaired555 = nameOf555For(filename, basePath);
    allFile_SGs.push_back(file);
    
    ifstream input;
    input.open(basePath + filename);
    input.seekg(0);
    
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
    
    header->file = file;
    
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
        
        map->file    = file;
        map->name    = string((char*) map->nameChars   );
        map->comment = string((char*) map->commentChars);
        map->namePaired555 = nameOf555For(map->name, basePath);
    }
    
    if (report) {
        cout << "\n\nReading image records...";
    }
    
    int numRecords = header->numRecords;
    for (int i = 0; i < numRecords; i++) {
        ImageRecord *record = new ImageRecord();
        int offsetInsideSG = (int) input.tellg();
        
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
        record->belongs = belongs;
        record->file    = file;
        File_555* file555 = lookup555For(record, basePath);
        record->file555 = file555;
        
        if (file555 != NULL) {
            file555->records.push_back(record);
            recordReference(file, file555);
        }
        
        int recordID = (int) belongs->records.size();
        stringstream label;
        label << string((char*) &(belongs->nameChars));
        label << "_" << recordID;
        
        record->label          = label.str();
        record->offsetInsideSG = offsetInsideSG;
        belongs->records.push_back(record);
        file   ->records.push_back(record);
        
        if (report) {
            cout << "\n  " << record->label;
        }
    }
    
    if (report) {
        cout << "\n  End of file? " << input.eof();
        cout << "\n";
    }
    
    input.close();
    return file;
}


void writeImageRecord(ImageRecord* record, ofstream *os) {
    
    (*os).write((char*) &(record->offset       ), 4);
    (*os).write((char*) &(record->dataLength   ), 4);
    (*os).write((char*) &(record->lengthNoComp ), 4);
    (*os).write((char*) &(record->unknown1     ), 4);
    (*os).write((char*) &(record->inverseOffset), 4);
    
    (*os).write((char*) &(record->width     ), 2);
    (*os).write((char*) &(record->height    ), 2);
    (*os).write((char*) &(record->unknown2  ), 6 * 1);
    (*os).write((char*) &(record->numAnims  ), 2);
    (*os).write((char*) &(record->unknown3  ), 2);
    (*os).write((char*) &(record->spriteOffX), 2);
    (*os).write((char*) &(record->spriteOffY), 2);
    (*os).write((char*) &(record->unknown4  ), 10 * 1);
    
    (*os).write((char*) &(record->canReverse    ), 1);
    (*os).write((char*) &(record->unknown5      ), 1);
    (*os).write((char*) &(record->imageType     ), 1);
    (*os).write((char*) &(record->compressed    ), 1);
    (*os).write((char*) &(record->externalData  ), 1);
    (*os).write((char*) &(record->partCompressed), 1);
    (*os).write((char*) &(record->unknown6      ), 2 * 1);
    (*os).write((char*) &(record->bitmapID      ), 1);
    (*os).write((char*) &(record->unknown7      ), 1);
    (*os).write((char*) &(record->animSpeedID   ), 1);
    (*os).write((char*) &(record->unknown8      ), 5 * 1);
}


void writeFile_SG(File_SG* file, string filepath, bool report) {
    
    ofstream output;
    output.open(filepath);
    
    SG_Header* header = &(file->header);
    
    output.write((char*) &(header->filesize     ), 4);
    output.write((char*) &(header->version      ), 4);
    output.write((char*) &(header->unknown1     ), 4);
    output.write((char*) &(header->numRecords   ), 4);
    output.write((char*) &(header->recordsUsed  ), 4);
    output.write((char*) &(header->unknown2     ), 4);
    output.write((char*) &(header->totalFilesize), 4);
    output.write((char*) &(header->inner555Size ), 4);
    output.write((char*) &(header->outer555Size ), 4);
    output.write((char*) &(header->unknown3     ), 11  * 4);
    output.write((char*) &(header->index        ), 300 * 2);
    
    for (int i = 0; i < SG_NUM_BITMAPS; i++) {
        Bitmap *map = &(file->bitmaps[i]);
        output.write((char*) &(map->nameChars   ), 65 * 1);
        output.write((char*) &(map->commentChars), 51 * 1);
        output.write((char*) &(map->width       ), 4);
        output.write((char*) &(map->height      ), 4);
        output.write((char*) &(map->numImages   ), 4);
        output.write((char*) &(map->startIndex  ), 4);
        output.write((char*) &(map->endIndex    ), 4);
        output.write((char*) &(map->unknown     ), 64 * 1);
    }
    
    int numRecords = header->numRecords;
    for (int i = 0; i < numRecords; i++) {
        ImageRecord* record = file->records[i];
        writeImageRecord(record, &output);
    }
    
    output.close();
}



void copyAndModifyFile_SG(
    File_SG* file, vector <ImageRecord*> changed, string outputDir
) {
    printf("\n\n  Updating %s", file->filename.c_str());
    
    //
    //  First, open the source file and copy it's contents to memory:
    ifstream input;
    input.open(file->fullpath, ifstream::ate);
    int totalBytes = (int) input.tellg();
    Bytes* copied = initBytes(totalBytes);
    input.seekg(0);
    input.read((char*) copied->data, totalBytes);
    input.close();
    
    //
    //  Then, open the destination file and write those contents out:
    string newPath = outputDir+file->filename;
    ofstream output;
    output.open(newPath);
    output.write((char*) copied->data, totalBytes);
    deleteBytes(copied);
    
    //
    //  Then, look for any image-records within the file that ought to be
    //  modified, and overwrite their bytes:
    int changedHere = 0;
    
    for (ImageRecord* record : changed) {
        if (record->file != file) continue;
        output.seekp(record->offsetInsideSG);
        writeImageRecord(record, &output);
        changedHere += 1;
    }
    
    int numRecords = file->header.numRecords;
    printf("\n  Total records updated: %i/%i", changedHere, numRecords);
    
    output.close();
}






