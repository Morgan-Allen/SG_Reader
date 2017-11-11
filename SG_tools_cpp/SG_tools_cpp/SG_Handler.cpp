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



File_SG* readFile(string filename) {
    File_SG* file = new File_SG();
    
    ifstream input;
    input.open(filename);
    
    int filesize = (int) input.tellg();
    bool good = input.good();
    cout << "\nREAD FILE SIZE: " << filesize;
    cout << "\n  EXISTS? " << (good ? "true" : "false");
    
    SG_Header *header = &(file->header);
    
    input.read((char*) &(header->filesize  ), 4);
    input.read((char*) &(header->version   ), 4);
    input.read((char*) &(header->unknown1  ), 4);
    input.read((char*) &(header->numRecords), 4);
    
    /*
    input >> header->filesize     ;
    input >> header->version      ;
    input >> header->unknown1     ;
    input >> header->numRecords   ;
    input >> header->recordsUsed  ;
    input >> header->unknown2     ;
    input >> header->totalFilesize;
    input >> header->inner555Size ;
    input >> header->outer555Size ;
    input.read((char*) &(header->unknown3), 11  * 4);
    input.read((char*) &(header->index   ), 300 * 2);
    //*/
    
    cout << "\n  End of file? " << input.eof();
    
    cout << "\n";
    
    input.close();
    return file;
}





