//
//  SG_Utils_555.cpp
//  SG_tools_cpp
//
//  Created by Morgan Allen on 11/11/2017.
//  Copyright © 2017 Morgan Allen. All rights reserved.
//
#include "SG_Utils_555.hpp"
#include "SG_Handler.hpp"
#include <SDL.h>
#include <iostream>
#include <fstream>

using namespace std;



//  Check these references:
//  https://wiki.libsdl.org/SDL_CreateRGBSurfaceFrom

const int TYPE_ISOMETRIC = 30;
bool packVerbose = false;



Bytes* initBytes(int capacity) {
    Bytes* b = new Bytes();
    b->data     = new byte[capacity];
    b->capacity = capacity;
    b->used     = 0;
    return b;
}

void say(string out) {
    cout << out;
}



const int
    BITS_5  = (1 << 5) - 1,
    MASK_R5 = BITS_5 << 10,
    MASK_G5 = BITS_5 << 5 ,
    MASK_B5 = BITS_5 << 0 ,
    BITS_8  = 0xff,
    MASK_R8 = BITS_8 << 16,
    MASK_G8 = BITS_8 << 8 ,
    MASK_B8 = BITS_8 << 0 
;

//  TODO:  Also, remember to un/lock the surface to ensure safety when reading
//  or writing:
//  https://wiki.libsdl.org/SDL_Surface

uint getRGB(SDL_Surface* img, int x, int y) {
    byte* pixel = (byte*) img->pixels;
    pixel += (y * img->pitch) + (x * 4);
    uint val = 0;
    val |= pixel[0] << 0 ;
    val |= pixel[1] << 8 ;
    val |= pixel[2] << 16;
    val |= pixel[3] << 24;
    return val;
}

void setRGB(SDL_Surface* img, int x, int y, int val) {
    byte* pixel = (byte*) img->pixels;
    pixel += (y * img->pitch) + (x * 4);
    *((uint*) pixel) = val;
}

int bytesToARGB(Bytes* bytes, int offset) {
    if (offset + 1 >= bytes->capacity) return 0;
    
    byte* raw = bytes->data;
    int color = raw[offset] | (raw[offset + 1] << 8);
    int ARGB  = 0xff000000;
    ARGB |= (color & MASK_R5) << 9;
    ARGB |= (color & MASK_G5) << 6;
    ARGB |= (color & MASK_B5) << 3;
    
    return ARGB;
}

void ARGBtoBytes(int ARGB, Bytes* store, int offset) {
    if (offset + 1 >= store->capacity) return;
    
    int stored = 1 << 15;
    stored |= ((ARGB & MASK_R8) >> 9) & MASK_R5;
    stored |= ((ARGB & MASK_G8) >> 6) & MASK_G5;
    stored |= ((ARGB & MASK_B8) >> 3) & MASK_B5;
    
    store->data[offset    ] = (byte) (stored >> 0);
    store->data[offset + 1] = (byte) (stored >> 8);
    store->used = offset + 2;
}




void plainImagePass(Bytes* bytes, ImageRecord* r, bool write) {
    SDL_Surface* store = r->imageData;
    for (int x, y = 0, i = 0; y < r->height; y++) {
        for (x = 0; x < r->width; x++, i += 2) {
            if (write) {
                int ARGB = getRGB(store, x, y);
                ARGBtoBytes(ARGB, bytes, i);
            }
            else {
                int ARGB = bytesToARGB(bytes, i);
                setRGB(store, x, y, ARGB);
            }
        }
    }
}



void readTransparentImage(
    Bytes* bytes, int offset, ImageRecord* r, int length
) {
    bool report = packVerbose;
    if (report) say("\nReading transparent image pixels...");
    
    SDL_Surface* store = r->imageData;
    int maxRead = offset + length;
    if (maxRead > bytes->used) maxRead = bytes->used;
    
    int i = offset;
    int x = 0, y = 0, j;
    int width = r->width;
    
    while (i < maxRead) {
        int c = bytes->data[i++];
        if (c == 255) {
            //  The next byte is the number of pixels to skip
            int skip = bytes->data[i++];
            //if (report) say("    Gap  x"+skip+", index: "+(i - 2));
            
            x += skip;
            while (x >= width) {
                x -= width;
                y++;
            }
        }
        else {
            //  `c' is the number of image data bytes
            //if (report) say("    Fill x"+c+", index: "+(i - 1));
            
            for (j = 0; j < c && i < maxRead; j++, i += 2) {
                int ARGB = bytesToARGB(bytes, i);
                
                setRGB(store, x, y, ARGB);
                x++;
                while (x >= width) {
                    x -= width;
                    y++;
                }
            }
        }
    }
}

