/*This source code copyrighted by Lazy Foo' Productions (2004-2015)
and may not be redistributed without written permission.*/

//Using SDL and standard IO
#include <SDL.h>
#include <stdio.h>
#include <iostream>
#include <string>

using namespace std;



int displayImage(SDL_Surface* image) {
    
    if (image == NULL) {
        printf("Image to display was not supplied!");
        return 1;
    }
    
    
    // Create an application window with the following settings:
    SDL_Init(SDL_INIT_VIDEO);
    SDL_Window* window = SDL_CreateWindow(
        "Image",                           // window title
        SDL_WINDOWPOS_UNDEFINED,           // initial x position
        SDL_WINDOWPOS_UNDEFINED,           // initial y position
        image->w,                          // width, in pixels
        image->h,                          // height, in pixels
        SDL_WINDOW_OPENGL                  // flags - see below
    );
    
    // Check that the window was successfully created
    if (window == NULL) {
        printf("Could not create window...");
        return 1;
    }
    
    
    SDL_Surface* screen = SDL_GetWindowSurface(window);
    
    if (screen == NULL) {
        printf("Could not initialise screen...");
        return 1;
    }
    
    // A basic main loop to prevent blocking
    bool is_running = true;
    SDL_Event event;
    while (is_running) {
        
        SDL_BlitSurface(image, NULL, screen, NULL);
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







