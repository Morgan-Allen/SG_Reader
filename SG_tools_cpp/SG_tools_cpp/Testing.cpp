//
//  tests_class.cpp
//  SG_tools_cpp
//
//  Created by Morgan Allen on 12/11/2017.
//  Copyright © 2017 Morgan Allen. All rights reserved.
//

#include "Testing.hpp"
#include "graphics_test.hpp"
#include <iostream>
#include <fstream>
#include <math.h>
#include <cstdlib>
#include <sstream>





bool checkPackingSame(Bytes* in, Bytes* out) {
    bool allOK = true;
    
    if (in->used != out->used) {
        printf("\n  Different lengths: %i -> %i", in->used, out->used);
        allOK = false;
    }
    
    int maxIndex = fmin(in->used, out->used);
    for (int i = 0; i <  maxIndex; i++) {
        int VI = in ->data[i] & 0xff;
        int VO = out->data[i] & 0xff;
        if (VI != VO) {
            printf("\n  Differ at index %i: %i -> %i", i, VI, VO);
            allOK = false;
            break;
        }
    }
    return allOK;
}


bool checkFilesSame(string basePath, string outputDir, string filename) {
    
    ifstream original, rewrite;
    original.open(basePath  + filename);
    rewrite .open(outputDir + filename);
    
    Bytes* bytesO = initBytes((int) original.tellg());
    Bytes* bytesR = initBytes((int) rewrite .tellg());
    
    original.read((char*) bytesO->data, bytesO->capacity);
    rewrite .read((char*) bytesR->data, bytesR->capacity);
    
    original.close();
    rewrite .close();
    
    return checkPackingSame(bytesO, bytesR);
}


const char* descARGB(uint value) {
    stringstream out;
    out << "a " << ((value >> 24) & 0xff);
    out << "r " << ((value >> 16) & 0xff);
    out << "g " << ((value >> 8 ) & 0xff);
    out << "b " << ((value >> 0 ) & 0xff);
    return out.str().c_str();
}


bool checkImagesSame(SDL_Surface* in, SDL_Surface* out) {
    bool allOK = true, pixOK = true;
    
    if (in->w != out->w) {
        printf("\nDifferent width: %i -> %i", in->w, out->w);
        allOK = false;
    }
    if (in->h != out->h) {
        printf("\nDifferent height: %i -> %i", in->h, out->h);
        allOK = false;
    }
    int maxWide = fmin(in->w, out->w);
    int maxHigh = fmin(in->h, out->h);
    
    for (int y = 0; y < maxHigh && pixOK; y++) {
        for (int x = 0; x < maxWide && pixOK; x++) {
            uint VI = getRGB(in , x, y);
            uint VO = getRGB(out, x, y);
            if (VI != VO) {
                printf("\n  Different at point: %i|%i", x, y);
                printf("\n    In value:  %i, %s", VI, descARGB(VI));
                printf("\n    Out value: %i, %s", VO, descARGB(VO));
                allOK = pixOK = false;
            }
        }
    }
    return allOK;
}



int bitAt(int index, int number) {
    return ((number & (1 << index)) == 0) ? 0 : 1;
}


void saveImage(SDL_Surface* image, string savepath) {
    SDL_SaveBMP(image, savepath.c_str());
}




void testFileIO(
    string basePath, string outputDir, string fileSG, int version, bool report
) {
    printf("\nTesting basic file I/O...");
    
    File_SG* file = readFile_SG(basePath + fileSG, report);
    writeFile_SG(file, outputDir + fileSG, report);
    
    printf("\n  Total records: %i, used: %i", file->header.numRecords, file->header.recordsUsed);
    bool filesSame = checkFilesSame(basePath, outputDir, fileSG);
    if (filesSame) {
        printf("\n  %s identical after I/O.\n", fileSG.c_str());
    }
    else {
        printf("\n  %s not identical after I/O.\n", fileSG.c_str());
    }
}