void writeTransparentImage(
    ImageRecord* r, Bytes* store, int offset, int maxY
) {
    bool report = packVerbose;
    if (report) say("\nWriting transparent image bytes...");
    
    SDL_Surface* img = r->imageData;
    int high = img->h, wide = img->w;
    int maxX = wide - 1;
    
    const int MAX_GAP = 255, MAX_FILL = 16;
    int index = offset;
    int fillBuffer[MAX_FILL];
    int fillCount = 0, gapCount = 0;
    
    for (int y = 0; y < high; y++) {
        if (maxY > 0 && y + 1 == maxY) break;
        int pixel = 0, next = getRGB(img, 0, y);
        
        for (int x = 0; x < wide; x++) {
            bool atEnd = x == maxX;
            pixel = next;
            next  = atEnd ? -1 : getRGB(img, x + 1, y);
            bool empty     = (pixel & 0xff000000) == 0;
            bool nextEmpty = (next  & 0xff000000) == 0;
            
            if (empty) {
                gapCount++;
                
                if (gapCount == MAX_GAP || atEnd || ! nextEmpty) {
                    /*
                    if (report) {
                        say("    Encoding gap of length "+gapCount+"/"+MAX_GAP);
                        say("      X: "+x+"/"+wide+"  Y: "+y+"/"+high+" index: "+index);
                    }
                    //*/
                    store->data[index++] = (byte) 0xff;
                    store->data[index++] = (byte) gapCount;
                    store->used = index;
                    gapCount = 0;
                }
            }
            else {
                fillBuffer[fillCount++] = pixel;
                
                if (fillCount == MAX_FILL || atEnd || nextEmpty) {
                    /*
                    if (report) {
                        say("\n    Pixel-fill ended with size "+fillCount+"/"+MAX_FILL);
                        say("      X: "+x+"/"+wide+"  Y: "+y+"/"+high+" index: "+index);
                    }
                    //*/
                    store->data[index++] = (byte) fillCount;
                    for (int i = 0; i < fillCount; i++) {
                        ARGBtoBytes(fillBuffer[i], store, index);
                        index += 2;
                    }
                    fillCount = 0;
                }
            }
        }
    }
}




const int
    TILE_WIDE      = 58  ,
    TILE_HIGH      = 30  ,
    TILE_BYTES     = 1800,
    BIG_TILE_WIDE  = 78  ,
    BIG_TILE_HIGH  = 40  ,
    BIG_TILE_BYTES = 3200
;

int isometricFringeHeight(ImageRecord* r) {
    SDL_Surface* store = r->imageData;
    int wide     = store->w;
    int halfHigh = (wide + 2) / 4;
    return store->h - halfHigh;
}

void isometricTilePass(
    Bytes* bytes, int offset, ImageRecord* r,
    int offX, int offY, int tileWide, int tileHigh,
    bool write, bool wipe
) {
    SDL_Surface* store = r->imageData;
    
    for (int y = 0, i = 0; y < tileHigh; y++) {
        int startX = (2 * y) - tileHigh;
        if (startX < 0) { startX *= -1; startX -= 2; }
        
        for (int x = startX; x < tileWide - startX; x++, i += 2) {
            if (write) {
                int ARGB = getRGB(store, offX + x, offY + y);
                ARGBtoBytes(ARGB, bytes, offset + i);
                if (wipe) setRGB(store, offX + x, offY + y, 0);
            }
            else {
                int ARGB = bytesToARGB(bytes, offset + i);
                setRGB(store, offX + x, offY + y, ARGB);
            }
        }
    }
}

