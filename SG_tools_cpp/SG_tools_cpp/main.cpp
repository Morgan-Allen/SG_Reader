//
//  main.cpp
//  SG_tools_cpp
//
//  Created by Morgan Allen on 09/11/2017.
//  Copyright © 2017 Morgan Allen. All rights reserved.
//

#include <iostream>
#include "SG_Handler.hpp"

#include <dirent.h>
#include <limits.h>




int main(int numArgs, const char *args[]) {
    
    char buf[PATH_MAX + 1];
    realpath(".", buf);
    cout << "\nWORKING DIRECTORY: " << buf;
    
    string basePath = "/Users/morganallen/Desktop/Programming/SG Tools Project/";
    string filename = basePath + "Caesar 3/C3_North.sg2";
    
    cout << "\nREADING FILES IN BASE DIRECTORY...";
    DIR* dirp = opendir(basePath.c_str());
    dirent *dp;
    while ((dp = readdir(dirp)) != NULL) {
        int len = dp->d_namlen;
        string name(dp->d_name, len);
        cout << "\n  " << name;
    }
    closedir(dirp);
    
    
    
    File_SG* file = readFile(filename);
    uint filesize   = file->header.filesize;
    uint version    = file->header.version;
    uint numRecords = file->header.numRecords;
    
    cout << "\nFinished reading file: " << filename;
    cout << "\n  File size:     " << filesize  ;
    cout << "\n  File version:  " << version   ;
    cout << "\n  Total records: " << numRecords;
    cout << "\n\n";
    
    return 0;
}


