//
//  SG_Utils_555.hpp
//  SG_tools_cpp
//
//  Created by Morgan Allen on 11/11/2017.
//  Copyright © 2017 Morgan Allen. All rights reserved.
//

#ifndef sg_utils
#define sg_utils
#include <stdio.h>
#include <SDL.h>
#include "SG_handler.hpp"



struct Bytes {
    byte* data;
    int used = -1, capacity = -1;
};

Bytes* initBytes(int capacity);


Bytes* extractRawBytes(ImageRecord* record, string filename);
SDL_Surface* imageFromBytes(Bytes* bytes, ImageRecord* record);
SDL_Surface* imageFromRecord(ImageRecord* record, string filename);

Bytes* bytesFromImage(ImageRecord* record, SDL_Surface* image);

int bytesToARGB(Bytes* bytes, int offset);
void ARGBtoBytes(int ARGB, Bytes* store, int offset);


#endif