static void isometricBasePass(
    Bytes* bytes, ImageRecord* r, bool write, bool wipe
) {
    SDL_Surface* store = r->imageData;
    int wide      = store->w;
    int high      = (wide + 2) / 2;
    //  TODO:  FIX THIS
    bool big      = false;//r->file.version == VERSION_EM;
    int tileWide  = big ? BIG_TILE_WIDE  : TILE_WIDE ;
    int tileHigh  = big ? BIG_TILE_HIGH  : TILE_HIGH ;
    int tileBytes = big ? BIG_TILE_BYTES : TILE_BYTES;
    int tileSpan  = high / tileHigh;
    
    if ((wide + 2) * high != r->lengthNoComp) {
        say("Isometric data size did not match: "+r->label);
        return;
    }
    
    int maxY    = (tileSpan * 2) - 1;
    int offsetY = store->h - high;
    int offsetX = 0;
    int index   = 0;
    
    for (int y = 0; y < maxY; y++) {
        bool lower = y < tileSpan;
        int nextY = y + 1;
        int maxX  = lower ? nextY : ((tileSpan * 2) - nextY);
        offsetX   = lower ? (tileSpan - nextY) : (nextY - tileSpan);
        offsetX   *= tileHigh;
        
        for (int x = 0; x < maxX; x++) {
            isometricTilePass(
                bytes, index * tileBytes, r,
                offsetX, offsetY,
                tileWide, tileHigh,
                write, wipe
            );
            index += 1;
            offsetX += tileWide + 2;
        }
        offsetY += tileHigh / 2;
    }
}



Bytes* extractRawBytes(ImageRecord* record, string filepath) {
    //  TODO:  Derive this correctly.
    ///string filename = "";
    ifstream input;
    input.open(filepath);
    
    //
    //  First, ensure that the record is accessible and allocate space for
    //  extracting the data:
    //  TODO:  In later versions of this format, there may be extra
    //  transparency pixels.  Check this.
    int dataLength = record->dataLength;
    Bytes* bytes = initBytes(dataLength);
    input.seekg(record->offset - (record->externalData != 0 ? 1 : 0));
    input.read((char*) bytes->data, dataLength);
    bytes->used = dataLength;
    
    input.close();
    return bytes;
}


SDL_Surface* imageFromBytes(Bytes* bytes, ImageRecord* record) {
    //
    //  Allocate the image first:
    SDL_Surface* image = record->imageData = SDL_CreateRGBSurface(
        0, record->width, record->height, 32, 0, 0, 0, 0
    );
    //
    //  Isometric images are actually stitched together from both a
    //  transparent upper and a diagonally-packed lower half, so they need
    //  special treatment:
    if (record->imageType == TYPE_ISOMETRIC) {
        isometricBasePass(bytes, record, false, false);
        int done = record->lengthNoComp;
        readTransparentImage(bytes, done, record, record->dataLength - done);
    }
    //
    //  Compression is used for any images with transparency, which means any
    //  other walker-sprites in practice:
    else if (record->compressed != 0) {
        readTransparentImage(bytes, 0, record, record->dataLength);
    }
    //
    //  And finally, plain images are stored without compression:
    else {
        plainImagePass(bytes, record, false);
    }
    return image;
}


SDL_Surface* imageFromRecord(ImageRecord* record, string filename) {
    Bytes* rawData = extractRawBytes(record, filename);
    return imageFromBytes(rawData, record);
}




Bytes* bytesFromImage(ImageRecord* record, SDL_Surface* image) {
    //
    //  If it hasn't been set already, store the image now:
    record->imageData = image;
    //
    //  We allocate a buffer with some (hopefully) excess capacity for storing
    //  the compressed image-
    int capacity = record->width * record->height * 4;
    Bytes* store = initBytes(capacity);
    //
    //  Isometric images are actually stitched together from both a
    //  transparent upper and a diagonally-packed lower half, so they need
    //  special treatment:
    if (record->imageType == TYPE_ISOMETRIC) {
        //
        //  We create a copy of the image, since we will need to wipe certain
        //  pixels as we go before creating transparency:
        SDL_Surface* copy = SDL_CreateRGBSurface(
            0, record->width, record->height, 32, 0, 0, 0, 0
        );
        SDL_BlitSurface(record->imageData, NULL, copy, NULL);
        delete(record->imageData);
        record->imageData = copy;
        
        isometricBasePass(store, record, true, true);
        int done = store->used, maxY = isometricFringeHeight(record);
        writeTransparentImage(record, store, done, maxY);
        
        //  TODO:  You'll need to create a new ImageRecord here?
        record->lengthNoComp = done;
        record->dataLength   = store->used;
    }
    //
    //  Compression is used for any images with transparency, which means any
    //  other walker-sprites in practice:
    else if (record->compressed != 0) {
        writeTransparentImage(record, store, 0, -1);
        
        //  TODO:  You'll need to create a new ImageRecord here?
        record->dataLength = store->used;
    }
    //
    //  And finally, plain images are stored without compression:
    else {
        plainImagePass(store, record, true);
    }
    //
    //  Finally, we trim the buffer down to size and return-
    byte* clipped = new byte[store->used];
    memcpy(clipped, store->data, store->used);
    delete[] store->data;
    store->data = clipped;
    return store;
}
















