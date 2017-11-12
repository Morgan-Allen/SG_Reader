//
//  main.cpp
//  SG_tools_cpp
//
//  Created by Morgan Allen on 09/11/2017.
//  Copyright © 2017 Morgan Allen. All rights reserved.
//

#include <iostream>
#include "SG_Handler.hpp"
#include "SG_Utils_555.hpp"
#include "graphics_test.hpp"

#include <dirent.h>
#include <limits.h>




int main(int numArgs, const char *args[]) {
    
    string basePath = "/Users/morganallen/Desktop/Programming/SG Tools Project/";
    string filename = basePath + "Caesar 3/C3.sg2";
    string name555  = basePath + "Caesar 3/C3.555";
    
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
    
    File_SG* file = readFile(filename, false);
    
    //  TODO:  Get those automated tests working again.
    
    string testFiles[] = {
        "empire_panels.bmp_3",
        "Carts.bmp_692",
        "Govt.bmp_0",
        "Govt.bmp_9",
        "Housng1a.bmp_42",
        "Housng1a.bmp_47"
    };
    
    for (string recordID : testFiles) {
        ImageRecord *record1 = recordWithID(recordID, file);
        SDL_Surface *image = imageFromRecord(record1, name555);
        display_image(image);
        SDL_FreeSurface(image);
    }
    //  TODO:  Consider passing in a vector of images at once and displaying
    //  them all on the same pane.
    
    delete file;
    
    return 0;
}










