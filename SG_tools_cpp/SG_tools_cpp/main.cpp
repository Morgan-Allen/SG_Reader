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
#include "Testing.hpp"
#include "graphics_test.hpp"

#include <dirent.h>
#include <limits.h>




int main(int numArgs, const char *args[]) {
    
    string rootPath = "/Users/morganallen/Desktop/Programming/";
    string basePath = rootPath + "SG Tools Project/Caesar 3/";
    string outPath  = rootPath + "SG Tools Project/output_test/";
    
    
    //*
    //testFileIO(basePath, outPath, "C3.sg2", VERSION_C3, false);
    
    vector <string> testFiles = {
        "empire_panels.bmp_3",
        "Carts.bmp_692",
        "Govt.bmp_0",
        "Govt.bmp_9",
        "Housng1a.bmp_42",
        "Housng1a.bmp_47"
    };
    testImagePacking(
        basePath, outPath, "C3.sg2", "C3.555", VERSION_C3,
        testFiles, false
    );
    //*/
    
    
    /*
    File_SG* file = readFile_SG(basePath + "C3.sg2", false);
    string path555 = basePath + "C3.555";
    
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
        SDL_Surface *image = imageFromRecord(record1, path555);
        displayImage(image);
        SDL_FreeSurface(image);
    }
    //  TODO:  Consider passing in a vector of images at once and displaying
    //  them all on the same pane.
    
    delete file;
    //*/
    
    return 0;
}