void testImagePacking(
    string basePath, string outputDir,
    string fileSG, string file555, int version,
    vector <string> testImageIDs, bool report
) {
    //
    //  Have to test basic byte un/packing first (note that the 16th bit of
    //  the short for a pixel is always filled....)
    Bytes* testIn  = initBytes(4);
    Bytes* testOut = initBytes(4);
    for (int i = 4; i-- > 0;) testIn->data[i] = (byte) (rand() * 256);
    testIn->data[1] |= 1 << 7;
    testIn->data[3] |= 1 << 7;
    testIn->used = testOut->used = 4;
    int pixels[] = {
        bytesToARGB(testIn, 0),
        bytesToARGB(testIn, 2)
    };
    ARGBtoBytes(pixels[0], testOut, 0);
    ARGBtoBytes(pixels[1], testOut, 2);
    
    printf("\n\nTesting byte-level conversions...");
    string inB = "  In: ", outB = "  Out:";
    for (int i = 0; i < 32; i++) {
        if (i % 8 == 0) { inB += " "; outB += " "; }
        inB  += bitAt(i % 8, testIn ->data[i / 8] & 0xff);
        outB += bitAt(i % 8, testOut->data[i / 8] & 0xff);
    }
    printf("\n%s", inB .c_str());
    printf("\n%s", outB.c_str());
    
    bool basicOK = checkPackingSame(testIn, testOut);
    if (basicOK) {
        printf("\n  Bytes packed correctly.");
    }
    else {
        printf("\n  Bytes not packed correctly!");
    }
    
    //
    //  Then, we test the full range of test images supplied:
    int dispX = 200;
    printf("\n\nTesting image un/packing...");
    File_SG* file = readFile_SG(basePath + fileSG, report);
    string path555 = basePath + file555;
    vector <SDL_Surface*> allImages;
    
    for (string ID : testImageIDs) {
        
        ImageRecord* record = recordWithID(ID, file);
        if (record == NULL) continue;
        
        printf(
            "\n\n  Image: %s  Size: %i x %i  Bytes: %i",
            ID.c_str(), record->width, record->height, record->dataLength
        );
        
        Bytes* bytesIn = extractRawBytes(record, path555);
        SDL_Surface* loaded = imageFromBytes(bytesIn, record);
        if (loaded == NULL) continue;
        saveImage(loaded, outputDir + ID + "_loaded.bmp");
        
        Bytes* bytesOut = bytesFromImage(record, loaded);
        SDL_Surface* packed = imageFromBytes(bytesOut, record);
        saveImage(packed, outputDir + ID + "_packed.bmp");
        allImages.push_back(packed);
        
        //  TODO:  There's a problem here.  The isometric images actually
        //  create a copy of the original image and de-allocate the old version,
        //  so de-allocating again afterward leads to a crash.
        //SDL_FreeSurface(loaded);
        //SDL_FreeSurface(packed);
        
        printf("\n");
        dispX += 100;
        //*
        bool imgSame = checkImagesSame(loaded, packed);
        if (imgSame) {
            printf("  Displayed images are identical.");
        }
        else {
            printf("  Displayed images do not match.");
        }
        //*/
        
        bool packSame = checkPackingSame(bytesIn, bytesOut);
        if (packSame) {
            printf("\n  Bytes packed identically.");
        }
        else {
            printf("\n  Did not pack bytes identically.");
        }
    }
    
    printf("\n");
    //handler.closeAllFileAccess();
    
    displayImages(allImages);
    
}




/*
static void testImageSubstitution(
    String basePath, String fileSG, int version,
    String recordID, String savePath, String outputDir,
    String... testFilenames
) {
    say("\nTesting file-record substitution...");
    
    BufferedImage extract = unpackSingleImage(
        basePath, fileSG, version, recordID
    );
    saveImage(extract, savePath);
    replaceSingleImage(
        basePath, fileSG, version, recordID, savePath, outputDir
    );
    
    //  Now verify that the SG files in question are identical-
    for (String filename : testFilenames) {
        boolean same = checkFilesSame(basePath, outputDir, filename);
        if (same) {
            say("  "+filename+" identical in output directory.");
        }
        else {
            say("  "+filename+" is not identical.");
        }
    }
}



const string
    C3_DIR_PATH = "C:/Program Files (x86)/GOG Galaxy/Games/Caesar 3/"
;


void runTests() {
    
    testFileIO(C3_DIR_PATH, "output_test/", "C3_North.sg2", VERSION_C3);
    
    final String testImageIDs[] = {
        "empire_panels_3",
        "Carts_692",
        "Govt_0",
        "Govt_9",
        "Housng1a_42",
        "Housng1a_47",
    };
    testImagePacking(
        C3_DIR_PATH, "output_test/", "C3.sg2", VERSION_C3, testImageIDs
    );
    
    testImageSubstitution(
        C3_DIR_PATH, "C3.sg2", VERSION_C3,
        "Housng1a_42", "output_test/temp_house_42.png", "output_test/",
        "C3.sg2",
        "C3.555"
    );
}
//*/













