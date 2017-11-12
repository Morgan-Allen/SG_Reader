//
//  graphics_test.hpp
//  SG_tools_cpp
//
//  Created by Morgan Allen on 11/11/2017.
//  Copyright © 2017 Morgan Allen. All rights reserved.
//
#ifndef graphics_test
#define graphics_test
#include <SDL.h>
#include <vector>
using namespace std;


int displayImage(SDL_Surface* image);
int displayImages(vector <SDL_Surface*> images);

#endif