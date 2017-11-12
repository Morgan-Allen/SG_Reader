/*This source code copyrighted by Lazy Foo' Productions (2004-2015)
and may not be redistributed without written permission.*/

//Using SDL and standard IO

#include "graphics_test.hpp"
#include <SDL.h>
#include <stdio.h>
#include <iostream>
#include <string>

using namespace std;



int displayImages(vector <SDL_Surface*> images) {
    
    int maxWide = 640, maxLineHigh = 0;
    int totWide = 0, totHigh = 0, x = 0, y = 0;
    vector <SDL_Rect> imgRects;
    
    for (SDL_Surface* image : images) {
        SDL_Rect rect;
        rect.x = x;
        rect.y = y;
        rect.w = image->w;
        rect.h = image->h;
        imgRects.push_back(rect);
        maxLineHigh = fmax(maxLineHigh, rect.h);
        
        x += rect.w;
        totWide = fmax(totWide, x + rect.w);
        totHigh = fmax(totHigh, y + rect.h);
        
        if (x + rect.w > maxWide) {
            x = 0;
            y += maxLineHigh;
            maxLineHigh = 0;
        }
    }
    
    
    // Create an application window with the following settings:
    SDL_Init(SDL_INIT_VIDEO);
    SDL_Window* window = SDL_CreateWindow(
        "Images",                          // window title
        SDL_WINDOWPOS_UNDEFINED,           // initial x position
        SDL_WINDOWPOS_UNDEFINED,           // initial y position
        totWide,                           // width, in pixels
        totHigh,                           // height, in pixels
        SDL_WINDOW_OPENGL                  // flags - see below
    );
    
    // Check that the window was successfully created
    if (window == NULL) {
        printf("Could not create window...");
        return 1;
    }
    
    
    SDL_Surface* screen = SDL_GetWindowSurface(window);
    SDL_FillRect(screen, NULL, 0x00000000);
    
    if (screen == NULL) {
        printf("Could not initialise screen...");
        return 1;
    }
    
    // A basic main loop to prevent blocking
    bool is_running = true;
    SDL_Event event;
    while (is_running) {
        
        for (int i = 0; i < images.size(); i++) {
            SDL_BlitSurface(images[i], NULL, screen, &imgRects[i]);
        }
        
        SDL_UpdateWindowSurface(window);
        
        while (SDL_PollEvent(&event)) {
            if (event.type == SDL_QUIT) {
                is_running = false;
            }
        }
        SDL_Delay(16);
    }
    
    //  Perform cleanup:
    SDL_DestroyWindow(window);
    SDL_Quit();
    
    return 0;
}



int displayImage(SDL_Surface* image) {
    
    if (image == NULL) {
        printf("Image to display was not supplied!");
        return 1;
    }
    
    vector <SDL_Surface*> images;
    images.push_back(image);
    return displayImages(images);
}









